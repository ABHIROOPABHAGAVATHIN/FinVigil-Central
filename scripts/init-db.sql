-- ==============================================================================
-- FinVigil Central: Database Schema Definition
-- PostgreSQL 16+ DDL Script
-- ==============================================================================

-- 1. Customers Table
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    customer_uuid VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customers_uuid ON customers(customer_uuid);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);

-- 2. Credit Applications Table
CREATE TABLE IF NOT EXISTS credit_applications (
    id BIGSERIAL PRIMARY KEY,
    application_uuid VARCHAR(64) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    income NUMERIC(15, 2) NOT NULL,
    employment_years INT NOT NULL,
    loan_amount NUMERIC(15, 2) NOT NULL,
    existing_loans INT NOT NULL,
    credit_score INT NOT NULL,
    debt_to_income_ratio NUMERIC(5, 4) NOT NULL,
    application_status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_credit_apps_uuid ON credit_applications(application_uuid);
CREATE INDEX IF NOT EXISTS idx_credit_apps_customer ON credit_applications(customer_id);

-- 3. Credit Decisions Table
CREATE TABLE IF NOT EXISTS credit_decisions (
    id BIGSERIAL PRIMARY KEY,
    decision_uuid VARCHAR(64) UNIQUE NOT NULL,
    application_id BIGINT NOT NULL REFERENCES credit_applications(id) ON DELETE CASCADE,
    risk_score NUMERIC(5, 4) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_credit_decisions_uuid ON credit_decisions(decision_uuid);
CREATE INDEX IF NOT EXISTS idx_credit_decisions_application ON credit_decisions(application_id);

-- 4. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_uuid VARCHAR(64) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    amount NUMERIC(15, 2) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    merchant VARCHAR(255) NOT NULL,
    transaction_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    currency VARCHAR(8) DEFAULT 'INR' NOT NULL,
    status VARCHAR(32) DEFAULT 'COMPLETED' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transactions_uuid ON transactions(transaction_uuid);
CREATE INDEX IF NOT EXISTS idx_transactions_customer ON transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(transaction_timestamp);

-- 5. AML Alerts Table
CREATE TABLE IF NOT EXISTS aml_alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_uuid VARCHAR(64) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    transaction_id BIGINT REFERENCES transactions(id) ON DELETE SET NULL,
    anomaly_score NUMERIC(5, 4) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    rule_score NUMERIC(5, 4) NOT NULL,
    final_risk_score NUMERIC(5, 4) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(32) DEFAULT 'OPEN' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_aml_alerts_uuid ON aml_alerts(alert_uuid);
CREATE INDEX IF NOT EXISTS idx_aml_alerts_customer ON aml_alerts(customer_id);
CREATE INDEX IF NOT EXISTS idx_aml_alerts_status ON aml_alerts(status);

-- 6. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(128) NOT NULL,
    action VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- 7. Employees Table (R&D Phase 1: Internal Identity & RBAC)
CREATE TABLE IF NOT EXISTS employees (
    id BIGSERIAL PRIMARY KEY,
    employee_uuid VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_employees_uuid ON employees(employee_uuid);
CREATE INDEX IF NOT EXISTS idx_employees_email ON employees(email);

