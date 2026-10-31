package com.internship.apigateway.jwt;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        if ("intern".equals(request.username()) && "password".equals(request.password())) {
            return new TokenResponse(jwtService.createToken(request.username()));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad username or password.");
    }

    public record LoginRequest(String username, String password) {
    }

    public record TokenResponse(String token) {
    }
}
