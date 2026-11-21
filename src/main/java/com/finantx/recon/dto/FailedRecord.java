package com.finantx.recon.dto;

public record FailedRecord(
        String txId,
        String failureReason
) {
}
