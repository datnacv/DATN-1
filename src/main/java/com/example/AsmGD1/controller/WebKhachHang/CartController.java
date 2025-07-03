package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Thêm sản phẩm vào giỏ hàng
     * @param payload Map chứa id (ChiTietSanPhamId), quantity
     * @param authentication Đối tượng xác thực để lấy nguoiDungId
     * @return ResponseEntity với thông báo thành công
     */
    @PostMapping("/add")
    public ResponseEntity<String> addToCart(@RequestBody Map<String, String> payload, Authentication authentication) {
        UUID chiTietSanPhamId = UUID.fromString(payload.get("id"));
        int quantity = Integer.parseInt(payload.get("quantity"));

        // Lấy nguoiDungId từ authentication (giả định đã tích hợp Spring Security)
        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return ResponseEntity.badRequest().body("Người dùng chưa đăng nhập");
        }

        // Tạo hoặc lấy giỏ hàng
        GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);

        // Thêm sản phẩm vào giỏ hàng
        khachHangGioHangService.addToGioHang(gioHang.getId(), chiTietSanPhamId, quantity);
        return ResponseEntity.ok("Added to cart");
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng
     * @param chiTietGioHangId ID của chi tiết giỏ hàng
     * @param quantity Số lượng mới
     * @return ResponseEntity với thông báo thành công
     */
    @PostMapping("/update-quantity/{chiTietGioHangId}")
    public ResponseEntity<String> updateQuantity(@PathVariable UUID chiTietGioHangId, @RequestParam int quantity) {
        ChiTietGioHang chiTiet = chiTietGioHangService.updateSoLuong(chiTietGioHangId, quantity);
        return ResponseEntity.ok("Quantity updated to " + quantity);
    }

    /**
     * Áp dụng mã giảm giá cho sản phẩm trong giỏ hàng
     * @param chiTietGioHangId ID của chi tiết giỏ hàng
     * @param discount Số tiền giảm giá
     * @return ResponseEntity với thông báo thành công
     */
    @PostMapping("/apply-discount/{chiTietGioHangId}")
    public ResponseEntity<String> applyDiscount(@PathVariable UUID chiTietGioHangId, @RequestParam BigDecimal discount) {
        ChiTietGioHang chiTiet = chiTietGioHangService.applyDiscount(chiTietGioHangId, discount);
        return ResponseEntity.ok("Discount of " + discount + " applied successfully");
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     * @param chiTietGioHangId ID của chi tiết giỏ hàng
     * @param authentication Đối tượng xác thực để lấy nguoiDungId
     * @return ResponseEntity với thông báo thành công
     */
    @PostMapping("/remove/{chiTietGioHangId}")
    public ResponseEntity<String> removeFromCart(@PathVariable UUID chiTietGioHangId, Authentication authentication) {
        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return ResponseEntity.badRequest().body("Người dùng chưa đăng nhập");
        }

        GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
        ChiTietGioHang chiTiet = chiTietGioHangService.getChiTietGioHang(chiTietGioHangId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại"));
        if (!chiTiet.getGioHang().getId().equals(gioHang.getId())) {
            return ResponseEntity.badRequest().body("Chi tiết giỏ hàng không thuộc về người dùng");
        }

        chiTietGioHangService.removeChiTietGioHang(gioHang.getId(), chiTiet.getChiTietSanPham().getId());
        return ResponseEntity.ok("Removed from cart");
    }

    /**
     * Lấy danh sách sản phẩm trong giỏ hàng
     * @param authentication Đối tượng xác thực để lấy nguoiDungId
     * @return ResponseEntity với danh sách chi tiết giỏ hàng
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(Authentication authentication) {
        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Người dùng chưa đăng nhập"));
        }

        GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
        Map<String, Object> response = new HashMap<>();
        response.put("gioHangId", gioHang.getId());
        response.put("chiTietGioHang", khachHangGioHangService.getGioHangChiTiets(gioHang.getId()));
        response.put("tongTien", gioHang.getTongTien());
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy nguoiDungId từ authentication (giả định tích hợp Spring Security)
     * @param authentication Đối tượng xác thực
     * @return UUID của nguoiDungId
     */
    private UUID getNguoiDungIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof NguoiDung) {
            return ((NguoiDung) authentication.getPrincipal()).getId();
        }
        return null; // Cần tích hợp Spring Security để lấy chính xác
    }
}