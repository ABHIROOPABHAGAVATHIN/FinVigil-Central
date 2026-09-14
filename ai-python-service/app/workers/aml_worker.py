"""
FinVigil Central - Real-Time AML RabbitMQ Worker
Consumes financial transaction events from 'aml.transaction.queue',
performs hybrid risk scoring combining deterministic compliance rules (40%) and
Isolation Forest ML anomaly detection (60%), and publishes actionable alerts to 'aml.alert'.
"""

import datetime
import json
import logging
import os
import signal
import sys
import time
from typing import Dict, Any, Optional, Tuple
import uuid
import pika

from app.services.aml_service import AmlModelService
from app.rules.aml_rules import evaluate_aml_rules

# Setup logger
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [AmlWorker] %(message)s"
)
logger = logging.getLogger("AmlWorker")

EXCHANGE_NAME = "finvigil.exchange"
AML_TRANSACTION_QUEUE = "aml.transaction.queue"
AML_ALERT_ROUTING_KEY = "aml.alert"
DLX_EXCHANGE = "finvigil.dlx"


class AmlEventWorker:
    """
    RabbitMQ Worker consuming real-time transaction events and
    evaluating hybrid AML risk (deterministic compliance rules + Isolation Forest ML).
    """

    def __init__(
        self,
        host: Optional[str] = None,
        port: Optional[int] = None,
        user: Optional[str] = None,
        password: Optional[str] = None,
        prefetch_count: int = 10,
        rule_weight: Optional[float] = None,
        ml_weight: Optional[float] = None,
        alert_threshold: Optional[float] = None,
    ):
        self.host = host or os.getenv("RABBITMQ_HOST", "localhost")
        self.port = port or int(os.getenv("RABBITMQ_PORT", 5672))
        self.user = user or os.getenv("RABBITMQ_USERNAME", os.getenv("RABBITMQ_USER", "guest"))
        self.password = password or os.getenv("RABBITMQ_PASSWORD", "guest")
        self.prefetch_count = prefetch_count

        self.rule_weight = rule_weight or float(os.getenv("AML_RULE_WEIGHT", "0.4"))
        self.ml_weight = ml_weight or float(os.getenv("AML_ML_WEIGHT", "0.6"))
        self.alert_threshold = alert_threshold or float(os.getenv("AML_ALERT_THRESHOLD", "0.55"))

        self.connection: Optional[pika.BlockingConnection] = None
        self.channel: Optional[pika.adapters.blocking_connection.BlockingChannel] = None
        self.is_running = False
        self.aml_service = AmlModelService.get_instance()

    def connect(self):
        """Establish resilient connection and channel to RabbitMQ."""
        credentials = pika.PlainCredentials(self.user, self.password)
        parameters = pika.ConnectionParameters(
            host=self.host,
            port=self.port,
            credentials=credentials,
            heartbeat=60,
            blocked_connection_timeout=300,
        )
        logger.info(f"Connecting to RabbitMQ at {self.host}:{self.port} as user '{self.user}'...")
        self.connection = pika.BlockingConnection(parameters)
        self.channel = self.connection.channel()
        self.channel.basic_qos(prefetch_count=self.prefetch_count)
        logger.info("RabbitMQ connection and channel established successfully.")

    def calculate_hybrid_score(self, rule_score: float, ml_score: float) -> float:
        """
        Computes weighted hybrid AML risk score:
        hybrid = (rule_weight * rule_score) + (ml_weight * ml_score)
        """
        total_weight = self.rule_weight + self.ml_weight
        w_rule = self.rule_weight / total_weight if total_weight > 0 else 0.4
        w_ml = self.ml_weight / total_weight if total_weight > 0 else 0.6
        score = (w_rule * rule_score) + (w_ml * ml_score)
        return round(float(score), 4)

    def process_event_payload(self, payload: Dict[str, Any]) -> Tuple[Optional[Dict[str, Any]], Dict[str, Any]]:
        """
        Processes transaction event through:
        1. Deterministic AML compliance rules
        2. Isolation Forest ML anomaly detection
        3. Weighted hybrid score synthesis
        Returns: (alert_event_or_none, evaluation_summary)
        """
        txn_uuid = payload.get("transactionUuid", payload.get("transaction_uuid", "unknown"))
        cust_uuid = payload.get("customerUuid", payload.get("customer_uuid", "unknown"))

        logger.info(f"Evaluating AML for transaction [{txn_uuid}] customer [{cust_uuid}]")

        # 1. Evaluate deterministic compliance rules
        rule_score, rule_risk, violations, triggered_rules = evaluate_aml_rules(payload)

        # 2. Evaluate ML Isolation Forest model
        ml_result = self.aml_service.predict_dict(payload)
        ml_score = float(ml_result["anomaly_score"])
        is_ml_anomaly = bool(ml_result["is_anomaly"])
        ml_reasons = ml_result.get("reasons", [])

        # 3. Compute weighted hybrid score
        hybrid_score = self.calculate_hybrid_score(rule_score, ml_score)

        # Determine overall categorical risk level
        has_high_rule = any(v.severity == "HIGH" for v in violations)
        if hybrid_score >= 0.65 or has_high_rule:
            final_risk_level = "HIGH"
        elif hybrid_score >= 0.35 or rule_score > 0 or is_ml_anomaly:
            final_risk_level = "MEDIUM"
        else:
            final_risk_level = "LOW"

        # Determine if an AML alert should be dispatched
        trigger_alert = (
            hybrid_score >= self.alert_threshold
            or final_risk_level == "HIGH"
            or is_ml_anomaly
        )

        # Merge unique explanatory reasons
        combined_reasons = list(dict.fromkeys(triggered_rules + ml_reasons))
        if trigger_alert and not combined_reasons:
            combined_reasons.append("Hybrid AML risk score exceeded tolerance threshold")

        summary = {
            "transactionUuid": txn_uuid,
            "customerUuid": cust_uuid,
            "ruleScore": rule_score,
            "mlScore": ml_score,
            "hybridScore": hybrid_score,
            "riskLevel": final_risk_level,
            "isAlertTriggered": trigger_alert,
            "triggeredRules": triggered_rules,
            "reasons": combined_reasons,
        }

        logger.info(
            f"Evaluated txn [{txn_uuid}]: hybridScore={hybrid_score} (rule={rule_score}, ml={ml_score}), "
            f"riskLevel={final_risk_level}, alert={trigger_alert}"
        )

        if trigger_alert:
            primary_reason = "; ".join(combined_reasons) if combined_reasons else "AML anomaly threshold exceeded"
            alert_event = {
                "alertUuid": str(uuid.uuid4()),
                "transactionUuid": txn_uuid,
                "customerUuid": cust_uuid,
                "ruleScore": rule_score,
                "mlScore": ml_score,
                "anomalyScore": ml_score,
                "hybridScore": hybrid_score,
                "finalRiskScore": hybrid_score,
                "riskLevel": final_risk_level,
                "status": "OPEN",
                "reasons": combined_reasons,
                "reason": primary_reason,
                "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            }
            return alert_event, summary

        return None, summary

    def on_message_callback(self, ch, method, properties, body):
        """RabbitMQ message callback."""
        delivery_tag = method.delivery_tag
        try:
            raw_data = json.loads(body.decode("utf-8"))
            alert_event, summary = self.process_event_payload(raw_data)

            if alert_event is not None:
                # Publish alert to exchange with aml.alert routing key
                ch.basic_publish(
                    exchange=EXCHANGE_NAME,
                    routing_key=AML_ALERT_ROUTING_KEY,
                    body=json.dumps(alert_event).encode("utf-8"),
                    properties=pika.BasicProperties(
                        content_type="application/json",
                        delivery_mode=pika.DeliveryMode.Persistent,
                    ),
                )
                logger.info(
                    f"DISPATCHED AML ALERT [{alert_event['alertUuid']}] for txn [{alert_event['transactionUuid']}], "
                    f"riskLevel={alert_event['riskLevel']}, hybridScore={alert_event['hybridScore']}"
                )
            else:
                logger.info(f"Transaction [{summary['transactionUuid']}] cleared (score={summary['hybridScore']}). No alert generated.")

            ch.basic_ack(delivery_tag=delivery_tag)

        except Exception as ex:
            logger.error(f"Failed to process AML transaction message: {ex}", exc_info=True)
            try:
                ch.basic_nack(delivery_tag=delivery_tag, requeue=False)
            except Exception as ack_err:
                logger.error(f"Error sending basic_nack: {ack_err}")

    def start_consuming(self, max_retries: int = 5, retry_delay: int = 5):
        """Starts continuous consumer loop with auto-reconnect."""
        self.is_running = True
        retries = 0

        while self.is_running and retries < max_retries:
            try:
                self.connect()
                self.channel.basic_consume(
                    queue=AML_TRANSACTION_QUEUE,
                    on_message_callback=self.on_message_callback,
                    auto_ack=False,
                )
                logger.info(f"AML Worker listening on '{AML_TRANSACTION_QUEUE}'...")
                retries = 0  # reset on successful connection
                self.channel.start_consuming()

            except pika.exceptions.AMQPConnectionError as conn_err:
                logger.warning(f"RabbitMQ connection lost ({conn_err}). Retrying in {retry_delay}s...")
                retries += 1
                time.sleep(retry_delay)
            except Exception as ex:
                if not self.is_running:
                    break
                logger.error(f"Unexpected worker error: {ex}", exc_info=True)
                retries += 1
                time.sleep(retry_delay)

    def stop(self):
        """Gracefully terminate consumer and connections."""
        logger.info("Stopping AML Worker...")
        self.is_running = False
        try:
            if self.channel and self.channel.is_open:
                self.channel.stop_consuming()
                self.channel.close()
            if self.connection and self.connection.is_open:
                self.connection.close()
            logger.info("AML Worker stopped gracefully.")
        except Exception as e:
            logger.warning(f"Error during AML worker shutdown: {e}")


def main():
    worker = AmlEventWorker()

    def handle_signal(sig, frame):
        logger.info("Shutdown signal received.")
        worker.stop()
        sys.exit(0)

    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    try:
        worker.start_consuming()
    except KeyboardInterrupt:
        worker.stop()


if __name__ == "__main__":
    main()
