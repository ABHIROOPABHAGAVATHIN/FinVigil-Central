"""
FinVigil Central - Tests for Credit Risk RabbitMQ Consumer Worker
Tests message decoding, inference pipeline, publication to 'credit.decision',
acknowledgement handling, and error dead-lettering.
"""

import json
from unittest.mock import MagicMock, patch
import pytest

from app.workers.credit_worker import (
    CreditEventWorker,
    EXCHANGE_NAME,
    DECISION_ROUTING_KEY,
)


@pytest.fixture
def worker():
    return CreditEventWorker(host="mock-rabbit", port=5672)


def test_process_event_payload_prime_borrower(worker):
    payload = {
        "applicationUuid": "app-test-uuid-101",
        "customerUuid": "cust-test-uuid-101",
        "income": 140000.0,
        "employmentYears": 8,
        "loanAmount": 20000.0,
        "existingLoans": 1,
        "creditScore": 770,
        "debtToIncomeRatio": 0.20,
    }

    decision_event = worker.process_event_payload(payload)

    assert decision_event["applicationUuid"] == "app-test-uuid-101"
    assert decision_event["customerUuid"] == "cust-test-uuid-101"
    assert decision_event["decision"] == "APPROVE"
    assert decision_event["riskLevel"] in ["LOW", "MEDIUM"]
    assert 0.0 <= decision_event["riskScore"] <= 1.0
    assert "decisionUuid" in decision_event
    assert "modelVersion" in decision_event
    assert "timestamp" in decision_event


def test_process_event_payload_high_risk_borrower(worker):
    payload = {
        "applicationUuid": "app-test-uuid-999",
        "customerUuid": "cust-test-uuid-999",
        "income": 20000.0,
        "employmentYears": 0,
        "loanAmount": 45000.0,
        "existingLoans": 5,
        "creditScore": 490,
        "debtToIncomeRatio": 0.65,
    }

    decision_event = worker.process_event_payload(payload)

    assert decision_event["applicationUuid"] == "app-test-uuid-999"
    assert decision_event["customerUuid"] == "cust-test-uuid-999"
    assert decision_event["decision"] == "REJECT"
    assert decision_event["riskLevel"] in ["HIGH"]
    assert decision_event["riskScore"] > 0.60


def test_on_message_callback_success(worker):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 42
    mock_properties = MagicMock()

    body_dict = {
        "applicationUuid": "app-async-001",
        "customerUuid": "cust-async-001",
        "income": 90000.0,
        "employmentYears": 5,
        "loanAmount": 15000.0,
        "existingLoans": 2,
        "creditScore": 720,
        "debtToIncomeRatio": 0.25,
    }
    body_bytes = json.dumps(body_dict).encode("utf-8")

    worker.on_message_callback(mock_channel, mock_method, mock_properties, body_bytes)

    # Verify message was acknowledged
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=42)
    mock_channel.basic_nack.assert_not_called()

    # Verify decision published to correct exchange and routing key
    assert mock_channel.basic_publish.call_count == 1
    call_kwargs = mock_channel.basic_publish.call_args[1]
    assert call_kwargs["exchange"] == EXCHANGE_NAME
    assert call_kwargs["routing_key"] == DECISION_ROUTING_KEY

    published_body = json.loads(call_kwargs["body"].decode("utf-8"))
    assert published_body["applicationUuid"] == "app-async-001"
    assert published_body["customerUuid"] == "cust-async-001"
    assert published_body["decision"] in ["APPROVE", "REVIEW", "REJECT"]


def test_on_message_callback_malformed_payload_nack(worker):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 99
    mock_properties = MagicMock()

    # Invalid JSON bytes
    body_bytes = b"INVALID_NON_JSON_DATA"

    worker.on_message_callback(mock_channel, mock_method, mock_properties, body_bytes)

    # Verify message was nacked without requeue
    mock_channel.basic_nack.assert_called_once_with(delivery_tag=99, requeue=False)
    mock_channel.basic_ack.assert_not_called()
    mock_channel.basic_publish.assert_not_called()


@patch("pika.BlockingConnection")
def test_worker_connect_and_stop(mock_blocking_connection, worker):
    mock_conn = MagicMock()
    mock_chan = MagicMock()
    mock_conn.channel.return_value = mock_chan
    mock_conn.is_open = True
    mock_chan.is_open = True
    mock_blocking_connection.return_value = mock_conn

    worker.connect()
    assert worker.connection == mock_conn
    assert worker.channel == mock_chan
    mock_chan.basic_qos.assert_called_once_with(prefetch_count=10)

    worker.stop()
    assert worker.is_running is False
    mock_chan.stop_consuming.assert_called_once()
    mock_chan.close.assert_called_once()
    mock_conn.close.assert_called_once()
