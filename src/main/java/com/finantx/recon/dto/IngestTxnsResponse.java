package com.finantx.recon.dto;

import java.util.List;

public record IngestTxnsResponse(
        int successCount,
        int failureCount,
        List<FailedRecord> failedRecords
) {
}
