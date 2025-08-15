package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ThanhToan.APIResponse;
import com.example.AsmGD1.dto.KhachHang.ThanhToan.CheckoutRequest;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.KHDonHangRepository;
import com.example.AsmGD1.repository.GiamGia.KHPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.NguoiDung.KHNguoiDungRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ThongBao.ChiTietThongBaoNhomRepository;
import com.example.AsmGD1.repository.ThongBao.ThongBaoNhomRepository;
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
import java.util.UUID;

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
    @Autowired
    private ThongBaoNhomRepository thongBaoNhomRepository;

    @Autowired
    private ChiTietThongBaoNhomRepository chiTietThongBaoNhomRepository;

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

            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieuGiamGia))) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Phiếu giảm giá không trong thời gian hiệu lực"));
            }

            BigDecimal tongTien;
            if ("buy-now".equalsIgnoreCase(source) && checkoutItems != null && !checkoutItems.isEmpty()) {
                // Tính tổng tiền từ mua ngay
                tongTien = checkoutItems.stream()
                        .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                // Tính tổng tiền từ giỏ hàng
                var gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
                List<ChiTietGioHang> chiTietList = chiTietGioHangService.getGioHangChiTietList(gioHang.getId());
                if (chiTietList.isEmpty()) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Giỏ hàng trống, không thể áp dụng mã giảm giá"));
                }
                tongTien = chiTietList.stream()
                        .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            if (phieuGiamGia.getGiaTriGiamToiThieu() != null &&
                    tongTien.compareTo(phieuGiamGia.getGiaTriGiamToiThieu()) < 0) {
                return ResponseEntity.badRequest().body(new ApiResponse(false,
                        String.format("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã (%s). Hiện tại: %s",
                                formatVND(phieuGiamGia.getGiaTriGiamToiThieu()), formatVND(tongTien))));
            }

            // Kiểm tra tính hợp lệ
            boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(phieuGiamGia.getKieuPhieu());
            boolean valid = isCaNhan
                    ? phieuGiamGiaCuaNguoiDungService.kiemTraPhieuCaNhan(nguoiDung.getId(), phieuGiamGia.getId())
                    : phieuGiamGia.getSoLuong() > 0;

            if (!valid) {
                return ResponseEntity.badRequest().body(new ApiResponse(false,
                        isCaNhan
                                ? "Mã giảm giá cá nhân không hợp lệ hoặc đã hết lượt sử dụng."
                                : "Mã giảm giá công khai đã hết lượt sử dụng."));
            }

            // 👉 Không trừ ở đây — chỉ tính giảm
            BigDecimal discount = phieuGiamGiaService.tinhTienGiamGia(phieuGiamGia, tongTien);

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

            DonHang donHang = checkoutService.createOrder(nguoiDung, request, request.getAddressId());
            logger.info("Order submitted successfully: {}", donHang.getMaDonHang());

            // ✅ Thêm đoạn tạo thông báo cho admin ở đây:
            ThongBaoNhom thongBao = new ThongBaoNhom();
            thongBao.setId(UUID.randomUUID());
            thongBao.setDonHang(donHang);
            thongBao.setVaiTroNhan("admin");
            thongBao.setTieuDe("Khách hàng đặt đơn hàng");
            thongBao.setNoiDung("Mã đơn: " + donHang.getMaDonHang());
            thongBao.setThoiGianTao(LocalDateTime.now());
            thongBao.setTrangThai("Mới");
            thongBaoNhomRepository.save(thongBao);

            List<NguoiDung> danhSachAdmin = nguoiDungRepository.findByVaiTro("admin");
            for (NguoiDung admin : danhSachAdmin) {
                ChiTietThongBaoNhom chiTiet = new ChiTietThongBaoNhom();
                chiTiet.setId(UUID.randomUUID());
                chiTiet.setNguoiDung(admin);
                chiTiet.setThongBaoNhom(thongBao);
                chiTiet.setDaXem(false);
                chiTietThongBaoNhomRepository.save(chiTiet);
            }

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