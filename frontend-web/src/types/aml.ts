import { RiskLevel } from './customer';

export type AlertStatus = 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'FALSE_POSITIVE';

export interface AmlAlertResponse {
  alertUuid: string;
  customerUuid?: string;
  transactionUuid?: string;
  ruleScore?: number;
  anomalyScore?: number;
  mlScore?: number;
  hybridScore?: number;
  riskLevel: RiskLevel;
  status: AlertStatus;
  reasons?: string[];
  resolutionNotes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface AlertStatusUpdateRequest {
  status: AlertStatus;
  resolutionNotes?: string;
}

export interface RuleDefinitionResponse {
  ruleName: string;
  ruleType: string;
  description: string;
  baseWeight: number;
}

export interface RuleEvaluationRequest {
  transactionUuid?: string;
  customerUuid?: string;
  amount: number;
  transactionType: string;
  merchant: string;
  currency?: string;
  transactionTimestamp?: string;
  velocityCount?: number;
  velocityAmount?: number;
}

export interface RuleEvaluationResponse {
  triggered: boolean;
  totalRuleScore: number;
  highestSeverity: string;
  triggeredRules: string[];
}
