import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  ShieldAlert,
  Users,
  Sliders,
  Cpu,
  LogOut,
  ChevronRight,
  ShieldCheck,
} from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { cn } from '../../lib/utils';

export const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const { name, email, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { label: 'AML Alerts Queue', path: '/alerts', icon: ShieldAlert },
    { label: 'Rules Engine', path: '/admin/rules', icon: Sliders },
    { label: 'ML Models & AI', path: '/admin/model-info', icon: Cpu },
  ];

  return (
    <aside className="w-64 bg-background-surface border-r border-border flex flex-col justify-between h-screen sticky top-0 shrink-0 select-none">
      <div>
        {/* Brand Header */}
        <div className="h-16 flex items-center gap-3 px-6 border-b border-border">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 via-indigo-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-indigo-500/20">
            <ShieldCheck className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="text-sm font-bold text-slate-100 tracking-tight">FinVigil Central</h1>
            <span className="text-[10px] font-mono uppercase tracking-wider text-cyan-400 font-semibold">
              Risk Intelligence
            </span>
          </div>
        </div>

        {/* Navigation Section */}
        <div className="px-3 py-6 space-y-1">
          <div className="px-3 pb-2 text-[10px] font-mono uppercase tracking-wider text-slate-500 font-semibold">
            Intelligence Console
          </div>
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  cn(
                    "flex items-center justify-between px-3 py-2.5 rounded-lg text-xs font-medium transition-all duration-150 group",
                    isActive
                      ? "bg-brand-500/15 text-indigo-400 font-semibold border border-brand-500/20"
                      : "text-slate-400 hover:text-slate-100 hover:bg-background-elevated"
                  )
                }
              >
                <div className="flex items-center gap-2.5">
                  <Icon className="w-4 h-4 transition-colors group-hover:text-indigo-400" />
                  <span>{item.label}</span>
                </div>
                <ChevronRight className="w-3.5 h-3.5 opacity-0 group-hover:opacity-100 transition-opacity text-slate-500" />
              </NavLink>
            );
          })}
        </div>
      </div>

      {/* User Session Footer */}
      <div className="p-4 border-t border-border bg-background-elevated/40">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded-full bg-brand-500/20 border border-brand-500/30 flex items-center justify-center text-xs font-semibold text-indigo-300 shrink-0">
              {(name || 'U').charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0">
              <div className="text-xs font-semibold text-slate-200 truncate">{name || 'Analyst'}</div>
              <div className="text-[11px] text-slate-500 truncate">{email || 'analyst@finvigil.local'}</div>
            </div>
          </div>
          <button
            onClick={handleLogout}
            title="Sign out"
            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
};
