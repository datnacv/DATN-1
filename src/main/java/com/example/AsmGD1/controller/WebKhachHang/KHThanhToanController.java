package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/thanh-toan")
public class KHThanhToanController {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @GetMapping
    public String showCheckoutPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("error", "Vui lòng đăng nhập để thanh toán!");
            return "WebKhachHang/thanh-toan";
        }

        try {
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                model.addAttribute("error", "Không thể xác định người dùng. Vui lòng thử lại!");
                return "WebKhachHang/thanh-toan";
            }

            // Tìm NguoiDung dựa trên email
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng với email " + email + " không tồn tại"));

            // Truyền thông tin người dùng vào model
            model.addAttribute("loggedInUser", nguoiDung);
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải thông tin người dùng: " + e.getMessage());
        }
        return "WebKhachHang/thanh-toan";
    }

    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            return (String) oauthToken.getPrincipal().getAttributes().get("email");
        } else if (authentication.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) authentication.getPrincipal();
            return user.getEmail();
        }
        return null; // Trường hợp không xác định được email
    }
}
