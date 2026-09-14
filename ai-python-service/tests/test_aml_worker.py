"""
FinVigil Central - Tests for RabbitMQ AML Event Worker
Validates deterministic rule evaluation, ML Isolation Forest fusion,
hybrid risk scoring, and RabbitMQ message ingestion & alert dispatch.
"""

import json
from unittest.mock import MagicMock, patch
import pytest

from app.workers.aml_worker import AmlEventWorker, EXCHANGE_NAME, AML_ALERT_ROUTING_KEY


@pytest.fixture
def aml_worker():
    return AmlEventWorker(rule_weight=0.4, ml_weight=0.6, alert_threshold=0.55)


def test_hybrid_score_calculation(aml_worker):
    # Test formula: 0.4 * 0.5 + 0.6 * 0.8 = 0.20 + 0.48 = 0.68
    score = aml_worker.calculate_hybrid_score(0.5, 0.8)
    assert score == 0.68

    # Clean txn: 0.4 * 0.0 + 0.6 * 0.15 = 0.09
    clean_score = aml_worker.calculate_hybrid_score(0.0, 0.15)
    assert clean_score == 0.09


def test_process_event_payload_clean_transaction(aml_worker):
    payload = {
        "transactionUuid": "clean-txn-001",
        "customerUuid": "cust-001",
        "amount": 120.0,
        "transactionType": "PURCHASE",
        "merchant": "Whole Foods Market",
        "currency": "INR",
        "transactionTimestamp": "2026-09-14T14:00:00Z",
        "velocityCount1m": 1,
        "velocityAmount1m": 120.0,
    }

    alert_event, summary = aml_worker.process_event_payload(payload)

    assert alert_event is None
    assert summary["riskLevel"] in ["LOW", "MEDIUM"]
    assert not summary["isAlertTriggered"]
    assert summary["ruleScore"] == 0.0


def test_process_event_payload_large_transaction_alert(aml_worker):
    payload = {
        "transactionUuid": "large-txn-002",
        "customerUuid": "cust-002",
        "amount": 150000.0,
        "transactionType": "TRANSFER",
        "merchant": "Standard Global Bank",
        "currency": "INR",
        "transactionTimestamp": "2026-09-14T15:30:00Z",
        "velocityCount1m": 1,
        "velocityAmount1m": 150000.0,
    }

    alert_event, summary = aml_worker.process_event_payload(payload)

    assert alert_event is not None
    assert alert_event["transactionUuid"] == "large-txn-002"
    assert alert_event["customerUuid"] == "cust-002"
    assert alert_event["riskLevel"] == "HIGH"
    assert alert_event["status"] == "OPEN"
    assert any("LARGE_TRANSACTION_THRESHOLD" in r for r in alert_event["reasons"])
    assert alert_event["hybridScore"] >= 0.50


def test_process_event_payload_structuring_alert(aml_worker):
    payload = {
        "transactionUuid": "structuring-txn-003",
        "customerUuid": "cust-003",
        "amount": 48500.0,  # Just below 50,000 threshold
        "transactionType": "TRANSFER",
        "merchant": "Retail Outlet Transfer",
        "currency": "INR",
        "transactionTimestamp": "2026-09-14T16:00:00Z",
        "velocityCount1m": 3,
        "velocityAmount1m": 145000.0,
    }

    alert_event, summary = aml_worker.process_event_payload(payload)

    assert alert_event is not None
    assert alert_event["transactionUuid"] == "structuring-txn-003"
    assert any("STRUCTURING_DETECTION" in r for r in alert_event["reasons"])


def test_process_event_payload_high_risk_merchant_alert(aml_worker):
    payload = {
        "transactionUuid": "merchant-txn-004",
        "customerUuid": "cust-004",
        "amount": 10000.0,
        "transactionType": "TRANSFER",
        "merchant": "Offshore Bitcoin Mixer Ltd",
        "currency": "INR",
        "transactionTimestamp": "2026-09-14T02:00:00Z",
        "velocityCount1m": 2,
        "velocityAmount1m": 20000.0,
    }

    alert_event, summary = aml_worker.process_event_payload(payload)

    assert alert_event is not None
    assert alert_event["riskLevel"] == "HIGH"
    assert any("HIGH_RISK_MERCHANT_CATEGORY" in r for r in alert_event["reasons"])


