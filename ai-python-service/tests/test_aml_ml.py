"""
FinVigil Central - Tests for AML Machine Learning Anomaly Detection
Tests dataset generation, Isolation Forest training, calibrated scoring, and FastAPI endpoints.
"""

from pathlib import Path
import pytest
from fastapi.testclient import TestClient
import pandas as pd

from main import app
from ml.generate_aml_dataset import generate_aml_dataset, AML_FEATURE_COLUMNS, TARGET_COLUMN
from ml.train_aml_model import train_and_save_aml_model
from app.services.aml_service import AmlModelService
from app.schemas.aml import AmlPredictRequest

client = TestClient(app)


@pytest.fixture(scope="session", autouse=True)
def setup_aml_model():
    """Ensure AML dataset and Isolation Forest artifact exist before running tests."""
    test_data_dir = Path(__file__).resolve().parent.parent / "ml" / "data"
    test_data_dir.mkdir(parents=True, exist_ok=True)
    test_data_file = test_data_dir / "aml_dataset.csv"

    test_model_dir = Path(__file__).resolve().parent.parent / "trained_models"
    test_model_dir.mkdir(parents=True, exist_ok=True)
    test_model_file = test_model_dir / "aml_isolation_forest_v1.joblib"

    if not test_data_file.exists():
        generate_aml_dataset(n_samples=5000, output_path=test_data_file)

    if not test_model_file.exists():
        train_and_save_aml_model(data_path=test_data_file, model_output_path=test_model_file)


def test_aml_dataset_generation(tmp_path):
    temp_csv = tmp_path / "test_aml_data.csv"
    df = generate_aml_dataset(n_samples=1000, anomaly_ratio=0.05, random_state=42, output_path=temp_csv)

    assert isinstance(df, pd.DataFrame)
    assert len(df) == 1000
    for col in AML_FEATURE_COLUMNS:
        assert col in df.columns
    assert TARGET_COLUMN in df.columns
    assert df[TARGET_COLUMN].isin([0, 1]).all()
    assert df.isnull().sum().sum() == 0
    # Verify anomaly distribution ~ 5%
    anomaly_rate = df[TARGET_COLUMN].mean()
    assert 0.03 <= anomaly_rate <= 0.07


def test_aml_model_training(tmp_path):
    temp_csv = tmp_path / "train_aml_data.csv"
    temp_model = tmp_path / "test_aml_model.joblib"

    generate_aml_dataset(n_samples=2500, random_state=42, output_path=temp_csv)
    metadata = train_and_save_aml_model(
        data_path=temp_csv,
        model_output_path=temp_model,
        random_state=42
    )

    assert temp_model.exists()
    assert "metrics" in metadata
    assert metadata["metrics"]["roc_auc"] >= 0.70
    assert metadata["algorithm"] == "Isolation Forest Anomaly Detector"


def test_aml_service_low_risk_inlier():
    service = AmlModelService.get_instance()

    # Normal daytime grocery purchase
    req = AmlPredictRequest(
        amount=150.0,
        transaction_type="PURCHASE",
        merchant="Fresh Grocery Store",
        currency="INR",
        transaction_timestamp="2026-09-14T14:30:00Z",
        velocity_count_10m=1,
        velocity_amount_10m=150.0,
        transaction_uuid="txn-inlier-1",
        customer_uuid="cust-inlier-1",
    )

    response = service.predict(req)
    assert response.transaction_uuid == "txn-inlier-1"
    assert response.customer_uuid == "cust-inlier-1"
    assert response.risk_level in ["LOW", "MEDIUM"]
    assert response.anomaly_score < 0.65
    assert not response.is_anomaly


def test_aml_service_high_risk_anomaly():
    service = AmlModelService.get_instance()

    # Suspicious late-night large transfer to crypto mixer with rapid velocity burst
    req = AmlPredictRequest(
        amount=120000.0,
        transaction_type="TRANSFER",
        merchant="Offshore Crypto Mixer Exchange",
        currency="INR",
        transaction_timestamp="2026-09-14T02:45:00Z",
        velocity_count_10m=7,
        velocity_amount_10m=350000.0,
        transaction_uuid="txn-outlier-9",
        customer_uuid="cust-outlier-9",
    )

    response = service.predict(req)
    assert response.transaction_uuid == "txn-outlier-9"
    assert response.customer_uuid == "cust-outlier-9"
    assert response.risk_level == "HIGH"
    assert response.anomaly_score >= 0.60
    assert response.is_anomaly
    assert len(response.reasons) > 0


def test_fastapi_aml_predict_endpoint():
    payload = {
        "amount": 49000.0,
        "transaction_type": "TRANSFER",
        "merchant": "Unregulated Offshore Casino",
        "currency": "INR",
        "transaction_timestamp": "2026-09-14T03:15:00Z",
        "velocity_count_10m": 5,
        "velocity_amount_10m": 150000.0,
        "transaction_uuid": "txn-api-test",
        "customer_uuid": "cust-api-test"
    }

    response = client.post("/api/v1/aml/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["transaction_uuid"] == "txn-api-test"
    assert "anomaly_score" in data
    assert "risk_level" in data
    assert isinstance(data["reasons"], list)
    assert "timestamp" in data


def test_fastapi_aml_model_info_endpoint():
    response = client.get("/api/v1/aml/model-info")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "LOADED"
    assert "Isolation Forest" in data["algorithm"]
    assert "contamination" in data


def test_unified_model_info_endpoint():
    response = client.get("/model-info")
    assert response.status_code == 200
    data = response.json()
    assert "credit_model" in data
    assert "aml_model" in data
    assert data["credit_model"]["status"] == "LOADED"
    assert data["aml_model"]["status"] == "LOADED"
