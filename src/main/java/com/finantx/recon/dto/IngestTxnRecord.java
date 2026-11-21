package com.finantx.recon.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record IngestTxnRecord(
        String txId,
        BigDecimal amount,
        Instant date,
        String fromAccount,
        String toAccount,
        String merchant,
        String idempotencyKey
) {
}
