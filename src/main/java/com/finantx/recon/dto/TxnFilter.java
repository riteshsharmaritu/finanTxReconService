package com.finantx.recon.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TxnFilter(
        String account,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        Instant fromDate,
        Instant toDate,
        String merchant
) {
}
