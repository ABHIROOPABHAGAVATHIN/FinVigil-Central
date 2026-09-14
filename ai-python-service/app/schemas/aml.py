"""
FinVigil Central - AML Machine Learning Pydantic Schemas
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any


class AmlPredictRequest(BaseModel):
    amount: float = Field(..., gt=0, description="Transaction monetary amount")
    transaction_type: str = Field(..., description="TRANSFER, PURCHASE, WITHDRAWAL, DEPOSIT")
    merchant: Optional[str] = Field("", description="Counterparty or merchant name")
    currency: Optional[str] = Field("INR", description="3-letter currency code")
    transaction_timestamp: Optional[str] = Field(None, description="ISO-8601 transaction timestamp")
    velocity_count_10m: Optional[int] = Field(1, ge=1, description="Transaction count in sliding 10-minute window")
    velocity_amount_10m: Optional[float] = Field(None, ge=0, description="Cumulative amount in sliding 10-minute window")
    transaction_uuid: Optional[str] = Field(None, description="Transaction UUID")
    customer_uuid: Optional[str] = Field(None, description="Customer UUID")

    model_config = {
        "json_schema_extra": {
            "example": {
                "amount": 48500.0,
                "transaction_type": "TRANSFER",
                "merchant": "Offshore Crypto Exchange",
                "currency": "INR",
                "velocity_count_10m": 4,
                "velocity_amount_10m": 120000.0,
                "transaction_uuid": "txn-aml-12345",
                "customer_uuid": "cust-aml-67890",
            }
        }
    }


class AmlPredictResponse(BaseModel):
    transaction_uuid: Optional[str] = None
    customer_uuid: Optional[str] = None
    anomaly_score: float = Field(..., description="Calibrated continuous anomaly score (0.00 - 1.00)")
    is_anomaly: bool = Field(..., description="True if anomaly score meets or exceeds detection threshold")
    risk_level: str = Field(..., description="LOW, MEDIUM, HIGH")
    reasons: List[str] = Field(default_factory=list, description="Explanatory indicators triggered by the model")
    model_version: str
    timestamp: str
    feature_values: Optional[Dict[str, Any]] = None