def test_on_message_callback_publishes_alert_for_suspicious_txn(aml_worker):
    mock_channel = MagicMock()
    mock_method = MagicMock(delivery_tag=101)
    mock_properties = MagicMock()

    payload = {
        "transactionUuid": "suspicious-txn-999",
        "customerUuid": "cust-999",
        "amount": 250000.0,
        "transactionType": "TRANSFER",
        "merchant": "Darknet Crypto Hub",
        "currency": "INR",
        "transactionTimestamp": "2026-09-14T01:30:00Z",
        "velocityCount1m": 8,
        "velocityAmount1m": 500000.0,
    }
    body = json.dumps(payload).encode("utf-8")

    aml_worker.on_message_callback(mock_channel, mock_method, mock_properties, body)

    # Verify basic_publish was called with alert payload
    assert mock_channel.basic_publish.called
    call_args = mock_channel.basic_publish.call_args
    assert call_args.kwargs["exchange"] == EXCHANGE_NAME
    assert call_args.kwargs["routing_key"] == AML_ALERT_ROUTING_KEY

    published_alert = json.loads(call_args.kwargs["body"].decode("utf-8"))
    assert published_alert["transactionUuid"] == "suspicious-txn-999"
    assert published_alert["riskLevel"] == "HIGH"
    assert published_alert["status"] == "OPEN"
    assert "anomalyScore" in published_alert
    assert "mlScore" in published_alert
    assert published_alert["anomalyScore"] == published_alert["mlScore"]
    assert "finalRiskScore" in published_alert
    assert published_alert["finalRiskScore"] == published_alert["hybridScore"]
    assert "reason" in published_alert

    # Verify message acknowledged
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=101)


def test_on_message_callback_clean_txn_only_acks_no_publish(aml_worker):
    mock_channel = MagicMock()
    mock_method = MagicMock(delivery_tag=102)
    mock_properties = MagicMock()

    payload = {
        "transactionUuid": "clean-txn-100",
        "customerUuid": "cust-100",
        "amount": 50.0,
        "transactionType": "PURCHASE",
        "merchant": "Coffee Shop",
        "currency": "INR",
        "transactionTimestamp": "2026-09-14T12:00:00Z",
        "velocityCount1m": 1,
        "velocityAmount1m": 50.0,
    }
    body = json.dumps(payload).encode("utf-8")

    aml_worker.on_message_callback(mock_channel, mock_method, mock_properties, body)

    # For clean transaction, no alert should be published
    mock_channel.basic_publish.assert_not_called()
    # But message must be acked
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=102)


def test_on_message_callback_malformed_payload_nacks(aml_worker):
    mock_channel = MagicMock()
    mock_method = MagicMock(delivery_tag=103)
    mock_properties = MagicMock()

    corrupt_body = b"NOT_VALID_JSON"

    aml_worker.on_message_callback(mock_channel, mock_method, mock_properties, corrupt_body)

    mock_channel.basic_nack.assert_called_once_with(delivery_tag=103, requeue=False)
    mock_channel.basic_publish.assert_not_called()


@patch("pika.BlockingConnection")
def test_worker_connect_and_stop(mock_blocking_conn, aml_worker):
    mock_conn_instance = MagicMock()
    mock_channel_instance = MagicMock()
    mock_blocking_conn.return_value = mock_conn_instance
    mock_conn_instance.channel.return_value = mock_channel_instance

    aml_worker.connect()
    assert aml_worker.connection is not None
    assert aml_worker.channel is not None
    mock_channel_instance.basic_qos.assert_called_once_with(prefetch_count=10)

    # Test stop
    aml_worker.stop()
    assert not aml_worker.is_running
    mock_channel_instance.stop_consuming.assert_called_once()
    mock_conn_instance.close.assert_called_once()
