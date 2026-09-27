import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { ShieldCheck, User, Mail, Phone, Lock, ArrowRight } from 'lucide-react';
import { authApi } from '../../api/auth';
import { useAuthStore } from '../../store/authStore';
import { useToastStore } from '../../components/ui/Toast';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';

const registerSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters').max(100, 'Name cannot exceed 100 characters'),
  email: z.string().email('Please enter a valid email address'),
  phone: z.string().regex(/^[0-9+ -]{8,20}$/, 'Phone must be between 8 and 20 digits (optional + and spaces)'),
  password: z.string().min(8, 'Password must be at least 8 characters').max(64, 'Password cannot exceed 64 characters'),
});

type RegisterFormData = z.infer<typeof registerSchema>;

export const RegisterPage: React.FC = () => {
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const { showToast } = useToastStore();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      name: '',
      email: '',
      phone: '',
      password: '',
    },
  });

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    try {
      const response = await authApi.register(data);
      setAuth(response);
      showToast({
        type: 'success',
        title: 'Customer Record Created',
        message: `Welcome, ${response.name}! Your account has been registered.`,
      });
      // Navigate directly to the newly registered customer's unified profile!
      navigate(`/customers/${response.customerUuid}`);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Registration failed';
      showToast({
        type: 'error',
        title: 'Registration Error',
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
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-brand-500/10 rounded-full blur-3xl pointer-events-none" />

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
          <h2 className="text-3xl font-extrabold text-white leading-tight">
            Register New Customer Entity
          </h2>
          <p className="text-sm text-slate-400 leading-relaxed">
            Registering initializes a Customer record in PostgreSQL, hashes credentials with BCrypt, and issues a 24-hour HMAC-SHA512 JWT token for instant underwriting and AML screening.
          </p>
        </div>

        <div className="relative z-10 text-xs text-slate-500 font-mono">
          Strict Compliance Standards • Automated Customer UUID Generation
        </div>
      </div>

      {/* Right Registration Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12">
        <div className="w-full max-w-md space-y-8">
          <div>
            <h2 className="text-2xl font-bold text-white tracking-tight">Create Account</h2>
            <p className="text-xs text-slate-400 mt-1.5">
              Enter customer entity details to initiate identity verification.
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Full Legal Name"
              placeholder="e.g. Johnathan Vance"
              leftIcon={<User className="w-4 h-4" />}
              error={errors.name?.message}
              {...register('name')}
            />

            <Input
              label="Email Address"
              type="email"
              placeholder="johnathan.vance@example.com"
              leftIcon={<Mail className="w-4 h-4" />}
              error={errors.email?.message}
              {...register('email')}
            />

            <Input
              label="Phone Number"
              placeholder="+1-555-0199"
              leftIcon={<Phone className="w-4 h-4" />}
              error={errors.phone?.message}
              {...register('phone')}
            />

            <Input
              label="Password"
              type="password"
              placeholder="At least 8 characters"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.password?.message}
              {...register('password')}
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full mt-2"
              isLoading={isLoading}
            >
              <span>Complete Registration</span>
              <ArrowRight className="w-4 h-4 ml-2" />
            </Button>
          </form>

          <div className="text-center text-xs text-slate-400">
            Already have an active credential?{' '}
            <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-semibold underline">
              Sign in here
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
