package com.finantx.recon.anomaly;

import java.util.List;

public record AnomalyRules(
        List<AnomalyRule> rules
) {
}
