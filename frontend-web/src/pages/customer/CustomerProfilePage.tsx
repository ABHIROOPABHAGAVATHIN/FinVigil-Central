import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  User,
  CreditCard,
  ShieldAlert,
  Activity,
  History,
  AlertTriangle,
  FileCheck2,
  PlusCircle,
  ExternalLink,
  Phone,
  Mail,
  RefreshCw,
  Clock,
  ShieldCheck,
} from 'lucide-react';
import { customersApi } from '../../api/customers';
import { amlApi } from '../../api/aml';
import { useAuthStore } from '../../store/authStore';
import { useToastStore } from '../../components/ui/Toast';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { ScoreGauge } from '../../components/common/ScoreGauge';
import { ScoreBreakdownBar } from '../../components/common/ScoreBreakdownBar';
import { VelocityMeter } from '../../components/common/VelocityMeter';
import { Modal } from '../../components/ui/Modal';
import { Select } from '../../components/ui/Select';
import { AlertStatus } from '../../types/aml';
import { formatCurrency, formatDate, formatScore } from '../../lib/utils';

export const CustomerProfilePage: React.FC = () => {
  const { idOrUuid } = useParams<{ idOrUuid: string }>();
  const queryClient = useQueryClient();
  const { addViewedCustomer } = useAuthStore();
  const { showToast } = useToastStore();

  // Status update modal state
  const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
  const [selectedAlertUuid, setSelectedAlertUuid] = useState<string | null>(null);
  const [newStatus, setNewStatus] = useState<AlertStatus>('UNDER_REVIEW');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);

  const {
    data: profile,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ['customerProfile', idOrUuid],
    queryFn: () => customersApi.getProfile(idOrUuid!),
    enabled: !!idOrUuid,
    refetchInterval: 8000, // Poll every 8s for live async decision updates
  });

  // Track customer in session history when loaded
  useEffect(() => {
    if (profile?.customer) {
      addViewedCustomer({
        customerUuid: profile.customer.customerUuid,
        name: profile.customer.name,
      });
    }
  }, [profile, addViewedCustomer]);

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAlertUuid) return;

    setIsUpdating(true);
    try {
      await amlApi.updateAlertStatus(selectedAlertUuid, {
        status: newStatus,
        resolutionNotes,
      });
      showToast({
        type: 'success',
        title: 'Alert Status Updated',
        message: `Alert status updated to ${newStatus}.`,
      });
      setIsUpdateModalOpen(false);
      setResolutionNotes('');
      queryClient.invalidateQueries({ queryKey: ['customerProfile', idOrUuid] });
    } catch (err: any) {
      showToast({
        type: 'error',
        title: 'Update Failed',
        message: err.response?.data?.message || err.message,
      });
    } finally {
      setIsUpdating(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 space-y-4">
        <RefreshCw className="w-8 h-8 text-brand-500 animate-spin" />
        <div className="text-sm text-slate-400">Loading Unified Customer Profile...</div>
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="max-w-2xl mx-auto py-16 text-center space-y-4">
        <div className="w-12 h-12 rounded-full bg-rose-500/10 border border-rose-500/20 text-rose-400 flex items-center justify-center mx-auto">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h2 className="text-lg font-semibold text-slate-100">Customer Profile Not Found</h2>
        <p className="text-xs text-slate-400">
          No customer was found matching ID or UUID <span className="font-mono text-indigo-400">{idOrUuid}</span>.
        </p>
        <div className="pt-2">
          <Link to="/dashboard">
            <Button variant="secondary" size="md">
              Return to Dashboard
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  const { customer, credit, transactions, aml, riskAggregation } = profile;

  // Highlight suspicious merchants
  const HIGH_RISK_KEYWORDS = ['crypto', 'casino', 'bet', 'dark', 'wire', 'offshore', 'royale'];
  const isSuspiciousMerchant = (merchant: string) => {
    const lower = (merchant || '').toLowerCase();
    return HIGH_RISK_KEYWORDS.some((kw) => lower.includes(kw));
  };

  return (
    <div className="space-y-8 max-w-7xl mx-auto pb-12">
      {/* 1. Header: Customer Identity & Flagship Risk Badge */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-background-elevated to-background-card border border-border flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-xl">
        <div className="flex items-start gap-4">
          <div className="w-14 h-14 rounded-2xl bg-brand-500/20 border border-brand-500/40 text-indigo-300 flex items-center justify-center font-bold text-xl shrink-0">
            {customer.name.charAt(0).toUpperCase()}
          </div>
          <div className="space-y-1">
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="text-xl font-bold text-slate-100">{customer.name}</h1>
              <Badge variant="status" status={customer.status} size="sm" />
            </div>
            <div className="flex items-center gap-4 text-xs text-slate-400 flex-wrap">
              <span className="font-mono text-slate-400 flex items-center gap-1">
                UUID: <span className="text-slate-200">{customer.customerUuid}</span>
              </span>
              <span className="flex items-center gap-1">
                <Mail className="w-3.5 h-3.5 text-slate-500" />
                {customer.email}
              </span>
              <span className="flex items-center gap-1">
                <Phone className="w-3.5 h-3.5 text-slate-500" />
                {customer.phone}
              </span>
            </div>
          </div>
        </div>

        {/* Prominent Overall Risk Badge & Actions */}
        <div className="flex items-center gap-4 self-end md:self-center">
          <div className="text-right">
            <div className="text-[10px] uppercase font-mono tracking-wider text-slate-400">
              Aggregated Risk Level
            </div>
            <div className="mt-1 flex items-center justify-end gap-2">
              <Badge variant="risk" riskLevel={riskAggregation?.overallRiskLevel || 'LOW'} size="md" />
              <span className="font-mono text-sm font-bold text-slate-100">
                {formatScore(riskAggregation?.compositeRiskScore, 4)}
              </span>
            </div>
          </div>

          <button
            onClick={() => refetch()}
            title="Refresh profile"
            className="p-2 rounded-lg bg-background-surface hover:bg-background-elevated text-slate-400 hover:text-white border border-border transition-colors"
          >
            <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin text-brand-500' : ''}`} />
          </button>
        </div>
      </div>

      {/* Quick Actions Bar */}
      <div className="flex items-center justify-between gap-4 p-4 rounded-xl bg-background-surface/80 border border-border">
        <div className="flex items-center gap-2 text-xs text-slate-400">
          <Activity className="w-4 h-4 text-cyan-400" />
          <span>Operational Actions for Customer:</span>
        </div>
        <div className="flex items-center gap-3">
          <Link to={`/customers/${customer.customerUuid}/apply-credit`}>
            <Button variant="outline" size="sm" className="border-indigo-500/30 text-indigo-300 hover:bg-indigo-500/10">
              <CreditCard className="w-3.5 h-3.5 mr-1.5" />
              <span>Apply for Credit</span>
            </Button>
          </Link>
          <Link to={`/customers/${customer.customerUuid}/transactions/new`}>
            <Button variant="primary" size="sm">
              <PlusCircle className="w-3.5 h-3.5 mr-1.5" />
              <span>Screen Transaction</span>
            </Button>
          </Link>
        </div>
      </div>

      {/* 2. Top Grid: Deterministic Risk Aggregation & Velocity Monitor */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Risk Aggregation Panel */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <div>
              <CardTitle>
                <ShieldCheck className="w-4 h-4 text-indigo-400" />
                <span>Deterministic Risk Aggregation</span>
              </CardTitle>
              <CardDescription>
                Consolidated formula: 50% AML + 30% Credit Risk + 20% Redis Velocity
              </CardDescription>
            </div>
            <Badge variant="risk" riskLevel={riskAggregation?.overallRiskLevel || 'LOW'} />
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 items-center">
              {/* Radial Score Gauge */}
              <div className="flex justify-center sm:justify-start">
                <ScoreGauge
                  score={riskAggregation?.compositeRiskScore}
                  riskLevel={riskAggregation?.overallRiskLevel}
                  size="md"
                  label="Composite Risk"
                  sublabel="Deterministic Rating"
                />
              </div>

              {/* Status & Standing Details */}
              <div className="sm:col-span-2 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="p-3 rounded-lg bg-background-surface border border-border">
                    <span className="text-[11px] text-slate-400">Account Standing</span>
                    <div className="mt-1">
                      <Badge variant="status" status={riskAggregation?.accountStanding || 'ACTIVE'} size="sm" />
                    </div>
                  </div>

                  <div className="p-3 rounded-lg bg-background-surface border border-border">
                    <span className="text-[11px] text-slate-400">Recommended Action</span>
                    <div className="mt-1">
                      <Badge variant="status" status={riskAggregation?.recommendedAction || 'APPROVE'} size="sm" />
                    </div>
                  </div>
                </div>

                {/* Contributing Factors */}
                <div>
                  <span className="text-xs font-medium text-slate-300 block mb-2">
                    Active Contributing Factors:
                  </span>
                  {riskAggregation?.contributingFactors?.length > 0 ? (
                    <div className="flex flex-wrap gap-2">
                      {riskAggregation.contributingFactors.map((factor, idx) => (
                        <span
                          key={idx}
                          className="px-2.5 py-1 rounded-md text-xs font-mono bg-rose-500/10 text-rose-300 border border-rose-500/20"
                        >
                          {factor}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <span className="text-xs text-slate-500">No elevated risk factors detected.</span>
                  )}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 60s Sliding Velocity Monitor */}
        <Card>
          <CardHeader>
            <CardTitle>
              <Activity className="w-4 h-4 text-cyan-400" />
              <span>Redis Velocity</span>
            </CardTitle>
            <CardDescription>60-second limit check</CardDescription>
          </CardHeader>
          <CardContent className="p-4">
            <VelocityMeter
              count={riskAggregation?.currentVelocityCount || 0}
              totalAmount={riskAggregation?.currentVelocityAmount || 0}
              countLimit={5}
              amountLimit={50000}
            />
          </CardContent>
        </Card>
      </div>

      {/* 3. Middle Grid: Credit Decisioning & AML Compliance Alerts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Credit Underwriting Panel */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <CreditCard className="w-4 h-4 text-indigo-400" />
                <span>Credit Underwriting (XGBoost ML)</span>
              </CardTitle>
              <CardDescription>Machine learning loan eligibility evaluation</CardDescription>
            </div>
            {credit?.hasApplication && (
              <Badge variant="status" status={credit.applicationStatus || 'PENDING'} />
            )}
          </CardHeader>
          <CardContent>
            {!credit?.hasApplication ? (
              <div className="text-center py-8 space-y-3">
                <CreditCard className="w-10 h-10 text-slate-600 mx-auto" />
                <p className="text-xs text-slate-400">No credit applications found for this customer.</p>
                <Link to={`/customers/${customer.customerUuid}/apply-credit`}>
                  <Button variant="outline" size="sm">
                    Submit Credit Application
                  </Button>
                </Link>
              </div>
            ) : (
              <div className="space-y-6">
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                  <div className="p-3 rounded-lg bg-background-surface border border-border">
                    <span className="text-[11px] text-slate-400">Loan Amount</span>
                    <div className="text-sm font-mono font-semibold text-slate-100 mt-1">
                      {formatCurrency(credit.loanAmount)}
                    </div>
                  </div>

                  <div className="p-3 rounded-lg bg-background-surface border border-border">
                    <span className="text-[11px] text-slate-400">Credit Score</span>
                    <div className="text-sm font-mono font-semibold text-slate-100 mt-1">
                      {credit.creditScore || 'N/A'}
                    </div>
                  </div>

                  <div className="p-3 rounded-lg bg-background-surface border border-border">
                    <span className="text-[11px] text-slate-400">XGBoost Risk Score</span>
                    <div className="text-sm font-mono font-semibold text-indigo-300 mt-1">
                      {credit.latestRiskScore !== undefined ? formatScore(credit.latestRiskScore, 4) : 'Pending'}
                    </div>
                  </div>

                  <div className="p-3 rounded-lg bg-background-surface border border-border">
                    <span className="text-[11px] text-slate-400">ML Decision</span>
                    <div className="mt-1">
                      <Badge variant="status" status={credit.decision || 'PENDING'} size="sm" />
                    </div>
                  </div>
                </div>

                {credit.applicationStatus === 'PENDING' && (
                  <div className="p-3 rounded-lg bg-amber-500/10 border border-amber-500/20 text-xs text-amber-300 flex items-center gap-2 animate-pulse">
                    <Clock className="w-4 h-4 text-amber-400 shrink-0" />
                    <span>Application is currently being evaluated by the XGBoost asynchronous worker...</span>
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {/* AML Compliance Alerts Panel */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <ShieldAlert className="w-4 h-4 text-rose-400" />
                <span>AML Alerts & Anomaly Scoring</span>
              </CardTitle>
              <CardDescription>
                Isolation Forest ML + Deterministic Rule Scoring
              </CardDescription>
            </div>
            {aml?.openAlerts > 0 ? (
              <Badge variant="status" status="OPEN" />
            ) : (
              <Badge variant="status" status="RESOLVED" />
            )}
          </CardHeader>
          <CardContent>
            {aml?.openAlerts === 0 && !aml?.latestAlertUuid ? (
              <div className="text-center py-8 space-y-2">
                <FileCheck2 className="w-10 h-10 text-emerald-500/50 mx-auto" />
                <p className="text-xs text-slate-300 font-medium">Clean AML Record</p>
                <p className="text-[11px] text-slate-500">No suspicious activities or alerts triggered.</p>
              </div>
            ) : (
              <div className="space-y-4">
                {/* Score Breakdown Bar */}
                <ScoreBreakdownBar
                  ruleScore={aml.ruleScore}
                  anomalyScore={aml.anomalyScore}
                  hybridScore={aml.latestHybridScore}
                />

                <div className="flex items-center justify-between pt-2 border-t border-border/60">
                  <div className="text-xs text-slate-400">
                    <span>Open Alerts: </span>
                    <span className="font-mono font-bold text-rose-400">{aml.openAlerts}</span>
                    {aml.latestStatus && (
                      <span className="ml-3">
                        Status: <Badge variant="status" status={aml.latestStatus} size="sm" />
                      </span>
                    )}
                  </div>

                  {aml.latestAlertUuid && (
                    <div className="flex items-center gap-2">
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => {
                          setSelectedAlertUuid(aml.latestAlertUuid!);
                          setIsUpdateModalOpen(true);
                        }}
                      >
                        Update Status
                      </Button>
                      <Link to={`/alerts/${aml.latestAlertUuid}`}>
                        <Button variant="outline" size="sm">
                          Details
                        </Button>
                      </Link>
                    </div>
                  )}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* 4. Bottom Grid: Transaction History Table */}
      <Card>
        <CardHeader>
          <div>
            <CardTitle>
              <History className="w-4 h-4 text-cyan-400" />
              <span>Customer Transaction History</span>
            </CardTitle>
            <CardDescription>
              Real-time transaction stream with automated AML merchant keyword flagging.
            </CardDescription>
          </div>
          <Link to={`/customers/${customer.customerUuid}/transactions/new`}>
            <Button variant="primary" size="sm">
              <PlusCircle className="w-3.5 h-3.5 mr-1" />
              <span>Submit New Transaction</span>
            </Button>
          </Link>
        </CardHeader>
        <CardContent className="p-0">
          {!transactions || transactions.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-500">
              No transactions recorded for this customer yet.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-background-surface/80 border-b border-border text-slate-400 font-mono uppercase tracking-wider">
                  <tr>
                    <th className="px-6 py-3">Timestamp</th>
                    <th className="px-6 py-3">Transaction UUID</th>
                    <th className="px-6 py-3">Type</th>
                    <th className="px-6 py-3">Merchant</th>
                    <th className="px-6 py-3">Amount</th>
                    <th className="px-6 py-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/60">
                  {transactions.map((txn) => {
                    const isSuspicious = isSuspiciousMerchant(txn.merchant);
                    return (
                      <tr key={txn.transactionUuid} className="hover:bg-background-elevated/40 transition-colors">
                        <td className="px-6 py-3.5 font-mono text-slate-400 whitespace-nowrap">
                          {formatDate(txn.timestamp)}
                        </td>
                        <td className="px-6 py-3.5 font-mono text-slate-300">
                          {txn.transactionUuid.substring(0, 8)}...
                        </td>
                        <td className="px-6 py-3.5">
                          <span className="font-mono text-slate-300">{txn.transactionType}</span>
                        </td>
                        <td className="px-6 py-3.5">
                          <div className="flex items-center gap-2">
                            <span className="font-medium text-slate-200">{txn.merchant}</span>
                            {isSuspicious && (
                              <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-rose-500/20 text-rose-300 border border-rose-500/30">
                                High-Risk Merchant
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-3.5 font-mono font-semibold text-slate-100 whitespace-nowrap">
                          {formatCurrency(txn.amount)}
                        </td>
                        <td className="px-6 py-3.5">
                          <Badge variant="status" status={txn.status} size="sm" />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Status Update Modal Dialog */}
      <Modal
        isOpen={isUpdateModalOpen}
        onClose={() => setIsUpdateModalOpen(false)}
        title="Update AML Alert Status"
        description={`Update compliance status for alert: ${selectedAlertUuid?.substring(0, 12)}...`}
      >
        <form onSubmit={handleUpdateStatus} className="space-y-4">
          <Select
            label="Compliance Workflow Status"
            value={newStatus}
            onChange={(e) => setNewStatus(e.target.value as AlertStatus)}
            options={[
              { value: 'OPEN', label: 'OPEN — Unresolved' },
              { value: 'UNDER_REVIEW', label: 'UNDER_REVIEW — Investigating' },
              { value: 'RESOLVED', label: 'RESOLVED — Compliant / Mitigated' },
              { value: 'FALSE_POSITIVE', label: 'FALSE_POSITIVE — Dismissed' },
            ]}
          />

          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-300">
              Resolution Notes & Audit Rationale
            </label>
            <textarea
              rows={3}
              value={resolutionNotes}
              onChange={(e) => setResolutionNotes(e.target.value)}
              placeholder="Provide compliance notes or reason for this status transition..."
              className="w-full rounded-lg bg-background-surface border border-border px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-border">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={() => setIsUpdateModalOpen(false)}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" isLoading={isUpdating}>
              Save Compliance Decision
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
