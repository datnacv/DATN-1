package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/customers")
public class QuenMKController {
    @Autowired
    private NguoiDungService nguoiDungService;
    @GetMapping("/auth/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("email", "");
        return "WebKhachHang/forgot-password";
    }

    @PostMapping("/auth/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        try {
            nguoiDungService.sendOtp(email);
            NguoiDung user = nguoiDungService.getUserByEmail(email);
            if (user != null) {
                model.addAttribute("email", email);
                model.addAttribute("otpExpiry", user.getOtpExpiry());
            }
            return "WebKhachHang/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", "Gửi OTP thất bại: " + e.getMessage());
            return "WebKhachHang/forgot-password";
        }
    }

    @PostMapping("/auth/verify-otp")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp, Model model) {
        String result = nguoiDungService.verifyOtp(email, otp);
        NguoiDung user = nguoiDungService.getUserByEmail(email);
        model.addAttribute("email", email);
        if (user != null && user.getOtpExpiry() != null) {
            model.addAttribute("otpExpiry", user.getOtpExpiry());
        }
        if ("expired".equals(result)) {
            model.addAttribute("error", "expired");
            return "WebKhachHang/verify-otp";
        }
        if ("invalid".equals(result)) {
            model.addAttribute("error", "invalid");
            return "WebKhachHang/verify-otp";
        }
        if ("valid".equals(result)) {
            return "WebKhachHang/reset-password";
        }
        model.addAttribute("error", "not_found");
        return "WebKhachHang/verify-otp";
    }

    @PostMapping("/auth/reset-password")
    public String processResetPassword(@RequestParam String email,
                                       @RequestParam String newPassword,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        try {
            nguoiDungService.resetPassword(email, newPassword);
            redirectAttributes.addFlashAttribute("message", "Đặt lại mật khẩu thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/customers/login";
        } catch (Exception e) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Đặt lại mật khẩu thất bại: " + e.getMessage());
            return "WebKhachHang/reset-password";
        }
    }

    @PostMapping("/auth/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Email không được để trống"));
        }
        try {
            nguoiDungService.sendOtp(email);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
