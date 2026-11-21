package com.finantx.recon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finantx.recon.anomaly.AnomalyRule;
import com.finantx.recon.anomaly.AnomalyRules;
import com.finantx.recon.anomaly.PayeeField;
import com.finantx.recon.entity.TransactionEntity;
import com.finantx.recon.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
public class AnomalyService {

    private final List<AnomalyRule> rules;
    private final TransactionRepository transactionRepository;

    public AnomalyService(@Value("${anomaly.rules-file}") Resource rulesFile,
                          ObjectMapper objectMapper,
                          TransactionRepository transactionRepository) {
        this.rules = loadEnabledRules(rulesFile, objectMapper);
        this.transactionRepository = transactionRepository;
    }

    public List<AnomalyRule> getRules() {
        return rules;
    }

    /**
     * @param pendingBatch unsaved transactions from the same ingest request, including {@code txn} itself;
     *                     they count towards frequency rules alongside already stored transactions.
     */
    public boolean isAnomaly(TransactionEntity txn, Collection<TransactionEntity> pendingBatch) {
        for (AnomalyRule rule : rules) {
            boolean triggered = switch (rule.type()) {
                case AMOUNT_THRESHOLD -> exceedsAmountThreshold(rule, txn);
                case PAYEE_FREQUENCY -> exceedsPayeeFrequency(rule, txn, pendingBatch);
            };
            if (triggered) {
                return true;
            }
        }
        return false;
    }

    private boolean exceedsAmountThreshold(AnomalyRule rule, TransactionEntity txn) {
        return txn.getAmount().compareTo(rule.thresholdAmount()) > 0;
    }

    private boolean exceedsPayeeFrequency(AnomalyRule rule, TransactionEntity txn,
                                          Collection<TransactionEntity> pendingBatch) {
        Instant windowEnd = txn.getDate();
        Instant windowStart = windowEnd.minus(Duration.ofHours(rule.windowHours()));

        for (PayeeField field : rule.groupBy()) {
            String payee = payeeOf(txn, field);
            if (payee == null) {
                continue;
            }
            long stored = switch (field) {
                case TO_ACCOUNT -> transactionRepository
                        .countByToAccountAndDateAfterAndDateLessThanEqual(payee, windowStart, windowEnd);
                case MERCHANT -> transactionRepository
                        .countByMerchantAndDateAfterAndDateLessThanEqual(payee, windowStart, windowEnd);
            };
            long pending = pendingBatch.stream()
                    .filter(other -> payee.equals(payeeOf(other, field)))
                    .filter(other -> other.getDate().isAfter(windowStart) && !other.getDate().isAfter(windowEnd))
                    .count();
            if (stored + pending > rule.maxCount()) {
                return true;
            }
        }
        return false;
    }

    private static String payeeOf(TransactionEntity txn, PayeeField field) {
        return switch (field) {
            case TO_ACCOUNT -> txn.getToAccount();
            case MERCHANT -> txn.getMerchant();
        };
    }

    private static List<AnomalyRule> loadEnabledRules(Resource rulesFile, ObjectMapper objectMapper) {
        try (InputStream in = rulesFile.getInputStream()) {
            AnomalyRules config = objectMapper.readValue(in, AnomalyRules.class);
            if (config.rules() == null) {
                return List.of();
            }
            List<AnomalyRule> enabled = config.rules().stream().filter(AnomalyRule::enabled).toList();
            enabled.forEach(AnomalyService::requireValid);
            return enabled;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load anomaly rules from " + rulesFile, e);
        }
    }

    private static void requireValid(AnomalyRule rule) {
        if (rule.type() == null) {
            throw new IllegalStateException("Anomaly rule " + rule.id() + " has no type");
        }
        boolean valid = switch (rule.type()) {
            case AMOUNT_THRESHOLD -> rule.thresholdAmount() != null;
            case PAYEE_FREQUENCY -> rule.groupBy() != null && !rule.groupBy().isEmpty()
                    && rule.maxCount() != null && rule.windowHours() != null;
        };
        if (!valid) {
            throw new IllegalStateException("Anomaly rule " + rule.id() + " is missing required settings for "
                    + rule.type());
        }
    }
}
