package com.finantx.recon.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finantx.recon.dto.FetchTxnsRequest;
import com.finantx.recon.dto.FetchTxnsResponse;
import com.finantx.recon.dto.IngestTxnsRequest;
import com.finantx.recon.dto.IngestTxnsResponse;
import com.finantx.recon.dto.TxnFilter;

import java.nio.file.Path;

/**
 * Reads transactions from a JSON file and calls the APIs.
 *
 * Usage (app must already be running):
 * {@code mvn -q exec:java}
 * {@code mvn -q exec:java "-Dexec.args=http://localhost:8080 samples/ingest-txns.json"}
 */
public final class TxReconApiClientDemo {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final String DEFAULT_TXNS_FILE = "samples/ingest-txns.json";

    private TxReconApiClientDemo() {
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : DEFAULT_BASE_URL;
        Path txnsFile = Path.of(args.length > 1 ? args[1] : DEFAULT_TXNS_FILE);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        IngestTxnsRequest ingestRequest = mapper.readValue(txnsFile.toFile(), IngestTxnsRequest.class);

        int count = ingestRequest.transactions() == null ? 0 : ingestRequest.transactions().size();
        System.out.println("Loaded " + count + " transaction(s) from " + txnsFile.toAbsolutePath());

        TxReconApiClient client = TxReconApiClient.create(baseUrl);

        IngestTxnsResponse ingest = client.ingestTxs(ingestRequest);
        System.out.println("ingest: success=" + ingest.successCount()
                + " failure=" + ingest.failureCount()
                + " failed=" + ingest.failedRecords());

        FetchTxnsResponse fetch = client.fetchTxns(new FetchTxnsRequest(
                new TxnFilter(null, null, null, null, null, null)));
        System.out.println("fetch: count=" + fetch.transactionList().size());
        fetch.transactionList().forEach(txn -> System.out.println("  " + txn));
    }
}
