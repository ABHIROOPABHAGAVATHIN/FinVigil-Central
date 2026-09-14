"""
FinVigil Central - AML Anomaly Detection API Router
"""

from fastapi import APIRouter, HTTPException, status
from app.schemas.aml import AmlPredictRequest, AmlPredictResponse
from app.services.aml_service import AmlModelService

router = APIRouter(prefix="/api/v1/aml", tags=["AML Anomaly Detection ML"])


@router.post("/predict", response_model=AmlPredictResponse, status_code=status.HTTP_200_OK)
def predict_aml_anomaly(request: AmlPredictRequest):
    """
    Evaluates financial transaction parameters against Isolation Forest anomaly detection model
    and outputs calibrated continuous anomaly score, risk level, and explanatory factors.
    """
    try:
        service = AmlModelService.get_instance()
        return service.predict(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"AML model inference error: {str(e)}"
        )


@router.get("/model-info", status_code=status.HTTP_200_OK)
def get_aml_model_info():
    """
    Returns metadata, training metrics, and configuration for AML Isolation Forest model.
    """
    service = AmlModelService.get_instance()
    return service.get_info()
