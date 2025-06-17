package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.NguoiDung;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/acvstore")
public class AuthController {

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("nguoiDung", new NguoiDung());
        return "login";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/acvstore/login?logout=true";
    }
}