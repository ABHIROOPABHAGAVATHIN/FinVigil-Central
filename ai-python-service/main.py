"""
FinVigil Central - AI/ML Risk & AML Service
Provides XGBoost Credit Underwriting and Isolation Forest AML anomaly detection endpoints.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os
from app.api.credit_router import router as credit_router
from app.api.aml_router import router as aml_router
from app.services.credit_service import CreditModelService
from app.services.aml_service import AmlModelService

app = FastAPI(
    title="FinVigil Central - AI Service",
    description="Unified Credit Risk & Real-Time AML Intelligence ML Service",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(credit_router)
app.include_router(aml_router)


@app.get("/health", tags=["Health"])
def health_check():
    return {
        "status": "UP",
        "service": "finvigil-ai-service",
        "version": "1.0.0"
    }


@app.get("/model-info", tags=["Models"])
def model_info():
    try:
        credit_info = CreditModelService.get_instance().get_info()
    except Exception as e:
        credit_info = {"status": "ERROR", "error": str(e)}

    try:
        aml_info = AmlModelService.get_instance().get_info()
    except Exception as e:
        aml_info = {"status": "ERROR", "error": str(e)}

    return {
        "credit_model": credit_info,
        "aml_model": aml_info,
    }


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("AI_SERVICE_PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)

