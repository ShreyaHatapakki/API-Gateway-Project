package com.internship.userservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public List<UserResponse> getUsers() {
        return List.of(
                new UserResponse(1, "Asha"),
                new UserResponse(2, "Rahul")
        );
    }

    public record UserResponse(int id, String name) {
    }
}
