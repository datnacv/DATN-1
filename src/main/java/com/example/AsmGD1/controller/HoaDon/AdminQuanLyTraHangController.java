package com.example.AsmGD1.controller.HoaDon;

import com.example.AsmGD1.dto.LichSuTraHangDTO;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuTraHangRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.ViThanhToan.LichSuGiaoDichViRepository;
import com.example.AsmGD1.repository.ViThanhToan.ViThanhToanRepository;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/quan-ly-tra-hang")
@PreAuthorize("hasRole('ADMIN')") // Chỉ admin mới truy cập
public class AdminQuanLyTraHangController {

    private static final Logger logger = LoggerFactory.getLogger(AdminQuanLyTraHangController.class);

    @Autowired
    private LichSuTraHangRepository lichSuTraHangRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private ChienDichGiamGiaService chienDichGiamGiaService;
    @Autowired
    private ViThanhToanRepository viThanhToanRepository;
    @Autowired
    private LichSuGiaoDichViRepository lichSuGiaoDichViRepository;

    // Trang danh sách yêu cầu trả hàng
    @GetMapping
    public String danhSachTraHang(
            @RequestParam(name = "status", defaultValue = "cho-xac-nhan") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "thoiGianTra");
        Pageable pageable = PageRequest.of(page, 10, sort);
        Page<LichSuTraHang> traHangPage;

        if ("tat-ca".equalsIgnoreCase(status)) {
            traHangPage = lichSuTraHangRepository.findAll(pageable);
        } else {
            String trangThaiDb = switch (status) {
                case "cho-xac-nhan" -> "Chờ xác nhận";
                case "da-xac-nhan" -> "Đã xác nhận";
                case "da-huy" -> "Đã hủy";
                default -> "Chờ xác nhận";
            };
            traHangPage = lichSuTraHangRepository.findByTrangThai(trangThaiDb, pageable);
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        List<LichSuTraHangDTO> formattedList = traHangPage.getContent().stream()
                .map(traHang -> new LichSuTraHangDTO(traHang, formatter))
                .collect(Collectors.toList());

        model.addAttribute("danhSachTraHang", formattedList);
        model.addAttribute("currentPage", traHangPage.getNumber());
        model.addAttribute("totalPages", traHangPage.getTotalPages());
        model.addAttribute("status", status);
        return "WebQuanLy/quan-ly-tra-hang/danh-sach";
    }

