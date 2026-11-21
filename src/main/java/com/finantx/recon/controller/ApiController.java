package com.finantx.recon.controller;

import com.finantx.recon.dto.FetchTxnsRequest;
import com.finantx.recon.dto.FetchTxnsResponse;
import com.finantx.recon.dto.IngestTxnsRequest;
import com.finantx.recon.dto.IngestTxnsResponse;
import com.finantx.recon.service.AggregationService;
import com.finantx.recon.service.IngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class ApiController {

    private final IngestService ingestService;
    private final AggregationService aggregationService;

    public ApiController(IngestService ingestService, AggregationService aggregationService) {
        this.ingestService = ingestService;
        this.aggregationService = aggregationService;
    }

    @PostMapping("/ingesttxs")
    public ResponseEntity<IngestTxnsResponse> ingestTxs(@RequestBody IngestTxnsRequest request) {
        return ResponseEntity.ok(ingestService.ingest(request));
    }

    @PostMapping("/fetchtxns")
    public ResponseEntity<FetchTxnsResponse> fetchTxns(@RequestBody FetchTxnsRequest request) {
        return ResponseEntity.ok(aggregationService.fetch(request));
    }
}
