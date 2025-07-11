package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OAuth2RegistrationController {

    private final NguoiDungService nguoiDungService;
    private final HttpSession session;

    public OAuth2RegistrationController(NguoiDungService nguoiDungService, HttpSession session) {
        this.nguoiDungService = nguoiDungService;
        this.session = session;
    }

    @GetMapping("/customers/oauth2/register")
    public String showRegistrationForm(Model model) {
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("pendingUser");
        if (nguoiDung == null) {
            return "redirect:/";
        }
        model.addAttribute("nguoiDung", nguoiDung);
        return "WebQuanLy/oauth2-register";
    }

    @PostMapping("/customers/oauth2/register")
    public String completeRegistration(
            @RequestParam("tenDangNhap") String tenDangNhap,
            @RequestParam("matKhau") String matKhau,
            @RequestParam("soDienThoai") String soDienThoai,
            @RequestParam("diaChi") String diaChi,
            @RequestParam("tinhThanhPho") String tinhThanhPho,
            @RequestParam("quanHuyen") String quanHuyen,
            @RequestParam("phuongXa") String phuongXa,
            @RequestParam("chiTietDiaChi") String chiTietDiaChi,
            Model model,
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request) {

        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("pendingUser");
        if (nguoiDung == null) {
            return "redirect:/";
        }

        if (nguoiDungService.findByTenDangNhap(tenDangNhap) != null) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại.");
            model.addAttribute("nguoiDung", nguoiDung);
            return "WebQuanLy/oauth2-register";
        }

        if (nguoiDungService.existsByPhone(soDienThoai)) {
            model.addAttribute("error", "Số điện thoại đã tồn tại.");
            model.addAttribute("nguoiDung", nguoiDung);
            return "WebQuanLy/oauth2-register";
        }

        nguoiDung.setTenDangNhap(tenDangNhap);
        nguoiDung.setMatKhau(matKhau);
        nguoiDung.setSoDienThoai(soDienThoai);
        nguoiDung.setDiaChi(diaChi);
        nguoiDung.setTinhThanhPho(tinhThanhPho);
        nguoiDung.setQuanHuyen(quanHuyen);
        nguoiDung.setPhuongXa(phuongXa);
        nguoiDung.setChiTietDiaChi(chiTietDiaChi);
        nguoiDungService.save(nguoiDung);

        // Cập nhật Authentication
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            Map<String, Object> attributes = new HashMap<>(((OAuth2User) authentication.getPrincipal()).getAttributes());
            attributes.put("id", nguoiDung.getId().toString());
            attributes.put("name", nguoiDung.getHoTen());
            attributes.put("email", nguoiDung.getEmail());

            OAuth2User updatedOAuth2User = new DefaultOAuth2User(
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().toUpperCase())),
                    attributes,
                    "name"
            );

            Authentication newAuth = new OAuth2AuthenticationToken(
                    updatedOAuth2User,
                    updatedOAuth2User.getAuthorities(),
                    authentication.getName()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            // Làm mới session
            request.getSession().invalidate();
            request.getSession(true); // Tạo session mới
        }

        session.removeAttribute("pendingUser");
        return "redirect:/cart";
    }
}