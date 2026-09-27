# FinVigil Central — Frontend Integration Notes & Backend Contract Verification

**Document Version**: 1.0.0
**Date**: September 15, 2026
**Scope**: Verification of REST contracts, discovery findings, and integration design decisions made during the construction of `frontend-web/`.

---

## 1. Critical Integration Findings & Source Code Verification

As mandated by the Critical Integration Safety Rules, all API integrations were audited against the active Spring Boot Java source (`backend-java-service/src/main/java/com/finvigil/**/controller/*.java`) and running services rather than relying on external assumptions.

### 1.1 Endpoint Path & Verification Matrix

| Prompt Assumption / Planned Path | Actual Backend Path | Controller Source | Status in Code | Integration Action Taken |
| :--- | :--- | :--- | :---: | :--- |
| `/api/credit/applications/{uuid}` (Plural) | `/api/credit/application/{id}` (Singular) | `CreditController.java:35` | **Verified Singular** | Built `creditApi.getApplicationByIdOrUuid` using `/api/credit/application/{id}`. Supports both numeric ID and UUID. |
| Customer Credit Applications | `/api/credit/customer/{customerId}` | `CreditController.java:42` | **Verified Exists** | Wired into `creditApi.getApplicationsByCustomer`. |
| `GET /api/aml/alerts/{uuid}` | `/api/aml/alerts/{alertUuid}` | `AmlAlertController.java:39` | **Verified Exists** | Wired into `amlApi.getAlertByUuid`. |
| `GET /api/aml/alerts/customer/{uuid}` | `/api/aml/alerts/customer/{customerUuid}` | `AmlAlertController.java:46` | **Verified Exists** | Wired into `amlApi.getAlertsByCustomer`. |
| `PATCH /api/aml/alerts/{alertUuid}/status` | `/api/aml/alerts/{alertUuid}/status` | `AmlAlertController.java:53` | **Verified Exists** | Wired into `amlApi.updateAlertStatus`. Accepts `{ status: AlertStatus, resolutionNotes?: string }`. |
| Global Alert Listing | `GET /api/aml/alerts` | `AmlAlertController.java:29` | **Verified Exists** | Wired into `amlApi.getAllAlerts`. Supports optional query params `?status=&riskLevel=`. |
| Global Customer Listing (`GET /api/customers`) | **Does Not Exist** | `CustomerController.java` | **Not Implemented** | **Zero simulated/mocked customer data.** The dashboard and navbar use explicit session-local lookup tracking ("Customers examined this session") and direct ID/UUID lookups. |
| Logout / Token Revocation Endpoint | **Does Not Exist** | `AuthController.java` | **Stateless JWT** | Client-side session purge from memory & `sessionStorage`. |

---

## 2. Risk Level Enum Adherence

* **Backend Source**: `com.finvigil.common.enums.RiskLevel`
  ```java
  public enum RiskLevel {
      LOW,
      MEDIUM,
      HIGH
  }
  ```
* **Frontend Compliance**:
  - The UI does **not** display or fabricate a `CRITICAL` risk tier anywhere in the application.
  - All badge components, score gauges, and tables strictly map `LOW`, `MEDIUM`, and `HIGH`.
  - A neutral fallback style is defined purely for defensive graceful degradation if backend enums evolve in future phases.

---

## 3. Asynchronous Polling Architecture

1. **Credit Underwriting Flow**:
   - `POST /api/credit/apply` returns `HTTP 202 Accepted` with initial status `PENDING`.
   - The frontend (`ApplyCreditPage`) polls `GET /api/credit/application/{uuid}` every 2 seconds until the XGBoost RabbitMQ worker writes the `CreditDecision` and the status transitions to `APPROVED`, `REJECTED`, or `REVIEW`.
2. **Transaction AML Screening Flow**:
   - `POST /api/transactions` updates Redis sliding velocity and enqueues an AML transaction event.
   - The frontend (`NewTransactionPage`) polls customer alerts (`GET /api/aml/alerts/customer/{uuid}`) for up to 8 seconds to capture any asynchronously generated AML alert (or confirms clearance).
3. **Unified Profile Polling**:
   - `CustomerProfilePage` polls `/api/customers/{id}/profile` at an 8-second interval via TanStack Query to maintain live telemetry without requiring manual analyst page refreshes.

---

## 4. Token Storage Security Architecture

* **Spring Boot Auth Model**: Returns JWT token in response body (`AuthResponse`), expiring in 24 hours. No `HttpOnly` cookie is set.
* **Frontend Strategy**:
  - Primary token copy is stored strictly **in-memory** in the Zustand store (`src/store/authStore.ts`).
  - A fallback copy is mirrored to `sessionStorage` **only** to survive browser page refresh during an active analyst session.
  - **`localStorage` is explicitly avoided** to prevent persistent cross-session token exposure.
  - Axios response interceptor intercepts any `401 Unauthorized` response, clears the session, and redirects to `/login?expired=true`.

---

## 5. Architectural Recommendations for Future Backend Extensions

These items are documented for future backend maintenance; the current frontend operates cleanly against the existing frozen services:

1. **Global Customer Directory / Search API**:
   - *Current State*: Customers can only be fetched by exact numeric ID or UUID via `/api/customers/{id}`.
   - *Recommendation*: Add `GET /api/customers?page=0&size=20&search=` with partial name/email search in a future backend release to support a full server-side customer directory.
2. **Pagination on `/api/aml/alerts`**:
   - *Current State*: `GET /api/aml/alerts` returns all alerts matching status/risk filter.
   - *Recommendation*: Introduce Spring Data `Pageable` (`page`, `size`) to optimize performance as alert volumes grow in production.
3. **Token Invalidation / Revocation**:
   - *Current State*: Stateless JWT with 24-hour expiration.
   - *Recommendation*: Add a Redis-backed token blacklist if immediate server-side logout invalidation is required.
