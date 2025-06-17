package com.example.AsmGD1.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/acvstore")
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/WebNhanVien/dashboard")
    public String employeeDashboard() {
        return "WebNhanVien/dashboard"; // Sử dụng template có sẵn
    }

    @GetMapping("/WebKhachHang/index")
    public String customerIndex() {
        return "WebKhachHang/index"; // Sử dụng template index.html trong WebKhachHang
    }
}