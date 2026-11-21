package com.finantx.recon.anomaly;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single rule from the anomaly rules file. Only the fields relevant to the rule's type are populated:
 * AMOUNT_THRESHOLD uses thresholdAmount; PAYEE_FREQUENCY uses groupBy, maxCount and windowHours.
 */
public record AnomalyRule(
        String id,
        AnomalyRuleType type,
        boolean enabled,
        String description,
        BigDecimal thresholdAmount,
        List<PayeeField> groupBy,
        Integer maxCount,
        Integer windowHours
) {
}
