package com.finantx.recon.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TxnFilter(
        String account,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        LocalDate fromDate,
        LocalDate toDate,
        String merchant
) {
}
