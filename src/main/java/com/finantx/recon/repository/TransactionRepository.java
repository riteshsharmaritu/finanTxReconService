package com.finantx.recon.repository;

import com.finantx.recon.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>,
        JpaSpecificationExecutor<TransactionEntity> {

    boolean existsByTxId(String txId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    long countByToAccountAndDateAfterAndDateLessThanEqual(String toAccount, Instant from, Instant to);

    long countByMerchantAndDateAfterAndDateLessThanEqual(String merchant, Instant from, Instant to);
}
