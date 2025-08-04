package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ThanhToan.APIResponse;
import com.example.AsmGD1.dto.KhachHang.ThanhToan.CheckoutRequest;
import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.BanHang.KHDonHangRepository;
import com.example.AsmGD1.repository.GiamGia.KHPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.NguoiDung.KHNguoiDungRepository;
import com.example.AsmGD1.service.WebKhachHang.CheckoutService;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;

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

    @Autowired
    private KhachHangGioHangService khachHangGioHangService;

    @Autowired
    private ChiTietGioHangService chiTietGioHangService;

    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungService phieuGiamGiaCuaNguoiDungService;

    // DTO để nhận dữ liệu checkoutItem từ frontend
    public static class CheckoutItem {
        private String chiTietSanPhamId;
        private BigDecimal gia;
        private int soLuong;

        // Getters and setters
        public String getChiTietSanPhamId() { return chiTietSanPhamId; }
        public void setChiTietSanPhamId(String chiTietSanPhamId) { this.chiTietSanPhamId = chiTietSanPhamId; }
        public BigDecimal getGia() { return gia; }
        public void setGia(BigDecimal gia) { this.gia = gia; }
        public int getSoLuong() { return soLuong; }
        public void setSoLuong(int soLuong) { this.soLuong = soLuong; }
    }

    private String formatVND(BigDecimal number) {
        return new DecimalFormat("#,##0").format(number) + " VNĐ";
    }

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
    public ResponseEntity<?> applyVoucher(@RequestParam String voucher,
                                          @RequestParam(required = false) String source,
                                          @RequestBody(required = false) List<CheckoutItem> checkoutItems,
                                          Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthenticated voucher apply attempt");
                return ResponseEntity.status(401).body(new ApiResponse(false, "Vui lòng đăng nhập"));
            }

            NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
            logger.info("Applying voucher {} for user: {}, source: {}", voucher, nguoiDung.getTenDangNhap(), source);

            PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findByMa(voucher)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));

            if (phieuGiamGia.getSoLuong() <= 0 || phieuGiamGia.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                logger.warn("Voucher invalid: {}", voucher);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã giảm giá đã hết hạn hoặc không còn số lượng"));
            }

            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieuGiamGia))) {
                logger.warn("Voucher not active: {}", voucher);
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Phiếu giảm giá không trong thời gian hiệu lực"));
            }

            BigDecimal tongTien;
            if ("buy-now".equalsIgnoreCase(source) && checkoutItems != null && !checkoutItems.isEmpty()) {
                // Xử lý "Mua ngay"
                tongTien = checkoutItems.stream()
                        .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                logger.info("Buy-now total: {}", formatVND(tongTien));
            } else {
                // Xử lý giỏ hàng
                var gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
                List<ChiTietGioHang> chiTietList = chiTietGioHangService.getGioHangChiTietList(gioHang.getId());
                if (chiTietList.isEmpty()) {
                    logger.warn("Cart is empty for user: {}", nguoiDung.getTenDangNhap());
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Giỏ hàng trống, không thể áp dụng mã giảm giá"));
                }
                tongTien = chiTietList.stream()
                        .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                logger.info("Cart total: {}", formatVND(tongTien));
            }

            if (phieuGiamGia.getGiaTriGiamToiThieu() != null && tongTien.compareTo(phieuGiamGia.getGiaTriGiamToiThieu()) < 0) {
                logger.warn("Order value too low for voucher: {}. Current: {}, Required: {}",
                        voucher, formatVND(tongTien), formatVND(phieuGiamGia.getGiaTriGiamToiThieu()));
                return ResponseEntity.badRequest().body(new ApiResponse(false,
                        String.format("Đơn hàng chưa đạt giá trị tối thiểu. Hiện tại: %s, Yêu cầu: %s",
                                formatVND(tongTien), formatVND(phieuGiamGia.getGiaTriGiamToiThieu()))));
            }

            boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(phieuGiamGia.getKieuPhieu());
            boolean used = isCaNhan
                    ? phieuGiamGiaCuaNguoiDungService.suDungPhieu(nguoiDung.getId(), phieuGiamGia.getId())
                    : phieuGiamGiaService.apDungPhieuGiamGia(phieuGiamGia.getId());

            if (!used) {
                logger.warn("Voucher not usable: {}", voucher);
                return ResponseEntity.badRequest().body(new ApiResponse(false, isCaNhan
                        ? "Mã giảm giá cá nhân không khả dụng hoặc đã hết lượt sử dụng"
                        : "Mã giảm giá công khai đã hết lượt sử dụng"));
            }

            BigDecimal discount = phieuGiamGiaService.tinhTienGiamGia(phieuGiamGia, tongTien);
            logger.info("Voucher applied successfully, discount: {}", formatVND(discount));
            return ResponseEntity.ok(new ApiResponse(true, "Áp dụng mã giảm giá thành công", discount));

        } catch (RuntimeException e) {
            logger.error("Error applying voucher: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Lỗi áp dụng mã giảm giá: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new ApiResponse(false, "Lỗi không xác định: " + e.getMessage()));
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
            DonHang donHang = checkoutService.createOrder(nguoiDung, request);
            logger.info("Order submitted successfully: {}", donHang.getMaDonHang());
            return ResponseEntity.ok(new APIResponse("Đặt hàng thành công", donHang.getMaDonHang()));
        } catch (RuntimeException e) {
            logger.error("Error submitting order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new APIResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new APIResponse("Lỗi không xác định: " + e.getMessage()));
        }
    }

    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}