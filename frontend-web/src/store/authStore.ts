import { create } from 'zustand';
import { AuthResponse } from '../types/auth';

interface AuthState {
  token: string | null;
  customerUuid: string | null;
  name: string | null;
  email: string | null;
  isAuthenticated: boolean;

  // Track viewed customers in this session (explicitly session-local)
  viewedCustomers: Array<{ customerUuid: string; name: string; viewedAt: string }>;

  setAuth: (data: AuthResponse) => void;
  logout: () => void;
  addViewedCustomer: (customer: { customerUuid: string; name: string }) => void;
}

// Security Architecture Tradeoff:
// The Spring Boot backend returns the JWT directly in the JSON response body rather than issuing
// an HttpOnly cookie. To balance security (preventing long-term XSS token extraction) and UX
// (surviving page refresh during analyst workflow), we store the primary token in-memory (Zustand)
// with a temporary sessionStorage fallback ONLY. NEVER use localStorage here, as localStorage persists
// indefinitely across browser sessions and tabs, making it vulnerable to persistent exfiltration.
const SESSION_KEY = 'finvigil_auth_session';

const getInitialState = () => {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      return {
        token: parsed.token || null,
        customerUuid: parsed.customerUuid || null,
        name: parsed.name || null,
        email: parsed.email || null,
        isAuthenticated: !!parsed.token,
      };
    }
  } catch (e) {
    console.error('Failed to parse auth from sessionStorage', e);
  }
  return {
    token: null,
    customerUuid: null,
    name: null,
    email: null,
    isAuthenticated: false,
  };
};

export const useAuthStore = create<AuthState>((set) => ({
  ...getInitialState(),
  viewedCustomers: [],

  setAuth: (data: AuthResponse) => {
    // Security Note: Token stored in-memory as primary, sessionStorage as refresh-survival only.
    const sessionData = {
      token: data.token,
      customerUuid: data.customerUuid,
      name: data.name,
      email: data.email,
    };
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(sessionData));

    set({
      token: data.token,
      customerUuid: data.customerUuid,
      name: data.name,
      email: data.email,
      isAuthenticated: true,
    });
  },

  logout: () => {
    sessionStorage.removeItem(SESSION_KEY);
    set({
      token: null,
      customerUuid: null,
      name: null,
      email: null,
      isAuthenticated: false,
      viewedCustomers: [],
    });
  },

  addViewedCustomer: (cust: { customerUuid: string; name: string }) => {
    set((state) => {
      const filtered = state.viewedCustomers.filter((c) => c.customerUuid !== cust.customerUuid);
      return {
        viewedCustomers: [
          { ...cust, viewedAt: new Date().toISOString() },
          ...filtered,
        ].slice(0, 10), // Keep top 10 most recent in session
      };
    });
  },
}));
