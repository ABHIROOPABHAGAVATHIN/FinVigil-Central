import { apiClient } from './client';
import {
  AlertStatus,
  AlertStatusUpdateRequest,
  AmlAlertResponse,
  RuleDefinitionResponse,
  RuleEvaluationRequest,
  RuleEvaluationResponse,
} from '../types/aml';
import { RiskLevel } from '../types/customer';

export const amlApi = {
  // GET /api/aml/alerts?status=&riskLevel= (verified existing in AmlAlertController.java)
  getAllAlerts: async (status?: AlertStatus, riskLevel?: RiskLevel): Promise<AmlAlertResponse[]> => {
    const params: Record<string, string> = {};
    if (status) params.status = status;
    if (riskLevel) params.riskLevel = riskLevel;
    const res = await apiClient.get<AmlAlertResponse[]>('/api/aml/alerts', { params });
    return res.data;
  },

  getAlertByUuid: async (alertUuid: string): Promise<AmlAlertResponse> => {
    const res = await apiClient.get<AmlAlertResponse>(`/api/aml/alerts/${alertUuid}`);
    return res.data;
  },

  getAlertsByCustomer: async (customerUuid: string): Promise<AmlAlertResponse[]> => {
    const res = await apiClient.get<AmlAlertResponse[]>(`/api/aml/alerts/customer/${customerUuid}`);
    return res.data;
  },

  updateAlertStatus: async (alertUuid: string, data: AlertStatusUpdateRequest): Promise<AmlAlertResponse> => {
    const res = await apiClient.patch<AmlAlertResponse>(`/api/aml/alerts/${alertUuid}/status`, data);
    return res.data;
  },

  getActiveRules: async (): Promise<RuleDefinitionResponse[]> => {
    const res = await apiClient.get<RuleDefinitionResponse[]>('/api/aml/rules');
    return res.data;
  },

  evaluateTransaction: async (data: RuleEvaluationRequest): Promise<RuleEvaluationResponse> => {
    const res = await apiClient.post<RuleEvaluationResponse>('/api/aml/rules/evaluate', data);
    return res.data;
  },
};
