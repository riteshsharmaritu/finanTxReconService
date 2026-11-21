package com.finantx.recon.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IngestTxnRecord(
        String txId,
        BigDecimal amount,
        LocalDate date,
        String fromAccount,
        String toAccount,
        String merchant,
        String idempotencyKey
) {
}
