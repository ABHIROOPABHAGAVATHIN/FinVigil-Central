"""
FinVigil Central - Phase 9 End-to-End Credit Verification Script
Verifies the complete pipeline:
1. Database Connectivity (PostgreSQL 16)
2. RabbitMQ Exchange & Queue Topology (finvigil.exchange, queues, DLQ)
3. Event Publication (credit.underwriting)
4. AI ML Model Inference (XGBoost Classifier)
5. Decision Event Publication (credit.decision)
6. State Consistency & Decision Verification
"""

import json
import os
import subprocess
import sys
import time
import uuid
from pathlib import Path
import pika

# Add ai-python-service to sys.path
SERVICE_ROOT = Path(__file__).resolve().parent.parent / "ai-python-service"
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.services.credit_service import CreditModelService
from app.workers.credit_worker import CreditEventWorker, EXCHANGE_NAME, UNDERWRITING_QUEUE, DECISION_ROUTING_KEY

RABBIT_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBIT_PORT = int(os.getenv("RABBITMQ_PORT", 5672))
RABBIT_USER = os.getenv("RABBITMQ_USERNAME", "guest")
RABBIT_PASS = os.getenv("RABBITMQ_PASSWORD", "guest")


def run_psql_query(sql: str) -> str:
    """Executes SQL via docker-compose postgres container."""
    cmd = [
        "docker", "compose", "exec", "-T", "postgres",
        "psql", "-U", "finvigil_user", "-d", "finvigil_db", "-t", "-A", "-c", sql
    ]
    res = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return res.stdout.strip()


def test_database_connection():
    print("[1/5] Testing PostgreSQL 16 database connectivity...")
    ver = run_psql_query("SELECT version();")
    print(f"      Connected to DB: {ver[:40]}...")
    
    tables = run_psql_query("""
        SELECT table_name FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name IN ('customers', 'credit_applications', 'credit_decisions', 'audit_logs');
    """).splitlines()
    print(f"      Verified core tables exist: {tables}")
    assert "customers" in tables, "customers table missing"
    assert "credit_applications" in tables, "credit_applications table missing"
    print("      PostgreSQL verification PASSED.")



def test_rabbitmq_topology():
    print("[2/5] Testing RabbitMQ broker & topology declaration...")
    creds = pika.PlainCredentials(RABBIT_USER, RABBIT_PASS)
    conn = pika.BlockingConnection(pika.ConnectionParameters(host=RABBIT_HOST, port=RABBIT_PORT, credentials=creds))
    channel = conn.channel()

    # Declare main exchange
    channel.exchange_declare(exchange=EXCHANGE_NAME, exchange_type="direct", durable=True)
    channel.exchange_declare(exchange="finvigil.dlx", exchange_type="direct", durable=True)
    
    # Declare DLQ
    channel.queue_declare(queue="finvigil.dlq", durable=True)
    channel.queue_bind(queue="finvigil.dlq", exchange="finvigil.dlx", routing_key="#")

    # Declare application queues
    queue_args = {
        "x-dead-letter-exchange": "finvigil.dlx",
        "x-dead-letter-routing-key": "dlq.dead.letter"
    }
    channel.queue_declare(queue="credit.underwriting.queue", durable=True, arguments=queue_args)
    channel.queue_declare(queue="credit.decision.queue", durable=True, arguments=queue_args)

    channel.queue_bind(queue="credit.underwriting.queue", exchange=EXCHANGE_NAME, routing_key="credit.underwriting")
    channel.queue_bind(queue="credit.decision.queue", exchange=EXCHANGE_NAME, routing_key="credit.decision")

    conn.close()
    print("      RabbitMQ exchanges & queues declared successfully.")


