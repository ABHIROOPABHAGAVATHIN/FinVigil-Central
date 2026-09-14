"""
FinVigil Central - Real-Time AML Anomaly Detection Engine
Loads Isolation Forest model artifact and performs real-time ML-based anomaly scoring.
"""

import datetime
from pathlib import Path
from typing import Dict, Any, Optional, List
import joblib
import numpy as np
import pandas as pd

from app.schemas.aml import AmlPredictRequest, AmlPredictResponse
from ml.generate_aml_dataset import AML_FEATURE_COLUMNS, TRANSACTION_TYPE_MAP
from ml.train_aml_model import calibrate_anomaly_score

DEFAULT_AML_MODEL_PATH = Path(__file__).resolve().parent.parent.parent / "trained_models" / "aml_isolation_forest_v1.joblib"

HIGH_RISK_KEYWORDS = [
    "casino", "gambling", "betting", "crypto", "bitcoin", "darknet",
    "mixer", "offshore", "forex", "binary", "unregulated"
]


class AmlModelService:
    _instance: Optional["AmlModelService"] = None

    def __init__(self, model_path: Path = DEFAULT_AML_MODEL_PATH):
        self.model_path = Path(model_path)
        self.model = None
        self.metadata = {}
        self._load_or_train()

    @classmethod
    def get_instance(cls, model_path: Path = DEFAULT_AML_MODEL_PATH) -> "AmlModelService":
        if cls._instance is None:
            cls._instance = AmlModelService(model_path)
        return cls._instance

    def _load_or_train(self):
        if not self.model_path.exists():
            print(f"[AmlModelService] Model file not found at {self.model_path}. Auto-training...")
            from ml.train_aml_model import train_and_save_aml_model
            self.metadata = train_and_save_aml_model(model_output_path=self.model_path)

        bundle = joblib.load(self.model_path)
        if isinstance(bundle, dict) and "model" in bundle:
            self.model = bundle["model"]
            self.metadata = bundle.get("metadata", {})
        else:
            self.model = bundle
            self.metadata = {"model_version": "aml_isolation_forest_v1.0"}
        print(f"[AmlModelService] Successfully loaded AML Isolation Forest model: {self.metadata.get('model_version')}")

    def predict(self, req: AmlPredictRequest) -> AmlPredictResponse:
        data = req.model_dump()
        result_dict = self.predict_dict(data)
        return AmlPredictResponse(**result_dict)

    def predict_dict(self, data: Dict[str, Any]) -> Dict[str, Any]:
        amount = float(data.get("amount", 0.0))

        # Handle transaction type (enum string or int code)
        txn_type_raw = str(data.get("transaction_type", data.get("transactionType", "PURCHASE"))).upper()
        txn_type_code = TRANSACTION_TYPE_MAP.get(txn_type_raw, 1)

        # Parse timestamp for temporal features
        timestamp_raw = data.get("transaction_timestamp", data.get("transactionTimestamp"))
        if timestamp_raw:
            try:
                # Handle ISO timestamps
                dt = datetime.datetime.fromisoformat(str(timestamp_raw).replace("Z", "+00:00"))
                hour = dt.hour
                day_of_week = dt.weekday()
            except Exception:
                now = datetime.datetime.now(datetime.timezone.utc)
                hour, day_of_week = now.hour, now.weekday()
        else:
            now = datetime.datetime.now(datetime.timezone.utc)
            hour, day_of_week = now.hour, now.weekday()

        velocity_count = int(data.get("velocity_count_10m", data.get("velocityCount1m", data.get("velocityCount", 1))))
        velocity_count = max(1, velocity_count)

        raw_velocity_amount = data.get("velocity_amount_10m", data.get("velocityAmount1m", data.get("velocityAmount")))
        if raw_velocity_amount is not None:
            velocity_amount = float(raw_velocity_amount)
        else:
            velocity_amount = amount * velocity_count

        velocity_amount = max(amount, velocity_amount)
        ratio = round(amount / velocity_amount, 4) if velocity_amount > 0 else 1.0

        # Check high-risk merchant keywords
        merchant = str(data.get("merchant", "")).lower()
        is_high_risk = 1 if any(kw in merchant for kw in HIGH_RISK_KEYWORDS) else 0

        # Construct feature vector
        feature_dict = {
            "amount": amount,
            "transaction_type_code": txn_type_code,
            "hour_of_day": hour,
            "day_of_week": day_of_week,
            "velocity_count_10m": velocity_count,
            "velocity_amount_10m": velocity_amount,
            "amount_to_velocity_ratio": ratio,
            "is_high_risk_merchant": is_high_risk,
        }

        df = pd.DataFrame([feature_dict])[AML_FEATURE_COLUMNS]

        # Model decision function & calibrated anomaly score
        decision = float(self.model.decision_function(df)[0])
        anomaly_score = float(calibrate_anomaly_score(np.array([decision]))[0])

        thresholds = self.metadata.get("score_thresholds", {
            "low_risk_below": 0.40,
            "medium_risk_below": 0.65,
            "high_risk_above": 0.65,
            "alert_trigger": 0.65,
        })

        if anomaly_score >= thresholds.get("high_risk_above", 0.65):
            risk_level = "HIGH"
            is_anomaly = True
        elif anomaly_score >= thresholds.get("low_risk_below", 0.40):
            risk_level = "MEDIUM"
            is_anomaly = (anomaly_score >= thresholds.get("alert_trigger", 0.65))
        else:
            risk_level = "LOW"
            is_anomaly = False

        # Generate explanatory factors for transparency
        reasons: List[str] = []
        if is_high_risk == 1:
            reasons.append(f"Counterparty '{data.get('merchant')}' matched high-risk AML category")
        if amount >= 50000.0:
            reasons.append(f"Transaction amount ({amount:.2f}) represents significant volume outlier")
        elif 45000.0 <= amount < 50000.0:
            reasons.append(f"Amount ({amount:.2f}) in critical structuring corridor below 50,000 threshold")
        if velocity_count >= 5:
            reasons.append(f"High velocity burst: {velocity_count} transactions recorded in sliding window")
        if hour in [0, 1, 2, 3, 4, 23]:
            reasons.append(f"Unusual temporal activity: transaction executed during off-hours ({hour:02d}:00)")
        if is_anomaly and not reasons:
            reasons.append("Multi-dimensional feature deviation flagged by Isolation Forest trees")

        txn_uuid = data.get("transaction_uuid", data.get("transactionUuid"))
        cust_uuid = data.get("customer_uuid", data.get("customerUuid"))
        model_version = self.metadata.get("model_version", "aml_isolation_forest_v1.0")
        iso_timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat()

        return {
            "transaction_uuid": txn_uuid,
            "customer_uuid": cust_uuid,
            "anomaly_score": anomaly_score,
            "is_anomaly": is_anomaly,
            "risk_level": risk_level,
            "reasons": reasons,
            "model_version": model_version,
            "timestamp": iso_timestamp,
            "feature_values": feature_dict,
            # Java event compatible aliases
            "transactionUuid": txn_uuid,
            "customerUuid": cust_uuid,
            "anomalyScore": anomaly_score,
            "isAnomaly": is_anomaly,
            "riskLevel": risk_level,
            "modelVersion": model_version,
        }

    def get_info(self) -> Dict[str, Any]:
        return {
            "status": "LOADED" if self.model is not None else "UNINITIALIZED",
            "model_version": self.metadata.get("model_version", "unknown"),
            "algorithm": self.metadata.get("algorithm", "Isolation Forest"),
            "contamination": self.metadata.get("contamination", 0.05),
            "metrics": self.metadata.get("metrics", {}),
            "score_thresholds": self.metadata.get("score_thresholds", {}),
            "trained_at": self.metadata.get("trained_at", "N/A"),
        }
