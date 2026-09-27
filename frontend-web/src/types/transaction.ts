export type TransactionType = 'TRANSFER' | 'PURCHASE' | 'WITHDRAWAL' | 'DEPOSIT';
export type TransactionStatus = 'COMPLETED' | 'PENDING' | 'FLAGGED' | 'REJECTED';

export interface TransactionResponse {
  transactionUuid: string;
  customerUuid: string;
  amount: number;
  transactionType: TransactionType;
  merchant: string;
  currency: string;
  status: TransactionStatus;
  transactionTimestamp: string;
  createdAt: string;
}

export interface TransactionCreateRequest {
  customerId: string;
  amount: number;
  transactionType: TransactionType;
  merchant: string;
  currency?: string;
  transactionTimestamp?: string;
}

export interface VelocityStats {
  count: number;
  totalAmount: number;
}
