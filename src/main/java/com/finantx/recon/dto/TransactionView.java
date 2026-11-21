package com.finantx.recon.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionView(
        String txId,
        BigDecimal amount,
        LocalDate date,
        String fromAccount,
        String toAccount,
        String merchant,
        boolean anomaly
) {
}
