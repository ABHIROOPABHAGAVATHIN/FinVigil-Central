import { apiClient } from './client';
import { CustomerCreateRequest, CustomerProfileResponse, CustomerResponse } from '../types/customer';

export const customersApi = {
  getByIdOrUuid: async (idOrUuid: string): Promise<CustomerResponse> => {
    const res = await apiClient.get<CustomerResponse>(`/api/customers/${idOrUuid}`);
    return res.data;
  },

  getProfile: async (idOrUuid: string): Promise<CustomerProfileResponse> => {
    const res = await apiClient.get<CustomerProfileResponse>(`/api/customers/${idOrUuid}/profile`);
    return res.data;
  },

  createCustomer: async (data: CustomerCreateRequest): Promise<CustomerResponse> => {
    const res = await apiClient.post<CustomerResponse>('/api/customers', data);
    return res.data;
  },
};
