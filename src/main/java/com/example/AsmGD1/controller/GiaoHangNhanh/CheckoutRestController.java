package com.example.AsmGD1.controller.GiaoHangNhanh;

import com.example.AsmGD1.controller.WebKhachHang.KHThanhToanController;
import com.example.AsmGD1.dto.KhachHang.GiaoHangNhanh.DiaChiResponse;
import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public ResponseEntity<KHThanhToanController.ApiResponse> getUserAddresses(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest()
                    .body(new KHThanhToanController.ApiResponse(false, "Vui lòng đăng nhập để lấy địa chỉ!"));
        }
        try {
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(new KHThanhToanController.ApiResponse(false, "Không thể xác định email của người dùng!"));
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng với email " + email + " không tồn tại"));
            List<DiaChiNguoiDung> addresses = diaChiNguoiDungRepository.findByNguoiDung_Id(nguoiDung.getId());
            List<DiaChiResponse> response = addresses.stream().map(address -> {
                DiaChiResponse res = new DiaChiResponse();
                res.setId(address.getId().toString());
                res.setTinhThanhPho(address.getTinhThanhPho());
                res.setQuanHuyen(address.getQuanHuyen());
                res.setPhuongXa(address.getPhuongXa());
                res.setChiTietDiaChi(address.getChiTietDiaChi());
                res.setNguoiNhan(address.getNguoiNhan());
                res.setSoDienThoaiNguoiNhan(address.getSoDienThoaiNguoiNhan());
                res.setMacDinh(address.getMacDinh());
                return res;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(new KHThanhToanController.ApiResponse(true, "Lấy danh sách địa chỉ thành công.", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KHThanhToanController.ApiResponse(false, "Lỗi khi lấy danh sách địa chỉ: " + e.getMessage()));
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