def run_e2e_underwriting_cycle():
    print("[3/5] Executing End-to-End Prime Applicant Underwriting Flow...")
    
    # 1. Create a customer and credit application in PostgreSQL
    customer_uuid = f"cust-e2e-{uuid.uuid4().hex[:8]}"
    app_uuid = f"app-e2e-{uuid.uuid4().hex[:8]}"

    # Insert test customer
    cust_id_raw = run_psql_query(f"""
        INSERT INTO customers (customer_uuid, name, email, phone, password_hash, status)
        VALUES ('{customer_uuid}', 'Alice E2E Prime', 'alice_{uuid.uuid4().hex[:6]}@example.com', '+1-555-0199', 'hashedpass', 'ACTIVE') 
        RETURNING id;
    """)
    cust_id = int(cust_id_raw.splitlines()[0].strip())

    # Insert credit application in PENDING status
    app_id_raw = run_psql_query(f"""
        INSERT INTO credit_applications 
        (application_uuid, customer_id, income, employment_years, loan_amount, existing_loans, credit_score, debt_to_income_ratio, application_status)
        VALUES ('{app_uuid}', {cust_id}, 120000.00, 7, 25000.00, 1, 760, 0.22, 'PENDING') 
        RETURNING id;
    """)
    app_id = int(app_id_raw.splitlines()[0].strip())


    print(f"      Created Customer (ID={cust_id}, UUID={customer_uuid}) & Application (ID={app_id}, UUID={app_uuid}) in DB")

    # 2. Publish CreditUnderwritingEvent to RabbitMQ
    underwriting_event = {
        "applicationUuid": app_uuid,
        "customerUuid": customer_uuid,
        "income": 120000.00,
        "employmentYears": 7,
        "loanAmount": 25000.00,
        "existingLoans": 1,
        "creditScore": 760,
        "debtToIncomeRatio": 0.22,
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    }

    creds = pika.PlainCredentials(RABBIT_USER, RABBIT_PASS)
    rabbit_conn = pika.BlockingConnection(pika.ConnectionParameters(host=RABBIT_HOST, port=RABBIT_PORT, credentials=creds))
    channel = rabbit_conn.channel()

    channel.basic_publish(
        exchange=EXCHANGE_NAME,
        routing_key="credit.underwriting",
        body=json.dumps(underwriting_event).encode("utf-8"),
        properties=pika.BasicProperties(content_type="application/json")
    )
    print(f"      Published CreditUnderwritingEvent for app {app_uuid} to 'credit.underwriting'")

    # 3. Simulate Worker Processing (Consume 1 message, process, publish decision)
    print("[4/5] Worker processing message from 'credit.underwriting.queue'...")
    method_frame, header_frame, body = channel.basic_get(queue="credit.underwriting.queue", auto_ack=False)
    assert method_frame is not None, "Expected message in credit.underwriting.queue"

    worker = CreditEventWorker()
    worker.on_message_callback(channel, method_frame, header_frame, body)

    # 4. Consume decision from 'credit.decision.queue'
    method_dec, header_dec, body_dec = channel.basic_get(queue="credit.decision.queue", auto_ack=True)
    assert method_dec is not None, "Expected message in credit.decision.queue"
    decision_event = json.loads(body_dec.decode("utf-8"))
    print(f"      Received CreditDecisionEvent: Decision={decision_event['decision']}, RiskScore={decision_event['riskScore']:.4f}, RiskLevel={decision_event['riskLevel']}")
    assert decision_event["applicationUuid"] == app_uuid
    assert decision_event["decision"] == "APPROVE"

    # 5. Persist Decision into PostgreSQL (representing Java Consumer Action)
    print("[5/5] Verifying database persistence & status transition...")
    decision_uuid = decision_event["decisionUuid"]
    run_psql_query(f"""
        INSERT INTO credit_decisions (decision_uuid, application_id, risk_score, risk_level, decision, model_version)
        VALUES ('{decision_uuid}', {app_id}, {decision_event['riskScore']}, '{decision_event['riskLevel']}', '{decision_event['decision']}', '{decision_event['modelVersion']}');
    """)

    new_status = "APPROVED" if decision_event["decision"] == "APPROVE" else "REJECTED"
    run_psql_query(f"""
        UPDATE credit_applications SET application_status = '{new_status}' WHERE id = {app_id};
    """)

    run_psql_query(f"""
        INSERT INTO audit_logs (event_type, entity_type, entity_id, action)
        VALUES ('CREDIT', 'CREDIT_DECISION', '{decision_uuid}', 'CREDIT_DECISION_RECEIVED');
    """)

    # Verify updated status
    app_status = run_psql_query(f"SELECT application_status FROM credit_applications WHERE id = {app_id};")
    assert app_status == "APPROVED", f"Expected APPROVED but got {app_status}"

    # Verify audit log
    audit_count = run_psql_query(f"SELECT count(*) FROM audit_logs WHERE entity_id = '{decision_uuid}';")
    assert int(audit_count) >= 1, "Audit log not recorded"

    rabbit_conn.close()
    print(f"      E2E Verification SUCCESS: Application {app_uuid} updated to {app_status} with Decision UUID {decision_uuid}")



def main():
    print("================================================================================")
    print("FINVIGIL CENTRAL - PHASE 9 END-TO-END CREDIT UNDERWRITING VERIFICATION")
    print("================================================================================")
    test_database_connection()
    test_rabbitmq_topology()
    run_e2e_underwriting_cycle()
    print("================================================================================")
    print("ALL END-TO-END VERIFICATION CHECKS PASSED (PostgreSQL + RabbitMQ + XGBoost ML)")
    print("================================================================================")


if __name__ == "__main__":
    main()
