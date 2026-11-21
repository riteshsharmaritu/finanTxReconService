package com.finantx.recon.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FallbackErrorController.class)
class FallbackErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void serverErrorIsReportedAs503() throws Exception {
        mockMvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.GENERIC_ERROR_MESSAGE));
    }

    @Test
    void missingStatusIsReportedAs503() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void clientErrorKeepsItsStatus() throws Exception {
        mockMvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound());
    }
}
