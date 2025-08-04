package com.example.AsmGD1.controller.GiaoHangNhanh;

import com.example.AsmGD1.dto.KhachHang.GiaoHangNhanh.DiaChiResponse;
import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutRestController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private DiaChiNguoiDungRepository diaChiNguoiDungRepository;

    @GetMapping("/addresses")
    public DiaChiResponse getUserAddress(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("Vui lòng đăng nhập để lấy địa chỉ!");
        }

        try {
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                throw new IllegalArgumentException("Không thể xác định email của người dùng!");
            }

            // Tìm NguoiDung dựa trên email
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng với email " + email + " không tồn tại"));

            // Lấy địa chỉ mặc định từ DiaChiNguoiDung
            DiaChiNguoiDung defaultAddress = diaChiNguoiDungRepository.findByNguoiDung_IdAndMacDinhTrue(nguoiDung.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ mặc định cho người dùng với email " + email));

            // Trả về địa chỉ từ DiaChiNguoiDung
            DiaChiResponse res = new DiaChiResponse();
            res.setTinhThanhPho(defaultAddress.getTinhThanhPho());
            res.setQuanHuyen(defaultAddress.getQuanHuyen());
            res.setPhuongXa(defaultAddress.getPhuongXa());
            res.setChiTietDiaChi(defaultAddress.getChiTietDiaChi());
            return res;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy địa chỉ: " + e.getMessage());
        }
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