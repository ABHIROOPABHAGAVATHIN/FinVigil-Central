import { apiClient } from './client';
import { mlClient } from './mlClient';
import { ModelInfoResponse, ServiceHealth } from '../types/ml';

export const systemApi = {
  getSpringHealth: async (): Promise<ServiceHealth> => {
    const res = await apiClient.get<ServiceHealth>('/actuator/health');
    return res.data;
  },

  getPythonHealth: async (): Promise<ServiceHealth> => {
    const res = await mlClient.get<ServiceHealth>('/health');
    return res.data;
  },

  getModelInfo: async (): Promise<ModelInfoResponse> => {
    const res = await mlClient.get<ModelInfoResponse>('/model-info');
    return res.data;
  },
};
