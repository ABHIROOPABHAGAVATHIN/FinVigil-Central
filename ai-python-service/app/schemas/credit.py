"""
FinVigil Central - Credit Risk Pydantic Schemas
"""

from pydantic import BaseModel, Field
from typing import Optional, Dict, Any


class CreditPredictionRequest(BaseModel):
    income: float = Field(..., gt=0, description="Annual/monthly income in base currency")
    employment_years: int = Field(..., ge=0, le=70, description="Years in relevant employment")
    loan_amount: float = Field(..., gt=0, description="Requested principal amount")
    existing_loans: int = Field(..., ge=0, le=50, description="Number of currently active open credit lines")
    credit_score: int = Field(..., ge=300, le=850, description="Credit bureau score (300-850)")
    debt_to_income_ratio: float = Field(..., ge=0.0, le=2.0, description="Monthly debt obligations / income (0.0 - 2.0)")
    application_uuid: Optional[str] = Field(None, description="Optional credit application UUID")
    customer_uuid: Optional[str] = Field(None, description="Optional customer UUID")

    model_config = {
        "json_schema_extra": {
            "example": {
                "income": 85000.0,
                "employment_years": 5,
                "loan_amount": 25000.0,
                "existing_loans": 2,
                "credit_score": 720,
                "debt_to_income_ratio": 0.28,
                "application_uuid": "app-12345",
                "customer_uuid": "cust-67890",
            }
        }
    }


class CreditPredictionResponse(BaseModel):
    decision_uuid: str
    application_uuid: Optional[str] = None
    customer_uuid: Optional[str] = None
    risk_score: float = Field(..., description="Estimated default risk probability (0.00 - 1.00)")
    risk_level: str = Field(..., description="LOW, MEDIUM, HIGH")
    decision: str = Field(..., description="APPROVE, REVIEW, REJECT")
    model_version: str
    timestamp: str
    feature_contributions: Optional[Dict[str, float]] = None

