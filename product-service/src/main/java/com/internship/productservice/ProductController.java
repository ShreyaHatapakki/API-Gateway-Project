package com.internship.productservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public List<ProductResponse> getProducts() {
        return List.of(
                new ProductResponse(1, "Laptop"),
                new ProductResponse(2, "Mouse")
        );
    }

    public record ProductResponse(int id, String name) {
    }
}
