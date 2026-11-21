package com.finantx.recon.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces Spring Boot's default /error endpoint, which handles failures that never reach
 * {@link GlobalExceptionHandler} (e.g. exceptions thrown in servlet filters), so those also never surface as 500.
 */
@RestController
public class FallbackErrorController implements ErrorController {

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ProblemDetail> handleError(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = statusCode instanceof Integer code ? HttpStatus.resolve(code) : null;

        if (status == null || status.is5xxServerError()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                    GlobalExceptionHandler.GENERIC_ERROR_MESSAGE);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
        }
        return ResponseEntity.status(status).body(ProblemDetail.forStatus(status));
    }
}
