export interface ModelMetrics {
  roc_auc: number;
  accuracy: number;
  precision: number;
  recall: number;
  f1_score: number;
}

export interface CreditModelInfo {
  status: string;
  model_version: string;
  algorithm: string;
  metrics: ModelMetrics;
  feature_importances: Record<string, number>;
  trained_at: string;
}

export interface AmlModelInfo {
  status: string;
  model_version: string;
  algorithm: string;
  contamination: number;
  metrics: ModelMetrics;
  score_thresholds: {
    low_risk_below: number;
    medium_risk_below: number;
    high_risk_above: number;
    alert_trigger: number;
  };
  trained_at: string;
}

export interface ModelInfoResponse {
  credit_model: CreditModelInfo;
  aml_model: AmlModelInfo;
}

export interface ServiceHealth {
  status: string;
  service?: string;
  version?: string;
  components?: Record<string, { status: string; details?: any }>;
}
