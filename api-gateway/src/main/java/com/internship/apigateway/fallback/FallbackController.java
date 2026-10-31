package com.internship.apigateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> usersFallback() {
        return fallback("/api/users", "User Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> productsFallback() {
        return fallback("/api/products", "Product Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> ordersFallback() {
        return fallback("/api/orders", "Order Service is temporarily unavailable. Please try again later.");
    }

    private ResponseEntity<Map<String, Object>> fallback(String path, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
