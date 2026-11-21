package com.finantx.recon.service;

import com.finantx.recon.dto.FailedRecord;
import com.finantx.recon.dto.IngestTxnRecord;
import com.finantx.recon.dto.IngestTxnsRequest;
import com.finantx.recon.dto.IngestTxnsResponse;
import com.finantx.recon.entity.TransactionEntity;
import com.finantx.recon.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IngestServiceTest {

    private static final Instant BASE_TIME = Instant.parse("2026-11-20T12:00:00Z");

    @Autowired
    private IngestService ingestService;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void clearDatabase() {
        transactionRepository.deleteAll();
    }

    @Test
    void savesValidRecords() {
        IngestTxnsResponse response = ingest(
                record("tx-1", "100.00", BASE_TIME, "ACC-1", "ACC-2", "Amazon", "key-1"));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failureCount()).isZero();
        TransactionEntity saved = byTxId().get("tx-1");
        assertThat(saved.getAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getDate()).isEqualTo(BASE_TIME);
        assertThat(saved.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(saved.isAnomaly()).isFalse();
    }

    @Test
    void rejectsRecordsWithMissingMandatoryFields() {
        IngestTxnsResponse response = ingest(
                record(null, null, BASE_TIME, "ACC-1", " ", null, "key-1"),
                record("tx-2", "10", BASE_TIME, "ACC-1", "ACC-2", null, "key-2"));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedRecords()).containsExactly(
                new FailedRecord(null, "Missing mandatory fields: txId, amount, toAccount"));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsIdempotencyKeyAlreadyStored() {
        ingest(record("tx-1", "10", BASE_TIME, "ACC-1", "ACC-2", null, "key-1"));

        IngestTxnsResponse response = ingest(record("tx-2", "10", BASE_TIME, "ACC-1", "ACC-2", null, "key-1"));

        assertThat(response.successCount()).isZero();
        assertThat(response.failedRecords()).containsExactly(
                new FailedRecord("tx-2", "Duplicate idempotencyKey: key-1"));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicatesWithinSameRequest() {
        IngestTxnsResponse response = ingest(
                record("tx-1", "10", BASE_TIME, "ACC-1", "ACC-2", null, "key-1"),
                record("tx-2", "10", BASE_TIME, "ACC-1", "ACC-2", null, "key-1"),
                record("tx-1", "10", BASE_TIME, "ACC-1", "ACC-2", null, "key-3"));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failedRecords()).containsExactly(
                new FailedRecord("tx-2", "Duplicate idempotencyKey: key-1"),
                new FailedRecord("tx-1", "Duplicate txId: tx-1"));
    }

    @Test
    void acceptsRecordsWithoutIdempotencyKey() {
        IngestTxnsResponse response = ingest(
                record("tx-1", "10", BASE_TIME, "ACC-1", "ACC-2", null, null),
                record("tx-2", "10", BASE_TIME, "ACC-1", "ACC-2", null, null));

        assertThat(response.successCount()).isEqualTo(2);
        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void flagsAmountAboveThreshold() {
        ingest(
                record("tx-at-limit", "5000.00", BASE_TIME, "ACC-1", "ACC-2", null, null),
                record("tx-over-limit", "5000.01", BASE_TIME, "ACC-1", "ACC-3", null, null));

        Map<String, TransactionEntity> saved = byTxId();
        assertThat(saved.get("tx-at-limit").isAnomaly()).isFalse();
        assertThat(saved.get("tx-over-limit").isAnomaly()).isTrue();
    }

    @Test
    void flagsSixthPaymentToSameAccountWithin24Hours() {
        ingest(record("tx-old", "10", BASE_TIME.minus(Duration.ofHours(25)), "ACC-1", "PAYEE", null, null));
        List<IngestTxnRecord> records = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            records.add(record("tx-" + i, "10", BASE_TIME.plus(Duration.ofMinutes(i)), "ACC-1", "PAYEE", null, null));
        }
        ingest(records.toArray(IngestTxnRecord[]::new));

        Map<String, TransactionEntity> saved = byTxId();
        for (int i = 1; i <= 5; i++) {
            assertThat(saved.get("tx-" + i).isAnomaly()).as("tx-" + i).isFalse();
        }
        assertThat(saved.get("tx-6").isAnomaly()).isTrue();
    }

    @Test
    void countsStoredTransactionsTowardsMerchantFrequency() {
        for (int i = 1; i <= 5; i++) {
            ingest(record("tx-" + i, "10", BASE_TIME.minus(Duration.ofHours(i)), "ACC-1", "ACC-" + (10 + i),
                    "Amazon", null));
        }

        ingest(record("tx-6", "10", BASE_TIME, "ACC-1", "ACC-99", "Amazon", null));

        assertThat(byTxId().get("tx-6").isAnomaly()).isTrue();
    }

    private IngestTxnsResponse ingest(IngestTxnRecord... records) {
        return ingestService.ingest(new IngestTxnsRequest(Arrays.asList(records)));
    }

    private Map<String, TransactionEntity> byTxId() {
        return transactionRepository.findAll().stream()
                .collect(Collectors.toMap(TransactionEntity::getTxId, Function.identity()));
    }

    private static IngestTxnRecord record(String txId, String amount, Instant date, String fromAccount,
                                          String toAccount, String merchant, String idempotencyKey) {
        return new IngestTxnRecord(txId, amount == null ? null : new BigDecimal(amount), date, fromAccount,
                toAccount, merchant, idempotencyKey);
    }
}
