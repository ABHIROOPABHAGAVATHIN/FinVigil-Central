import { apiClient } from './client';
import { CreditApplicationResponse, CreditApplyRequest } from '../types/credit';

export const creditApi = {
  applyForCredit: async (data: CreditApplyRequest): Promise<CreditApplicationResponse> => {
    // 202 Accepted, async underwriting
    const res = await apiClient.post<CreditApplicationResponse>('/api/credit/apply', data);
    return res.data;
  },

  // Singular path verified against CreditController.java: /api/credit/application/{id}
  getApplicationByIdOrUuid: async (idOrUuid: string): Promise<CreditApplicationResponse> => {
    const res = await apiClient.get<CreditApplicationResponse>(`/api/credit/application/${idOrUuid}`);
    return res.data;
  },

  getApplicationsByCustomer: async (customerIdOrUuid: string): Promise<CreditApplicationResponse[]> => {
    const res = await apiClient.get<CreditApplicationResponse[]>(`/api/credit/customer/${customerIdOrUuid}`);
    return res.data;
  },
};
