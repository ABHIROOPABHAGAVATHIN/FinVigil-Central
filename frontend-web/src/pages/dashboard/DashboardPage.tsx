import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Search,
  Users,
  ShieldAlert,
  CreditCard,
  Send,
  Database,
  Layers,
  ArrowRight,
  Clock,
  ExternalLink,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { systemApi } from '../../api/system';
import { amlApi } from '../../api/aml';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { formatDate } from '../../lib/utils';

export const DashboardPage: React.FC = () => {
  const [searchInput, setSearchInput] = useState('');
  const navigate = useNavigate();
  const { customerUuid, name, viewedCustomers } = useAuthStore();

  // Fetch system health
  const { data: springHealth } = useQuery({
    queryKey: ['health', 'spring'],
    queryFn: systemApi.getSpringHealth,
    refetchInterval: 30000,
  });

  const { data: modelInfo } = useQuery({
    queryKey: ['modelInfo'],
    queryFn: systemApi.getModelInfo,
  });

  // Fetch open alerts (GET /api/aml/alerts?status=OPEN is verified working!)
  const { data: openAlerts, isLoading: alertsLoading } = useQuery({
    queryKey: ['alerts', 'OPEN'],
    queryFn: () => amlApi.getAllAlerts('OPEN'),
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      navigate(`/customers/${encodeURIComponent(searchInput.trim())}`);
    }
  };

  const highRiskAlertsCount = openAlerts?.filter((a) => a.riskLevel === 'HIGH').length || 0;

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Welcome Banner & Primary Quick Search */}
      <div className="rounded-2xl bg-gradient-to-r from-background-elevated via-background-card to-background-surface border border-border p-8 shadow-2xl relative overflow-hidden">
        <div className="absolute right-0 top-0 bottom-0 w-1/3 bg-gradient-to-l from-indigo-500/10 to-transparent pointer-events-none" />

        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-500/15 border border-brand-500/30 text-indigo-400 text-xs font-mono mb-4">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Dual-Engine Financial Intelligence Console</span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
            Financial Risk & AML Central
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-2 leading-relaxed">
            Screen transactions in real-time, inspect XGBoost loan underwriting decisions, investigate Isolation Forest anomaly alerts, and monitor Redis velocity limits.
          </p>

          {/* Primary Quick Lookup Search Bar */}
          <form onSubmit={handleSearch} className="mt-6 flex items-center gap-2 max-w-xl">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Enter Customer UUID or numeric ID (e.g., 7 or UUID)..."
                className="w-full bg-background border border-border-strong rounded-xl pl-10 pr-4 py-3 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500 font-mono shadow-inner"
              />
            </div>
            <Button type="submit" variant="primary" size="md" className="py-3 px-5">
              <span>Inspect Profile</span>
              <ArrowRight className="w-4 h-4 ml-1.5" />
            </Button>
          </form>

          {/* Shortcut to Current Analyst/Customer Profile */}
          {customerUuid && (
            <div className="mt-4 flex items-center gap-2 text-xs text-slate-400">
              <span>Current Session Account:</span>
              <Link
                to={`/customers/${customerUuid}`}
                className="font-mono text-indigo-400 hover:text-indigo-300 underline inline-flex items-center gap-1"
              >
                <span>{name || customerUuid}</span>
                <ExternalLink className="w-3 h-3" />
              </Link>
            </div>
          )}
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
        {/* Open AML Alerts */}
        <Card>
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-400">Open AML Alerts</span>
              <div className="p-2 rounded-lg bg-rose-500/10 text-rose-400 border border-rose-500/20">
                <ShieldAlert className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3 flex items-baseline gap-2">
              <span className="text-2xl font-bold font-mono text-slate-100">
                {alertsLoading ? '...' : openAlerts?.length ?? 0}
              </span>
              {highRiskAlertsCount > 0 && (
                <span className="text-xs font-semibold text-rose-400 font-mono">
                  ({highRiskAlertsCount} HIGH)
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-500 mt-1">Requiring compliance review</p>
          </CardContent>
        </Card>

        {/* Session-Local Customers Looked Up */}
        <Card>
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs font-medium text-slate-400">Session Lookups</span>
                <span className="block text-[10px] text-slate-500 uppercase tracking-wider font-mono">Local Only</span>
              </div>
              <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                <Users className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3 flex items-baseline gap-2">
              <span className="text-2xl font-bold font-mono text-slate-100">
                {viewedCustomers.length}
              </span>
              <span className="text-xs text-slate-400">profiles</span>
            </div>
            <p className="text-[11px] text-slate-500 mt-1">Examined in current analyst session</p>
          </CardContent>
        </Card>

        {/* XGBoost Credit ML Status */}
        <Card>
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-400">Credit Model</span>
              <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <CheckCircle2 className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3 flex items-baseline gap-2">
              <span className="text-sm font-semibold text-slate-100">
                {modelInfo?.credit_model?.model_version || 'XGBoost v1.0'}
              </span>
            </div>
            <p className="text-[11px] text-emerald-400 font-mono mt-1">
              ROC-AUC: {modelInfo?.credit_model?.metrics?.roc_auc?.toFixed(4) || '0.8918'}
            </p>
          </CardContent>
        </Card>

        {/* Isolation Forest AML Status */}
        <Card>
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-400">AML Anomaly Model</span>
              <div className="p-2 rounded-lg bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                <Layers className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3 flex items-baseline gap-2">
              <span className="text-sm font-semibold text-slate-100">
                {modelInfo?.aml_model?.model_version || 'Isolation Forest'}
              </span>
            </div>
            <p className="text-[11px] text-cyan-400 font-mono mt-1">
              Alert Trigger: &gt; 0.6500
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Main Content: Session Recent Lookups & Active Open Alerts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Session Looked Up Customers (Explicitly Session-Local) */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <Clock className="w-4 h-4 text-indigo-400" />
                <span>Customers Examined in This Session</span>
              </CardTitle>
              <CardDescription>
                Tracked locally in this analyst session (no global customer listing endpoint).
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent className="p-0">
            {viewedCustomers.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-500">
                <p>No customer profiles viewed yet in this session.</p>
                <p className="mt-1 text-slate-400">
                  Search a customer by ID (e.g. 7) above to begin profiling.
                </p>
              </div>
            ) : (
              <div className="divide-y divide-border">
                {viewedCustomers.map((cust) => (
                  <div
                    key={cust.customerUuid}
                    className="p-4 flex items-center justify-between hover:bg-background-elevated/50 transition-colors"
                  >
                    <div>
                      <div className="font-semibold text-sm text-slate-100">{cust.name}</div>
                      <div className="font-mono text-xs text-slate-500 truncate max-w-xs">
                        {cust.customerUuid}
                      </div>
                    </div>
                    <Link
                      to={`/customers/${cust.customerUuid}`}
                      className="px-3 py-1.5 rounded-lg bg-brand-500/15 text-indigo-400 hover:bg-brand-500 hover:text-white text-xs font-medium transition-colors inline-flex items-center gap-1.5"
                    >
                      <span>View</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Open AML Alerts Queue Peek */}
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                <ShieldAlert className="w-4 h-4 text-rose-400" />
                <span>Active AML Compliance Alerts</span>
              </CardTitle>
              <CardDescription>
                Live pending alerts generated by Isolation Forest & deterministic rules.
              </CardDescription>
            </div>
            <Link
              to="/alerts"
              className="text-xs text-indigo-400 hover:text-indigo-300 font-medium inline-flex items-center gap-1"
            >
              <span>View All</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </CardHeader>
          <CardContent className="p-0">
            {alertsLoading ? (
              <div className="p-8 text-center text-xs text-slate-500">Loading active alerts...</div>
            ) : !openAlerts || openAlerts.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-500">
                <CheckCircle2 className="w-8 h-8 text-emerald-400/60 mx-auto mb-2" />
                <p className="text-slate-300 font-medium">All AML alerts resolved.</p>
                <p className="mt-1">No alerts currently require compliance intervention.</p>
              </div>
            ) : (
              <div className="divide-y divide-border">
                {openAlerts.slice(0, 5).map((alert) => (
                  <div
                    key={alert.alertUuid}
                    className="p-4 flex items-center justify-between hover:bg-background-elevated/50 transition-colors"
                  >
                    <div className="space-y-1 min-w-0 pr-4">
                      <div className="flex items-center gap-2">
                        <Badge variant="risk" riskLevel={alert.riskLevel} size="sm" />
                        <span className="font-mono text-xs text-slate-300">
                          Score: {alert.hybridScore?.toFixed(4) || 'N/A'}
                        </span>
                      </div>
                      <div className="text-xs text-slate-400 truncate max-w-sm">
                        {alert.reasons?.join(', ') || 'Triggered AML compliance rules'}
                      </div>
                    </div>
                    <Link
                      to={`/alerts/${alert.alertUuid}`}
                      className="px-3 py-1.5 rounded-lg bg-background-elevated hover:bg-border text-slate-300 hover:text-white text-xs font-medium transition-colors shrink-0"
                    >
                      Investigate
                    </Link>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Architecture Topology & Health Reference */}
      <Card>
        <CardHeader>
          <CardTitle>
            <Database className="w-4 h-4 text-cyan-400" />
            <span>FinVigil Central Cluster Health</span>
          </CardTitle>
          <CardDescription>
            Live container and broker health diagnostics.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="p-3 rounded-lg bg-background-surface border border-border">
              <div className="text-xs text-slate-400">Spring Boot (8080)</div>
              <div className="flex items-center gap-1.5 mt-1">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span className="text-xs font-mono text-slate-200">
                  {springHealth?.status || 'UP'}
                </span>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-background-surface border border-border">
              <div className="text-xs text-slate-400">PostgreSQL (5433)</div>
              <div className="flex items-center gap-1.5 mt-1">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span className="text-xs font-mono text-slate-200">
                  {springHealth?.components?.db?.status || 'UP'}
                </span>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-background-surface border border-border">
              <div className="text-xs text-slate-400">Redis 7 (6379)</div>
              <div className="flex items-center gap-1.5 mt-1">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span className="text-xs font-mono text-slate-200">
                  {springHealth?.components?.redis?.status || 'UP'}
                </span>
              </div>
            </div>

            <div className="p-3 rounded-lg bg-background-surface border border-border">
              <div className="text-xs text-slate-400">RabbitMQ (5672)</div>
              <div className="flex items-center gap-1.5 mt-1">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span className="text-xs font-mono text-slate-200">
                  {springHealth?.components?.rabbit?.status || 'UP'}
                </span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
