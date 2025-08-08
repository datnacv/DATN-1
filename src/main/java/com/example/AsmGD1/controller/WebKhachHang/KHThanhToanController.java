package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/thanh-toan")
public class KHThanhToanController {

    private static final Logger logger = LoggerFactory.getLogger(KHThanhToanController.class);

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private KhachHangGioHangService khachHangGioHangService;

    @Autowired
    private ChiTietGioHangService chiTietGioHangService;

    @Autowired
    private DonHangRepository donHangRepo;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepo;

    @Autowired
    private HoaDonRepository hoaDonRepo;

    @Autowired
    private LichSuHoaDonRepository lichSuRepo;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucRepo;

    @Autowired
    private ViThanhToanService viThanhToanService;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungService phieuGiamGiaCuaNguoiDungService;

    @Autowired
    private DiaChiNguoiDungRepository diaChiNguoiDungRepository;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private HoaDonService hoaDonService;

    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof NguoiDung) {
            return ((NguoiDung) authentication.getPrincipal()).getEmail();
        }
        return authentication.getName();
    }
    private String dinhDangTien(BigDecimal soTien) {
        return String.format("%,.0f", soTien).replace(",", ".") + " VND";
    }

    @GetMapping
    public String showCheckoutPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("error", "Vui lòng đăng nhập để thanh toán!");
            return "WebKhachHang/thanh-toan";
        }
        try {
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                model.addAttribute("error", "Không thể xác định người dùng. Vui lòng thử lại!");
                return "WebKhachHang/thanh-toan";
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng với email " + email + " không tồn tại"));
            model.addAttribute("loggedInUser", nguoiDung);

            // Fetch all addresses for the user
            List<DiaChiNguoiDung> addresses = diaChiNguoiDungRepository.findByNguoiDung_Id(nguoiDung.getId());
            model.addAttribute("addresses", addresses);

            // Set default address if available
            Optional<DiaChiNguoiDung> defaultAddress = diaChiNguoiDungRepository.findByNguoiDung_IdAndMacDinhTrue(nguoiDung.getId());
            if (defaultAddress.isPresent()) {
                DiaChiNguoiDung address = defaultAddress.get();
                model.addAttribute("defaultAddress", address.getChiTietDiaChi() + ", " +
                        address.getPhuongXa() + ", " +
                        address.getQuanHuyen() + ", " +
                        address.getTinhThanhPho());
                model.addAttribute("nguoiNhan", address.getNguoiNhan() != null ? address.getNguoiNhan() : nguoiDung.getHoTen());
                model.addAttribute("soDienThoaiNguoiNhan", address.getSoDienThoaiNguoiNhan() != null ? address.getSoDienThoaiNguoiNhan() : nguoiDung.getSoDienThoai());
                model.addAttribute("chiTietDiaChi", address.getChiTietDiaChi() != null ? address.getChiTietDiaChi() : nguoiDung.getChiTietDiaChi());
            } else {
                model.addAttribute("defaultAddress", "");
                model.addAttribute("nguoiNhan", nguoiDung.getHoTen());
                model.addAttribute("soDienThoaiNguoiNhan", nguoiDung.getSoDienThoai());
            }

            // Existing voucher logic remains unchanged
            List<PhieuGiamGia> publicVouchers = phieuGiamGiaService.layTatCa().stream()
                    .filter(p -> "cong_khai".equalsIgnoreCase(p.getKieuPhieu()))
                    .filter(p -> "Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(p)))
                    .filter(p -> p.getGioiHanSuDung() != null && p.getGioiHanSuDung() > 0)
                    .toList();
            List<PhieuGiamGia> privateVouchers = phieuGiamGiaCuaNguoiDungService.layPhieuCaNhanConHan(nguoiDung.getId());
            List<PhieuGiamGia> allVouchers = new ArrayList<>();
            allVouchers.addAll(publicVouchers);
            allVouchers.addAll(privateVouchers);
            model.addAttribute("vouchers", allVouchers);
            logger.info("Tổng số voucher truyền ra view: {}", allVouchers.size());
            allVouchers.forEach(v -> logger.info(" - Voucher: {}", v.getMa()));
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải thông tin người dùng: " + e.getMessage());
            model.addAttribute("defaultAddress", "");
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(extractEmailFromAuthentication(authentication)).orElse(null);
            model.addAttribute("nguoiNhan", nguoiDung != null ? nguoiDung.getHoTen() : "");
            model.addAttribute("soDienThoaiNguoiNhan", nguoiDung != null ? nguoiDung.getSoDienThoai() : "");
        }
        return "WebKhachHang/thanh-toan";
    }
    @Transactional
    @PostMapping("/dat-hang")
    public String datHang(
            @RequestParam("ptThanhToan") String ptThanhToan,
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            @RequestParam("addressId") UUID addressId, // New parameter for address selection
            @RequestParam(value = "address", required = false) String customAddress, // Optional custom address
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "voucher", required = false) String voucher,
            @RequestParam(value = "shippingFee", required = false, defaultValue = "0") BigDecimal shippingFee,
            Authentication authentication,
            RedirectAttributes redirect) {
        String email = extractEmailFromAuthentication(authentication);
        if (email == null) {
            redirect.addFlashAttribute("error", "Không thể xác định email người dùng. Vui lòng đăng nhập lại.");
            return "redirect:/thanh-toan";
        }
        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email).orElse(null);
        if (nguoiDung == null) {
            redirect.addFlashAttribute("error", "Không tìm thấy người dùng với email: " + email);
            return "redirect:/thanh-toan";
        }
        GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
        List<ChiTietGioHang> chiTietList = chiTietGioHangService.getGioHangChiTietList(gioHang.getId());
        if (chiTietList.isEmpty()) {
            redirect.addFlashAttribute("error", "Giỏ hàng của bạn hiện đang rỗng.");
            return "redirect:/thanh-toan";
        }
        DonHang donHang = new DonHang();
        donHang.setNguoiDung(nguoiDung);
        donHang.setMaDonHang("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTrangThai("CHO_XAC_NHAN");
        donHang.setPhuongThucBanHang("Online");
        donHang.setPhiVanChuyen(shippingFee);
        donHang.setGhiChu(note);

        // Set address based on selected addressId
        if (addressId != null) {
            DiaChiNguoiDung selectedAddress = diaChiNguoiDungRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ."));
            donHang.setDiaChi(selectedAddress);
            donHang.setDiaChiGiaoHang(selectedAddress.getChiTietDiaChi() + ", " +
                    selectedAddress.getPhuongXa() + ", " +
                    selectedAddress.getQuanHuyen() + ", " +
                    selectedAddress.getTinhThanhPho());
            // Override recipient details if provided
            donHang.setGhiChu(note != null ? note + " | Người nhận: " + fullName + ", SĐT: " + phone : "Người nhận: " + fullName + ", SĐT: " + phone);
        } else if (customAddress != null && !customAddress.trim().isEmpty()) {
            // Handle custom address input
            donHang.setDiaChiGiaoHang(customAddress);
            donHang.setGhiChu(note != null ? note + " | Người nhận: " + fullName + ", SĐT: " + phone : "Người nhận: " + fullName + ", SĐT: " + phone);
        } else {
            // Fallback to default address
            Optional<DiaChiNguoiDung> defaultAddress = diaChiNguoiDungRepository.findByNguoiDung_IdAndMacDinhTrue(nguoiDung.getId());
            if (defaultAddress.isPresent()) {
                donHang.setDiaChi(defaultAddress.get());
                donHang.setDiaChiGiaoHang(defaultAddress.get().getChiTietDiaChi() + ", " +
                        defaultAddress.get().getPhuongXa() + ", " +
                        defaultAddress.get().getQuanHuyen() + ", " +
                        defaultAddress.get().getTinhThanhPho());
                donHang.setGhiChu(note != null ? note + " | Người nhận: " + fullName + ", SĐT: " + phone : "Người nhận: " + fullName + ", SĐT: " + phone);
            } else {
                redirect.addFlashAttribute("error", "Vui lòng chọn hoặc nhập địa chỉ giao hàng.");
                return "redirect:/thanh-toan";
            }
        }

        PhuongThucThanhToan pttt = phuongThucRepo.findById(UUID.fromString(ptThanhToan))
                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không hợp lệ."));
        donHang.setPhuongThucThanhToan(pttt);

        // Existing logic for order items, vouchers, and totals remains unchanged
        BigDecimal tongTien = BigDecimal.ZERO;
        List<ChiTietDonHang> chiTietListDH = new ArrayList<>();
        for (ChiTietGioHang item : chiTietList) {
            ChiTietSanPham chiTietSP = item.getChiTietSanPham();
            if (chiTietSP == null || chiTietSP.getSanPham() == null || item.getGia() == null) {
                redirect.addFlashAttribute("error", "Dữ liệu giỏ hàng không hợp lệ.");
                return "redirect:/thanh-toan";
            }
            if (chiTietSP.getSoLuongTonKho() < item.getSoLuong()) {
                redirect.addFlashAttribute("error", "Sản phẩm " + chiTietSP.getSanPham().getTenSanPham() + " không đủ số lượng.");
                return "redirect:/thanh-toan";
            }
            BigDecimal gia = item.getGia();
            int soLuong = item.getSoLuong();
            BigDecimal thanhTien = gia.multiply(BigDecimal.valueOf(soLuong));
            ChiTietDonHang ct = new ChiTietDonHang();
            ct.setDonHang(donHang);
            ct.setChiTietSanPham(chiTietSP);
            ct.setSoLuong(soLuong);
            ct.setGia(gia);
            ct.setTenSanPham(chiTietSP.getSanPham().getTenSanPham());
            ct.setThanhTien(thanhTien);
            chiTietListDH.add(ct);
            chiTietSP.setSoLuongTonKho(chiTietSP.getSoLuongTonKho() - soLuong);
            chiTietSanPhamRepository.save(chiTietSP);
            tongTien = tongTien.add(thanhTien);
        }

        BigDecimal giamGia = BigDecimal.ZERO;
        if (voucher != null && !voucher.trim().isEmpty()) {
            PhieuGiamGia phieu = phieuGiamGiaService.layTatCa().stream()
                    .filter(p -> p.getMa() != null && p.getMa().equalsIgnoreCase(voucher.trim()))
                    .findFirst()
                    .orElse(null);
            if (phieu == null) {
                redirect.addFlashAttribute("error", "Mã giảm giá không tồn tại.");
                return "redirect:/thanh-toan";
            }
            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieu))) {
                redirect.addFlashAttribute("error", "Phiếu giảm giá không trong thời gian hiệu lực.");
                return "redirect:/thanh-toan";
            }
            if (phieu.getGiaTriGiamToiThieu() != null &&
                    tongTien.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                redirect.addFlashAttribute("error", "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã.");
                return "redirect:/thanh-toan";
            }
            boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(phieu.getKieuPhieu());
            boolean used = isCaNhan
                    ? phieuGiamGiaCuaNguoiDungService.suDungPhieu(nguoiDung.getId(), phieu.getId())
                    : phieuGiamGiaService.apDungPhieuGiamGia(phieu.getId());
            if (!used) {
                redirect.addFlashAttribute("error", isCaNhan
                        ? "Mã giảm giá cá nhân không khả dụng hoặc đã hết lượt sử dụng."
                        : "Mã giảm giá công khai đã hết lượt sử dụng.");
                return "redirect:/thanh-toan";
            }
            giamGia = phieuGiamGiaService.tinhTienGiamGia(phieu, tongTien);
            donHang.setPhieuGiamGia(phieu);
        }

        donHang.setTongTien(tongTien.add(shippingFee).subtract(giamGia));
        donHang.setTienGiam(giamGia);
        donHangRepo.save(donHang);
        donHangRepo.flush();
        chiTietDonHangRepo.saveAll(chiTietListDH);
        hoaDonService.createHoaDonFromDonHang(donHang); // ← BẮT BUỘC
        System.out.println("🔥 Đã lưu đơn hàng, chuẩn bị gửi thông báo tới admin...");
        thongBaoService.taoThongBaoHeThong(
                "admin",
                "Khách hàng đặt đơn hàng online",
                "Mã đơn hàng: " + donHang.getMaDonHang(),
                donHang // ✅ Gửi vào để tránh lỗi null
        );
        chiTietGioHangService.clearGioHang(gioHang.getId());

        redirect.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn hàng: " + donHang.getMaDonHang());
        return "redirect:/thanh-toan";
    }


    @PostMapping("/api/checkout/apply-voucher")
    public ResponseEntity<?> applyVoucher(@RequestParam("voucher") String voucherCode, Authentication authentication) {
        try {
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Không thể xác định người dùng."));
            }

            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email).orElse(null);
            if (nguoiDung == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Người dùng không tồn tại."));
            }

            PhieuGiamGia phieu = phieuGiamGiaService.layTatCa().stream()
                    .filter(p -> p.getMa().equalsIgnoreCase(voucherCode.trim()))
                    .findFirst()
                    .orElse(null);

            if (phieu == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã giảm giá không tồn tại."));
            }

            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieu))) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Phiếu giảm giá không trong thời gian hiệu lực."));
            }

            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
            List<ChiTietGioHang> chiTietList = chiTietGioHangService.getGioHangChiTietList(gioHang.getId());
            BigDecimal tongTien = chiTietList.stream()
                    .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (phieu.getGiaTriGiamToiThieu() != null && tongTien.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Đơn hàng chưa đạt giá trị tối thiểu."));
            }

            boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(phieu.getKieuPhieu());
            boolean used = isCaNhan
                    ? phieuGiamGiaCuaNguoiDungService.suDungPhieu(nguoiDung.getId(), phieu.getId())
                    : phieuGiamGiaService.apDungPhieuGiamGia(phieu.getId());

            if (!used) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Mã giảm giá không khả dụng hoặc đã hết lượt sử dụng."));
            }

            BigDecimal giamGia = phieuGiamGiaService.tinhTienGiamGia(phieu, tongTien);
            logger.info("🎯 Áp dụng mã tại /apply-voucher - Mã: {}, Loại: {}, Giá trị giảm: {}, Tổng tiền: {}, Giảm giá tính được: {}",
                    phieu.getMa(), phieu.getLoai(), phieu.getGiaTriGiam(), tongTien, giamGia);

            return ResponseEntity.ok(new ApiResponse(true, "Áp dụng mã giảm giá thành công.", giamGia));

        } catch (Exception e) {
            logger.error("Lỗi khi áp dụng mã giảm giá: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi áp dụng mã giảm giá: " + e.getMessage()));
        }
    }

    @GetMapping("/api/checkout/default-address")
    public ResponseEntity<?> getDefaultAddress(Authentication authentication) {
        try {
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Không thể xác định người dùng."));
            }

            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại."));

            Optional<DiaChiNguoiDung> defaultAddress = diaChiNguoiDungRepository.findByNguoiDung_IdAndMacDinhTrue(nguoiDung.getId());
            if (defaultAddress.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(true, "Không tìm thấy địa chỉ mặc định.", null));
            }

            DiaChiNguoiDung address = defaultAddress.get();
            return ResponseEntity.ok(new ApiResponse(true, "Lấy địa chỉ mặc định thành công.", address));
        } catch (Exception e) {
            logger.error("Lỗi khi lấy địa chỉ mặc định: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy địa chỉ mặc định: " + e.getMessage()));
        }
    }

    // Lớp hỗ trợ cho phản hồi API
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