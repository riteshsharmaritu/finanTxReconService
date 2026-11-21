package com.finantx.recon.service;

import com.finantx.recon.anomaly.AnomalyRule;
import com.finantx.recon.anomaly.AnomalyRuleType;
import com.finantx.recon.anomaly.PayeeField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnomalyServiceTest {

    @Autowired
    private AnomalyService anomalyService;

    @Test
    void loadsRulesFromFile() {
        Map<String, AnomalyRule> rules = anomalyService.getRules().stream()
                .collect(Collectors.toMap(AnomalyRule::id, Function.identity()));

        AnomalyRule highAmount = rules.get("HIGH_AMOUNT");
        assertThat(highAmount.type()).isEqualTo(AnomalyRuleType.AMOUNT_THRESHOLD);
        assertThat(highAmount.thresholdAmount()).isEqualByComparingTo(new BigDecimal("5000"));

        AnomalyRule frequency = rules.get("HIGH_FREQUENCY_SAME_PAYEE");
        assertThat(frequency.type()).isEqualTo(AnomalyRuleType.PAYEE_FREQUENCY);
        assertThat(frequency.groupBy()).containsExactly(PayeeField.TO_ACCOUNT, PayeeField.MERCHANT);
        assertThat(frequency.maxCount()).isEqualTo(5);
        assertThat(frequency.windowHours()).isEqualTo(24);
    }
}
