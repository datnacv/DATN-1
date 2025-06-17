package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OAuth2RegistrationController {

    private final NguoiDungService nguoiDungService;
    private final HttpSession session;

    public OAuth2RegistrationController(NguoiDungService nguoiDungService, HttpSession session) {
        this.nguoiDungService = nguoiDungService;
        this.session = session;
    }

    @GetMapping("/acvstore/oauth2/register")
    public String showRegistrationForm(Model model) {
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("pendingUser");
        if (nguoiDung == null) {
            return "redirect:/acvstore";
        }
        model.addAttribute("nguoiDung", nguoiDung);
        return "oauth-register";
    }

    @PostMapping("/acvstore/oauth2/register")
    public String completeRegistration(
            @RequestParam("tenDangNhap") String tenDangNhap,
            @RequestParam("matKhau") String matKhau,
            @RequestParam("soDienThoai") String soDienThoai,
            @RequestParam("diaChi") String diaChi,
            RedirectAttributes redirectAttributes) {

        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("pendingUser");
        if (nguoiDung == null) {
            redirectAttributes.addFlashAttribute("message", "Phiên đăng ký không hợp lệ!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/acvstore";
        }

        // Kiểm tra xem tên đăng nhập đã tồn tại chưa
        if (nguoiDungService.findByTenDangNhap(tenDangNhap) != null) {
            redirectAttributes.addFlashAttribute("message", "Tên đăng nhập đã tồn tại!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/acvstore/oauth2/register";
        }

        nguoiDung.setTenDangNhap(tenDangNhap);
        nguoiDung.setMatKhau(matKhau); // Mật khẩu sẽ được mã hóa trong NguoiDungService
        nguoiDung.setSoDienThoai(soDienThoai);
        nguoiDung.setDiaChi(diaChi);
        nguoiDung.setVaiTro("customer"); // Gán vai trò mặc định là customer
        nguoiDung.setTrangThai(true); // Đặt trạng thái mặc định là true

        try {
            nguoiDungService.save(nguoiDung);
            session.removeAttribute("pendingUser"); // Xóa khỏi session sau khi hoàn tất
            redirectAttributes.addFlashAttribute("message", "Đăng ký OAuth2 thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/acvstore";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Đăng ký OAuth2 thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/acvstore/oauth2/register";
        }
    }
}