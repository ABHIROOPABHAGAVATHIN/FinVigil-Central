import { apiClient } from './client';
import { TransactionCreateRequest, TransactionResponse } from '../types/transaction';

export const transactionsApi = {
  processTransaction: async (data: TransactionCreateRequest): Promise<TransactionResponse> => {
    const res = await apiClient.post<TransactionResponse>('/api/transactions', data);
    return res.data;
  },

  getTransactionByIdOrUuid: async (idOrUuid: string): Promise<TransactionResponse> => {
    const res = await apiClient.get<TransactionResponse>(`/api/transactions/${idOrUuid}`);
    return res.data;
  },

  getTransactionsByCustomer: async (customerIdOrUuid: string): Promise<TransactionResponse[]> => {
    const res = await apiClient.get<TransactionResponse[]>(`/api/transactions/customer/${customerIdOrUuid}`);
    return res.data;
  },
};
