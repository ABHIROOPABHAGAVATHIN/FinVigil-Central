import React from 'react';
import { formatScore } from '../../lib/utils';

export interface ScoreBreakdownBarProps {
  ruleScore?: number | null;
  anomalyScore?: number | null;
  hybridScore?: number | null;
}

export const ScoreBreakdownBar: React.FC<ScoreBreakdownBarProps> = ({
  ruleScore = 0,
  anomalyScore = 0,
  hybridScore = 0,
}) => {
  const safeRule = Number(ruleScore || 0);
  const safeAnomaly = Number(anomalyScore || 0);
  const safeHybrid = Number(hybridScore || (0.40 * safeRule + 0.60 * safeAnomaly));

  const ruleContribution = (0.40 * safeRule);
  const anomalyContribution = (0.60 * safeAnomaly);

  return (
    <div className="space-y-3 p-4 rounded-xl bg-background-surface/80 border border-border">
      <div className="flex items-center justify-between text-xs">
        <span className="font-medium text-slate-300">Hybrid AML Score Composition</span>
        <span className="font-mono font-bold text-slate-100 bg-background-elevated px-2 py-0.5 rounded border border-border">
          {formatScore(safeHybrid, 4)}
        </span>
      </div>

      {/* Visual Weight Stack Bar */}
      <div className="h-3.5 w-full bg-background-elevated rounded-full overflow-hidden flex border border-border/80">
        <div
          style={{ width: `${Math.min(100, Math.max(0, safeRule * 40))}%` }}
          className="bg-indigo-500 h-full transition-all duration-500 ease-out"
          title={`Rule Score (40% weight): ${formatScore(safeRule)}`}
        />
        <div
          style={{ width: `${Math.min(100, Math.max(0, safeAnomaly * 60))}%` }}
          className="bg-cyan-500 h-full transition-all duration-500 ease-out"
          title={`Anomaly Score (60% weight): ${formatScore(safeAnomaly)}`}
        />
      </div>

      {/* Metric Breakdown Details */}
      <div className="grid grid-cols-2 gap-3 pt-1 text-xs">
        <div className="flex items-center gap-2 p-2 rounded-lg bg-background-elevated/60 border border-border/60">
          <div className="w-2.5 h-2.5 rounded-full bg-indigo-500 shrink-0" />
          <div className="min-w-0">
            <div className="text-[11px] text-slate-400">Deterministic Rules (40%)</div>
            <div className="font-mono font-semibold text-indigo-300">
              {formatScore(safeRule, 4)}{' '}
              <span className="text-[10px] font-normal text-slate-400">
                (+{formatScore(ruleContribution, 3)})
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 p-2 rounded-lg bg-background-elevated/60 border border-border/60">
          <div className="w-2.5 h-2.5 rounded-full bg-cyan-500 shrink-0" />
          <div className="min-w-0">
            <div className="text-[11px] text-slate-400">Isolation Forest ML (60%)</div>
            <div className="font-mono font-semibold text-cyan-300">
              {formatScore(safeAnomaly, 4)}{' '}
              <span className="text-[10px] font-normal text-slate-400">
                (+{formatScore(anomalyContribution, 3)})
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
