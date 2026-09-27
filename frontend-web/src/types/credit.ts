import { CreditDecisionType, RiskLevel } from './customer';

export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REVIEW' | 'REJECTED';

export interface CreditDecisionResponse {
  decisionUuid: string;
  applicationUuid: string;
  riskScore: number;
  riskLevel: RiskLevel;
  decision: CreditDecisionType;
  modelVersion: string;
  createdAt: string;
}

export interface CreditApplicationResponse {
  applicationUuid: string;
  customerUuid: string;
  income: number;
  employmentYears: number;
  loanAmount: number;
  existingLoans: number;
  creditScore: number;
  debtToIncomeRatio: number;
  applicationStatus: ApplicationStatus;
  createdAt: string;
  decision?: CreditDecisionResponse;
}

export interface CreditApplyRequest {
  customerId: string;
  income: number;
  employmentYears: number;
  loanAmount: number;
  existingLoans: number;
  creditScore: number;
  debtToIncomeRatio: number;
}
