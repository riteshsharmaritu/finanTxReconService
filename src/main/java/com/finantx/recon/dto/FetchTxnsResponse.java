package com.finantx.recon.dto;

import java.util.List;

public record FetchTxnsResponse(
        List<TransactionView> transactionList
) {
}
