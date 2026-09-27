import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { systemApi } from '../../api/system';

export const HealthIndicator: React.FC = () => {
  const { data: springHealth, isError: isSpringError } = useQuery({
    queryKey: ['health', 'spring'],
    queryFn: systemApi.getSpringHealth,
    refetchInterval: 30000,
    retry: 1,
  });

  const { data: pythonHealth, isError: isPythonError } = useQuery({
    queryKey: ['health', 'python'],
    queryFn: systemApi.getPythonHealth,
    refetchInterval: 30000,
    retry: 1,
  });

  const isSpringUp = !isSpringError && springHealth?.status === 'UP';
  const isPythonUp = !isPythonError && pythonHealth?.status === 'UP';

  return (
    <div className="flex items-center gap-3 text-xs font-mono">
      {/* Spring Boot Service Status */}
      <div
        className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-background-elevated border border-border"
        title={isSpringUp ? "Spring Boot (Port 8080) UP - Postgres, Redis, RabbitMQ Healthy" : "Spring Boot (Port 8080) Offline"}
      >
        <span
          className={`w-2 h-2 rounded-full ${
            isSpringUp ? 'bg-emerald-400 shadow-sm shadow-emerald-400/50' : 'bg-rose-500 animate-pulse'
          }`}
        />
        <span className="text-slate-300">Java :8080</span>
      </div>

      {/* Python ML Service Status */}
      <div
        className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-background-elevated border border-border"
        title={isPythonUp ? "FastAPI ML Service (Port 8000) UP - XGBoost & Isolation Forest Loaded" : "FastAPI ML Service (Port 8000) Offline"}
      >
        <span
          className={`w-2 h-2 rounded-full ${
            isPythonUp ? 'bg-emerald-400 shadow-sm shadow-emerald-400/50' : 'bg-rose-500 animate-pulse'
          }`}
        />
        <span className="text-slate-300">Python :8000</span>
      </div>
    </div>
  );
};
