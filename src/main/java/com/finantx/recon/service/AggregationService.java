package com.finantx.recon.service;

import com.finantx.recon.dto.FetchTxnsRequest;
import com.finantx.recon.dto.FetchTxnsResponse;
import com.finantx.recon.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AggregationService {

    private final TransactionRepository transactionRepository;

    public AggregationService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public FetchTxnsResponse fetch(FetchTxnsRequest request) {
        // TODO: implement filtering
        return new FetchTxnsResponse(List.of());
    }
}
