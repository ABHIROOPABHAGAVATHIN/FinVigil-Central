import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ShieldAlert,
  Clock,
  User,
  ArrowLeft,
  FileText,
  Activity,
  CheckCircle2,
  RefreshCw,
  Edit3,
} from 'lucide-react';
import { amlApi } from '../../api/aml';
import { AlertStatus } from '../../types/aml';
import { useToastStore } from '../../components/ui/Toast';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Select } from '../../components/ui/Select';
import { ScoreBreakdownBar } from '../../components/common/ScoreBreakdownBar';
import { formatDate, formatScore } from '../../lib/utils';

export const AlertDetailPage: React.FC = () => {
  const { alertUuid } = useParams<{ alertUuid: string }>();
  const queryClient = useQueryClient();
  const { showToast } = useToastStore();

  const [newStatus, setNewStatus] = useState<AlertStatus>('UNDER_REVIEW');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);

  const {
    data: alert,
    isLoading,
    isError,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ['alert', alertUuid],
    queryFn: () => amlApi.getAlertByUuid(alertUuid!),
    enabled: !!alertUuid,
  });

  const handleStatusSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!alertUuid) return;

    setIsUpdating(true);
    try {
      await amlApi.updateAlertStatus(alertUuid, {
        status: newStatus,
        resolutionNotes,
      });
      showToast({
        type: 'success',
        title: 'Compliance Status Updated',
        message: `Alert marked as ${newStatus}`,
      });
      setResolutionNotes('');
      refetch();
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
    } catch (err: any) {
      showToast({
        type: 'error',
        title: 'Update Error',
        message: err.response?.data?.message || err.message,
      });
    } finally {
      setIsUpdating(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 space-y-3">
        <RefreshCw className="w-8 h-8 text-brand-500 animate-spin" />
        <div className="text-xs text-slate-400">Loading AML alert investigation dossier...</div>
      </div>
    );
  }

  if (isError || !alert) {
    return (
      <div className="max-w-xl mx-auto py-16 text-center space-y-4">
        <ShieldAlert className="w-12 h-12 text-rose-400 mx-auto" />
        <h2 className="text-lg font-bold text-slate-100">AML Alert Not Found</h2>
        <p className="text-xs text-slate-400">
          No compliance alert found with UUID <span className="font-mono text-indigo-400">{alertUuid}</span>.
        </p>
        <Link to="/alerts">
          <Button variant="secondary" size="md">
            Return to Alerts Queue
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto space-y-8 pb-12">
      {/* Back button and page header */}
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <Link
            to="/alerts"
            className="inline-flex items-center gap-1 text-xs text-slate-400 hover:text-white transition-colors mb-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to AML Queue</span>
          </Link>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-slate-100">Alert Dossier</h1>
            <Badge variant="risk" riskLevel={alert.riskLevel} />
            <Badge variant="status" status={alert.status} />
          </div>
          <p className="text-xs font-mono text-slate-400">
            UUID: <span className="text-slate-200">{alert.alertUuid}</span>
          </p>
        </div>

        <button
          onClick={() => refetch()}
          className="p-2 rounded-lg bg-background-elevated hover:bg-border text-slate-400 hover:text-white border border-border"
        >
          <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin text-brand-500' : ''}`} />
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Details, Score Breakdown, Reasons, Audit trail */}
        <div className="lg:col-span-2 space-y-6">
          {/* Hybrid Score Composition */}
          <Card>
            <CardHeader>
              <CardTitle>
                <Activity className="w-4 h-4 text-cyan-400" />
                <span>Multi-Model Score Breakdown</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ScoreBreakdownBar
                ruleScore={alert.ruleScore}
                anomalyScore={alert.anomalyScore}
                hybridScore={alert.hybridScore}
              />
            </CardContent>
          </Card>

          {/* Triggered Rule Violations */}
          <Card>
            <CardHeader>
              <CardTitle>
                <ShieldAlert className="w-4 h-4 text-rose-400" />
                <span>Triggered Compliance Violations & Reason Codes</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              {!alert.reasons || alert.reasons.length === 0 ? (
                <p className="text-xs text-slate-500">No explicit rule reason codes attached.</p>
              ) : (
                <div className="space-y-2">
                  {alert.reasons.map((reason, idx) => (
                    <div
                      key={idx}
                      className="p-3 rounded-lg bg-background-surface border border-border flex items-start gap-3"
                    >
                      <span className="w-2 h-2 rounded-full bg-rose-400 mt-1.5 shrink-0" />
                      <div>
                        <div className="text-xs font-semibold text-slate-200">{reason}</div>
                        <div className="text-[11px] text-slate-400 mt-0.5 font-mono">
                          Severity: HIGH • Weight contribution calculated into RuleScore
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Entity & Transaction Links */}
          <Card>
            <CardHeader>
              <CardTitle>
                <FileText className="w-4 h-4 text-indigo-400" />
                <span>Associated Entities & Transaction Linkage</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div className="p-3.5 rounded-lg bg-background-surface border border-border space-y-1">
                  <span className="text-slate-400">Target Customer UUID:</span>
                  <div className="font-mono text-slate-100 break-all">{alert.customerUuid || 'N/A'}</div>
                  {alert.customerUuid && (
                    <div className="pt-2">
                      <Link
                        to={`/customers/${alert.customerUuid}`}
                        className="text-xs text-indigo-400 hover:text-indigo-300 underline font-medium"
                      >
                        Inspect Unified Customer Profile →
                      </Link>
                    </div>
                  )}
                </div>

                <div className="p-3.5 rounded-lg bg-background-surface border border-border space-y-1">
                  <span className="text-slate-400">Transaction UUID:</span>
                  <div className="font-mono text-slate-100 break-all">{alert.transactionUuid || 'N/A'}</div>
                  <div className="text-[11px] text-slate-500 pt-1">
                    Trigger event screened at time of ingestion
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Right Col: Resolution Workflow & Audit Trail */}
        <div className="space-y-6">
          {/* Resolution Form */}
          <Card className="border-border-strong">
            <CardHeader>
              <CardTitle>
                <Edit3 className="w-4 h-4 text-brand-400" />
                <span>Compliance Workflow Action</span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleStatusSubmit} className="space-y-4">
                <Select
                  label="Update Operational Status"
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value as AlertStatus)}
                  options={[
                    { value: 'OPEN', label: 'OPEN — Unresolved' },
                    { value: 'UNDER_REVIEW', label: 'UNDER_REVIEW — In Investigation' },
                    { value: 'RESOLVED', label: 'RESOLVED — Verified & Mitigated' },
                    { value: 'FALSE_POSITIVE', label: 'FALSE_POSITIVE — Dismiss Alert' },
                  ]}
                />

                <div className="space-y-1.5">
                  <label className="block text-xs font-medium text-slate-300">
                    Resolution Notes & Rationale
                  </label>
                  <textarea
                    rows={4}
                    value={resolutionNotes}
                    onChange={(e) => setResolutionNotes(e.target.value)}
                    placeholder="Enter compliance resolution notes (e.g. Customer confirmed identity; legitimate transaction)..."
                    className="w-full rounded-lg bg-background-surface border border-border p-3 text-xs text-slate-100 placeholder-slate-500 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 font-sans"
                  />
                </div>

                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  className="w-full"
                  isLoading={isUpdating}
                >
                  Save Compliance Decision
                </Button>
              </form>
            </CardContent>
          </Card>

          {/* Audit Timestamp Trail */}
          <Card>
            <CardHeader>
              <CardTitle>
                <Clock className="w-4 h-4 text-slate-400" />
                <span>Audit Trail & Timestamps</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-xs">
              <div className="p-3 rounded-lg bg-background-surface border border-border">
                <div className="text-slate-400">Created At:</div>
                <div className="font-mono text-slate-200 mt-0.5">{formatDate(alert.createdAt)}</div>
              </div>

              <div className="p-3 rounded-lg bg-background-surface border border-border">
                <div className="text-slate-400">Last Updated:</div>
                <div className="font-mono text-slate-200 mt-0.5">{formatDate(alert.updatedAt)}</div>
              </div>

              {alert.resolutionNotes && (
                <div className="p-3 rounded-lg bg-background-surface border border-border">
                  <div className="text-slate-400">Latest Resolution Notes:</div>
                  <div className="text-slate-200 mt-1 italic">{alert.resolutionNotes}</div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};
