package com.finantx.recon.client;

import com.finantx.recon.dto.FailedRecord;
import com.finantx.recon.dto.FetchTxnsRequest;
import com.finantx.recon.dto.FetchTxnsResponse;
import com.finantx.recon.dto.IngestTxnRecord;
import com.finantx.recon.dto.IngestTxnsRequest;
import com.finantx.recon.dto.IngestTxnsResponse;
import com.finantx.recon.dto.TransactionView;
import com.finantx.recon.dto.TxnFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TxReconApiClientTest {

    private MockRestServiceServer server;
    private TxReconApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TxReconApiClient(builder.build());
    }

    @Test
    void ingestTxsPostsJsonAndParsesResponse() {
        server.expect(requestTo("http://localhost/v1/ingesttxs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactions[0].txId").value("tx-1"))
                .andExpect(jsonPath("$.transactions[0].idempotencyKey").value("key-1"))
                .andRespond(withSuccess("""
                        {
                          "successCount": 1,
                          "failureCount": 1,
                          "failedRecords": [{"txId":"tx-bad","failureReason":"Missing mandatory fields: amount"}]
                        }
                        """, MediaType.APPLICATION_JSON));

        IngestTxnsResponse response = client.ingestTxs(new IngestTxnsRequest(List.of(
                new IngestTxnRecord("tx-1", new BigDecimal("10.00"), Instant.parse("2026-11-20T12:00:00Z"),
                        "ACC-1", "ACC-2", "Amazon", "key-1"))));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.failedRecords()).containsExactly(
                new FailedRecord("tx-bad", "Missing mandatory fields: amount"));
        server.verify();
    }

    @Test
    void fetchTxnsPostsFilterAndParsesResponse() {
        server.expect(requestTo("http://localhost/v1/fetchtxns"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.filter.account").value("ACC-1"))
                .andExpect(jsonPath("$.filter.merchant").value("Amazon"))
                .andRespond(withSuccess("""
                        {
                          "transactionList": [{
                            "txId": "tx-1",
                            "amount": 10.00,
                            "date": "2026-11-20T12:00:00Z",
                            "fromAccount": "ACC-1",
                            "toAccount": "ACC-2",
                            "merchant": "Amazon",
                            "anomaly": false
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        FetchTxnsResponse response = client.fetchTxns(new FetchTxnsRequest(
                new TxnFilter("ACC-1", null, null, null, null, "Amazon")));

        assertThat(response.transactionList()).containsExactly(
                new TransactionView("tx-1", new BigDecimal("10.00"), Instant.parse("2026-11-20T12:00:00Z"),
                        "ACC-1", "ACC-2", "Amazon", false));
        server.verify();
    }
}
