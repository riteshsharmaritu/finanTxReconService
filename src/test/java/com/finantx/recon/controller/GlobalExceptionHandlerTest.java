package com.finantx.recon.controller;

import com.finantx.recon.service.AggregationService;
import com.finantx.recon.service.IngestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngestService ingestService;

    @MockBean
    private AggregationService aggregationService;

    @Test
    void unexpectedExceptionReturns503WithGenericMessage() throws Exception {
        when(ingestService.ingest(any())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/v1/ingesttxs").contentType(MediaType.APPLICATION_JSON).content("{\"transactions\":[]}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.GENERIC_ERROR_MESSAGE));
    }

    @Test
    void databaseExceptionReturns503() throws Exception {
        when(aggregationService.fetch(any())).thenThrow(new DataAccessResourceFailureException("db down"));

        mockMvc.perform(post("/v1/fetchtxns").contentType(MediaType.APPLICATION_JSON).content("{\"filter\":{}}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.GENERIC_ERROR_MESSAGE));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/v1/ingesttxs").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTimestampReturns400() throws Exception {
        mockMvc.perform(post("/v1/ingesttxs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactions\":[{\"txId\":\"tx-1\",\"date\":\"2026-11-20\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(get("/v1/ingesttxs"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/v1/ingesttxs").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
