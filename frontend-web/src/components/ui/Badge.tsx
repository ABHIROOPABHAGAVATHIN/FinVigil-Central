import React from 'react';
import { cn } from '../../lib/utils';
import { RiskLevel } from '../../types/customer';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'risk' | 'status' | 'outline';
  riskLevel?: RiskLevel | string;
  status?: string;
  size?: 'sm' | 'md';
}

export const Badge: React.FC<BadgeProps> = ({
  className,
  variant = 'default',
  riskLevel,
  status,
  size = 'md',
  children,
  ...props
}) => {
  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs font-semibold';

  // Strict backend adherence: backend produces LOW, MEDIUM, HIGH.
  // We include graceful fallback for unrecognized future values without fabricating CRITICAL.
  if (variant === 'risk' || riskLevel) {
    const level = (riskLevel || '').toUpperCase();
    let riskStyles = "bg-slate-800/80 text-slate-300 border border-slate-700"; // fallback

    if (level === 'LOW') {
      riskStyles = "bg-risk-low/15 text-emerald-400 border border-risk-low/30 glow-risk-low";
    } else if (level === 'MEDIUM') {
      riskStyles = "bg-risk-medium/15 text-amber-400 border border-risk-medium/30 glow-risk-medium";
    } else if (level === 'HIGH') {
      riskStyles = "bg-risk-high/15 text-rose-400 border border-risk-high/30 glow-risk-high";
    }

    return (
      <span
        className={cn(
          "inline-flex items-center gap-1.5 rounded-full font-mono uppercase tracking-wider",
          sizeClasses,
          riskStyles,
          className
        )}
        {...props}
      >
        <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulse" />
        {children || level}
      </span>
    );
  }

  if (variant === 'status' || status) {
    const st = (status || '').toUpperCase();
    let statusStyles = "bg-slate-800 text-slate-300 border border-slate-700";

    switch (st) {
      case 'ACTIVE':
      case 'APPROVED':
      case 'COMPLETED':
      case 'RESOLVED':
        statusStyles = "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20";
        break;
      case 'PENDING':
      case 'UNDER_REVIEW':
        statusStyles = "bg-amber-500/10 text-amber-400 border border-amber-500/20";
        break;
      case 'UNDER_INVESTIGATION':
      case 'FLAGGED':
      case 'OPEN':
      case 'REVIEW':
        statusStyles = "bg-rose-500/10 text-rose-400 border border-rose-500/20";
        break;
      case 'REJECTED':
      case 'SUSPENDED':
      case 'FALSE_POSITIVE':
        statusStyles = "bg-slate-800 text-slate-400 border border-slate-700";
        break;
    }

    return (
      <span
        className={cn(
          "inline-flex items-center rounded-md font-mono font-medium tracking-wide uppercase",
          sizeClasses,
          statusStyles,
          className
        )}
        {...props}
      >
        {children || st}
      </span>
    );
  }

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md bg-background-elevated text-slate-300 border border-border font-medium",
        sizeClasses,
        className
      )}
      {...props}
    >
      {children}
    </span>
  );
};
