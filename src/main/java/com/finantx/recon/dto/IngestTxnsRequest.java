package com.finantx.recon.dto;

import java.util.List;

public record IngestTxnsRequest(
        List<IngestTxnRecord> transactions
) {
}
