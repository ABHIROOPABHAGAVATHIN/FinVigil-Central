"""
FinVigil Central - RabbitMQ Credit Risk ML Worker
Listens to 'credit.underwriting.queue', executes XGBoost risk inference,
and publishes credit decisions to 'finvigil.exchange' -> 'credit.decision'.
"""

import json
import logging
import os
import signal
import sys
import time
from typing import Dict, Any, Optional
import pika

from app.services.credit_service import CreditModelService

# Setup logger
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [CreditWorker] %(message)s"
)
logger = logging.getLogger("CreditWorker")

EXCHANGE_NAME = "finvigil.exchange"
UNDERWRITING_QUEUE = "credit.underwriting.queue"
DECISION_ROUTING_KEY = "credit.decision"
DLX_EXCHANGE = "finvigil.dlx"


class CreditEventWorker:
    """
    RabbitMQ Worker consuming credit underwriting requests and
    producing automated XGBoost credit risk decisions.
    """

    def __init__(
        self,
        host: Optional[str] = None,
        port: Optional[int] = None,
        user: Optional[str] = None,
        password: Optional[str] = None,
        prefetch_count: int = 10,
    ):
        self.host = host or os.getenv("RABBITMQ_HOST", "localhost")
        self.port = port or int(os.getenv("RABBITMQ_PORT", 5672))
        self.user = user or os.getenv("RABBITMQ_USERNAME", os.getenv("RABBITMQ_USER", "guest"))
        self.password = password or os.getenv("RABBITMQ_PASSWORD", "guest")
        self.prefetch_count = prefetch_count


        self.connection: Optional[pika.BlockingConnection] = None
        self.channel: Optional[pika.adapters.blocking_connection.BlockingChannel] = None
        self.is_running = False
        self.model_service = CreditModelService.get_instance()

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

    def process_event_payload(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """
        Processes credit underwriting features and builds CreditDecisionEvent.
        """
        logger.info(
            f"Processing underwriting request: appUuid={payload.get('applicationUuid', payload.get('application_uuid'))}, "
            f"custUuid={payload.get('customerUuid', payload.get('customer_uuid'))}"
        )
        
        # Run ML model inference
        decision_data = self.model_service.predict_dict(payload)

        # Build payload adhering exactly to Java CreditDecisionEvent schema
        decision_event = {
            "decisionUuid": decision_data["decision_uuid"],
            "applicationUuid": decision_data["application_uuid"],
            "customerUuid": decision_data["customer_uuid"],
            "riskScore": decision_data["risk_score"],
            "riskLevel": decision_data["risk_level"],
            "decision": decision_data["decision"],
            "modelVersion": decision_data["model_version"],
            "timestamp": decision_data["timestamp"],
        }

        logger.info(
            f"Evaluated decision: appUuid={decision_event['applicationUuid']}, "
            f"decision={decision_event['decision']}, riskScore={decision_event['riskScore']}, "
            f"riskLevel={decision_event['riskLevel']}"
        )
        return decision_event

    def on_message_callback(self, ch, method, properties, body):
        """RabbitMQ message callback."""
        delivery_tag = method.delivery_tag
        try:
            raw_data = json.loads(body.decode("utf-8"))
            decision_event = self.process_event_payload(raw_data)

            # Publish decision event back to exchange
            ch.basic_publish(
                exchange=EXCHANGE_NAME,
                routing_key=DECISION_ROUTING_KEY,
                body=json.dumps(decision_event).encode("utf-8"),
                properties=pika.BasicProperties(
                    content_type="application/json",
                    delivery_mode=pika.DeliveryMode.Persistent,
                ),
            )
            ch.basic_ack(delivery_tag=delivery_tag)
            logger.info(f"Published decision for application: {decision_event['applicationUuid']}")

        except Exception as ex:
            logger.error(f"Failed to process credit underwriting message: {ex}", exc_info=True)
            # Send to dead letter queue without requeueing
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
                    queue=UNDERWRITING_QUEUE,
                    on_message_callback=self.on_message_callback,
                    auto_ack=False,
                )
                logger.info(f"Credit Worker listening on '{UNDERWRITING_QUEUE}'...")
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
        logger.info("Stopping Credit Worker...")
        self.is_running = False
        try:
            if self.channel and self.channel.is_open:
                self.channel.stop_consuming()
                self.channel.close()
            if self.connection and self.connection.is_open:
                self.connection.close()
            logger.info("Credit Worker stopped gracefully.")
        except Exception as e:
            logger.warning(f"Error during worker shutdown: {e}")


def main():
    worker = CreditEventWorker()

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
