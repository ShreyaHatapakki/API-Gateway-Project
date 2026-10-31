package com.internship.apigateway.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getRawPath();
        if (!isProtected(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header. Use: Bearer <token>"));
        }

        String token = header.substring(7);
        try {
            jwtService.parseToken(token);
            return chain.filter(exchange);
        } catch (ExpiredJwtException ex) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token has expired."));
        } catch (JwtException | IllegalArgumentException ex) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid token."));
        }
    }

    private boolean isProtected(String path) {
        return path.startsWith("/api/users")
                || path.startsWith("/api/products")
                || path.startsWith("/api/orders");
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
