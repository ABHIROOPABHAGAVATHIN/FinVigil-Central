import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
  CreditCard,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  ArrowRight,
  ShieldCheck,
  RefreshCw,
  Cpu,
} from 'lucide-react';
import { creditApi } from '../../api/credit';
import { CreditApplicationResponse } from '../../types/credit';
import { useToastStore } from '../../components/ui/Toast';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { ScoreGauge } from '../../components/common/ScoreGauge';
import { formatCurrency, formatScore } from '../../lib/utils';

const creditSchema = z.object({
  income: z.number().positive('Income must be greater than zero'),
  employmentYears: z.number().int().min(0, 'Min 0 years').max(60, 'Max 60 years'),
  loanAmount: z.number().positive('Loan amount must be greater than zero'),
  existingLoans: z.number().int().min(0, 'Existing loans must be 0 or positive'),
  creditScore: z.number().int().min(300, 'Credit score must be between 300 and 850').max(850, 'Credit score must be between 300 and 850'),
  debtToIncomeRatio: z.number().min(0.0, 'DTI must be between 0.0 and 1.0').max(1.0, 'DTI must be between 0.0 and 1.0'),
});

type CreditFormData = z.infer<typeof creditSchema>;

export const ApplyCreditPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useToastStore();

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittedApp, setSubmittedApp] = useState<CreditApplicationResponse | null>(null);
  const [isPolling, setIsPolling] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<CreditFormData>({
    resolver: zodResolver(creditSchema),
    defaultValues: {
      income: 85000,
      employmentYears: 5,
      loanAmount: 25000,
      existingLoans: 1,
      creditScore: 740,
      debtToIncomeRatio: 0.28,
    },
  });

  const onSubmit = async (data: CreditFormData) => {
    if (!id) return;
    setIsSubmitting(true);
    try {
      const response = await creditApi.applyForCredit({
        customerId: id,
        ...data,
      });

      setSubmittedApp(response);
      setIsPolling(true);
      showToast({
        type: 'info',
        title: 'Application Submitted',
        message: 'Credit application accepted (202). Evaluating with XGBoost...',
      });
    } catch (err: any) {
      showToast({
        type: 'error',
        title: 'Submission Failed',
        message: err.response?.data?.message || err.message,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Poll for decision resolution
  useEffect(() => {
    let interval: any = null;
    if (isPolling && submittedApp?.applicationUuid) {
      interval = setInterval(async () => {
        try {
          const app = await creditApi.getApplicationByIdOrUuid(submittedApp.applicationUuid);
          setSubmittedApp(app);
          // If decision has arrived and status is not PENDING, stop polling
          if (app.applicationStatus !== 'PENDING' || app.decision) {
            setIsPolling(false);
            showToast({
              type: app.decision?.decision === 'APPROVE' ? 'success' : 'warning',
              title: 'Decision Received',
              message: `XGBoost completed underwriting: ${app.decision?.decision || app.applicationStatus}`,
            });
          }
        } catch (e) {
          console.error('Polling error', e);
        }
      }, 2000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isPolling, submittedApp?.applicationUuid, showToast]);

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-12">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-indigo-400 uppercase tracking-wider mb-1">
            <Cpu className="w-3.5 h-3.5" />
            <span>XGBoost ML Underwriting Pipeline</span>
          </div>
          <h1 className="text-2xl font-bold text-slate-100">Submit Credit Application</h1>
          <p className="text-xs text-slate-400 mt-1">
            Applicant Customer: <span className="font-mono text-slate-200">{id}</span>
          </p>
        </div>
        <Link to={`/customers/${id}`}>
          <Button variant="secondary" size="sm">
            Back to Profile
          </Button>
        </Link>
      </div>

      {/* When application is submitted and decision is pending or received */}
      {submittedApp ? (
        <Card className="border-indigo-500/30">
          <CardHeader>
            <div>
              <CardTitle>
                <ShieldCheck className="w-5 h-5 text-indigo-400" />
                <span>Underwriting Assessment Results</span>
              </CardTitle>
              <CardDescription>
                Application UUID: <span className="font-mono">{submittedApp.applicationUuid}</span>
              </CardDescription>
            </div>
            <Badge
              variant="status"
              status={submittedApp.decision?.decision || submittedApp.applicationStatus}
            />
          </CardHeader>
          <CardContent className="space-y-6">
            {isPolling ? (
              <div className="p-8 text-center space-y-4 rounded-xl bg-background-surface/80 border border-border">
                <RefreshCw className="w-10 h-10 text-brand-500 animate-spin mx-auto" />
                <div className="space-y-1">
                  <h3 className="text-base font-semibold text-slate-200">
                    Evaluating Loan with XGBoost ML...
                  </h3>
                  <p className="text-xs text-slate-400">
                    The application event is being processed asynchronously via RabbitMQ and evaluated against our trained gradient-boosted decision model.
                  </p>
                </div>
                <div className="flex items-center justify-center gap-2 text-xs font-mono text-cyan-400">
                  <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
                  <span>Polling decision queue...</span>
                </div>
              </div>
            ) : submittedApp.decision ? (
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-center p-6 rounded-xl bg-background-surface border border-border">
                  <div className="flex justify-center">
                    <ScoreGauge
                      score={submittedApp.decision.riskScore}
                      riskLevel={submittedApp.decision.riskLevel}
                      size="lg"
                      label="XGBoost Default Probability"
                    />
                  </div>

                  <div className="md:col-span-2 space-y-4">
                    <div className="flex items-center gap-3">
                      {submittedApp.decision.decision === 'APPROVE' ? (
                        <div className="flex items-center gap-2 text-emerald-400 font-bold text-lg">
                          <CheckCircle2 className="w-6 h-6" />
                          <span>APPLICATION APPROVED</span>
                        </div>
                      ) : submittedApp.decision.decision === 'REJECT' ? (
                        <div className="flex items-center gap-2 text-rose-400 font-bold text-lg">
                          <XCircle className="w-6 h-6" />
                          <span>APPLICATION REJECTED</span>
                        </div>
                      ) : (
                        <div className="flex items-center gap-2 text-amber-400 font-bold text-lg">
                          <AlertTriangle className="w-6 h-6" />
                          <span>MANUAL UNDERWRITING REVIEW REQUIRED</span>
                        </div>
                      )}
                    </div>

                    <div className="grid grid-cols-2 gap-4 text-xs">
                      <div className="p-3 rounded-lg bg-background-elevated border border-border">
                        <span className="text-slate-400">Model Version:</span>
                        <div className="font-mono text-slate-200 mt-0.5">
                          {submittedApp.decision.modelVersion || 'credit_xgboost_v1.0'}
                        </div>
                      </div>

                      <div className="p-3 rounded-lg bg-background-elevated border border-border">
                        <span className="text-slate-400">Decision UUID:</span>
                        <div className="font-mono text-slate-200 mt-0.5 truncate">
                          {submittedApp.decision.decisionUuid}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-border">
                  <Button
                    variant="secondary"
                    size="md"
                    onClick={() => {
                      setSubmittedApp(null);
                    }}
                  >
                    Submit Another Application
                  </Button>
                  <Button
                    variant="primary"
                    size="md"
                    onClick={() => navigate(`/customers/${id}`)}
                  >
                    <span>View Customer Profile</span>
                    <ArrowRight className="w-4 h-4 ml-1.5" />
                  </Button>
                </div>
              </div>
            ) : null}
          </CardContent>
        </Card>
      ) : (
        /* Application Submission Form */
        <Card>
          <CardHeader>
            <CardTitle>
              <CreditCard className="w-4 h-4 text-indigo-400" />
              <span>Financial Inputs for Loan Underwriting</span>
            </CardTitle>
            <CardDescription>
              Submit applicant financial attributes for automated XGBoost inference.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <Input
                  label="Annual Income (₹ or $)"
                  type="number"
                  step="any"
                  error={errors.income?.message}
                  {...register('income', { valueAsNumber: true })}
                />

                <Input
                  label="Requested Loan Amount (₹ or $)"
                  type="number"
                  step="any"
                  error={errors.loanAmount?.message}
                  {...register('loanAmount', { valueAsNumber: true })}
                />

                <Input
                  label="Credit Score (300 – 850)"
                  type="number"
                  error={errors.creditScore?.message}
                  helperText="Standard FICO / CIBIL credit score range"
                  {...register('creditScore', { valueAsNumber: true })}
                />

                <Input
                  label="Debt-to-Income Ratio (0.00 – 1.00)"
                  type="number"
                  step="0.01"
                  error={errors.debtToIncomeRatio?.message}
                  helperText="e.g. 0.28 represents 28% DTI"
                  {...register('debtToIncomeRatio', { valueAsNumber: true })}
                />

                <Input
                  label="Employment Years (0 – 60)"
                  type="number"
                  error={errors.employmentYears?.message}
                  {...register('employmentYears', { valueAsNumber: true })}
                />

                <Input
                  label="Existing Active Loans Count"
                  type="number"
                  error={errors.existingLoans?.message}
                  {...register('existingLoans', { valueAsNumber: true })}
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
                  <span>Submit & Trigger ML Decision</span>
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
