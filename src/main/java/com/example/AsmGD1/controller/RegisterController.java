package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/acvstore")
public class RegisterController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("nguoiDung", new NguoiDung());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("nguoiDung") NguoiDung nguoiDung, RedirectAttributes redirectAttributes) {
        try {
            nguoiDung.setVaiTro("customer"); // Mặc định vai trò là customer
            nguoiDung.setTrangThai(true);
            // Không cần mã hóa mật khẩu vì dùng NoOpPasswordEncoder
            nguoiDungService.save(nguoiDung);
            redirectAttributes.addFlashAttribute("message", "Đăng ký thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/acvstore/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Đăng ký thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/acvstore/register";
        }
    }
}