    private String formatCurrency(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount);
    }

    // Trang chi tiết yêu cầu trả hàng
    @GetMapping("/chi-tiet/{id}")
    public String chiTietTraHang(@PathVariable("id") UUID id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        try {
            Optional<LichSuTraHang> lichSuOpt = lichSuTraHangRepository.findById(id);
            if (lichSuOpt.isEmpty()) {
                model.addAttribute("lichSuTraHang", null);
                return "WebQuanLy/quan-ly-tra-hang/chi-tiet";
            }

            LichSuTraHang lichSu = lichSuOpt.get();

            // Kiểm tra null safety
            if (lichSu.getHoaDon() == null || lichSu.getChiTietDonHang() == null) {
                logger.warn("LichSuTraHang {} thiếu thông tin hoaDon hoặc chiTietDonHang", id);
                model.addAttribute("lichSuTraHang", null);
                return "WebQuanLy/quan-ly-tra-hang/chi-tiet";
            }

            HoaDon hoaDon = lichSu.getHoaDon();
            ChiTietDonHang chiTiet = lichSu.getChiTietDonHang();

            // Định dạng tiền tệ an toàn
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#,###", symbols);

            // Format giá sản phẩm
            if (chiTiet.getGia() != null) {
                try {
                    chiTiet.setFormattedGia(formatter.format(chiTiet.getGia()));
                } catch (Exception e) {
                    logger.error("Lỗi format giá sản phẩm: {}", e.getMessage());
                    chiTiet.setFormattedGia("0");
                }
            } else {
                chiTiet.setFormattedGia("0");
            }

            // Xử lý giảm giá an toàn
            ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();
            if (chiTietSanPham != null && chiTietSanPham.getGia() != null) {
                try {
                    Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPham.getId());
                    if (activeCampaign.isPresent()) {
                        ChienDichGiamGia campaign = activeCampaign.get();
                        BigDecimal discount = chiTietSanPham.getGia()
                                .multiply(campaign.getPhanTramGiam())
                                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                        BigDecimal giaSauGiam = chiTietSanPham.getGia().subtract(discount);
                        chiTiet.setFormattedGia(formatter.format(giaSauGiam));
                    }
                } catch (Exception e) {
                    logger.error("Lỗi xử lý giảm giá: {}", e.getMessage());
                    // Giữ nguyên giá gốc đã format
                }
            }

            // Tính và format tổng hoàn tiền an toàn
            String formattedTongHoan = "0";
            try {
                BigDecimal tongHoan = BigDecimal.ZERO;

                if ("Đã xác nhận".equals(lichSu.getTrangThai()) && lichSu.getTongTienHoan() != null) {
                    // Đã xác nhận - dùng số tiền hoàn thực tế
                    tongHoan = lichSu.getTongTienHoan();
                } else {
                    // Chưa xác nhận - tính ước tính
                    if (chiTiet.getThanhTien() != null) {
                        tongHoan = chiTiet.getThanhTien();
                    }
                }

                formattedTongHoan = formatter.format(tongHoan) + " VNĐ";
            } catch (Exception e) {
                logger.error("Lỗi format tổng hoàn: {}", e.getMessage());
                formattedTongHoan = "0 VNĐ";
            }

            model.addAttribute("lichSuTraHang", lichSu);
            model.addAttribute("hoaDon", hoaDon);
            model.addAttribute("chiTietDonHang", chiTiet);
            model.addAttribute("formattedTongHoan", formattedTongHoan);

            return "WebQuanLy/quan-ly-tra-hang/chi-tiet";

        } catch (Exception e) {
            logger.error("Lỗi không mong muốn trong chiTietTraHang: {}", e.getMessage(), e);
            model.addAttribute("lichSuTraHang", null);
            return "WebQuanLy/quan-ly-tra-hang/chi-tiet";
        }
    }

    // API xác nhận trả hàng
    @PostMapping("/api/xac-nhan/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> xacNhanTraHang(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để xác nhận.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<LichSuTraHang> lichSuOpt = lichSuTraHangRepository.findById(id);
            if (lichSuOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy yêu cầu trả hàng.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            LichSuTraHang lichSu = lichSuOpt.get();
            if (!"Chờ xác nhận".equals(lichSu.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Yêu cầu này không ở trạng thái chờ xác nhận.");
                return ResponseEntity.badRequest().body(response);
            }

            HoaDon hoaDon = lichSu.getHoaDon();
            ChiTietDonHang chiTietDonHang = lichSu.getChiTietDonHang();
            NguoiDung nguoiDung = hoaDon.getDonHang().getNguoiDung();

            // Validation thêm
            if (chiTietDonHang.getThanhTien() == null) {
                response.put("success", false);
                response.put("message", "Không thể tính toán tiền hoàn - thiếu thông tin giá.");
                return ResponseEntity.badRequest().body(response);
            }

            // 1. Tính toán tiền hoàn thực tế với try-catch
            BigDecimal tongTienHoanThucTe = BigDecimal.ZERO;
            try {
                BigDecimal tongTienHangTra = chiTietDonHang.getThanhTien();
                BigDecimal tongTienGocHoaDon = hoaDon.getDonHang().getChiTietDonHangs().stream()
                        .filter(item -> item.getThanhTien() != null)
                        .map(ChiTietDonHang::getThanhTien)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal tyLeHoanTra = tongTienGocHoaDon.compareTo(BigDecimal.ZERO) > 0
                        ? tongTienHangTra.divide(tongTienGocHoaDon, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                BigDecimal tienGiamHoaDon = hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO;
                BigDecimal giamGiaHoanTra = tienGiamHoaDon.multiply(tyLeHoanTra).setScale(0, RoundingMode.HALF_UP);
                tongTienHoanThucTe = tongTienHangTra.subtract(giamGiaHoanTra);

                if (tongTienHoanThucTe.compareTo(BigDecimal.ZERO) < 0) {
                    tongTienHoanThucTe = BigDecimal.ZERO;
                }
            } catch (Exception e) {
                logger.error("Lỗi tính toán tiền hoàn: {}", e.getMessage());
                response.put("success", false);
                response.put("message", "Lỗi tính toán tiền hoàn.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            // 2. Cập nhật tồn kho
            ChiTietSanPham chiTietSanPham = chiTietDonHang.getChiTietSanPham();
            if (chiTietSanPham != null) {
                chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() + lichSu.getSoLuong());
                chiTietSanPhamRepository.save(chiTietSanPham);
            }

            // 3. Hoàn tiền vào ví (nếu cần)
            String pttt = hoaDon.getPhuongThucThanhToan() != null
                    ? hoaDon.getPhuongThucThanhToan().getTenPhuongThuc().trim()
                    : "";

            if (tongTienHoanThucTe.compareTo(BigDecimal.ZERO) > 0 &&
                    List.of("Ví Thanh Toán", "Ví", "Chuyển khoản", "Tiền mặt").contains(pttt)) {

                try {
                    Optional<ViThanhToan> viOpt = viThanhToanRepository.findByNguoiDung(nguoiDung);
                    if (viOpt.isPresent()) {
                        ViThanhToan viThanhToan = viOpt.get();
                        viThanhToan.setSoDu(viThanhToan.getSoDu().add(tongTienHoanThucTe));
                        viThanhToan.setThoiGianCapNhat(LocalDateTime.now());
                        viThanhToanRepository.save(viThanhToan);

                        LichSuGiaoDichVi lichSuGiaoDich = new LichSuGiaoDichVi();
                        lichSuGiaoDich.setIdViThanhToan(viThanhToan.getId());
                        lichSuGiaoDich.setLoaiGiaoDich("Hoàn tiền");
                        lichSuGiaoDich.setSoTien(tongTienHoanThucTe);
                        lichSuGiaoDich.setMoTa("Hoàn tiền trả hàng cho đơn hàng: " + hoaDon.getDonHang().getMaDonHang());
                        lichSuGiaoDich.setCreatedAt(LocalDateTime.now());
                        lichSuGiaoDich.setThoiGianGiaoDich(LocalDateTime.now());
                        lichSuGiaoDichViRepository.save(lichSuGiaoDich);
                    }
                } catch (Exception e) {
                    logger.error("Lỗi cập nhật ví: {}", e.getMessage());
                    // Tiếp tục xử lý, không dừng lại
                }
            }

            // 4. Cập nhật trạng thái - ĐẢM BẢO SET tongTienHoan
            lichSu.setTongTienHoan(tongTienHoanThucTe); // QUAN TRỌNG: Set giá trị này
            lichSu.setTrangThai("Đã xác nhận");
            lichSu.setThoiGianXacNhan(LocalDateTime.now());
            lichSuTraHangRepository.save(lichSu);

            // Flush để đảm bảo dữ liệu được commit
            lichSuTraHangRepository.flush();

            // 5. Cập nhật hóa đơn
            List<ChiTietDonHang> chiTietDonHangs = hoaDon.getDonHang().getChiTietDonHangs();
            long totalItems = chiTietDonHangs.size();
            long returnedItems = chiTietDonHangs.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getTrangThaiHoanTra()))
                    .count();

            String trangThaiTraHang = returnedItems == totalItems ? "Đã trả hàng" : "Đã trả hàng một phần";

            BigDecimal tongTienMoi = hoaDon.getTongTien().subtract(tongTienHoanThucTe);
            if (tongTienMoi.compareTo(BigDecimal.ZERO) < 0) {
                tongTienMoi = BigDecimal.ZERO;
            }

            hoaDon.setTongTien(tongTienMoi);
            hoaDon.setTrangThai(trangThaiTraHang);
            hoaDonRepository.save(hoaDon);

            // 6. Lịch sử hóa đơn
            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai(trangThaiTraHang);
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Admin xác nhận trả hàng: " + request.getOrDefault("ghiChu", "Không có ghi chú") +
                    ". Tổng tiền hoàn: " + formatCurrency(tongTienHoanThucTe));
            lichSuHoaDonRepository.save(lichSuHoaDon);

            // 7. Thông báo
            try {
                thongBaoService.taoThongBaoHeThong(
                        nguoiDung.getTenDangNhap(),
                        "Yêu cầu trả hàng được xác nhận",
                        "Yêu cầu trả hàng cho đơn hàng " + hoaDon.getDonHang().getMaDonHang() +
                                " đã được xác nhận. Tổng tiền hoàn: " + formatCurrency(tongTienHoanThucTe) +
                                " đã được cộng vào ví của bạn.",
                        hoaDon.getDonHang()
                );
            } catch (Exception e) {
                logger.error("Lỗi gửi thông báo: {}", e.getMessage());
                // Không dừng transaction vì thông báo không quan trọng bằng việc cập nhật dữ liệu
            }

            response.put("success", true);
            response.put("message", "Đã xác nhận trả hàng thành công. Tồn kho và ví khách hàng đã được cập nhật.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi không mong muốn khi xác nhận trả hàng: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống khi xác nhận: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    // API hủy yêu cầu trả hàng (nếu cần)
    @PostMapping("/api/huy/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> huyTraHang(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        // Tương tự kiểm tra authentication...

        Optional<LichSuTraHang> lichSuOpt = lichSuTraHangRepository.findById(id);
        if (lichSuOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy yêu cầu trả hàng.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        LichSuTraHang lichSu = lichSuOpt.get();
        if (!"Chờ xác nhận".equals(lichSu.getTrangThai())) {
            response.put("success", false);
            response.put("message", "Yêu cầu này không ở trạng thái chờ xác nhận.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            lichSu.setTrangThai("Đã hủy");
            lichSu.setThoiGianXacNhan(LocalDateTime.now());
            lichSuTraHangRepository.save(lichSu);

            // Cập nhật trạng thái hóa đơn nếu cần (ví dụ: quay về "Hoàn thành")
            HoaDon hoaDon = lichSu.getHoaDon();
            hoaDon.setTrangThai("Hoàn thành");
            hoaDonRepository.save(hoaDon);

            // Thêm lịch sử
            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai("Hoàn thành");
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Admin hủy yêu cầu trả hàng: " + request.getOrDefault("ghiChu", "Không có ghi chú"));
            lichSuHoaDonRepository.save(lichSuHoaDon);

            // Thông báo khách hàng
            thongBaoService.taoThongBaoHeThong(
                    hoaDon.getDonHang().getNguoiDung().getTenDangNhap(),
                    "Yêu cầu trả hàng bị hủy",
                    "Yêu cầu trả hàng cho đơn hàng " + hoaDon.getDonHang().getMaDonHang() + " đã bị hủy bởi admin.",
                    hoaDon.getDonHang()
            );

            response.put("success", true);
            response.put("message", "Đã hủy yêu cầu trả hàng thành công.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi hủy trả hàng: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi hủy: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
