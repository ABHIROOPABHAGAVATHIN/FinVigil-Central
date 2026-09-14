"""
FinVigil Central - Credit Dataset Generator
Generates a realistic synthetic historical dataset of loan applications
with calibrated default probabilities for XGBoost model training.
"""

import numpy as np
import pandas as pd
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent / "data"
DATA_DIR.mkdir(parents=True, exist_ok=True)
DEFAULT_OUTPUT_PATH = DATA_DIR / "credit_dataset.csv"


def generate_credit_dataset(
    n_samples: int = 10000,
    random_state: int = 42,
    output_path: Path = DEFAULT_OUTPUT_PATH
) -> pd.DataFrame:
    """
    Generate synthetic credit dataset with features:
    - income: Annual applicant income ($)
    - employment_years: Total years in current/relevant employment
    - loan_amount: Requested loan principal ($)
    - existing_loans: Count of active open credit lines
    - credit_score: FICO-style credit score (300 - 850)
    - debt_to_income_ratio: Total monthly debt obligations / monthly income (0.05 - 0.85)
    - default_label: Ground truth default indicator (0 = Non-default, 1 = Default)
    """
    rng = np.random.default_rng(random_state)

    # 1. Income: Log-normal distribution (median ~ $65,000, range ~ $20,000 to $350,000)
    log_income = rng.normal(loc=11.0, scale=0.55, size=n_samples)
    income = np.clip(np.exp(log_income), 18000.0, 400000.0)

    # 2. Employment years: Gamma distribution clipped between 0 and 35
    employment_years = np.clip(
        rng.gamma(shape=2.5, scale=2.0, size=n_samples).astype(int),
        0,
        35
    )

    # 3. Credit Score: Truncated normal distribution (mean ~ 680, std ~ 75, range 300 - 850)
    raw_credit_score = rng.normal(loc=675.0, scale=75.0, size=n_samples)
    credit_score = np.clip(np.round(raw_credit_score), 300, 850).astype(int)

    # 4. Debt to Income Ratio (DTI): Beta distribution (mean ~ 0.32, range 0.05 - 0.80)
    dti = np.clip(rng.beta(a=2.8, b=6.0, size=n_samples) * 0.9 + 0.05, 0.05, 0.85)

    # 5. Existing Loans: Poisson distribution (mean ~ 2.2, range 0 - 9)
    existing_loans = np.clip(rng.poisson(lam=2.2, size=n_samples), 0, 9).astype(int)

    # 6. Loan Amount: Correlated with income but varying with requested leverage
    leverage = rng.uniform(0.1, 0.6, size=n_samples)
    raw_loan = income * leverage + rng.normal(0, 3000, size=n_samples)
    loan_amount = np.clip(np.round(raw_loan, -2), 1000.0, 150000.0)

    # 7. Calibrated Default Probability Formula (Logistic response)
    loan_to_income = loan_amount / np.maximum(income, 1.0)
    
    # Standardized components:
    z_credit = (680.0 - credit_score) / 60.0
    z_dti = (dti - 0.35) / 0.12
    z_lti = (loan_to_income - 0.30) / 0.15
    z_emp = (3.0 - employment_years) / 3.0
    z_loans = (existing_loans - 2.0) / 1.5

    # Base logit + interaction terms
    logit = (
        -2.4
        + 1.35 * z_credit
        + 0.95 * z_dti
        + 0.70 * z_lti
        + 0.45 * z_emp
        + 0.30 * z_loans
        + 0.50 * (z_credit * z_dti)
        + rng.normal(0, 0.35, size=n_samples)
    )

    prob_default = 1.0 / (1.0 + np.exp(-logit))
    default_label = (rng.uniform(0, 1, size=n_samples) < prob_default).astype(int)

    df = pd.DataFrame({
        "income": np.round(income, 2),
        "employment_years": employment_years,
        "loan_amount": np.round(loan_amount, 2),
        "existing_loans": existing_loans,
        "credit_score": credit_score,
        "debt_to_income_ratio": np.round(dti, 4),
        "default_label": default_label,
    })

    df.to_csv(output_path, index=False)
    print(f"Generated {len(df)} records saved to {output_path}")
    print(f"Default rate: {df['default_label'].mean():.2%}")
    return df


if __name__ == "__main__":
    generate_credit_dataset()
