package com.internship.orderservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping
    public List<OrderResponse> getOrders() {
        return List.of(
                new OrderResponse(101, "Created"),
                new OrderResponse(102, "Shipped")
        );
    }

    public record OrderResponse(int id, String status) {
    }
}
