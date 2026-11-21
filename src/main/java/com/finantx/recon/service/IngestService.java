package com.finantx.recon.service;

import com.finantx.recon.dto.FailedRecord;
import com.finantx.recon.dto.IngestTxnRecord;
import com.finantx.recon.dto.IngestTxnsRequest;
import com.finantx.recon.dto.IngestTxnsResponse;
import com.finantx.recon.entity.TransactionEntity;
import com.finantx.recon.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class IngestService {

    static final String GENERIC_FAILURE_MESSAGE = "Unable to process transaction, please retry later";

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final TransactionRepository transactionRepository;
    private final AnomalyService anomalyService;
    private final Clock clock;

    public IngestService(TransactionRepository transactionRepository, AnomalyService anomalyService, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.anomalyService = anomalyService;
        this.clock = clock;
    }

    /**
     * Not transactional on purpose: each record is saved in its own transaction so that a database error on one
     * record cannot roll back the others.
     */
    public IngestTxnsResponse ingest(IngestTxnsRequest request) {
        List<IngestTxnRecord> records = request == null || request.transactions() == null
                ? List.of()
                : request.transactions();
        Instant ingestedAt = Instant.now(clock);

        List<FailedRecord> failedRecords = new ArrayList<>();
        List<TransactionEntity> accepted = new ArrayList<>();
        Set<String> batchTxIds = new HashSet<>();
        Set<String> batchIdempotencyKeys = new HashSet<>();

        for (IngestTxnRecord record : records) {
            String failureReason;
            try {
                failureReason = validate(record, batchTxIds, batchIdempotencyKeys);
            } catch (DataAccessException e) {
                log.error("Database error while validating transaction {}", record.txId(), e);
                failureReason = GENERIC_FAILURE_MESSAGE;
            }
            if (failureReason != null) {
                failedRecords.add(new FailedRecord(record == null ? null : record.txId(), failureReason));
                continue;
            }
            batchTxIds.add(record.txId());
            if (StringUtils.hasText(record.idempotencyKey())) {
                batchIdempotencyKeys.add(record.idempotencyKey());
            }
            accepted.add(toEntity(record, ingestedAt));
        }

        List<TransactionEntity> checked = new ArrayList<>();
        for (TransactionEntity txn : accepted) {
            try {
                txn.setAnomaly(anomalyService.isAnomaly(txn, accepted));
                checked.add(txn);
            } catch (DataAccessException e) {
                log.error("Database error while running anomaly check for transaction {}", txn.getTxId(), e);
                failedRecords.add(new FailedRecord(txn.getTxId(), GENERIC_FAILURE_MESSAGE));
            }
        }

        int successCount = 0;
        for (TransactionEntity txn : checked) {
            try {
                transactionRepository.saveAndFlush(txn);
                successCount++;
            } catch (DataAccessException e) {
                log.error("Database error while saving transaction {}", txn.getTxId(), e);
                failedRecords.add(new FailedRecord(txn.getTxId(), GENERIC_FAILURE_MESSAGE));
            }
        }

        return new IngestTxnsResponse(successCount, failedRecords.size(), failedRecords);
    }

    private String validate(IngestTxnRecord record, Set<String> batchTxIds, Set<String> batchIdempotencyKeys) {
        if (record == null) {
            return "Record is null";
        }

        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(record.txId())) {
            missing.add("txId");
        }
        if (record.amount() == null) {
            missing.add("amount");
        }
        if (!StringUtils.hasText(record.fromAccount())) {
            missing.add("fromAccount");
        }
        if (!StringUtils.hasText(record.toAccount())) {
            missing.add("toAccount");
        }
        if (!missing.isEmpty()) {
            return "Missing mandatory fields: " + String.join(", ", missing);
        }

        String idempotencyKey = record.idempotencyKey();
        if (StringUtils.hasText(idempotencyKey)
                && (batchIdempotencyKeys.contains(idempotencyKey)
                || transactionRepository.existsByIdempotencyKey(idempotencyKey))) {
            return "Duplicate idempotencyKey: " + idempotencyKey;
        }

        if (batchTxIds.contains(record.txId()) || transactionRepository.existsByTxId(record.txId())) {
            return "Duplicate txId: " + record.txId();
        }

        return null;
    }

    private TransactionEntity toEntity(IngestTxnRecord record, Instant ingestedAt) {
        TransactionEntity entity = new TransactionEntity();
        entity.setTxId(record.txId());
        entity.setAmount(record.amount());
        entity.setDate(record.date() != null ? record.date() : ingestedAt);
        entity.setFromAccount(record.fromAccount());
        entity.setToAccount(record.toAccount());
        entity.setMerchant(StringUtils.hasText(record.merchant()) ? record.merchant() : null);
        entity.setIdempotencyKey(StringUtils.hasText(record.idempotencyKey()) ? record.idempotencyKey() : null);
        return entity;
    }
}
