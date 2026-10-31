package com.internship.apigateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(ex, status);

        log.warn("Gateway error {} {} -> {} {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getRawPath(),
                status.value(),
                message);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", exchange.getRequest().getURI().getRawPath());

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException jsonException) {
            return Mono.error(jsonException);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            HttpStatusCode statusCode = responseStatusException.getStatusCode();
            return HttpStatus.resolve(statusCode.value()) != null
                    ? HttpStatus.valueOf(statusCode.value())
                    : HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Throwable root = rootCause(ex);
        if (root instanceof ConnectException || root instanceof UnknownHostException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (root instanceof TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        if (status == HttpStatus.UNAUTHORIZED) {
            if (ex instanceof ResponseStatusException responseStatusException
                    && responseStatusException.getReason() != null) {
                return responseStatusException.getReason();
            }
            return "Unauthorized.";
        }
        if (status == HttpStatus.NOT_FOUND) {
            return "No route found for this path.";
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "The backend service is not reachable. Check that it is running.";
        }
        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            return "The backend service took too long to respond.";
        }
        String detail = ex.getMessage();
        return (detail == null || detail.isBlank())
                ? "Unexpected error in the API Gateway."
                : detail;
    }

    private Throwable rootCause(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
