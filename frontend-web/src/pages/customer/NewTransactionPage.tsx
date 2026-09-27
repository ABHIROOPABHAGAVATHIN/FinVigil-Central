import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
  Send,
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  RefreshCw,
  Clock,
  ArrowRight,
  Activity,
  AlertTriangle,
} from 'lucide-react';
import { transactionsApi } from '../../api/transactions';
import { amlApi } from '../../api/aml';
import { TransactionResponse } from '../../types/transaction';
import { AmlAlertResponse } from '../../types/aml';
import { useToastStore } from '../../components/ui/Toast';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Select } from '../../components/ui/Select';
import { Badge } from '../../components/ui/Badge';
import { ScoreBreakdownBar } from '../../components/common/ScoreBreakdownBar';
import { formatCurrency, formatScore } from '../../lib/utils';

const txnSchema = z.object({
  amount: z.number().positive('Amount must be greater than zero'),
  transactionType: z.enum(['TRANSFER', 'PURCHASE', 'WITHDRAWAL', 'DEPOSIT']),
  merchant: z.string().min(1, 'Merchant name is required'),
  currency: z.string().default('INR'),
});

type TxnFormData = z.infer<typeof txnSchema>;

export const NewTransactionPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useToastStore();

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdTxn, setCreatedTxn] = useState<TransactionResponse | null>(null);
  const [isScreening, setIsScreening] = useState(false);
  const [detectedAlert, setDetectedAlert] = useState<AmlAlertResponse | null>(null);
  const [screeningFinished, setScreeningFinished] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<TxnFormData>({
    resolver: zodResolver(txnSchema),
    defaultValues: {
      amount: 1500,
      transactionType: 'TRANSFER',
      merchant: 'Fresh Market Groceries',
      currency: 'INR',
    },
  });

  const selectedAmount = watch('amount');
  const selectedMerchant = watch('merchant');

  // Quick preset suspicious button for testing AML
  const applySuspiciousPreset = () => {
    setValue('amount', 75000);
    setValue('transactionType', 'TRANSFER');
    setValue('merchant', 'Crypto Casino Royale');
  };

  const onSubmit = async (data: TxnFormData) => {
    if (!id) return;
    setIsSubmitting(true);
    setScreeningFinished(false);
    setDetectedAlert(null);

    try {
      const response = await transactionsApi.processTransaction({
        customerId: id,
        ...data,
      });

      setCreatedTxn(response);
      setIsScreening(true);
      showToast({
        type: 'info',
        title: 'Transaction Ingested',
        message: 'Screening with Isolation Forest ML and deterministic rules...',
      });

      // Poll for AML Alert generated for this transaction UUID (up to 8 seconds)
      let attempts = 0;
      const interval = setInterval(async () => {
        attempts++;
        try {
          // Check alerts for customer
          const alerts = await amlApi.getAlertsByCustomer(id);
          const matched = alerts.find(
            (a) => a.transactionUuid === response.transactionUuid
          );

          if (matched) {
            clearInterval(interval);
            setIsScreening(false);
            setDetectedAlert(matched);
            setScreeningFinished(true);
            showToast({
              type: 'warning',
              title: 'AML Alert Triggered!',
              message: `Suspicious activity flagged: Risk Level ${matched.riskLevel}`,
            });
            return;
          }

          if (attempts >= 4) {
            // After 8s with no alert, transaction passed screening cleanly
            clearInterval(interval);
            setIsScreening(false);
            setScreeningFinished(true);
            showToast({
              type: 'success',
              title: 'Screening Passed',
              message: 'No AML rules triggered and no anomaly detected.',
            });
          }
        } catch (e) {
          console.error('Screening check error', e);
          if (attempts >= 4) {
            clearInterval(interval);
            setIsScreening(false);
            setScreeningFinished(true);
          }
        }
      }, 2000);
    } catch (err: any) {
      showToast({
        type: 'error',
        title: 'Transaction Failed',
        message: err.response?.data?.message || err.message,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-12">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-cyan-400 uppercase tracking-wider mb-1">
            <Activity className="w-3.5 h-3.5" />
            <span>Real-Time AML Transaction Screener</span>
          </div>
          <h1 className="text-2xl font-bold text-slate-100">Submit Financial Transaction</h1>
          <p className="text-xs text-slate-400 mt-1">
            Originating Customer: <span className="font-mono text-slate-200">{id}</span>
          </p>
        </div>
        <Link to={`/customers/${id}`}>
          <Button variant="secondary" size="sm">
            Back to Profile
          </Button>
        </Link>
      </div>

      {/* Post-submission Screening Results */}
      {createdTxn ? (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <div>
                <CardTitle>
                  <Send className="w-4 h-4 text-cyan-400" />
                  <span>Transaction Ingestion Status</span>
                </CardTitle>
                <CardDescription>
                  UUID: <span className="font-mono">{createdTxn.transactionUuid}</span>
                </CardDescription>
              </div>
              <Badge variant="status" status={createdTxn.status} />
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
                <div className="p-3 rounded-lg bg-background-surface border border-border">
                  <span className="text-slate-400">Amount:</span>
                  <div className="font-mono font-semibold text-slate-100 mt-0.5">
                    {formatCurrency(createdTxn.amount, createdTxn.currency)}
                  </div>
                </div>

                <div className="p-3 rounded-lg bg-background-surface border border-border">
                  <span className="text-slate-400">Type:</span>
                  <div className="font-mono text-slate-100 mt-0.5">{createdTxn.transactionType}</div>
                </div>

                <div className="p-3 rounded-lg bg-background-surface border border-border">
                  <span className="text-slate-400">Merchant:</span>
                  <div className="font-medium text-slate-100 mt-0.5 truncate">{createdTxn.merchant}</div>
                </div>

                <div className="p-3 rounded-lg bg-background-surface border border-border">
                  <span className="text-slate-400">Redis Velocity:</span>
                  <div className="text-emerald-400 font-mono mt-0.5">Updated in 60s window</div>
                </div>
              </div>

              {/* Live AML Screening Stage */}
              {isScreening ? (
                <div className="p-6 rounded-xl bg-background-surface/90 border border-cyan-500/30 text-center space-y-3">
                  <RefreshCw className="w-8 h-8 text-cyan-400 animate-spin mx-auto" />
                  <div>
                    <h4 className="text-sm font-semibold text-slate-200">
                      Real-time AML Screening In Progress...
                    </h4>
                    <p className="text-xs text-slate-400 mt-1">
                      Evaluating transaction amount, high-risk merchant keywords, and Isolation Forest anomaly score.
                    </p>
                  </div>
                </div>
              ) : detectedAlert ? (
                <div className="space-y-4 p-6 rounded-xl bg-rose-500/10 border border-rose-500/30">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-rose-400 font-bold text-sm">
                      <ShieldAlert className="w-5 h-5" />
                      <span>AML COMPLIANCE ALERT TRIGGERED!</span>
                    </div>
                    <Badge variant="risk" riskLevel={detectedAlert.riskLevel} />
                  </div>

                  <ScoreBreakdownBar
                    ruleScore={detectedAlert.ruleScore}
                    anomalyScore={detectedAlert.anomalyScore}
                    hybridScore={detectedAlert.hybridScore}
                  />

                  {detectedAlert.reasons && detectedAlert.reasons.length > 0 && (
                    <div className="space-y-1.5 pt-2">
                      <span className="text-xs font-semibold text-slate-300">Triggered Rules & Reasons:</span>
                      <div className="flex flex-wrap gap-2">
                        {detectedAlert.reasons.map((r, i) => (
                          <span
                            key={i}
                            className="px-2.5 py-1 rounded bg-rose-500/20 text-rose-300 text-xs font-mono border border-rose-500/30"
                          >
                            {r}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="flex justify-end pt-3">
                    <Link to={`/alerts/${detectedAlert.alertUuid}`}>
                      <Button variant="primary" size="sm">
                        <span>Investigate Alert Record</span>
                        <ArrowRight className="w-4 h-4 ml-1.5" />
                      </Button>
                    </Link>
                  </div>
                </div>
              ) : screeningFinished ? (
                <div className="p-6 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center gap-4">
                  <CheckCircle2 className="w-8 h-8 text-emerald-400 shrink-0" />
                  <div>
                    <h4 className="text-sm font-semibold text-emerald-300">
                      Transaction Cleared Compliance Screening
                    </h4>
                    <p className="text-xs text-slate-400 mt-0.5">
                      No high-risk keywords detected, transaction volume within safe thresholds, and Isolation Forest anomaly score below 0.65.
                    </p>
                  </div>
                </div>
              ) : null}

              <div className="flex justify-end gap-3 pt-4 border-t border-border">
                <Button
                  variant="secondary"
                  size="md"
                  onClick={() => {
                    setCreatedTxn(null);
                    setDetectedAlert(null);
                    setScreeningFinished(false);
                  }}
                >
                  Submit Another Transaction
                </Button>
                <Link to={`/customers/${id}`}>
                  <Button variant="outline" size="md">
                    Return to Profile
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>
        </div>
      ) : (
        /* Form */
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <Send className="w-4 h-4 text-cyan-400" />
                <span>Transaction Parameters</span>
              </CardTitle>
              <CardDescription>
                Ingest payment or transfer event into the live velocity pipeline.
              </CardDescription>
            </div>
            {/* Quick Testing Shortcut */}
            <button
              type="button"
              onClick={applySuspiciousPreset}
              className="text-[11px] font-mono text-amber-400 hover:text-amber-300 border border-amber-500/30 bg-amber-500/10 px-2.5 py-1 rounded-lg transition-colors flex items-center gap-1.5"
            >
              <AlertTriangle className="w-3.5 h-3.5" />
              <span>Fill Suspicious Preset (Crypto Casino)</span>
            </button>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <Input
                  label="Transaction Amount (₹)"
                  type="number"
                  step="any"
                  error={errors.amount?.message}
                  helperText="Amounts above ₹50,000 trigger velocity/large transaction checks"
                  {...register('amount', { valueAsNumber: true })}
                />

                <Select
                  label="Transaction Type"
                  options={[
                    { value: 'TRANSFER', label: 'TRANSFER — Wire / Account Transfer' },
                    { value: 'PURCHASE', label: 'PURCHASE — Merchant POS / Online' },
                    { value: 'WITHDRAWAL', label: 'WITHDRAWAL — Cash Out' },
                    { value: 'DEPOSIT', label: 'DEPOSIT — Cash In' },
                  ]}
                  error={errors.transactionType?.message}
                  {...register('transactionType')}
                />

                <Input
                  label="Merchant / Counterparty"
                  placeholder="e.g. Fresh Market or Crypto Casino Royale"
                  error={errors.merchant?.message}
                  helperText="Screened against high-risk AML compliance keyword dictionary"
                  {...register('merchant')}
                />

                <Input
                  label="Currency Code"
                  placeholder="INR"
                  error={errors.currency?.message}
                  {...register('currency')}
                />
              </div>

              <div className="flex justify-end gap-3 pt-6 border-t border-border">
                <Link to={`/customers/${id}`}>
                  <Button type="button" variant="secondary" size="md">
                    Cancel
                  </Button>
                </Link>
                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  isLoading={isSubmitting}
                >
                  <span>Process & Screen Transaction</span>
                  <ArrowRight className="w-4 h-4 ml-1.5" />
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}
    </div>
  );
};
