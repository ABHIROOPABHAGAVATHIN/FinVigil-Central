# FinVigil Central — Frontend Web Console

Modern, compliance-grade financial risk intelligence & AML dashboard built for **FinVigil Central**. Designed with an analyst-first, dark financial console aesthetic inspired by Stripe Radar, Chainalysis, and Plaid.

---

## ⚡ Tech Stack

- **React 18** + **TypeScript** + **Vite**
- **Tailwind CSS** with specialized dark financial console palette (`#0B0F17` backdrop, `#161F33` card surface)
- **Strict Risk Color Semantics**:
  - `LOW` = Emerald (`#10B981`)
  - `MEDIUM` = Amber (`#F59E0B`)
  - `HIGH` = Rose / Red (`#EF4444`)
  - *No fabricated `CRITICAL` badges for real backend data*
- **TanStack Query (React Query)**: Automatic caching, window refetch, and interval polling for asynchronous ML underwriting & AML event generation.
- **React Router v6**: Route guards, nested application layout, customer parameter routing.
- **React Hook Form + Zod**: Strict client-side validation reflecting backend Bean Validation rules.
- **Recharts**: Radial risk gauges, score breakdown bars, and feature importance distributions.
- **Axios with JWT Interceptor**: Automatic `Authorization: Bearer <token>` injection, 401 redirect handling.
- **Zustand**: In-memory session state with `sessionStorage` fallback (never `localStorage`, protecting tokens from persistent exfiltration).

---

## 🚀 Getting Started

### 1. Prerequisites
- **Node.js**: v18+ (tested on Node v22.21.0)
- **Backend Services**:
  - Spring Boot running on `http://localhost:8080`
  - FastAPI ML Service running on `http://localhost:8000`

### 2. Installation
```bash
cd frontend-web
npm install
```

### 3. Environment Configuration
Copy `.env.example` to `.env` (already preconfigured for local development):
```ini
VITE_API_BASE_URL=http://localhost:8080
VITE_ML_API_BASE_URL=http://localhost:8000
```

### 4. Run Development Server
```bash
npm run dev
```
Open your browser at `http://localhost:5173`.

### 5. Production Build
```bash
npm run build
npm run preview
```

---

## 🧭 Routes & Key Screens

| Route | View | Description |
| :--- | :--- | :--- |
| `/login` | Analyst Sign In | Split-screen branding, email/password validation, JWT acquisition |
| `/register` | Customer Registration | Registers customer entity in PostgreSQL, returns active JWT token |
| `/dashboard` | Intelligence Overview | Session-local customer lookups, quick UUID search, service cluster health |
| `/customers/:idOrUuid` | **Unified Customer Profile** | Flagship view: Identity, Risk Aggregation, XGBoost Underwriting, AML Hybrid Breakdown, Redis Velocity, and Transaction Stream |
| `/customers/:id/apply-credit` | Credit Application | Financial input form + live polling until async XGBoost decision resolves |
| `/customers/:id/transactions/new` | Transaction Screener | Payment ingestion with live Redis velocity indicator & AML screener |
| `/alerts` | AML Compliance Queue | Live queue filterable by Status (OPEN, UNDER_REVIEW, RESOLVED, FALSE_POSITIVE) and Risk Level |
| `/alerts/:alertUuid` | Alert Investigation Dossier | Full score breakdown (0.40 Rule + 0.60 ML), rule violation reasons, and compliance resolution workflow dialog |
| `/admin/rules` | Rules Engine & Sandbox | Registered AML compliance policies + live transaction evaluation sandbox |
| `/admin/model-info` | AI/ML Models Registry | Direct FastAPI diagnostic page showing XGBoost & Isolation Forest metrics, ROC-AUC, and feature importances |

---

## 🔗 Endpoints Consumed

### Spring Boot Java Backend (`http://localhost:8080`)
- `POST /api/auth/register` — Customer registration
- `POST /api/auth/login` — Authentication & JWT generation
- `GET /api/customers/{id}/profile` — Unified Customer Profile (Identity + Credit + AML + Risk Aggregation + Velocity)
- `GET /api/customers/{id}` — Customer details
- `POST /api/credit/apply` — Loan application ingestion (202 Accepted)
- `GET /api/credit/application/{id}` — Single credit application & decision status (singular path verified)
- `GET /api/credit/customer/{id}` — Customer credit applications
- `POST /api/transactions` — Transaction ingestion & Redis velocity tracking
- `GET /api/transactions/customer/{id}` — Customer transaction history
- `GET /api/aml/alerts` — AML alerts list with status/riskLevel filtering
- `GET /api/aml/alerts/{alertUuid}` — AML alert details
- `GET /api/aml/alerts/customer/{uuid}` — Customer-scoped AML alerts
- `PATCH /api/aml/alerts/{alertUuid}/status` — Operational workflow status update
- `GET /api/aml/rules` — Registered AML rules
- `POST /api/aml/rules/evaluate` — Rule evaluation sandbox
- `GET /actuator/health` — Spring Boot service & infrastructure health (Postgres, Redis, RabbitMQ)

### FastAPI Python ML Service (`http://localhost:8000`)
- `GET /health` — Python service health
- `GET /model-info` — XGBoost and Isolation Forest model metadata & benchmarks
