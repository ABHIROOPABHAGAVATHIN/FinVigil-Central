import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Sliders, CheckCircle2, ShieldAlert, Play, RefreshCw, Info } from 'lucide-react';
import { amlApi } from '../../api/aml';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Select } from '../../components/ui/Select';
import { Badge } from '../../components/ui/Badge';
import { formatScore } from '../../lib/utils';

export const RulesPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'rules' | 'evaluator'>('rules');

  // Sandbox form state
  const [amount, setAmount] = useState<number>(75000);
  const [txnType, setTxnType] = useState('TRANSFER');
  const [merchant, setMerchant] = useState('Crypto Casino Royale');
  const [velocityCount, setVelocityCount] = useState<number>(6);
  const [velocityAmount, setVelocityAmount] = useState<number>(85000);
  const [evalResult, setEvalResult] = useState<any>(null);
  const [isEvaluating, setIsEvaluating] = useState(false);

  const {
    data: rules,
    isLoading,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ['amlRules'],
    queryFn: amlApi.getActiveRules,
  });

  const handleEvaluate = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsEvaluating(true);
    try {
      const res = await amlApi.evaluateTransaction({
        amount: Number(amount),
        transactionType: txnType,
        merchant,
        currency: 'INR',
        velocityCount: Number(velocityCount),
        velocityAmount: Number(velocityAmount),
      });
      setEvalResult(res);
    } catch (e) {
      console.error('Failed to evaluate rules', e);
    } finally {
      setIsEvaluating(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8 pb-12">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-indigo-400 uppercase tracking-wider mb-1">
            <Sliders className="w-3.5 h-3.5" />
            <span>Deterministic AML Compliance Framework</span>
          </div>
          <h1 className="text-2xl font-bold text-slate-100">AML Rules Engine</h1>
          <p className="text-xs text-slate-400 mt-1">
            Deterministic rule definitions, base weights, and live compliance simulation sandbox.
          </p>
        </div>

        <div className="flex items-center gap-2 bg-background-surface p-1 rounded-xl border border-border">
          <button
            onClick={() => setActiveTab('rules')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              activeTab === 'rules'
                ? 'bg-brand-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Active Rules ({rules?.length ?? 0})
          </button>
          <button
            onClick={() => setActiveTab('evaluator')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              activeTab === 'evaluator'
                ? 'bg-brand-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Interactive Sandbox
          </button>
        </div>
      </div>

      {activeTab === 'rules' ? (
        /* Active Rules Table */
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <span>Registered AML Policy Matrix</span>
              </CardTitle>
              <CardDescription>
                Rules contributing to the 40% deterministic component of the hybrid score.
              </CardDescription>
            </div>
            <button
              onClick={() => refetch()}
              className="p-2 rounded-lg bg-background-surface hover:bg-background-elevated text-slate-400 hover:text-white border border-border"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isFetching ? 'animate-spin text-brand-500' : ''}`} />
            </button>
          </CardHeader>
          <CardContent className="p-0">
            {isLoading ? (
              <div className="p-12 text-center text-xs text-slate-400">Loading active rules...</div>
            ) : !rules || rules.length === 0 ? (
              <div className="p-12 text-center text-xs text-slate-400">No active rules loaded.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-background-surface/80 border-b border-border text-slate-400 font-mono uppercase tracking-wider">
                    <tr>
                      <th className="px-6 py-3">Rule Name</th>
                      <th className="px-6 py-3">Rule Type</th>
                      <th className="px-6 py-3">Base Weight</th>
                      <th className="px-6 py-3">Description & Trigger Condition</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    {rules.map((rule, i) => (
                      <tr key={i} className="hover:bg-background-elevated/40 transition-colors">
                        <td className="px-6 py-4 font-mono font-semibold text-slate-200">
                          {rule.ruleName}
                        </td>
                        <td className="px-6 py-4">
                          <span className="font-mono text-xs px-2 py-0.5 rounded bg-background-elevated border border-border text-slate-300">
                            {rule.ruleType}
                          </span>
                        </td>
                        <td className="px-6 py-4 font-mono text-cyan-400 font-bold">
                          {formatScore(rule.baseWeight, 2)}
                        </td>
                        <td className="px-6 py-4 text-slate-300 max-w-md">
                          {rule.description}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      ) : (
        /* Sandbox Evaluator */
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <Card>
            <CardHeader>
              <CardTitle>
                <Play className="w-4 h-4 text-cyan-400" />
                <span>Simulate Transaction Against Rules</span>
              </CardTitle>
              <CardDescription>
                Test rule triggers without modifying database records.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleEvaluate} className="space-y-4">
                <Input
                  label="Transaction Amount (₹)"
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(Number(e.target.value))}
                />

                <Select
                  label="Transaction Type"
                  value={txnType}
                  onChange={(e) => setTxnType(e.target.value)}
                  options={[
                    { value: 'TRANSFER', label: 'TRANSFER' },
                    { value: 'PURCHASE', label: 'PURCHASE' },
                    { value: 'WITHDRAWAL', label: 'WITHDRAWAL' },
                    { value: 'DEPOSIT', label: 'DEPOSIT' },
                  ]}
                />

                <Input
                  label="Merchant / Counterparty"
                  value={merchant}
                  onChange={(e) => setMerchant(e.target.value)}
                />

                <div className="grid grid-cols-2 gap-4">
                  <Input
                    label="Velocity Count (60s)"
                    type="number"
                    value={velocityCount}
                    onChange={(e) => setVelocityCount(Number(e.target.value))}
                  />

                  <Input
                    label="Velocity Amount (₹ 60s)"
                    type="number"
                    value={velocityAmount}
                    onChange={(e) => setVelocityAmount(Number(e.target.value))}
                  />
                </div>

                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  className="w-full mt-2"
                  isLoading={isEvaluating}
                >
                  <span>Evaluate Compliance Rules</span>
                </Button>
              </form>
            </CardContent>
          </Card>

          {/* Sandbox Evaluation Results */}
          <Card>
            <CardHeader>
              <CardTitle>
                <span>Evaluation Output</span>
              </CardTitle>
              <CardDescription>Live result from /api/aml/rules/evaluate</CardDescription>
            </CardHeader>
            <CardContent>
              {!evalResult ? (
                <div className="p-8 text-center text-xs text-slate-500 space-y-2">
                  <Info className="w-8 h-8 text-slate-600 mx-auto" />
                  <p>Submit parameters to observe deterministic rule output.</p>
                </div>
              ) : (
                <div className="space-y-5">
                  <div className="flex items-center justify-between p-4 rounded-xl bg-background-surface border border-border">
                    <div>
                      <span className="text-xs text-slate-400">Trigger Status</span>
                      <div className="mt-1 flex items-center gap-2">
                        {evalResult.triggered ? (
                          <div className="flex items-center gap-1.5 text-rose-400 font-bold text-sm">
                            <ShieldAlert className="w-4 h-4" />
                            <span>RULES TRIGGERED</span>
                          </div>
                        ) : (
                          <div className="flex items-center gap-1.5 text-emerald-400 font-bold text-sm">
                            <CheckCircle2 className="w-4 h-4" />
                            <span>CLEARED</span>
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="text-right">
                      <span className="text-xs text-slate-400">Total Rule Score</span>
                      <div className="font-mono font-bold text-lg text-indigo-300 mt-0.5">
                        {formatScore(evalResult.totalRuleScore, 4)}
                      </div>
                    </div>
                  </div>

                  {evalResult.triggeredRules && evalResult.triggeredRules.length > 0 && (
                    <div className="space-y-2">
                      <span className="text-xs font-semibold text-slate-300">
                        Triggered Rules List:
                      </span>
                      <div className="space-y-1.5">
                        {evalResult.triggeredRules.map((ruleName: string, idx: number) => (
                          <div
                            key={idx}
                            className="p-2.5 rounded-lg bg-rose-500/10 border border-rose-500/20 text-xs font-mono text-rose-300 flex items-center gap-2"
                          >
                            <span className="w-1.5 h-1.5 rounded-full bg-rose-400" />
                            <span>{ruleName}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};
