import React from 'react';
import { cn } from '../../lib/utils';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  glow?: 'none' | 'brand' | 'low' | 'medium' | 'high';
}

export const Card: React.FC<CardProps> = ({
  className,
  glow = 'none',
  children,
  ...props
}) => {
  const glows = {
    none: '',
    brand: 'border-brand-500/30 glow-brand',
    low: 'border-risk-low/30 glow-risk-low',
    medium: 'border-risk-medium/30 glow-risk-medium',
    high: 'border-risk-high/30 glow-risk-high',
  };

  return (
    <div
      className={cn(
        "rounded-xl bg-background-card border border-border transition-all duration-200 overflow-hidden",
        glows[glow],
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
};

export const CardHeader: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div className={cn("px-6 py-4 border-b border-border/80 flex items-center justify-between", className)} {...props}>
    {children}
  </div>
);

export const CardTitle: React.FC<React.HTMLAttributes<HTMLHeadingElement>> = ({
  className,
  children,
  ...props
}) => (
  <h3 className={cn("text-base font-semibold text-slate-100 flex items-center gap-2", className)} {...props}>
    {children}
  </h3>
);

export const CardDescription: React.FC<React.HTMLAttributes<HTMLParagraphElement>> = ({
  className,
  children,
  ...props
}) => (
  <p className={cn("text-xs text-slate-400 mt-0.5", className)} {...props}>
    {children}
  </p>
);

export const CardContent: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div className={cn("p-6", className)} {...props}>
    {children}
  </div>
);

export const CardFooter: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div className={cn("px-6 py-3 bg-background-surface/50 border-t border-border/60 flex items-center justify-between text-xs text-slate-400", className)} {...props}>
    {children}
  </div>
);
