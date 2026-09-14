"""
FinVigil Central - Credit Underwriting API Router
"""

from fastapi import APIRouter, HTTPException, status
from app.schemas.credit import CreditPredictionRequest, CreditPredictionResponse
from app.services.credit_service import CreditModelService

router = APIRouter(prefix="/api/v1/credit", tags=["Credit Underwriting ML"])


@router.post("/predict", response_model=CreditPredictionResponse, status_code=status.HTTP_200_OK)
def predict_credit_risk(request: CreditPredictionRequest):
    """
    Evaluates loan applicant features against XGBoost underwriting model
    and outputs default risk probability, risk category, and automated decision.
    """
    try:
        service = CreditModelService.get_instance()
        return service.predict(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Credit model inference error: {str(e)}"
        )


@router.get("/model-info", status_code=status.HTTP_200_OK)
def get_credit_model_info():
    """
    Returns metadata, training metrics, and feature importances for Credit XGBoost model.
    """
    service = CreditModelService.get_instance()
    return service.get_info()
