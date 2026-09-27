import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, ArrowRight } from 'lucide-react';
import { HealthIndicator } from '../common/HealthIndicator';
import { useAuthStore } from '../../store/authStore';

export const Navbar: React.FC = () => {
  const [searchInput, setSearchInput] = useState('');
  const navigate = useNavigate();
  const { viewedCustomers } = useAuthStore();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const query = searchInput.trim();
    if (query) {
      navigate(`/customers/${encodeURIComponent(query)}`);
      setSearchInput('');
    }
  };

  return (
    <header className="h-16 bg-background/80 backdrop-blur-md border-b border-border px-8 flex items-center justify-between sticky top-0 z-30">
      {/* Customer Quick Lookup Search Bar */}
      <form onSubmit={handleSearch} className="max-w-md w-full relative flex items-center">
        <Search className="w-4 h-4 text-slate-400 absolute left-3 pointer-events-none" />
        <input
          type="text"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Lookup customer by UUID or numeric ID (e.g. 7 or uuid)..."
          className="w-full bg-background-elevated border border-border rounded-lg pl-9 pr-20 py-1.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500 transition-colors font-mono"
        />
        <button
          type="submit"
          disabled={!searchInput.trim()}
          className="absolute right-1.5 px-2 py-1 bg-brand-500/20 text-indigo-400 hover:bg-brand-500 hover:text-white rounded text-[11px] font-medium transition-colors disabled:opacity-30 disabled:pointer-events-none flex items-center gap-1"
        >
          <span>Find</span>
          <ArrowRight className="w-3 h-3" />
        </button>
      </form>

      {/* Right Controls: Session Count & Live System Health */}
      <div className="flex items-center gap-6">
        {viewedCustomers.length > 0 && (
          <div className="hidden md:flex items-center gap-1.5 text-xs text-slate-400">
            <span className="text-[11px] text-slate-500">Session Lookups:</span>
            <span className="font-mono px-2 py-0.5 rounded bg-background-elevated border border-border text-slate-200">
              {viewedCustomers.length}
            </span>
          </div>
        )}

        {/* Real-time Health Pings */}
        <HealthIndicator />
      </div>
    </header>
  );
};
