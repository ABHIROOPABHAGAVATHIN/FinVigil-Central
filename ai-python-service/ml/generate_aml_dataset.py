"""
FinVigil Central - AML Dataset Generator
Generates realistic financial transaction streams with normal inlier transactions
and calibrated money-laundering / anomaly patterns for Isolation Forest model training.
"""

from pathlib import Path
import numpy as np
import pandas as pd

DATA_DIR = Path(__file__).resolve().parent / "data"
DATA_DIR.mkdir(parents=True, exist_ok=True)
DEFAULT_AML_OUTPUT_PATH = DATA_DIR / "aml_dataset.csv"

AML_FEATURE_COLUMNS = [
    "amount",
    "transaction_type_code",
    "hour_of_day",
    "day_of_week",
    "velocity_count_10m",
    "velocity_amount_10m",
    "amount_to_velocity_ratio",
    "is_high_risk_merchant",
]

TARGET_COLUMN = "is_anomaly"

# Mapping transaction types to integer codes
TRANSACTION_TYPE_MAP = {
    "TRANSFER": 0,
    "PURCHASE": 1,
    "WITHDRAWAL": 2,
    "DEPOSIT": 3,
}


def generate_aml_dataset(
    n_samples: int = 15000,
    anomaly_ratio: float = 0.05,
    random_state: int = 42,
    output_path: Path = DEFAULT_AML_OUTPUT_PATH,
) -> pd.DataFrame:
    """
    Generates synthetic transaction monitoring dataset:
    - 95% normal financial activity (purchases, regular deposits, daytime transfers)
    - 5% calibrated AML typologies:
        1. Structuring / Smurfing (sub-threshold clustering with rapid velocity)
        2. High-Velocity burst drains
        3. High-Dollar outlier spikes
        4. Late-night high-risk merchant transfers
    """
    rng = np.random.default_rng(random_state)
    n_anomalies = int(n_samples * anomaly_ratio)
    n_normals = n_samples - n_anomalies

    # -------------------------------------------------------------
    # 1. Normal Transactions
    # -------------------------------------------------------------
    # Amounts: Log-normal distribution (median ~ 150 - 500, rarely > 10,000)
    normal_amounts = np.clip(
        np.exp(rng.normal(loc=5.5, scale=1.0, size=n_normals)),
        5.0,
        15000.0,
    )

    # Transaction types: mostly PURCHASE (55%), TRANSFER (25%), DEPOSIT (10%), WITHDRAWAL (10%)
    normal_types = rng.choice(
        [1, 0, 3, 2],
        p=[0.55, 0.25, 0.10, 0.10],
        size=n_normals,
    )

    # Hours: peak daytime 8:00 to 21:00
    normal_hours = np.clip(
        np.round(rng.normal(loc=14.0, scale=4.0, size=n_normals)),
        0,
        23,
    ).astype(int)

    # Day of week: 0 to 6 uniformly
    normal_days = rng.integers(0, 7, size=n_normals)

    # Velocity count 10m: mostly 1, occasionally 2 or 3
    normal_velocity_counts = np.clip(
        rng.poisson(lam=0.3, size=n_normals) + 1,
        1,
        4,
    )

    # Velocity amount 10m: cumulative ~ count * current amount
    normal_velocity_amounts = normal_amounts * (
        1.0 + (normal_velocity_counts - 1) * rng.uniform(0.7, 1.2, size=n_normals)
    )

    # High-risk merchant: very low frequency (~ 1%)
    normal_high_risk = rng.choice([0, 1], p=[0.985, 0.015], size=n_normals)

    # -------------------------------------------------------------
    # 2. Anomalous Transactions (AML Typologies)
    # -------------------------------------------------------------
    # Split anomalies across 4 distinct typologies
    n_structuring = n_anomalies // 4
    n_velocity_burst = n_anomalies // 4
    n_high_dollar = n_anomalies // 4
    n_night_merchant = n_anomalies - (n_structuring + n_velocity_burst + n_high_dollar)

    # Typology 1: Structuring / Smurfing (just below reporting threshold 50,000 or 10,000)
    anom_amounts_1 = rng.uniform(45000.0, 49800.0, size=n_structuring)
    anom_types_1 = rng.choice([0, 2, 3], size=n_structuring)  # transfers/withdrawals/deposits
    anom_hours_1 = rng.integers(9, 18, size=n_structuring)
    anom_days_1 = rng.integers(0, 7, size=n_structuring)
    anom_velocity_counts_1 = rng.integers(3, 8, size=n_structuring)
    anom_velocity_amounts_1 = anom_amounts_1 * anom_velocity_counts_1 * rng.uniform(0.9, 1.1, size=n_structuring)
    anom_high_risk_1 = rng.choice([0, 1], p=[0.7, 0.3], size=n_structuring)

    # Typology 2: Rapid High-Velocity Drain
    anom_amounts_2 = rng.uniform(5000.0, 35000.0, size=n_velocity_burst)
    anom_types_2 = rng.choice([0, 2], size=n_velocity_burst)  # TRANSFER or WITHDRAWAL
    anom_hours_2 = rng.integers(0, 24, size=n_velocity_burst)
    anom_days_2 = rng.integers(0, 7, size=n_velocity_burst)
    anom_velocity_counts_2 = rng.integers(6, 15, size=n_velocity_burst)  # High burst count
    anom_velocity_amounts_2 = anom_amounts_2 * anom_velocity_counts_2 * rng.uniform(0.8, 1.2, size=n_velocity_burst)
    anom_high_risk_2 = rng.choice([0, 1], p=[0.6, 0.4], size=n_velocity_burst)

    # Typology 3: High-Dollar Outlier Spikes
    anom_amounts_3 = rng.uniform(90000.0, 450000.0, size=n_high_dollar)
    anom_types_3 = rng.choice([0, 2], p=[0.75, 0.25], size=n_high_dollar)
    anom_hours_3 = rng.integers(0, 24, size=n_high_dollar)
    anom_days_3 = rng.integers(0, 7, size=n_high_dollar)
    anom_velocity_counts_3 = rng.integers(1, 4, size=n_high_dollar)
    anom_velocity_amounts_3 = anom_amounts_3 * anom_velocity_counts_3
    anom_high_risk_3 = rng.choice([0, 1], p=[0.7, 0.3], size=n_high_dollar)

    # Typology 4: Late-Night High-Risk Merchant Outflow
    anom_amounts_4 = rng.uniform(8000.0, 60000.0, size=n_night_merchant)
    anom_types_4 = np.full(n_night_merchant, 0)  # TRANSFER
    anom_hours_4 = rng.choice([0, 1, 2, 3, 4, 23], size=n_night_merchant)  # Deep night
    anom_days_4 = rng.choice([5, 6, 0], size=n_night_merchant)  # Weekend / late nights
    anom_velocity_counts_4 = rng.integers(2, 6, size=n_night_merchant)
    anom_velocity_amounts_4 = anom_amounts_4 * anom_velocity_counts_4
    anom_high_risk_4 = np.ones(n_night_merchant, dtype=int)  # 100% high-risk merchant

    # Combine all anomalies
    anom_amounts = np.concatenate([anom_amounts_1, anom_amounts_2, anom_amounts_3, anom_amounts_4])
    anom_types = np.concatenate([anom_types_1, anom_types_2, anom_types_3, anom_types_4])
    anom_hours = np.concatenate([anom_hours_1, anom_hours_2, anom_hours_3, anom_hours_4])
    anom_days = np.concatenate([anom_days_1, anom_days_2, anom_days_3, anom_days_4])
    anom_velocity_counts = np.concatenate([anom_velocity_counts_1, anom_velocity_counts_2, anom_velocity_counts_3, anom_velocity_counts_4])
    anom_velocity_amounts = np.concatenate([anom_velocity_amounts_1, anom_velocity_amounts_2, anom_velocity_amounts_3, anom_velocity_amounts_4])
    anom_high_risk = np.concatenate([anom_high_risk_1, anom_high_risk_2, anom_high_risk_3, anom_high_risk_4])

    # Concatenate normals + anomalies
    amounts = np.concatenate([normal_amounts, anom_amounts])
    types = np.concatenate([normal_types, anom_types])
    hours = np.concatenate([normal_hours, anom_hours])
    days = np.concatenate([normal_days, anom_days])
    velocity_counts = np.concatenate([normal_velocity_counts, anom_velocity_counts])
    velocity_amounts = np.concatenate([normal_velocity_amounts, anom_velocity_amounts])
    high_risk_merchants = np.concatenate([normal_high_risk, anom_high_risk])

    labels = np.concatenate([
        np.zeros(n_normals, dtype=int),
        np.ones(n_anomalies, dtype=int)
    ])

    # Ratio of current amount to cumulative velocity amount
    ratios = np.clip(amounts / np.maximum(velocity_amounts, amounts), 0.01, 1.0)

    # Build DataFrame
    df = pd.DataFrame({
        "amount": np.round(amounts, 2),
        "transaction_type_code": types,
        "hour_of_day": hours,
        "day_of_week": days,
        "velocity_count_10m": velocity_counts,
        "velocity_amount_10m": np.round(velocity_amounts, 2),
        "amount_to_velocity_ratio": np.round(ratios, 4),
        "is_high_risk_merchant": high_risk_merchants,
        TARGET_COLUMN: labels,
    })

    # Shuffle dataset
    df = df.sample(frac=1.0, random_state=random_state).reset_index(drop=True)

    df.to_csv(output_path, index=False)
    print(f"Generated {len(df)} AML transaction records saved to {output_path}")
    print(f"Anomaly rate: {df[TARGET_COLUMN].mean():.2%}")
    return df


if __name__ == "__main__":
    generate_aml_dataset()
