"""
FinVigil Central - Tests for Credit Risk Machine Learning Engine
Tests dataset generation, XGBoost training, calibrated decision boundaries, and FastAPI endpoints.
"""

import os
from pathlib import Path
import pytest
from fastapi.testclient import TestClient
import pandas as pd

from main import app
from ml.generate_credit_dataset import generate_credit_dataset
from ml.train_credit_model import train_and_save_credit_model, FEATURE_COLUMNS
from app.services.credit_service import CreditModelService
from app.schemas.credit import CreditPredictionRequest

client = TestClient(app)


@pytest.fixture(scope="session", autouse=True)
def setup_credit_model():
    """Ensure credit dataset and model artifact exist before running test suite."""
    test_data_dir = Path(__file__).resolve().parent.parent / "ml" / "data"
    test_data_dir.mkdir(parents=True, exist_ok=True)
    test_data_file = test_data_dir / "credit_dataset.csv"

    test_model_dir = Path(__file__).resolve().parent.parent / "trained_models"
    test_model_dir.mkdir(parents=True, exist_ok=True)
    test_model_file = test_model_dir / "credit_xgboost_v1.joblib"

    if not test_data_file.exists():
        generate_credit_dataset(n_samples=5000, output_path=test_data_file)
    
    if not test_model_file.exists():
        train_and_save_credit_model(data_path=test_data_file, model_output_path=test_model_file)


def test_dataset_generation(tmp_path):
    temp_csv = tmp_path / "test_credit_data.csv"
    df = generate_credit_dataset(n_samples=1000, random_state=123, output_path=temp_csv)
    
    assert isinstance(df, pd.DataFrame)
    assert len(df) == 1000
    for col in FEATURE_COLUMNS:
        assert col in df.columns
    assert "default_label" in df.columns
    assert df["default_label"].isin([0, 1]).all()
    assert df["income"].min() >= 18000.0
    assert df["credit_score"].min() >= 300
    assert df["credit_score"].max() <= 850
    assert df.isnull().sum().sum() == 0


def test_model_training(tmp_path):
    temp_csv = tmp_path / "train_data.csv"
    temp_model = tmp_path / "test_model.joblib"
    
    generate_credit_dataset(n_samples=2000, random_state=42, output_path=temp_csv)
    metadata = train_and_save_credit_model(
        data_path=temp_csv,
        model_output_path=temp_model,
        random_state=42
    )

    assert temp_model.exists()
    assert "metrics" in metadata
    assert metadata["metrics"]["roc_auc"] >= 0.75
    assert metadata["metrics"]["accuracy"] >= 0.70
    assert "feature_importances" in metadata


def test_credit_service_low_risk_approval():
    service = CreditModelService.get_instance()
    
    # Prime borrower: high income, great credit score, low DTI, stable tenure
    req = CreditPredictionRequest(
        income=180000.0,
        employment_years=10,
        loan_amount=15000.0,
        existing_loans=1,
        credit_score=790,
        debt_to_income_ratio=0.15,
        application_uuid="app-prime-001",
        customer_uuid="cust-001",
    )
    
    response = service.predict(req)
    assert response.application_uuid == "app-prime-001"
    assert response.customer_uuid == "cust-001"
    assert response.decision == "APPROVE"
    assert response.risk_score < 0.30
    assert response.risk_level in ["LOW", "MEDIUM"]


def test_credit_service_high_risk_rejection():
    service = CreditModelService.get_instance()
    
    # High-risk borrower: very low score, high DTI, high leverage
    req = CreditPredictionRequest(
        income=22000.0,
        employment_years=0,
        loan_amount=40000.0,
        existing_loans=6,
        credit_score=480,
        debt_to_income_ratio=0.70,
        application_uuid="app-subprime-999",
        customer_uuid="cust-999",
    )
    
    response = service.predict(req)
    assert response.application_uuid == "app-subprime-999"
    assert response.decision == "REJECT"
    assert response.risk_score > 0.60
    assert response.risk_level in ["HIGH"]


def test_credit_service_predict_dict_compatibility():
    service = CreditModelService.get_instance()
    
    # Test camelCase dictionary compatibility matching Java event
    event_data = {
        "applicationUuid": "app-camel-123",
        "customerUuid": "cust-camel-456",
        "income": 95000.0,
        "employmentYears": 6,
        "loanAmount": 20000.0,
        "existingLoans": 2,
        "creditScore": 730,
        "debtToIncomeRatio": 0.25,
    }
    
    result = service.predict_dict(event_data)
    assert result["application_uuid"] == "app-camel-123"
    assert result["customer_uuid"] == "cust-camel-456"
    assert "risk_score" in result
    assert result["decision"] in ["APPROVE", "REVIEW", "REJECT"]
    assert "model_version" in result


def test_fastapi_credit_predict_endpoint():
    payload = {
        "income": 120000.0,
        "employment_years": 8,
        "loan_amount": 25000.0,
        "existing_loans": 2,
        "credit_score": 750,
        "debt_to_income_ratio": 0.22,
        "application_uuid": "app-api-test",
        "customer_uuid": "cust-api-test"
    }
    
    response = client.post("/api/v1/credit/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["decision_uuid"] is not None
    assert data["application_uuid"] == "app-api-test"
    assert data["customer_uuid"] == "cust-api-test"
    assert "risk_score" in data
    assert data["decision"] == "APPROVE"



def test_fastapi_credit_model_info_endpoint():
    response = client.get("/api/v1/credit/model-info")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "LOADED"
    assert "roc_auc" in data["metrics"]
    assert data["algorithm"] == "XGBoost Classifier"
