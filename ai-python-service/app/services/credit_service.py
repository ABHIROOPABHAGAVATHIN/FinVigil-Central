"""
FinVigil Central - Credit Risk Prediction Engine
Loads XGBoost model artifact and performs real-time credit underwriting risk scoring.
"""

import datetime
from pathlib import Path
from typing import Dict, Any, Optional
import uuid
import joblib
import pandas as pd

from app.schemas.credit import CreditPredictionRequest, CreditPredictionResponse

DEFAULT_MODEL_PATH = Path(__file__).resolve().parent.parent.parent / "trained_models" / "credit_xgboost_v1.joblib"


class CreditModelService:
    _instance: Optional["CreditModelService"] = None

    def __init__(self, model_path: Path = DEFAULT_MODEL_PATH):
        self.model_path = Path(model_path)
        self.model = None
        self.metadata = {}
        self._load_or_train()

    @classmethod
    def get_instance(cls, model_path: Path = DEFAULT_MODEL_PATH) -> "CreditModelService":
        if cls._instance is None:
            cls._instance = CreditModelService(model_path)
        return cls._instance

    def _load_or_train(self):
        if not self.model_path.exists():
            print(f"[CreditModelService] Model file not found at {self.model_path}. Auto-training...")
            from ml.train_credit_model import train_and_save_credit_model
            self.metadata = train_and_save_credit_model(model_output_path=self.model_path)
        
        bundle = joblib.load(self.model_path)
        if isinstance(bundle, dict) and "model" in bundle:
            self.model = bundle["model"]
            self.metadata = bundle.get("metadata", {})
        else:
            self.model = bundle
            self.metadata = {"model_version": "credit_xgboost_v1.0"}
        print(f"[CreditModelService] Successfully loaded credit XGBoost model: {self.metadata.get('model_version')}")

    def predict(self, req: CreditPredictionRequest) -> CreditPredictionResponse:
        data = req.model_dump()
        result_dict = self.predict_dict(data)
        return CreditPredictionResponse(**result_dict)

    def predict_dict(self, data: Dict[str, Any]) -> Dict[str, Any]:
        features = ["income", "employment_years", "loan_amount", "existing_loans", "credit_score", "debt_to_income_ratio"]
        
        # Support camelCase or snake_case
        feature_values = {
            "income": float(data.get("income", 0.0)),
            "employment_years": int(data.get("employment_years", data.get("employmentYears", 0))),
            "loan_amount": float(data.get("loan_amount", data.get("loanAmount", 0.0))),
            "existing_loans": int(data.get("existing_loans", data.get("existingLoans", 0))),
            "credit_score": int(data.get("credit_score", data.get("creditScore", 650))),
            "debt_to_income_ratio": float(data.get("debt_to_income_ratio", data.get("debtToIncomeRatio", 0.30))),
        }

        df = pd.DataFrame([feature_values])[features]
        
        # Predict probability of default (class 1)
        prob_default = float(self.model.predict_proba(df)[0, 1])
        risk_score = round(prob_default, 4)

        # Determine decision & risk level based on calibrated risk bands
        if risk_score < 0.30:
            decision = "APPROVE"
            risk_level = "LOW"
        elif risk_score <= 0.60:
            decision = "REVIEW"
            risk_level = "MEDIUM"
        else:
            decision = "REJECT"
            risk_level = "HIGH"

        decision_uuid = str(uuid.uuid4())
        application_uuid = data.get("application_uuid", data.get("applicationUuid"))
        customer_uuid = data.get("customer_uuid", data.get("customerUuid"))
        model_version = self.metadata.get("model_version", "credit_xgboost_v1.0")
        iso_timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat()

        # Feature importances / contribution indicators
        importances = self.metadata.get("feature_importances", {})

        return {
            "decision_uuid": decision_uuid,
            "application_uuid": application_uuid,
            "customer_uuid": customer_uuid,
            "risk_score": risk_score,
            "risk_level": risk_level,
            "decision": decision,
            "model_version": model_version,
            "timestamp": iso_timestamp,
            "feature_contributions": importances,
            # Java event compatible aliases
            "decisionUuid": decision_uuid,
            "applicationUuid": application_uuid,
            "customerUuid": customer_uuid,
            "riskScore": risk_score,
            "riskLevel": risk_level,
            "modelVersion": model_version,
        }


    def get_info(self) -> Dict[str, Any]:
        return {
            "status": "LOADED" if self.model is not None else "UNINITIALIZED",
            "model_version": self.metadata.get("model_version", "unknown"),
            "algorithm": self.metadata.get("algorithm", "XGBoost Classifier"),
            "metrics": self.metadata.get("metrics", {}),
            "feature_importances": self.metadata.get("feature_importances", {}),
            "trained_at": self.metadata.get("trained_at", "N/A"),
        }
