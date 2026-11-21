package com.finantx.recon.service;

import com.finantx.recon.dto.IngestTxnsRequest;
import com.finantx.recon.dto.IngestTxnsResponse;
import com.finantx.recon.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngestService {

    private final TransactionRepository transactionRepository;
    private final AnomalyService anomalyService;

    public IngestService(TransactionRepository transactionRepository, AnomalyService anomalyService) {
        this.transactionRepository = transactionRepository;
        this.anomalyService = anomalyService;
    }

    public IngestTxnsResponse ingest(IngestTxnsRequest request) {
        // TODO: implement ingestion
        return new IngestTxnsResponse(0, 0, List.of());
    }
}
