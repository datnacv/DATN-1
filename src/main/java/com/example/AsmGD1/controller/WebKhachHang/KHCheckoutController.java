package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ThanhToan.APIResponse;
import com.example.AsmGD1.dto.KhachHang.ThanhToan.CheckoutRequest;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.BanHang.KHDonHangRepository;
import com.example.AsmGD1.repository.GiamGia.KHPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.NguoiDung.KHNguoiDungRepository;
import com.example.AsmGD1.service.WebKhachHang.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/checkout")
public class KHCheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(KHCheckoutController.class);

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private KHNguoiDungRepository nguoiDungRepository;

    @Autowired
    private KHPhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private KHDonHangRepository donHangRepository;

    @GetMapping("/check-auth")
    public ResponseEntity<APIResponse> checkAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
            logger.info("User authenticated: {}", nguoiDung.getTenDangNhap());
            return ResponseEntity.ok(new APIResponse("Authenticated", nguoiDung));
        }
        logger.warn("Unauthorized access attempt");
        return ResponseEntity.status(401).body(new APIResponse("Unauthorized"));
    }

    @GetMapping("/get-user")
    public ResponseEntity<APIResponse> getUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
            logger.info("User retrieved: {}", nguoiDung.getTenDangNhap());
            return ResponseEntity.ok(new APIResponse("User found", nguoiDung));
        }
        logger.warn("Unauthorized access attempt");
        return ResponseEntity.status(401).body(new APIResponse("Unauthorized"));
    }

    @PostMapping("/apply-voucher")
    public ResponseEntity<APIResponse> applyVoucher(@RequestParam String voucher, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated voucher apply attempt");
                return ResponseEntity.status(401).body(new APIResponse("Vui lòng đăng nhập"));
            }

            NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
            logger.info("Applying voucher {} for user: {}", voucher, nguoiDung.getTenDangNhap());
            PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findByMa(voucher)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));

            if (phieuGiamGia.getSoLuong() <= 0 || phieuGiamGia.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                logger.warn("Voucher invalid: {}", voucher);
                return ResponseEntity.badRequest().body(new APIResponse("Mã giảm giá đã hết hạn hoặc không còn số lượng"));
            }


            BigDecimal discount = checkoutService.calculateDiscount(phieuGiamGia);
            logger.info("Voucher applied successfully, discount: {}", discount);
            return ResponseEntity.ok(new APIResponse("Áp dụng mã giảm giá thành công", discount));
        } catch (RuntimeException e) {
            logger.error("Error applying voucher: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new APIResponse("Lỗi áp dụng mã giảm giá: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new APIResponse("Lỗi không xác định: " + e.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<APIResponse> submitOrder(@RequestBody CheckoutRequest request, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated order submission attempt");
                return ResponseEntity.status(401).body(new APIResponse("Vui lòng đăng nhập"));
            }

            NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
            logger.info("Submitting order for user: {}", nguoiDung.getTenDangNhap());
            DonHang donHang = checkoutService.createOrder(nguoiDung, request, request.getAddressId());
            logger.info("Order submitted successfully: {}", donHang.getMaDonHang());
            return ResponseEntity.ok(new APIResponse("Đặt hàng thành công", donHang.getMaDonHang()));
        } catch (RuntimeException e) {
            logger.error("Error submitting order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new APIResponse(e.getMessage())); // Trả về thông báo lỗi cụ thể
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new APIResponse("Lỗi không xác định: " + e.getMessage()));
        }
    }
}