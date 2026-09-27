import React from 'react';
import { formatCurrency } from '../../lib/utils';
import { Activity, Clock } from 'lucide-react';

export interface VelocityMeterProps {
  count: number;
  totalAmount: number;
  countLimit?: number;
  amountLimit?: number;
}

export const VelocityMeter: React.FC<VelocityMeterProps> = ({
  count = 0,
  totalAmount = 0,
  countLimit = 5,
  amountLimit = 50000,
}) => {
  const countPercent = Math.min(100, Math.round((count / countLimit) * 100));
  const amountPercent = Math.min(100, Math.round((totalAmount / amountLimit) * 100));

  const isCountWarning = count >= countLimit;
  const isAmountWarning = totalAmount >= amountLimit;

  return (
    <div className="space-y-4 p-4 rounded-xl bg-background-surface/80 border border-border">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Activity className="w-4 h-4 text-cyan-400" />
          <h4 className="text-xs font-semibold text-slate-200">Redis Velocity Monitor</h4>
        </div>
        <div className="flex items-center gap-1.5 text-[11px] text-slate-400 font-mono">
          <Clock className="w-3 h-3 text-slate-500" />
          <span>60s Sliding Window</span>
        </div>
      </div>

      {/* Transaction Count Progress */}
      <div className="space-y-1.5">
        <div className="flex justify-between text-xs">
          <span className="text-slate-400">Transaction Frequency</span>
          <span className="font-mono font-medium text-slate-200">
            <span className={isCountWarning ? "text-rose-400 font-bold" : "text-slate-100"}>
              {count}
            </span>{" "}
            / {countLimit} txns
          </span>
        </div>
        <div className="h-2 w-full bg-background-elevated rounded-full overflow-hidden border border-border/80">
          <div
            style={{ width: `${countPercent}%` }}
            className={`h-full transition-all duration-500 ${
              isCountWarning ? 'bg-rose-500' : countPercent > 60 ? 'bg-amber-500' : 'bg-cyan-500'
            }`}
          />
        </div>
      </div>

      {/* Transaction Amount Progress */}
      <div className="space-y-1.5">
        <div className="flex justify-between text-xs">
          <span className="text-slate-400">Cumulative Volume</span>
          <span className="font-mono font-medium text-slate-200">
            <span className={isAmountWarning ? "text-rose-400 font-bold" : "text-slate-100"}>
              {formatCurrency(totalAmount)}
            </span>{" "}
            / {formatCurrency(amountLimit)}
          </span>
        </div>
        <div className="h-2 w-full bg-background-elevated rounded-full overflow-hidden border border-border/80">
          <div
            style={{ width: `${amountPercent}%` }}
            className={`h-full transition-all duration-500 ${
              isAmountWarning ? 'bg-rose-500' : amountPercent > 60 ? 'bg-amber-500' : 'bg-brand-500'
            }`}
          />
        </div>
      </div>

      {(isCountWarning || isAmountWarning) && (
        <div className="p-2.5 rounded-lg bg-rose-500/10 border border-rose-500/20 text-xs text-rose-300 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-rose-400 animate-ping" />
          <span>Velocity threshold exceeded: Next transaction will trigger high-frequency AML rule!</span>
        </div>
      )}
    </div>
  );
};
