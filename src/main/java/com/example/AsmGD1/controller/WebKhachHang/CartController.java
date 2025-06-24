package com.example.AsmGD1.controller.WebKhachHang;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @PostMapping("/add")
    public ResponseEntity<String> addToCart(@RequestBody Map<String, String> payload) {
        String productId = payload.get("id");
        String size = payload.get("size");
        String color = payload.get("color");
        int quantity = Integer.parseInt(payload.get("quantity"));
        // Logic lưu vào session hoặc database
        return ResponseEntity.ok("Added to cart");
    }
}
