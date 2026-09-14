"""
FinVigil Central - Deterministic AML Rules Engine (Python)
Evaluates transaction attributes against regulatory compliance rules:
- Large Transaction Reporting (CTR)
- Structuring / Smurfing detection
- High-Velocity bursts
- High-Risk Merchant / Counterparty categories
- Rapid Outflow capital drain
"""

import os
from typing import Dict, Any, List, Tuple

DEFAULT_LARGE_THRESHOLD = float(os.getenv("AML_LARGE_TRANSACTION_THRESHOLD", "50000.0"))
DEFAULT_VELOCITY_LIMIT = int(os.getenv("AML_VELOCITY_LIMIT", "5"))

HIGH_RISK_KEYWORDS = [
    "casino", "gambling", "betting", "crypto", "bitcoin", "darknet",
    "mixer", "offshore", "forex", "binary", "unregulated"
]


class RuleViolationInfo:
    def __init__(self, rule_name: str, description: str, severity: str, score: float):
        self.rule_name = rule_name
        self.description = description
        self.severity = severity
        self.score = score

    def to_dict(self) -> Dict[str, Any]:
        return {
            "ruleName": self.rule_name,
            "description": self.description,
            "severity": self.severity,
            "score": self.score,
        }


def evaluate_aml_rules(
    data: Dict[str, Any],
    large_threshold: float = DEFAULT_LARGE_THRESHOLD,
    velocity_limit: int = DEFAULT_VELOCITY_LIMIT,
) -> Tuple[float, str, List[RuleViolationInfo], List[str]]:
    """
    Evaluates transaction dictionary against deterministic AML rules.
    Returns: (rule_score, risk_level, violations, triggered_rule_names)
    """
    amount = float(data.get("amount", 0.0))
    merchant = str(data.get("merchant", "")).lower()
    txn_type = str(data.get("transactionType", data.get("transaction_type", "PURCHASE"))).upper()
    velocity_count = int(data.get("velocityCount1m", data.get("velocity_count_10m", data.get("velocityCount", 1))))
    raw_velocity_amount = data.get("velocityAmount1m", data.get("velocity_amount_10m", data.get("velocityAmount")))
    velocity_amount = float(raw_velocity_amount) if raw_velocity_amount is not None else amount * velocity_count

    violations: List[RuleViolationInfo] = []

    # Rule 1: Large Transaction Threshold
    if amount >= large_threshold:
        is_extreme = amount >= (2 * large_threshold)
        severity = "HIGH" if is_extreme else "MEDIUM"
        score = 0.45 if is_extreme else 0.35
        violations.append(RuleViolationInfo(
            "LARGE_TRANSACTION_THRESHOLD",
            f"Transaction amount {amount:.2f} meets or exceeds threshold {large_threshold:.2f}",
            severity,
            score,
        ))

    # Rule 2: Structuring / Smurfing
    is_just_below = (0.80 * large_threshold) <= amount < large_threshold
    is_velocity_accumulation = (velocity_count >= 2 and amount < large_threshold and velocity_amount >= large_threshold)
    if is_just_below or is_velocity_accumulation:
        severity = "HIGH" if (is_just_below and velocity_count >= 2) else "MEDIUM"
        score = 0.45 if severity == "HIGH" else 0.40
        desc = (
            f"Transaction amount {amount:.2f} is in structuring corridor [80%-100%] of {large_threshold:.2f}"
            if is_just_below
            else f"Cumulative velocity {velocity_amount:.2f} across {velocity_count} txns exceeds threshold"
        )
        violations.append(RuleViolationInfo(
            "STRUCTURING_DETECTION",
            desc,
            severity,
            score,
        ))

    # Rule 3: High-Velocity Burst
    if velocity_count >= velocity_limit:
        is_severe = velocity_count >= (2 * velocity_limit)
        severity = "HIGH" if is_severe else "MEDIUM"
        score = 0.45 if is_severe else 0.35
        violations.append(RuleViolationInfo(
            "HIGH_VELOCITY_BURST",
            f"Transaction count ({velocity_count}) meets or exceeds limit ({velocity_limit})",
            severity,
            score,
        ))

    # Rule 4: High-Risk Merchant Category
    matched_keyword = next((kw for kw in HIGH_RISK_KEYWORDS if kw in merchant), None)
    if matched_keyword:
        violations.append(RuleViolationInfo(
            "HIGH_RISK_MERCHANT_CATEGORY",
            f"Merchant '{data.get('merchant')}' matched high-risk keyword '{matched_keyword}'",
            "HIGH",
            0.40,
        ))

    # Rule 5: Rapid Outflow Drain
    if txn_type in ["TRANSFER", "WITHDRAWAL"] and amount >= (0.50 * large_threshold) and velocity_count >= 3:
        violations.append(RuleViolationInfo(
            "RAPID_OUTFLOW_DRAIN",
            f"Rapid capital drain: {txn_type} of {amount:.2f} with {velocity_count} recent transactions",
            "MEDIUM",
            0.30,
        ))

    # Calculate probabilistic rule score: 1 - prod(1 - score_i)
    if not violations:
        return 0.0, "LOW", [], []

    not_risky_prob = 1.0
    has_high = False
    for v in violations:
        not_risky_prob *= (1.0 - min(0.95, v.score))
        if v.severity == "HIGH":
            has_high = True

    calculated_score = round(1.0 - not_risky_prob, 4)

    if calculated_score >= 0.70 or (has_high and calculated_score >= 0.40):
        risk_level = "HIGH"
    elif calculated_score >= 0.30 or violations:
        risk_level = "MEDIUM"
    else:
        risk_level = "LOW"

    triggered_names = [v.rule_name for v in violations]
    return calculated_score, risk_level, violations, triggered_names
