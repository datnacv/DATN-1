package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Controller
public class OAuth2RegistrationController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ghn.api.token}")
    private String ghnToken;

    @Value("${ghn.api.url}")
    private String ghnApiUrl;

    private final NguoiDungService nguoiDungService;
    private final HttpSession session;

    public OAuth2RegistrationController(NguoiDungService nguoiDungService, HttpSession session) {
        this.nguoiDungService = nguoiDungService;
        this.session = session;
    }

    @GetMapping("/customers/oauth2/register")
    public String showRegistrationForm(Model model) {
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("pendingUser");
        if (nguoiDung == null) return "redirect:/";

        model.addAttribute("nguoiDung", nguoiDung);

        // Gọi API GHN để load tỉnh/thành
        try {
            String provinceUrl = ghnApiUrl + "/master-data/province";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", ghnToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            Object response = restTemplate.exchange(provinceUrl, HttpMethod.GET, entity, Object.class).getBody();
            model.addAttribute("provinces", response);
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải danh sách tỉnh/thành: " + e.getMessage());
        }

        return "WebQuanLy/oauth2-register";
    }

    @PostMapping("/customers/oauth2/register")
    public String completeRegistration(
            @RequestParam("tenDangNhap") String tenDangNhap,
            @RequestParam("matKhau") String matKhau,
            @RequestParam("soDienThoai") String soDienThoai,
            @RequestParam("tinhThanhPho") String tinhThanhPho,
            @RequestParam("quanHuyen") String quanHuyen,
            @RequestParam("phuongXa") String phuongXa,
            @RequestParam("chiTietDiaChi") String chiTietDiaChi,
            Model model,
            Authentication authentication,
            HttpSession session) {

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

        // Update and save user
        nguoiDung.setTenDangNhap(tenDangNhap);
        nguoiDung.setMatKhau(matKhau); // Ensure this is encoded if required
        nguoiDung.setSoDienThoai(soDienThoai);
        nguoiDung.setTinhThanhPho(tinhThanhPho);
        nguoiDung.setQuanHuyen(quanHuyen);
        nguoiDung.setPhuongXa(phuongXa);
        nguoiDung.setChiTietDiaChi(chiTietDiaChi);
        nguoiDungService.save(nguoiDung);

        // Remove pendingUser and update authentication
        session.removeAttribute("pendingUser");

        // Re-authenticate with NguoiDung
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                nguoiDung,
                nguoiDung.getPassword(),
                nguoiDung.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        return "redirect:/cart";
    }
}