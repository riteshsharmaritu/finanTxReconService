package com.finantx.recon.client;

import com.finantx.recon.dto.FetchTxnsRequest;
import com.finantx.recon.dto.FetchTxnsResponse;
import com.finantx.recon.dto.IngestTxnsRequest;
import com.finantx.recon.dto.IngestTxnsResponse;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Thin HTTP client for the transaction recon REST APIs.
 * Create with {@link #create(String)} or inject a custom {@link RestClient}.
 */
public class TxReconApiClient {

    private final RestClient restClient;

    public TxReconApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public static TxReconApiClient create(String baseUrl) {
        return new TxReconApiClient(RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .build());
    }

    public IngestTxnsResponse ingestTxs(IngestTxnsRequest request) {
        return restClient.post()
                .uri("/v1/ingesttxs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(IngestTxnsResponse.class);
    }

    public FetchTxnsResponse fetchTxns(FetchTxnsRequest request) {
        return restClient.post()
                .uri("/v1/fetchtxns")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(FetchTxnsResponse.class);
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
