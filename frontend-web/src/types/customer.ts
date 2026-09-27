export type CustomerStatus = 'ACTIVE' | 'SUSPENDED' | 'UNDER_INVESTIGATION';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type CreditDecisionType = 'APPROVE' | 'REVIEW' | 'REJECT';

export interface CustomerResponse {
  customerUuid: string;
  name: string;
  email: string;
  phone: string;
  status: CustomerStatus;
  createdAt?: string;
}

export interface CustomerCreateRequest {
  name: string;
  email: string;
  phone: string;
}

export interface ProfileCreditData {
  hasApplication: boolean;
  applicationUuid?: string;
  applicationStatus?: 'PENDING' | 'APPROVED' | 'REVIEW' | 'REJECTED';
  loanAmount?: number;
  creditScore?: number;
  latestRiskScore?: number;
  riskLevel?: RiskLevel;
  decision?: CreditDecisionType;
  status?: string;
}

export interface ProfileTransactionItem {
  transactionUuid: string;
  amount: number;
  transactionType: 'TRANSFER' | 'PURCHASE' | 'WITHDRAWAL' | 'DEPOSIT';
  merchant: string;
  status: 'COMPLETED' | 'PENDING' | 'FLAGGED' | 'REJECTED';
  timestamp: string;
}

export interface ProfileAmlData {
  openAlerts: number;
  latestAlertUuid?: string;
  ruleScore?: number;
  anomalyScore?: number;
  latestHybridScore?: number;
  riskLevel?: RiskLevel;
  latestStatus?: 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'FALSE_POSITIVE';
}

export interface ProfileRiskAggregationData {
  overallRiskLevel: RiskLevel;
  compositeRiskScore: number;
  accountStanding: CustomerStatus;
  recommendedAction: CreditDecisionType;
  contributingFactors: string[];
  currentVelocityCount: number;
  currentVelocityAmount: number;
}

export interface CustomerProfileResponse {
  customer: CustomerResponse;
  credit: ProfileCreditData;
  transactions: ProfileTransactionItem[];
  aml: ProfileAmlData;
  riskAggregation: ProfileRiskAggregationData;
}
