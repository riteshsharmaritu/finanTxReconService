package com.finantx.recon.service;

import com.finantx.recon.dto.FailedRecord;
import com.finantx.recon.dto.IngestTxnRecord;
import com.finantx.recon.dto.IngestTxnsRequest;
import com.finantx.recon.dto.IngestTxnsResponse;
import com.finantx.recon.entity.TransactionEntity;
import com.finantx.recon.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class IngestServiceDbErrorTest {

    private static final Instant BASE_TIME = Instant.parse("2026-11-20T12:00:00Z");

    @Autowired
    private IngestService ingestService;

    @SpyBean
    private TransactionRepository transactionRepository;

    @BeforeEach
    void reset() {
        Mockito.reset(transactionRepository);
        transactionRepository.deleteAll();
    }

    @Test
    void constraintViolationOnSaveBecomesFailedRecordWithoutAffectingOthers() {
        ingest(record("tx-1", "key-1"));
        // Simulates a concurrent request that inserted key-1 after our duplicate check ran.
        doReturn(false).when(transactionRepository).existsByIdempotencyKey("key-1");

        IngestTxnsResponse response = ingest(record("tx-2", "key-1"), record("tx-3", "key-3"));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedRecords()).containsExactly(
                new FailedRecord("tx-2", IngestService.GENERIC_FAILURE_MESSAGE));
        assertThat(transactionRepository.findAll()).extracting(TransactionEntity::getTxId)
                .containsExactlyInAnyOrder("tx-1", "tx-3");
    }

    @Test
    void databaseErrorDuringValidationBecomesFailedRecord() {
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(transactionRepository).existsByIdempotencyKey("key-1");

        IngestTxnsResponse response = ingest(record("tx-1", "key-1"), record("tx-2", "key-2"));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedRecords()).containsExactly(
                new FailedRecord("tx-1", IngestService.GENERIC_FAILURE_MESSAGE));
    }

    @Test
    void databaseErrorDuringAnomalyCheckBecomesFailedRecord() {
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(transactionRepository)
                .countByToAccountAndDateAfterAndDateLessThanEqual(anyString(), Mockito.any(), Mockito.any());

        IngestTxnsResponse response = ingest(record("tx-1", "key-1"));

        assertThat(response.successCount()).isZero();
        assertThat(response.failedRecords()).containsExactly(
                new FailedRecord("tx-1", IngestService.GENERIC_FAILURE_MESSAGE));
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void missingDateDefaultsToIngestionTime() {
        Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        ingest(new IngestTxnRecord("tx-1", new BigDecimal("10"), null, "ACC-1", "ACC-2", null, null));

        Instant stored = transactionRepository.findAll().get(0).getDate();
        assertThat(stored).isBetween(before, Instant.now());
    }

    private IngestTxnsResponse ingest(IngestTxnRecord... records) {
        return ingestService.ingest(new IngestTxnsRequest(List.of(records)));
    }

    private static IngestTxnRecord record(String txId, String idempotencyKey) {
        return new IngestTxnRecord(txId, new BigDecimal("10"), BASE_TIME, "ACC-1", "ACC-2", null, idempotencyKey);
    }
}
