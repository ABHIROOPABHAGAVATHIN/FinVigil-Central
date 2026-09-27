import React from 'react';
import { RiskLevel } from '../../types/customer';
import { cn, formatScore } from '../../lib/utils';

export interface ScoreGaugeProps {
  score: number | null | undefined;
  riskLevel?: RiskLevel | string;
  size?: 'sm' | 'md' | 'lg';
  label?: string;
  sublabel?: string;
}

export const ScoreGauge: React.FC<ScoreGaugeProps> = ({
  score,
  riskLevel,
  size = 'md',
  label = 'Risk Score',
  sublabel,
}) => {
  const safeScore = score !== null && score !== undefined && !isNaN(Number(score)) ? Number(score) : 0;
  const clamped = Math.max(0, Math.min(1, safeScore));
  const percentage = Math.round(clamped * 100);

  // Derive risk level if not provided: <0.30 LOW, 0.30-0.60 MEDIUM, >=0.60 HIGH
  let level = riskLevel;
  if (!level) {
    if (clamped >= 0.60) level = 'HIGH';
    else if (clamped >= 0.30) level = 'MEDIUM';
    else level = 'LOW';
  }

  const colorMap = {
    LOW: {
      stroke: '#10B981',
      bg: 'rgba(16, 185, 129, 0.1)',
      text: 'text-emerald-400',
    },
    MEDIUM: {
      stroke: '#F59E0B',
      bg: 'rgba(245, 158, 11, 0.1)',
      text: 'text-amber-400',
    },
    HIGH: {
      stroke: '#EF4444',
      bg: 'rgba(239, 68, 68, 0.1)',
      text: 'text-rose-400',
    },
  };

  const currentTheme = colorMap[level as keyof typeof colorMap] || colorMap.LOW;

  const dimensions = {
    sm: { radius: 36, strokeWidth: 6, width: 90, height: 90, text: 'text-lg', label: 'text-[10px]' },
    md: { radius: 52, strokeWidth: 8, width: 130, height: 130, text: 'text-2xl', label: 'text-xs' },
    lg: { radius: 70, strokeWidth: 10, width: 170, height: 170, text: 'text-3xl', label: 'text-sm' },
  };

  const dim = dimensions[size];
  const circumference = 2 * Math.PI * dim.radius;
  const strokeDashoffset = circumference - (clamped * circumference);

  return (
    <div className="flex flex-col items-center justify-center text-center">
      <div className="relative flex items-center justify-center" style={{ width: dim.width, height: dim.height }}>
        <svg
          className="transform -rotate-90"
          width={dim.width}
          height={dim.height}
        >
          {/* Background circle */}
          <circle
            cx={dim.width / 2}
            cy={dim.height / 2}
            r={dim.radius}
            stroke="#1C2538"
            strokeWidth={dim.strokeWidth}
            fill="transparent"
          />
          {/* Progress circle */}
          <circle
            cx={dim.width / 2}
            cy={dim.height / 2}
            r={dim.radius}
            stroke={currentTheme.stroke}
            strokeWidth={dim.strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            fill="transparent"
            className="transition-all duration-700 ease-out"
          />
        </svg>

        <div className="absolute flex flex-col items-center justify-center">
          <span className={cn("font-mono font-bold tracking-tight", dim.text, currentTheme.text)}>
            {score !== null && score !== undefined ? formatScore(score, 3) : '0.000'}
          </span>
          <span className={cn("font-medium text-slate-400 uppercase tracking-wider", dim.label)}>
            {level}
          </span>
        </div>
      </div>

      {label && <p className="mt-2 text-xs font-medium text-slate-300">{label}</p>}
      {sublabel && <p className="text-[11px] text-slate-500">{sublabel}</p>}
    </div>
  );
};
