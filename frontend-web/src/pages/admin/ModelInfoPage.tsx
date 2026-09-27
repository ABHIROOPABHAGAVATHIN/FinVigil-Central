import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Cpu, CheckCircle2, RefreshCw, BarChart3, Layers, ShieldCheck, Activity } from 'lucide-react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, Cell } from 'recharts';
import { systemApi } from '../../api/system';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { formatDate, formatPercent, formatScore } from '../../lib/utils';

export const ModelInfoPage: React.FC = () => {
  const {
    data: models,
    isLoading,
    isError,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ['modelInfo'],
    queryFn: systemApi.getModelInfo,
    refetchInterval: 30000,
  });

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 space-y-3">
        <RefreshCw className="w-8 h-8 text-brand-500 animate-spin" />
        <div className="text-xs text-slate-400">Querying FastAPI ML Model Registry (:8000)...</div>
      </div>
    );
  }

  if (isError || !models) {
    return (
      <div className="max-w-xl mx-auto py-16 text-center space-y-4">
        <Cpu className="w-12 h-12 text-rose-400 mx-auto" />
        <h2 className="text-lg font-bold text-slate-100">ML Model Registry Offline</h2>
        <p className="text-xs text-slate-400">
          Unable to reach FastAPI ML service at <span className="font-mono text-cyan-400">http://localhost:8000</span>.
        </p>
      </div>
    );
  }

  const { credit_model, aml_model } = models;

  // Prepare feature importances for chart
  const featureData = Object.entries(credit_model?.feature_importances || {}).map(([key, val]) => ({
    name: key.replace(/_/g, ' '),
    importance: Math.round(val * 1000) / 10, // percentage with 1 decimal
  })).sort((a, b) => b.importance - a.importance);

  return (
    <div className="max-w-6xl mx-auto space-y-8 pb-12">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-cyan-400 uppercase tracking-wider mb-1">
            <Cpu className="w-3.5 h-3.5" />
            <span>FastAPI Python Service Registry (:8000)</span>
          </div>
          <h1 className="text-2xl font-bold text-slate-100">AI / ML Model Intelligence</h1>
          <p className="text-xs text-slate-400 mt-1">
            Performance metrics, feature importance rankings, and operational thresholds for production models.
          </p>
        </div>

        <button
          onClick={() => refetch()}
          className="p-2 rounded-lg bg-background-elevated hover:bg-border text-slate-400 hover:text-white border border-border"
        >
          <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin text-brand-500' : ''}`} />
        </button>
      </div>

      {/* Model Cards Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* XGBoost Credit Model */}
        <Card className="border-indigo-500/20">
          <CardHeader>
            <div>
              <CardTitle>
                <Cpu className="w-4 h-4 text-indigo-400" />
                <span>Credit Underwriting Model</span>
              </CardTitle>
              <CardDescription>
                Algorithm: <span className="font-mono text-slate-200">{credit_model?.algorithm}</span>
              </CardDescription>
            </div>
            <Badge variant="status" status="ACTIVE" />
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 rounded-lg bg-background-surface border border-border">
                <span className="text-slate-400">Model Version:</span>
                <div className="font-mono text-slate-200 mt-0.5">{credit_model?.model_version}</div>
              </div>

              <div className="p-3 rounded-lg bg-background-surface border border-border">
                <span className="text-slate-400">Trained At:</span>
                <div className="font-mono text-slate-200 mt-0.5">{formatDate(credit_model?.trained_at)}</div>
              </div>
            </div>

            {/* Metrics Grid */}
            <div>
              <h4 className="text-xs font-semibold text-slate-300 mb-3 flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                <span>Validation Benchmark Metrics</span>
              </h4>
              <div className="grid grid-cols-3 sm:grid-cols-5 gap-2.5 text-center">
                <div className="p-2.5 rounded-lg bg-background-surface border border-border">
                  <span className="text-[10px] text-slate-400 block">ROC-AUC</span>
                  <span className="font-mono font-bold text-sm text-emerald-400">
                    {formatScore(credit_model?.metrics?.roc_auc, 4)}
                  </span>
                </div>
                <div className="p-2.5 rounded-lg bg-background-surface border border-border">
                  <span className="text-[10px] text-slate-400 block">Accuracy</span>
                  <span className="font-mono font-bold text-sm text-slate-200">
                    {formatPercent(credit_model?.metrics?.accuracy)}
                  </span>
                </div>
                <div className="p-2.5 rounded-lg bg-background-surface border border-border">
                  <span className="text-[10px] text-slate-400 block">Precision</span>
                  <span className="font-mono font-bold text-sm text-slate-200">
                    {formatPercent(credit_model?.metrics?.precision)}
                  </span>
                </div>
                <div className="p-2.5 rounded-lg bg-background-surface border border-border">
                  <span className="text-[10px] text-slate-400 block">Recall</span>
                  <span className="font-mono font-bold text-sm text-slate-200">
                    {formatPercent(credit_model?.metrics?.recall)}
                  </span>
                </div>
                <div className="p-2.5 rounded-lg bg-background-surface border border-border">
                  <span className="text-[10px] text-slate-400 block">F1-Score</span>
                  <span className="font-mono font-bold text-sm text-indigo-300">
                    {formatScore(credit_model?.metrics?.f1_score, 4)}
                  </span>
                </div>
              </div>
            </div>

            {/* Feature Importances Chart */}
            <div>
              <h4 className="text-xs font-semibold text-slate-300 mb-3 flex items-center gap-1.5">
                <BarChart3 className="w-4 h-4 text-indigo-400" />
                <span>Feature Importance Weights (%)</span>
              </h4>
              <div className="h-56 w-full bg-background-surface p-3 rounded-xl border border-border">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={featureData} layout="vertical" margin={{ top: 5, right: 20, left: 40, bottom: 5 }}>
                    <XAxis type="number" stroke="#64748b" tickFormatter={(v) => `${v}%`} fontSize={10} />
                    <YAxis dataKey="name" type="category" stroke="#94a3b8" width={100} fontSize={10} />
                    <Tooltip
                      formatter={(val: any) => [`${val}%`, 'Importance']}
                      contentStyle={{ backgroundColor: '#151B2B', borderColor: '#222B40', fontSize: '12px' }}
                    />
                    <Bar dataKey="importance" fill="#6366F1" radius={[0, 4, 4, 0]}>
                      {featureData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={index === 0 ? '#818CF8' : index === 1 ? '#6366F1' : '#4F46E5'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Isolation Forest AML Anomaly Model */}
        <Card className="border-cyan-500/20">
          <CardHeader>
            <div>
              <CardTitle>
                <Layers className="w-4 h-4 text-cyan-400" />
                <span>AML Anomaly Detector</span>
              </CardTitle>
              <CardDescription>
                Algorithm: <span className="font-mono text-slate-200">{aml_model?.algorithm}</span>
              </CardDescription>
            </div>
            <Badge variant="status" status="ACTIVE" />
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 rounded-lg bg-background-surface border border-border">
                <span className="text-slate-400">Model Version:</span>
                <div className="font-mono text-slate-200 mt-0.5">{aml_model?.model_version}</div>
              </div>

              <div className="p-3 rounded-lg bg-background-surface border border-border">
                <span className="text-slate-400">Trained At:</span>
                <div className="font-mono text-slate-200 mt-0.5">{formatDate(aml_model?.trained_at)}</div>
              </div>
            </div>

            {/* Contamination Rate & Operational Thresholds */}
            <div>
              <h4 className="text-xs font-semibold text-slate-300 mb-3 flex items-center gap-1.5">
                <Activity className="w-4 h-4 text-cyan-400" />
                <span>Operational Score Thresholds</span>
              </h4>
              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 rounded-lg bg-background-surface border border-border">
                  <span className="text-slate-400">Model Contamination:</span>
                  <div className="font-mono font-bold text-slate-100 text-sm mt-0.5">
                    {formatPercent(aml_model?.contamination)}
                  </div>
                </div>

                <div className="p-3 rounded-lg bg-background-surface border border-border">
                  <span className="text-slate-400">Alert Trigger Threshold:</span>
                  <div className="font-mono font-bold text-rose-400 text-sm mt-0.5">
                    &gt; {formatScore(aml_model?.score_thresholds?.alert_trigger, 2)}
                  </div>
                </div>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-background-surface border border-border space-y-3">
              <div className="text-xs font-medium text-slate-300">Risk Tier Boundary Mapping:</div>
              <div className="space-y-2 text-xs font-mono">
                <div className="flex justify-between items-center p-2 rounded bg-emerald-500/10 border border-emerald-500/20 text-emerald-300">
                  <span>LOW RISK TIER</span>
                  <span>Score &lt; {formatScore(aml_model?.score_thresholds?.low_risk_below, 2)}</span>
                </div>
                <div className="flex justify-between items-center p-2 rounded bg-amber-500/10 border border-amber-500/20 text-amber-300">
                  <span>MEDIUM RISK TIER</span>
                  <span>{formatScore(aml_model?.score_thresholds?.low_risk_below, 2)} &le; Score &lt; {formatScore(aml_model?.score_thresholds?.high_risk_above, 2)}</span>
                </div>
                <div className="flex justify-between items-center p-2 rounded bg-rose-500/10 border border-rose-500/20 text-rose-300">
                  <span>HIGH RISK TIER (Alert Fired)</span>
                  <span>Score &ge; {formatScore(aml_model?.score_thresholds?.high_risk_above, 2)}</span>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
