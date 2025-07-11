package com.example.AsmGD1.controller.WebKhachHang;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/thanh-toan")
public class KHThanhToanController {

    @GetMapping
    public String showCheckoutPage() {
        return "WebKhachHang/thanh-toan";
    }
}