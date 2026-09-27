import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute } from './components/layout/ProtectedRoute';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { CustomerProfilePage } from './pages/customer/CustomerProfilePage';
import { ApplyCreditPage } from './pages/customer/ApplyCreditPage';
import { NewTransactionPage } from './pages/customer/NewTransactionPage';
import { AlertsQueuePage } from './pages/alerts/AlertsQueuePage';
import { AlertDetailPage } from './pages/alerts/AlertDetailPage';
import { RulesPage } from './pages/admin/RulesPage';
import { ModelInfoPage } from './pages/admin/ModelInfoPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5000,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Public Auth Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Protected Console Routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />

              {/* Customer Intelligence & Actions */}
              <Route path="/customers/:idOrUuid" element={<CustomerProfilePage />} />
              <Route path="/customers/:id/apply-credit" element={<ApplyCreditPage />} />
              <Route path="/customers/:id/transactions/new" element={<NewTransactionPage />} />

              {/* AML Alerts Investigation */}
              <Route path="/alerts" element={<AlertsQueuePage />} />
              <Route path="/alerts/:alertUuid" element={<AlertDetailPage />} />

              {/* Administrative & Technical Diagnostic Views */}
              <Route path="/admin/rules" element={<RulesPage />} />
              <Route path="/admin/model-info" element={<ModelInfoPage />} />
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default App;
