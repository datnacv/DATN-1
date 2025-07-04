package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private KhachHangGioHangService khachHangGioHangService;

    @Autowired
    private ChiTietGioHangService chiTietGioHangService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng đăng nhập để xem giỏ hàng"));
            }

            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
            Map<String, Object> response = new HashMap<>();
            response.put("gioHangId", gioHang.getId());
            response.put("chiTietGioHang", khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) != null
                    ? khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) : java.util.Collections.emptyList());
            response.put("tongTien", gioHang.getTongTien() != null ? gioHang.getTongTien() : BigDecimal.ZERO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể tải giỏ hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<String> addToCart(@RequestBody Map<String, String> payload, Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                return ResponseEntity.badRequest().body("Vui lòng đăng nhập để thêm sản phẩm");
            }

            UUID chiTietSanPhamId = UUID.fromString(payload.get("id"));
            int quantity = Integer.parseInt(payload.get("quantity"));

            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
            khachHangGioHangService.addToGioHang(gioHang.getId(), chiTietSanPhamId, quantity);
            return ResponseEntity.ok("Sản phẩm đã được thêm vào giỏ hàng");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi thêm sản phẩm: " + e.getMessage());
        }
    }

    @PutMapping("/update-quantity/{chiTietGioHangId}")
    public ResponseEntity<String> updateQuantity(@PathVariable UUID chiTietGioHangId, @RequestParam Integer quantity, Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để cập nhật giỏ hàng");
            }
            chiTietGioHangService.updateSoLuong(chiTietGioHangId, quantity);
            return ResponseEntity.ok("Cập nhật số lượng thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi cập nhật số lượng: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<String> removeFromCart(@RequestParam UUID gioHangId, @RequestParam UUID chiTietSanPhamId, Authentication authentication) {
        try {
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để xóa sản phẩm");
            }
            chiTietGioHangService.removeChiTietGioHang(gioHangId, chiTietSanPhamId);
            return ResponseEntity.ok("Xóa sản phẩm thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Boolean>> checkAuthentication(Authentication authentication) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isAuthenticated", authentication != null && authentication.isAuthenticated());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-user")
    public ResponseEntity<Map<String, Object>> getUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Không có người dùng được xác thực"));
        }
        NguoiDung user = (NguoiDung) authentication.getPrincipal();
        Map<String, Object> response = new HashMap<>();
        response.put("hoTen", user.getHoTen());
        response.put("tenDangNhap", user.getTenDangNhap());
        return ResponseEntity.ok(response);
    }

    private UUID getNguoiDungIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof NguoiDung) {
            return ((NguoiDung) authentication.getPrincipal()).getId();
        }
        return null;
    }
}