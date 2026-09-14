"""
FinVigil Central - AML Isolation Forest Model Training
Trains an Isolation Forest anomaly detector on transaction monitoring datasets,
calibrates continuous anomaly scores, evaluates detection performance, and persists joblib bundle.
"""

import datetime
from pathlib import Path
import sys
import joblib
import numpy as np
import pandas as pd

# Add service root directory to sys.path
SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from sklearn.ensemble import IsolationForest
from sklearn.metrics import (
    accuracy_score,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split

from ml.generate_aml_dataset import (
    generate_aml_dataset,
    DEFAULT_AML_OUTPUT_PATH,
    AML_FEATURE_COLUMNS,
    TARGET_COLUMN,
)

MODELS_DIR = Path(__file__).resolve().parent.parent / "trained_models"
MODELS_DIR.mkdir(parents=True, exist_ok=True)
AML_MODEL_ARTIFACT_PATH = MODELS_DIR / "aml_isolation_forest_v1.joblib"
MODEL_VERSION = "aml_isolation_forest_v1.0"


def calibrate_anomaly_score(decision_scores: np.ndarray, alpha: float = 12.0) -> np.ndarray:
    """
    Transforms raw Isolation Forest decision_function outputs into a smooth [0.0, 1.0] anomaly score.
    decision_function > 0 is inlier (normal), < 0 is outlier (anomalous).
    Lower decision values yield higher anomaly scores closer to 1.0.
    """
    # Sigmoid inversion: score = 1 / (1 + exp(alpha * decision_score))
    scores = 1.0 / (1.0 + np.exp(np.clip(alpha * decision_scores, -20.0, 20.0)))
    return np.round(scores, 4)


def train_and_save_aml_model(
    data_path: Path = DEFAULT_AML_OUTPUT_PATH,
    model_output_path: Path = AML_MODEL_ARTIFACT_PATH,
    n_samples: int = 15000,
    contamination: float = 0.05,
    random_state: int = 42,
) -> dict:
    """
    Trains Isolation Forest on AML feature matrix, evaluates metrics, and saves model bundle.
    """
    if not data_path.exists():
        print(f"AML data file {data_path} not found. Generating {n_samples} samples...")
        df = generate_aml_dataset(n_samples=n_samples, random_state=random_state, output_path=data_path)
    else:
        df = pd.read_csv(data_path)

    X = df[AML_FEATURE_COLUMNS]
    y = df[TARGET_COLUMN]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=random_state, stratify=y
    )

    print(f"Training Isolation Forest on {len(X_train)} samples, testing on {len(X_test)} samples...")

    model = IsolationForest(
        n_estimators=150,
        max_samples="auto",
        contamination=contamination,
        random_state=random_state,
        n_jobs=-1,
    )

    model.fit(X_train)

    # In Scikit-Learn: decision_function returns signed distance (lower = anomalous)
    test_decisions = model.decision_function(X_test)
    test_anomaly_scores = calibrate_anomaly_score(test_decisions)

    # Binary predictions (threshold at calibrated 0.50 score)
    y_pred = (test_anomaly_scores >= 0.50).astype(int)

    roc_auc = float(roc_auc_score(y_test, test_anomaly_scores))
    accuracy = float(accuracy_score(y_test, y_pred))
    precision = float(precision_score(y_test, y_pred, zero_division=0))
    recall = float(recall_score(y_test, y_pred, zero_division=0))
    f1 = float(f1_score(y_test, y_pred, zero_division=0))

    metadata = {
        "model_version": MODEL_VERSION,
        "algorithm": "Isolation Forest Anomaly Detector",
        "trained_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "feature_columns": AML_FEATURE_COLUMNS,
        "contamination": contamination,
        "metrics": {
            "roc_auc": round(roc_auc, 4),
            "accuracy": round(accuracy, 4),
            "precision": round(precision, 4),
            "recall": round(recall, 4),
            "f1_score": round(f1, 4),
        },
        "score_thresholds": {
            "low_risk_below": 0.40,
            "medium_risk_below": 0.65,
            "high_risk_above": 0.65,
            "alert_trigger": 0.65,
        },
    }

    bundle = {
        "model": model,
        "metadata": metadata,
        "feature_columns": AML_FEATURE_COLUMNS,
    }

    joblib.dump(bundle, model_output_path)
    print(f"Isolation Forest model saved to {model_output_path}")
    print(f"Evaluation Metrics: ROC-AUC={roc_auc:.4f}, Precision={precision:.4f}, Recall={recall:.4f}, F1={f1:.4f}")
    return metadata


if __name__ == "__main__":
    train_and_save_aml_model()
