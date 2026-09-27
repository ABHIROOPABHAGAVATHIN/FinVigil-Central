import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { ShieldCheck, Lock, Mail, AlertTriangle, ArrowRight } from 'lucide-react';
import { authApi } from '../../api/auth';
import { useAuthStore } from '../../store/authStore';
import { useToastStore } from '../../components/ui/Toast';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';

const loginSchema = z.object({
  email: z.string().email('Please enter a valid business email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const { showToast } = useToastStore();

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(data);
      setAuth(response);
      showToast({
        type: 'success',
        title: 'Authentication Successful',
        message: `Welcome back, ${response.name || 'Analyst'}.`,
      });
      navigate('/dashboard');
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Invalid credentials';
      showToast({
        type: 'error',
        title: 'Authentication Failed',
        message: msg,
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-background">
      {/* Left Brand Panel */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-background via-background-surface to-background-elevated border-r border-border p-12 flex-col justify-between relative overflow-hidden">
        {/* Subtle background glow */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-brand-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-cyan-500 flex items-center justify-center shadow-xl shadow-indigo-500/20">
            <ShieldCheck className="w-6 h-6 text-white" />
          </div>
          <div>
            <span className="text-lg font-bold text-white tracking-tight">FinVigil Central</span>
            <span className="block text-[11px] font-mono text-cyan-400 font-semibold tracking-wider uppercase">
              Financial Intelligence Platform
            </span>
          </div>
        </div>

        <div className="relative z-10 max-w-md space-y-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-500/15 border border-brand-500/30 text-indigo-300 text-xs font-mono">
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
            Dual-Engine ML Pipeline Live
          </div>

          <h2 className="text-3xl font-extrabold text-white leading-tight">
            Next-Generation Credit Risk & Real-Time AML Compliance.
          </h2>

          <p className="text-sm text-slate-400 leading-relaxed">
            Unifying XGBoost loan underwriting, Isolation Forest anomaly scoring, deterministic AML rules, and sliding-window Redis velocity into a single compliance intelligence console.
          </p>

          <div className="grid grid-cols-2 gap-4 pt-4 border-t border-border/80">
            <div className="p-3.5 rounded-lg bg-background-elevated/60 border border-border">
              <div className="text-xs text-slate-400">Credit Decisioning</div>
              <div className="text-sm font-semibold text-slate-200 mt-1">XGBoost ML v1.0</div>
              <div className="text-[11px] text-emerald-400 font-mono mt-0.5">ROC-AUC: 0.8918</div>
            </div>
            <div className="p-3.5 rounded-lg bg-background-elevated/60 border border-border">
              <div className="text-xs text-slate-400">Anomaly Detection</div>
              <div className="text-sm font-semibold text-slate-200 mt-1">Isolation Forest</div>
              <div className="text-[11px] text-cyan-400 font-mono mt-0.5">60s Sliding Velocity</div>
            </div>
          </div>
        </div>

        <div className="relative z-10 text-xs text-slate-500 font-mono">
          FinVigil Central v1.0 • Enterprise Bank Grade Security • 24h JWT Session
        </div>
      </div>

      {/* Right Login Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12">
        <div className="w-full max-w-md space-y-8">
          <div>
            <h2 className="text-2xl font-bold text-white tracking-tight">Analyst Sign In</h2>
            <p className="text-xs text-slate-400 mt-1.5">
              Access the Unified Customer Risk & AML Compliance Console.
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input
              label="Business Email"
              type="email"
              placeholder="analyst@finvigil.com"
              leftIcon={<Mail className="w-4 h-4" />}
              error={errors.email?.message}
              {...register('email')}
            />

            <Input
              label="Password"
              type="password"
              placeholder="••••••••••••"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.password?.message}
              {...register('password')}
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full"
              isLoading={isLoading}
            >
              <span>Sign In to Console</span>
              <ArrowRight className="w-4 h-4 ml-2" />
            </Button>
          </form>

          <div className="p-4 rounded-xl bg-background-elevated/80 border border-border text-xs space-y-2">
            <div className="flex items-center gap-2 font-medium text-slate-300">
              <AlertTriangle className="w-4 h-4 text-amber-400" />
              <span>Need a new test customer or analyst account?</span>
            </div>
            <p className="text-[11px] text-slate-400">
              Register a customer record to automatically generate a valid JWT credential and customer profile.
            </p>
            <div className="pt-2">
              <Link
                to="/register"
                className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 underline"
              >
                Create new account →
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
