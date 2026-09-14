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

from sklearn.metrics import (
    accuracy_score,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split
from xgboost import XGBClassifier

from ml.generate_credit_dataset import generate_credit_dataset, DEFAULT_OUTPUT_PATH


MODELS_DIR = Path(__file__).resolve().parent.parent / "trained_models"
MODELS_DIR.mkdir(parents=True, exist_ok=True)
MODEL_ARTIFACT_PATH = MODELS_DIR / "credit_xgboost_v1.joblib"

FEATURE_COLUMNS = [
    "income",
    "employment_years",
    "loan_amount",
    "existing_loans",
    "credit_score",
    "debt_to_income_ratio",
]
TARGET_COLUMN = "default_label"
MODEL_VERSION = "credit_xgboost_v1.0"


def train_and_save_credit_model(
    data_path: Path = DEFAULT_OUTPUT_PATH,
    model_output_path: Path = MODEL_ARTIFACT_PATH,
    n_samples: int = 15000,
    random_state: int = 42,
) -> dict:
    """
    Trains XGBoost on credit data, evaluates metrics, and saves model bundle.
    """
    if not data_path.exists():
        print(f"Data file {data_path} not found. Generating {n_samples} samples...")
        df = generate_credit_dataset(n_samples=n_samples, random_state=random_state, output_path=data_path)
    else:
        df = pd.read_csv(data_path)

    X = df[FEATURE_COLUMNS]
    y = df[TARGET_COLUMN]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=random_state, stratify=y
    )

    print(f"Training XGBoost on {len(X_train)} samples, testing on {len(X_test)} samples...")

    # Calculate scale_pos_weight to handle class imbalance if any
    pos_count = np.sum(y_train == 1)
    neg_count = np.sum(y_train == 0)
    scale_pos = (neg_count / max(pos_count, 1)) if pos_count > 0 else 1.0

    model = XGBClassifier(
        n_estimators=150,
        max_depth=4,
        learning_rate=0.08,
        subsample=0.85,
        colsample_bytree=0.85,
        scale_pos_weight=min(scale_pos, 3.0),
        eval_metric="logloss",
        random_state=random_state,
    )

    model.fit(X_train, y_train)

    y_pred_proba = model.predict_proba(X_test)[:, 1]
    y_pred = (y_pred_proba >= 0.50).astype(int)

    roc_auc = float(roc_auc_score(y_test, y_pred_proba))
    accuracy = float(accuracy_score(y_test, y_pred))
    precision = float(precision_score(y_test, y_pred, zero_division=0))
    recall = float(recall_score(y_test, y_pred, zero_division=0))
    f1 = float(f1_score(y_test, y_pred, zero_division=0))

    feature_importances = {
        feat: float(imp)
        for feat, imp in zip(FEATURE_COLUMNS, model.feature_importances_)
    }

    metadata = {
        "model_version": MODEL_VERSION,
        "algorithm": "XGBoost Classifier",
        "trained_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "feature_columns": FEATURE_COLUMNS,
        "target_column": TARGET_COLUMN,
        "metrics": {
            "roc_auc": round(roc_auc, 4),
            "accuracy": round(accuracy, 4),
            "precision": round(precision, 4),
            "recall": round(recall, 4),
            "f1_score": round(f1, 4),
        },
        "feature_importances": feature_importances,
        "decision_thresholds": {
            "approved_below": 0.30,
            "manual_review_below": 0.60,
            "rejected_above": 0.60,
        },
    }

    bundle = {
        "model": model,
        "metadata": metadata,
    }

    joblib.dump(bundle, model_output_path)
    print(f"Model saved to {model_output_path}")
    print(f"Evaluation Metrics: ROC-AUC={roc_auc:.4f}, Accuracy={accuracy:.4f}, F1={f1:.4f}")
    return metadata


if __name__ == "__main__":
    train_and_save_credit_model()
