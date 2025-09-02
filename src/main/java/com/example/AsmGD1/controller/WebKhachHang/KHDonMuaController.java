package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuTraHangRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.ThongBao.ChiTietThongBaoNhomRepository;
import com.example.AsmGD1.repository.ThongBao.ThongBaoNhomRepository;
import com.example.AsmGD1.repository.WebKhachHang.DanhGiaRepository;
import com.example.AsmGD1.repository.WebKhachHang.LichSuDoiSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import com.example.AsmGD1.service.WebKhachHang.EmailService;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dsdon-mua")
public class KHDonMuaController {

    @Autowired
    private ThongBaoNhomRepository thongBaoNhomRepository;

    @Autowired
    private ChiTietThongBaoNhomRepository chiTietThongBaoNhomRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private NguoiDungService nguoiDungService;

    private static final Logger logger = LoggerFactory.getLogger(KHDonMuaController.class);

    @Autowired
    private DanhGiaRepository danhGiaRepository;

    @Autowired
    private HoaDonRepository hoaDonRepo;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LichSuTraHangRepository lichSuTraHangRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private LichSuDoiSanPhamRepository lichSuDoiSanPhamRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    private final String UPLOAD_DIR;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private ChienDichGiamGiaService chienDichGiamGiaService;
    @Autowired
    private DonHangPhieuGiamGiaRepository donHangPhieuGiamGiaRepository;




    public KHDonMuaController() {
        String os = System.getProperty("os.name").toLowerCase();
        UPLOAD_DIR = os.contains("win") ? "C:/DATN/uploads/danh_gia/" : System.getProperty("user.home") + "/DATN/uploads/danh_gia/";
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created directory: {}", UPLOAD_DIR);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + UPLOAD_DIR, e);
        }
    }

    private UUID getNguoiDungIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            NguoiDung nguoiDung = nguoiDungService.findByTenDangNhap(userDetails.getUsername());
            return nguoiDung != null ? nguoiDung.getId() : null;
        }

        if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            NguoiDung nguoiDung = nguoiDungService.findByEmail(email);
            return nguoiDung != null ? nguoiDung.getId() : null;
        }

        return null;
    }

    @GetMapping
    public String donMuaPage(@RequestParam(name = "status", defaultValue = "tat-ca") String status,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             Model model,
                             Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return "redirect:/dang-nhap";
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            return "redirect:/dang-nhap";
        }

        model.addAttribute("user", nguoiDung);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        Sort sort = Sort.by(Sort.Direction.DESC, "ngayTao");
        Pageable pageable = PageRequest.of(page, 5, sort);
        Page<HoaDon> hoaDonPage;

        if ("tat-ca".equalsIgnoreCase(status)) {
            hoaDonPage = hoaDonRepo.findByDonHang_NguoiDungId(nguoiDung.getId(), pageable);
        } else {
            String statusDb = switch (status) {
                case "cho-xac-nhan" -> "Chưa xác nhận";
                case "da-xac-nhan" -> "Đã xác nhận Online";
                case "dang-xu-ly-online" -> "Đang xử lý Online";
                case "dang-van-chuyen" -> "Đang vận chuyển";
                case "van-chuyen-thanh-cong" -> "Vận chuyển thành công";
                case "hoan-thanh" -> "Hoàn thành";
                case "da-huy" -> "Hủy đơn hàng";
                case "da-tra-hang" -> "Đã trả hàng";
                case "da-doi-hang" -> "Đã đổi hàng";
                case "cho-doi-hang" -> "Chờ xử lý đổi hàng";
                default -> "";
            };
            hoaDonPage = hoaDonRepo.findByDonHang_NguoiDungIdAndTrangThai(nguoiDung.getId(), statusDb, pageable);
        }

        List<HoaDon> danhSachHoaDon = hoaDonPage.getContent();
        for (HoaDon hoaDon : danhSachHoaDon) {
            hoaDon.setFormattedTongTien(hoaDon.getTongTien() != null ? formatter.format(hoaDon.getTongTien()) : "0");

            for (ChiTietDonHang chiTiet : hoaDon.getDonHang().getChiTietDonHangs()) {
                chiTiet.setFormattedGia(chiTiet.getGia() != null ? formatter.format(chiTiet.getGia()) : "0");

                // Lấy thông tin giảm giá từ ChiTietSanPham
                ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();
                Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPham.getId());
                if (activeCampaign.isPresent()) {
                    ChienDichGiamGia campaign = activeCampaign.get();
                    ChiTietSanPhamDto chiTietSanPhamDto = new ChiTietSanPhamDto();
                    chiTietSanPhamDto.setId(chiTietSanPham.getId());
                    chiTietSanPhamDto.setGia(chiTietSanPham.getGia());
                    chiTietSanPhamDto.setOldPrice(chiTietSanPham.getGia());
                    chiTietSanPhamDto.setDiscountPercentage(campaign.getPhanTramGiam());
                    chiTietSanPhamDto.setDiscountCampaignName(campaign.getTen());

                    // Tính giá sau giảm
                    BigDecimal discount = chiTietSanPham.getGia()
                            .multiply(campaign.getPhanTramGiam())
                            .divide(BigDecimal.valueOf(100));
                    chiTietSanPhamDto.setGia(chiTietSanPham.getGia().subtract(discount));
                    chiTietSanPhamDto.setDiscount(formatter.format(campaign.getPhanTramGiam()) + "%");

                    // Gán DTO vào chiTiet để sử dụng trong view
                    chiTiet.setChiTietSanPhamDto(chiTietSanPhamDto);
                    chiTiet.setFormattedGia(formatter.format(chiTietSanPhamDto.getGia()));
                }

                boolean daDanhGia = danhGiaRepository.existsByHoaDonIdAndChiTietSanPhamIdAndNguoiDungId(
                        hoaDon.getId(), chiTiet.getChiTietSanPham().getId(), nguoiDung.getId()
                );
                chiTiet.setDaDanhGia(daDanhGia);
            }
        }

        model.addAttribute("danhSachHoaDon", danhSachHoaDon);
        model.addAttribute("currentPage", hoaDonPage.getNumber());
        model.addAttribute("totalPages", hoaDonPage.getTotalPages());
        model.addAttribute("status", status);
        return "WebKhachHang/don-mua";
    }

    @GetMapping("/api/orders/status")
    public ResponseEntity<Map<String, Object>> getOrderStatus(
            @RequestParam("maDonHang") String maDonHang,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để tiếp tục.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            response.put("success", false);
            response.put("message", "Không thể xác định người dùng.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            response.put("success", false);
            response.put("message", "Người dùng không tồn tại.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Optional<HoaDon> hoaDonOpt = hoaDonRepo.findByDonHang_MaDonHang(maDonHang);
        if (hoaDonOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không tìm thấy đơn hàng.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        HoaDon hoaDon = hoaDonOpt.get();
        if (!hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDungId)) {
            response.put("success", false);
            response.put("message", "Bạn không có quyền truy cập đơn hàng này.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        String currentStatus = hoaDonService.getCurrentStatus(hoaDon);

        response.put("success", true);
        response.put("trangThai", currentStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/orders/search")
    public ResponseEntity<List<Map<String, Object>>> searchOrders(
            @RequestParam("maDonHang") String maDonHang,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<HoaDon> hoaDons = hoaDonRepo.findByDonHang_NguoiDungIdAndDonHang_MaDonHangContainingIgnoreCase(
                nguoiDung.getId(), maDonHang);

        List<Map<String, Object>> results = hoaDons.stream()
                .map(hoaDon -> {
                    Map<String, Object> order = new HashMap<>();
                    order.put("id", hoaDon.getId());
                    order.put("maDonHang", hoaDon.getDonHang().getMaDonHang());
                    order.put("trangThai", hoaDon.getTrangThai());
                    order.put("ngayTao", hoaDon.getNgayTao().toString());
                    return order;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @GetMapping("/chi-tiet/{id}")
    public String chiTietDonHang(@PathVariable("id") UUID id,
                                 Model model,
                                 Authentication authentication) {

        // 1) Bảo vệ đăng nhập
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }
        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return "redirect:/dang-nhap";
        }
        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            return "redirect:/dang-nhap";
        }

        // 2) Tải hóa đơn và kiểm tra quyền sở hữu
        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);
        if (hoaDon == null || hoaDon.getDonHang() == null
                || hoaDon.getDonHang().getNguoiDung() == null
                || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
            return "redirect:/dsdon-mua";
        }

        // 3) Định dạng tiền
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        hoaDon.setFormattedTongTien(
                hoaDon.getTongTien() != null ? formatter.format(hoaDon.getTongTien()) : "0"
        );

        // 4) Bơm thông tin giảm giá theo chiến dịch (nếu có) cho từng dòng sản phẩm
        for (ChiTietDonHang chiTiet : hoaDon.getDonHang().getChiTietDonHangs()) {
            chiTiet.setFormattedGia(
                    chiTiet.getGia() != null ? formatter.format(chiTiet.getGia()) : "0"
            );

            ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();
            if (chiTietSanPham != null) {
                Optional<ChienDichGiamGia> activeCampaign =
                        chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPham.getId());

                if (activeCampaign.isPresent()) {
                    ChienDichGiamGia campaign = activeCampaign.get();

                    ChiTietSanPhamDto dto = new ChiTietSanPhamDto();
                    dto.setId(chiTietSanPham.getId());
                    dto.setGia(chiTietSanPham.getGia());
                    dto.setOldPrice(chiTietSanPham.getGia());
                    dto.setDiscountPercentage(campaign.getPhanTramGiam());
                    dto.setDiscountCampaignName(campaign.getTen());

                    // Giá sau giảm
                    BigDecimal discount = chiTietSanPham.getGia()
                            .multiply(campaign.getPhanTramGiam())
                            .divide(BigDecimal.valueOf(100));
                    dto.setGia(chiTietSanPham.getGia().subtract(discount));
                    dto.setDiscount(formatter.format(campaign.getPhanTramGiam()) + "%");

                    // Gán DTO vào chi tiết
                    chiTiet.setChiTietSanPhamDto(dto);
                    chiTiet.setFormattedGia(formatter.format(dto.getGia()));
                }
            }
        }

        // 5) Lấy trạng thái hiện tại
        String currentStatus = hoaDonService.getCurrentStatus(hoaDon);

        // 6) Lấy chi tiết từng phiếu giảm giá áp cho đơn (ORDER/SHIPPING)
        // Cần khai báo @Autowired DonHangPhieuGiamGiaRepository donHangPhieuGiamGiaRepository;
        List<DonHangPhieuGiamGia> allVouchers =
                donHangPhieuGiamGiaRepository.findByDonHang_IdOrderByThoiGianApDungAsc(hoaDon.getDonHang().getId());

        List<DonHangPhieuGiamGia> voucherOrder = allVouchers.stream()
                .filter(p -> "ORDER".equalsIgnoreCase(p.getLoaiGiamGia()))
                .toList();

        List<DonHangPhieuGiamGia> voucherShip = allVouchers.stream()
                .filter(p -> "SHIPPING".equalsIgnoreCase(p.getLoaiGiamGia()))
                .toList();

        // (Tuỳ chọn) Tổng của từng nhóm – nếu muốn dùng ở View
        BigDecimal tongGiamOrder = voucherOrder.stream()
                .map(DonHangPhieuGiamGia::getGiaTriGiam)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tongGiamShip = voucherShip.stream()
                .map(DonHangPhieuGiamGia::getGiaTriGiam)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7) Đẩy dữ liệu ra view
        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("user", nguoiDung);
        model.addAttribute("currentStatus", currentStatus);
        model.addAttribute("statusHistory",
                hoaDon.getLichSuHoaDons() != null ? hoaDon.getLichSuHoaDons() : new ArrayList<>());

        // các list chi tiết phiếu để render bảng/khối “Chi tiết giảm giá”
        model.addAttribute("voucherOrder", voucherOrder);
        model.addAttribute("voucherShip", voucherShip);
        model.addAttribute("tongGiamOrder", tongGiamOrder);
        model.addAttribute("tongGiamShip", tongGiamShip);

        return "WebKhachHang/chi-tiet-don-mua";
    }


    @GetMapping("/danh-gia/{id}")
    public String danhGiaPage(@PathVariable("id") UUID id,
                              @RequestParam(value = "chiTietSanPhamId", required = false) UUID chiTietSanPhamId,
                              Model model,
                              Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return "redirect:/dang-nhap";
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            return "redirect:/dang-nhap";
        }

        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);
        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId()) || !"Hoàn thành".equals(hoaDon.getTrangThai())) {
            return "redirect:/dsdon-mua";
        }

        List<ChiTietDonHang> productsToRate = hoaDon.getDonHang().getChiTietDonHangs().stream()
                .filter(chiTiet -> !danhGiaRepository.existsByHoaDonIdAndChiTietSanPhamIdAndNguoiDungId(hoaDon.getId(), chiTiet.getChiTietSanPham().getId(), nguoiDung.getId()))
                .collect(Collectors.toList());

        if (productsToRate.isEmpty()) {
            model.addAttribute("message", "Bạn đã đánh giá tất cả sản phẩm trong hóa đơn này.");
        }

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("user", nguoiDung);
        model.addAttribute("productsToRate", productsToRate);
        return "WebKhachHang/danh-gia";
    }

    @PostMapping("/api/orders/cancel/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<?> cancelOrder(@PathVariable("id") UUID id, @RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để hủy đơn hàng.");
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Không thể xác định người dùng.");
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Người dùng không tồn tại.");
        }

        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);

        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng hoặc bạn không có quyền hủy đơn hàng này.");
        }

        String ghiChu = request.get("ghiChu");
        if (ghiChu == null || ghiChu.trim().isEmpty()) {
            ghiChu = "Khách hàng hủy đơn hàng";
        }

        try {
            hoaDonService.cancelOrder(id, ghiChu);
            thongBaoService.taoThongBaoHeThong(
                    "admin",
                    "Khách hàng hủy đơn hàng",
                    "Đơn hàng mã " + hoaDon.getDonHang().getMaDonHang() + " đã bị khách hàng hủy.",
                    hoaDon.getDonHang()
            );

            String emailContent = "<h2>Thông báo hủy đơn hàng</h2>" +
                    "<p>Xin chào " + nguoiDung.getHoTen() + ",</p>" +
                    "<p>Đơn hàng của bạn với mã <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã được hủy thành công.</p>" +
                    "<p><strong>Lý do hủy:</strong> " + ghiChu + "</p>" +
                    "<p>Cảm ơn bạn đã sử dụng dịch vụ của ACV Store!</p>" +
                    "<p>Trân trọng,<br>Đội ngũ ACV Store</p>";
            emailService.sendEmail(nguoiDung.getEmail(), "Hủy đơn hàng - ACV Store", emailContent);

            return ResponseEntity.ok("Đơn hàng đã được hủy thành công.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Chỉ có thể hủy đơn hàng ở trạng thái 'Chưa xác nhận', 'Đã xác nhận', 'Đã xác nhận Online', 'Đang xử lý Online' hoặc 'Đang vận chuyển'.");
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi xung đột đồng thời khi hủy đơn hàng. Vui lòng thử lại.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi hủy đơn hàng: " + e.getMessage());
        }
    }

    @PostMapping("/api/orders/confirm-received/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmReceivedOrder(@PathVariable("id") UUID id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("Received confirmReceivedOrder request for id: " + id);

            // Kiểm tra authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để xác nhận.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Lấy thông tin người dùng
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                response.put("success", false);
                response.put("message", "Không thể xác định người dùng.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
            if (nguoiDung == null) {
                response.put("success", false);
                response.put("message", "Người dùng không tồn tại.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Tìm hóa đơn
            Optional<HoaDon> hoaDonOpt = hoaDonRepo.findById(id);
            if (hoaDonOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy đơn hàng.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            HoaDon hoaDon = hoaDonOpt.get();

            // Kiểm tra quyền sở hữu
            if (hoaDon.getDonHang() == null ||
                    hoaDon.getDonHang().getNguoiDung() == null ||
                    !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền xác nhận đơn hàng này.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Kiểm tra trạng thái
            if (!"Vận chuyển thành công".equals(hoaDon.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Chỉ có thể xác nhận khi đơn hàng ở trạng thái 'Vận chuyển thành công'. Trạng thái hiện tại: " + hoaDon.getTrangThai());
                return ResponseEntity.badRequest().body(response);
            }

            // Cập nhật trạng thái hóa đơn
            hoaDon.setTrangThai("Hoàn thành");
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            hoaDon.setGhiChu("Khách hàng xác nhận đã nhận hàng");

            // Cập nhật trạng thái đơn hàng
            DonHang donHang = hoaDon.getDonHang();
            if (donHang != null) {
                donHang.setTrangThai("THANH_CONG");
                donHangRepository.save(donHang);
            }

            // Thêm lịch sử hóa đơn
            try {
                hoaDonService.addLichSuHoaDon(hoaDon, "Hoàn thành", "Khách hàng xác nhận đã nhận hàng");
            } catch (Exception e) {
                System.err.println("Lỗi khi thêm lịch sử hóa đơn: " + e.getMessage());
                // Tạo lịch sử thủ công nếu service bị lỗi
                LichSuHoaDon lichSu = new LichSuHoaDon();
                lichSu.setHoaDon(hoaDon);
                lichSu.setTrangThai("Hoàn thành");
                lichSu.setThoiGian(LocalDateTime.now());
                lichSu.setGhiChu("Khách hàng xác nhận đã nhận hàng");
                lichSuHoaDonRepository.save(lichSu);
            }

            // Lưu hóa đơn
            HoaDon savedHoaDon = hoaDonRepo.save(hoaDon);

            // Gửi thông báo (không để lỗi này làm fail transaction)
            try {
                thongBaoService.taoThongBaoHeThong(
                        "admin",
                        "Đơn hàng đã hoàn thành",
                        "Đơn hàng mã " + hoaDon.getDonHang().getMaDonHang() + " đã được khách hàng xác nhận hoàn thành.",
                        hoaDon.getDonHang()
                );
            } catch (Exception e) {
                System.err.println("Lỗi khi gửi thông báo: " + e.getMessage());
            }

            // Gửi email (không để lỗi này làm fail transaction)
            try {
                String emailContent = "<h2>Thông báo hoàn thành đơn hàng</h2>" +
                        "<p>Xin chào " + nguoiDung.getHoTen() + ",</p>" +
                        "<p>Đơn hàng của bạn với mã <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã được xác nhận hoàn thành.</p>" +
                        "<p>Cảm ơn bạn đã mua sắm tại ACV Store! Chúng tôi mong được phục vụ bạn trong tương lai.</p>" +
                        "<p>Trân trọng,<br>Đội ngũ ACV Store</p>";
                emailService.sendEmail(nguoiDung.getEmail(), "Hoàn thành đơn hàng - ACV Store", emailContent);
            } catch (Exception e) {
                System.err.println("Lỗi khi gửi email: " + e.getMessage());
            }

            // Trả về response thành công
            response.put("success", true);
            response.put("message", "Đã xác nhận nhận hàng thành công.");
            response.put("newStatus", "Hoàn thành");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.err.println("Lỗi tham số không hợp lệ: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Tham số không hợp lệ: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (ObjectOptimisticLockingFailureException e) {
            System.err.println("Lỗi xung đột dữ liệu: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Dữ liệu đã được cập nhật bởi người khác. Vui lòng thử lại.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            System.err.println("Lỗi không mong muốn khi xác nhận đơn hàng: " + e.getMessage());
            e.printStackTrace(); // In stack trace để debug
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xác nhận đơn hàng. Vui lòng thử lại sau.");
            response.put("error", e.getMessage()); // Thêm chi tiết lỗi để debug
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/api/orders/{id}/products")
    public ResponseEntity<List<Map<String, Object>>> getOrderProducts(@PathVariable("id") UUID id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);

        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<Map<String, Object>> products = hoaDon.getDonHang().getChiTietDonHangs().stream()
                .map(chiTiet -> {
                    Map<String, Object> product = new HashMap<>();
                    product.put("id", chiTiet.getChiTietSanPham().getId());
                    product.put("tenSanPham", chiTiet.getTenSanPham());
                    product.put("mauSac", chiTiet.getChiTietSanPham().getMauSac() != null ? chiTiet.getChiTietSanPham().getMauSac().getTenMau() : "N/A");
                    product.put("kichCo", chiTiet.getChiTietSanPham().getKichCo() != null ? chiTiet.getChiTietSanPham().getKichCo().getTen() : "N/A");
                    return product;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(products);
    }

    @PostMapping("/api/ratings")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> submitRating(
            @RequestParam("hoaDonId") UUID hoaDonId,
            @RequestParam("userId") UUID userId,
            @RequestParam("chiTietSanPhamId") UUID chiTietSanPhamId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "media", required = false) MultipartFile[] media,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để gửi đánh giá.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null || !nguoiDungId.equals(userId)) {
            response.put("success", false);
            response.put("message", "Bạn không có quyền gửi đánh giá này.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            response.put("success", false);
            response.put("message", "Người dùng không tồn tại.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        HoaDon hoaDon = hoaDonRepo.findById(hoaDonId).orElse(null);
        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(userId) || !"Hoàn thành".equals(hoaDon.getTrangThai())) {
            response.put("success", false);
            response.put("message", "Hóa đơn không hợp lệ hoặc chưa hoàn thành.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        boolean alreadyRated = danhGiaRepository.existsByHoaDonIdAndChiTietSanPhamIdAndNguoiDungId(hoaDonId, chiTietSanPhamId, userId);
        if (alreadyRated) {
            response.put("success", false);
            response.put("message", "Bạn đã đánh giá sản phẩm này trong hóa đơn này rồi.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        DanhGia danhGia = new DanhGia();
        danhGia.setHoaDon(hoaDon);
        danhGia.setChiTietSanPham(hoaDon.getDonHang().getChiTietDonHangs().stream()
                .filter(chiTiet -> chiTiet.getChiTietSanPham().getId().equals(chiTietSanPhamId))
                .findFirst().get().getChiTietSanPham());
        danhGia.setNguoiDung(nguoiDung);
        danhGia.setXepHang(rating);
        danhGia.setNoiDung(content);
        danhGia.setTrangThai(true);
        danhGia.setThoiGianDanhGia(LocalDateTime.now());

        if (media != null && media.length > 0) {
            java.util.List<String> urls = new java.util.ArrayList<>();
            for (MultipartFile f : media) {
                if (f == null || f.isEmpty()) continue;
                String ct = f.getContentType();
                if (ct == null || !ct.startsWith("image/")) continue;
                String url = uploadFile(f);
                urls.add(url);
            }
            if (!urls.isEmpty()) {
                danhGia.setUrlHinhAnh(String.join(",", urls));
            }
        }

        danhGiaRepository.save(danhGia);

        boolean allRated = hoaDon.getDonHang().getChiTietDonHangs().stream()
                .allMatch(chiTiet -> danhGiaRepository.existsByHoaDonIdAndChiTietSanPhamIdAndNguoiDungId(hoaDonId, chiTiet.getChiTietSanPham().getId(), userId));

        if (allRated) {
            response.put("success", true);
            response.put("message", "Bạn đã hoàn thành việc đánh giá tất cả sản phẩm trong hóa đơn này.");
        } else {
            response.put("success", true);
            response.put("message", "Đánh giá của bạn đã được gửi thành công.");
        }

        return ResponseEntity.ok(response);
    }

    private String uploadFile(MultipartFile file) {
        try {
            if (!file.getContentType().startsWith("image/")) {
                throw new RuntimeException("Chỉ được phép tải lên tệp hình ảnh.");
            }
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());
            if (!Files.exists(filePath)) {
                logger.error("Tệp không được lưu đúng cách: {}", filePath);
                throw new RuntimeException("Không thể lưu tệp: " + fileName);
            }
            logger.info("Đã lưu tệp: {}", filePath);
            return "/images/danh_gia/" + fileName;
        } catch (IOException e) {
            logger.error("Không thể lưu tệp: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Không thể lưu tệp: " + file.getOriginalFilename(), e);
        }
    }

    @GetMapping("/tra-hang/{id}")
    public String traHangPage(@PathVariable("id") UUID id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return "redirect:/dang-nhap";
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            return "redirect:/dang-nhap";
        }

        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);
        if (hoaDon == null || !hoaDon.getNguoiDung().getId().equals(nguoiDung.getId())) {
            return "redirect:/dsdon-mua";
        }

        if (!"Vận chuyển thành công".equals(hoaDon.getTrangThai())) {
            return "redirect:/dsdon-mua";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        hoaDon.setFormattedTongTien(hoaDon.getTongTien() != null ? formatter.format(hoaDon.getTongTien()) : "0");

        for (ChiTietDonHang chiTiet : hoaDon.getDonHang().getChiTietDonHangs()) {
            chiTiet.setFormattedGia(chiTiet.getGia() != null ? formatter.format(chiTiet.getGia()) : "0");

            ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();
            Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPham.getId());
            if (activeCampaign.isPresent()) {
                ChienDichGiamGia campaign = activeCampaign.get();
                ChiTietSanPhamDto chiTietSanPhamDto = new ChiTietSanPhamDto();
                chiTietSanPhamDto.setId(chiTietSanPham.getId());
                chiTietSanPhamDto.setGia(chiTietSanPham.getGia());
                chiTietSanPhamDto.setOldPrice(chiTietSanPham.getGia());
                chiTietSanPhamDto.setDiscountPercentage(campaign.getPhanTramGiam());
                chiTietSanPhamDto.setDiscountCampaignName(campaign.getTen());

                BigDecimal discount = chiTietSanPham.getGia()
                        .multiply(campaign.getPhanTramGiam())
                        .divide(BigDecimal.valueOf(100));
                chiTietSanPhamDto.setGia(chiTietSanPham.getGia().subtract(discount));
                chiTietSanPhamDto.setDiscount(formatter.format(campaign.getPhanTramGiam()) + "%");

                chiTiet.setChiTietSanPhamDto(chiTietSanPhamDto);
                chiTiet.setFormattedGia(formatter.format(chiTietSanPhamDto.getGia()));
            }
        }

        // Lấy chi tiết từng phiếu giảm giá áp cho đơn (ORDER/SHIPPING)
        List<DonHangPhieuGiamGia> allVouchers = donHangPhieuGiamGiaRepository.findByDonHang_IdOrderByThoiGianApDungAsc(hoaDon.getDonHang().getId());

        List<DonHangPhieuGiamGia> voucherOrder = allVouchers.stream()
                .filter(p -> "ORDER".equalsIgnoreCase(p.getLoaiGiamGia()))
                .toList();

        List<DonHangPhieuGiamGia> voucherShip = allVouchers.stream()
                .filter(p -> "SHIPPING".equalsIgnoreCase(p.getLoaiGiamGia()))
                .toList();

        // Tính tổng giảm giá cho từng loại
        BigDecimal tongGiamOrder = voucherOrder.stream()
                .map(DonHangPhieuGiamGia::getGiaTriGiam)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tongGiamShip = voucherShip.stream()
                .map(DonHangPhieuGiamGia::getGiaTriGiam)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Đẩy dữ liệu ra view
        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("user", nguoiDung);
        model.addAttribute("tongGiamOrder", tongGiamOrder);
        model.addAttribute("tongGiamShip", tongGiamShip);

        return "WebKhachHang/tra-hang";
    }

    @PostMapping("/api/orders/return/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> submitReturnRequest(
            @PathVariable("id") UUID id,
            @RequestParam("selectedProducts") List<UUID> selectedProductIds,
            @RequestParam("reason") String reason,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("email") String email, // giữ nếu cần dùng cho mục đích khác
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1) Kiểm tra đăng nhập
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để gửi yêu cầu trả hàng.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 2) Lấy & kiểm tra người dùng
            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                response.put("success", false);
                response.put("message", "Không thể xác định người dùng.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
            if (nguoiDung == null) {
                response.put("success", false);
                response.put("message", "Người dùng không tồn tại.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 3) Kiểm tra hóa đơn & quyền sở hữu
            Optional<HoaDon> hoaDonOpt = hoaDonRepo.findById(id);
            if (hoaDonOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Hóa đơn không tồn tại.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            HoaDon hoaDon = hoaDonOpt.get();
            if (hoaDon.getDonHang() == null
                    || hoaDon.getDonHang().getNguoiDung() == null
                    || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
                response.put("success", false);
                response.put("message", "Hóa đơn không hợp lệ hoặc không thuộc về bạn.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 4) Kiểm tra danh sách sản phẩm trả
            if (selectedProductIds == null || selectedProductIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một sản phẩm để trả hàng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // (Khuyến nghị) Xác nhận các ID được chọn thuộc chi tiết của đơn này
            var validIds = hoaDon.getDonHang().getChiTietDonHangs()
                    .stream().map(ChiTietDonHang::getId).collect(Collectors.toSet());
            boolean allBelong = selectedProductIds.stream().allMatch(validIds::contains);
            if (!allBelong) {
                response.put("success", false);
                response.put("message", "Có sản phẩm không thuộc hóa đơn này.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 5) Gọi service xử lý trả hàng (tính tiền hoàn, pro-rate giảm giá, hoàn ví nếu PTTT phù hợp,
            //    cập nhật tồn kho, lịch sử trả, cập nhật trạng thái + thông báo/email)
            String lyDoGop = (description != null && !description.isBlank())
                    ? (reason + ": " + description)
                    : reason;
            hoaDonService.processReturnKH(id, selectedProductIds, lyDoGop);

            // 6) Lấy trạng thái sau cập nhật để trả về client
            HoaDon hoaDonAfter = hoaDonRepo.findById(id).orElse(hoaDon);
            String currentStatus = hoaDonService.getCurrentStatus(hoaDonAfter);

            response.put("success", true);
            response.put("message", "Yêu cầu trả hàng đã được ghi nhận. Nếu đơn thanh toán bằng ví/chuyển khoản, tiền sẽ được hoàn vào ví.");
            response.put("currentStatus", currentStatus);
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Ví dụ: trạng thái hóa đơn không cho phép trả hàng (service đã kiểm tra)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi xung đột đồng thời. Vui lòng thử lại."));
        } catch (Exception e) {
            logger.error("Lỗi khi gửi yêu cầu trả hàng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi khi gửi yêu cầu trả hàng: " + e.getMessage()));
        }
    }

    private String formatVND(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(value) + " VNĐ";
    }

    @GetMapping("/tra-doi-san-pham/{id}")
    public String showExchangeForm(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) List<UUID> chiTietIds, // Chấp nhận danh sách chiTietIds
            Model model,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
        if (nguoiDungId == null) {
            return "redirect:/dang-nhap";
        }

        NguoiDung nguoiDung = nguoiDungService.findById(nguoiDungId);
        if (nguoiDung == null) {
            return "redirect:/dang-nhap";
        }

        Optional<HoaDon> hoaDonOpt = hoaDonRepo.findById(id);
        if (hoaDonOpt.isEmpty()) {
            model.addAttribute("hoaDon", null);
            return "WebKhachHang/tra-doi-san-pham";
        }

        HoaDon hoaDon = hoaDonOpt.get();
        if (!hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDungId) ||
                !List.of("Vận chuyển thành công", "Chờ xử lý đổi hàng", "Hoàn thành").contains(hoaDon.getTrangThai())) {
            model.addAttribute("hoaDon", null);
            return "WebKhachHang/tra-doi-san-pham";
        }

        // Kiểm tra nếu hóa đơn đã được đổi
        boolean daDoi = "Đã đổi hàng".equals(hoaDon.getTrangThai()) ||
                lichSuDoiSanPhamRepository.existsByHoaDonIdAndTrangThai(hoaDon.getId(), "Đã xác nhận");
        if (daDoi) {
            model.addAttribute("hoaDon", null);
            model.addAttribute("message", "Hóa đơn này đã đổi hàng. Không thể tạo yêu cầu đổi thêm.");
            return "WebKhachHang/tra-doi-san-pham";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        // Lấy chi tiết đơn hàng dựa trên chiTietIds nếu có, nếu không lấy toàn bộ
        List<ChiTietDonHang> chiTietDonHangs;
        if (chiTietIds != null && !chiTietIds.isEmpty()) {
            chiTietDonHangs = chiTietDonHangRepository.findAllById(chiTietIds).stream()
                    .filter(chiTiet -> chiTiet.getDonHang().getId().equals(hoaDon.getDonHang().getId()))
                    .collect(Collectors.toList());
        } else {
            chiTietDonHangs = chiTietDonHangRepository.findByDonHangId(hoaDon.getDonHang().getId());
        }

        // Kiểm tra nếu sản phẩm đã được đổi
        if (chiTietDonHangs.stream().anyMatch(ChiTietDonHang::getTrangThaiDoiSanPham)) {
            model.addAttribute("hoaDon", null);
            model.addAttribute("message", "Một hoặc nhiều sản phẩm đã được yêu cầu đổi trước đó.");
            return "WebKhachHang/tra-doi-san-pham";
        }

        // Xử lý danh sách sản phẩm trong đơn hàng
        for (ChiTietDonHang chiTiet : chiTietDonHangs) {
            chiTiet.setFormattedGia(chiTiet.getGia() != null ? formatter.format(chiTiet.getGia()) : "0");

            ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();
            Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPham.getId());
            if (activeCampaign.isPresent()) {
                ChienDichGiamGia campaign = activeCampaign.get();
                ChiTietSanPhamDto chiTietSanPhamDto = new ChiTietSanPhamDto();
                chiTietSanPhamDto.setId(chiTietSanPham.getId());
                chiTietSanPhamDto.setGia(chiTietSanPham.getGia());
                chiTietSanPhamDto.setOldPrice(chiTietSanPham.getGia());
                chiTietSanPhamDto.setDiscountPercentage(campaign.getPhanTramGiam());
                chiTietSanPhamDto.setDiscountCampaignName(campaign.getTen());

                BigDecimal discount = chiTietSanPham.getGia()
                        .multiply(campaign.getPhanTramGiam())
                        .divide(BigDecimal.valueOf(100));
                chiTietSanPhamDto.setGia(chiTietSanPham.getGia().subtract(discount));
                chiTietSanPhamDto.setDiscount(formatter.format(campaign.getPhanTramGiam()) + "%");

                chiTiet.setChiTietSanPhamDto(chiTietSanPhamDto);
                chiTiet.setFormattedGia(formatter.format(chiTietSanPhamDto.getGia()));
            }
        }

        // Xử lý danh sách sản phẩm thay thế với phân trang
        BigDecimal minPriceAfterDiscount = chiTietDonHangs.stream()
                .map(chiTiet -> chiTiet.getChiTietSanPhamDto() != null ? chiTiet.getChiTietSanPhamDto().getGia() : chiTiet.getGia())
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "gia"));
        Page<ChiTietSanPham> productPage;
        if (keyword.trim().isEmpty()) {
            productPage = chiTietSanPhamRepository.findBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqual(0, minPriceAfterDiscount, pageable);
        } else {
            productPage = chiTietSanPhamRepository.findBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqualAndSanPham_TenSanPhamContainingIgnoreCase(
                    0, minPriceAfterDiscount, keyword, pageable);
        }

        List<ChiTietSanPham> replacementProducts = productPage.getContent();
        for (ChiTietSanPham chiTietSanPham : replacementProducts) {
            Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPham.getId());
            BigDecimal replacementPriceAfterDiscount = chiTietSanPham.getGia();

            if (activeCampaign.isPresent()) {
                ChienDichGiamGia campaign = activeCampaign.get();
                ChiTietSanPhamDto chiTietSanPhamDto = new ChiTietSanPhamDto();
                chiTietSanPhamDto.setId(chiTietSanPham.getId());
                chiTietSanPhamDto.setGia(chiTietSanPham.getGia());
                chiTietSanPhamDto.setOldPrice(chiTietSanPham.getGia());
                chiTietSanPhamDto.setDiscountPercentage(campaign.getPhanTramGiam());
                chiTietSanPhamDto.setDiscountCampaignName(campaign.getTen());

                BigDecimal discount = chiTietSanPham.getGia()
                        .multiply(campaign.getPhanTramGiam())
                        .divide(BigDecimal.valueOf(100));
                replacementPriceAfterDiscount = chiTietSanPham.getGia().subtract(discount);
                chiTietSanPhamDto.setGia(replacementPriceAfterDiscount);
                chiTietSanPhamDto.setDiscount(formatter.format(campaign.getPhanTramGiam()) + "%");

                chiTietSanPham.setChiTietSanPhamDto(chiTietSanPhamDto);
            } else {
                ChiTietSanPhamDto chiTietSanPhamDto = new ChiTietSanPhamDto();
                chiTietSanPhamDto.setId(chiTietSanPham.getId());
                chiTietSanPhamDto.setGia(chiTietSanPham.getGia());
                chiTietSanPhamDto.setOldPrice(chiTietSanPham.getGia());
                chiTietSanPham.setChiTietSanPhamDto(chiTietSanPhamDto);
            }
        }

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("chiTietDonHangs", chiTietDonHangs);
        model.addAttribute("replacementProducts", replacementProducts);
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("keyword", keyword);
        model.addAttribute("user", nguoiDung);
        model.addAttribute("cartCount", 0);
        model.addAttribute("chiTietIds", chiTietIds != null ? chiTietIds : Collections.emptyList());
        return "WebKhachHang/tra-doi-san-pham";
    }

    @PostMapping("/api/orders/exchange/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> processExchange(
            @PathVariable UUID id,
            @RequestParam List<UUID> selectedProducts,
            @RequestParam List<UUID> replacementProducts,
            @RequestParam List<Integer> soLuong,
            @RequestParam String reason,
            @RequestParam(required = false) String description,
            @RequestParam String email,
            @RequestParam BigDecimal priceDifference,
            HttpSession session,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        logger.info("Bắt đầu xử lý yêu cầu đổi hàng cho id: {}, user: {}", id, authentication.getName());
        logger.info("Dữ liệu nhận được: selectedProducts={}, replacementProducts={}, soLuong={}, reason={}, email={}, priceDifference={}",
                selectedProducts, replacementProducts, soLuong, reason, email, priceDifference);

        try {
            // Validation ban đầu giữ nguyên
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để tiếp tục.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                response.put("success", false);
                response.put("message", "Không thể xác định người dùng.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            NguoiDung user = nguoiDungService.findById(nguoiDungId);
            if (user == null) {
                response.put("success", false);
                response.put("message", "Người dùng không tồn tại.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<HoaDon> hoaDonOpt = hoaDonRepo.findById(id);
            if (hoaDonOpt.isEmpty()) {
                logger.error("Hóa đơn không tồn tại với id: {}", id);
                response.put("success", false);
                response.put("message", "Hóa đơn không tồn tại.");
                return ResponseEntity.badRequest().body(response);
            }

            HoaDon hoaDon = hoaDonOpt.get();
            if ("Đã đổi hàng".equals(hoaDon.getTrangThai()) ||
                    lichSuDoiSanPhamRepository.existsByHoaDonIdAndTrangThai(hoaDon.getId(), "Đã xác nhận")) {
                response.put("success", false);
                response.put("message", "Hóa đơn này đã đổi hàng. Không thể gửi yêu cầu đổi thêm.");
                return ResponseEntity.badRequest().body(response);
            }

            if (!hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDungId)) {
                logger.error("Người dùng {} không có quyền đổi hóa đơn {}", nguoiDungId, id);
                response.put("success", false);
                response.put("message", "Bạn không có quyền đổi đơn hàng này.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            List<String> allowedStatuses = Arrays.asList("Vận chuyển thành công", "Chờ xử lý đổi hàng", "Hoàn thành");
            if (!allowedStatuses.contains(hoaDon.getTrangThai())) {
                logger.error("Trạng thái hóa đơn {} không cho phép đổi: {}", id, hoaDon.getTrangThai());
                response.put("success", false);
                response.put("message", "Chỉ có thể đổi sản phẩm trong các trạng thái cho phép.");
                return ResponseEntity.badRequest().body(response);
            }

            // Validation sản phẩm được chọn và thay thế
            if (selectedProducts == null || selectedProducts.isEmpty()) {
                logger.error("Số lượng sản phẩm được chọn không hợp lệ: {}", selectedProducts);
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một sản phẩm để đổi.");
                return ResponseEntity.badRequest().body(response);
            }

            if (replacementProducts == null || replacementProducts.isEmpty() || soLuong == null || soLuong.isEmpty() || replacementProducts.size() != soLuong.size()) {
                logger.error("Số lượng hoặc sản phẩm thay thế không hợp lệ: replacementProducts={}, soLuong={}", replacementProducts, soLuong);
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một sản phẩm thay thế với số lượng hợp lệ.");
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy danh sách chi tiết đơn hàng
            List<ChiTietDonHang> chiTietDonHangs = chiTietDonHangRepository.findAllById(selectedProducts);
            if (chiTietDonHangs.size() != selectedProducts.size()) {
                response.put("success", false);
                response.put("message", "Một số sản phẩm được chọn không tồn tại.");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra tổng số lượng
            int totalSelectedQuantity = chiTietDonHangs.stream().mapToInt(ChiTietDonHang::getSoLuong).sum();
            int totalReplacementQuantity = soLuong.stream().mapToInt(Integer::intValue).sum();
            if (totalReplacementQuantity != totalSelectedQuantity) {
                logger.error("Tổng số lượng đổi {} không khớp với tổng số lượng đã chọn {}", totalReplacementQuantity, totalSelectedQuantity);
                response.put("success", false);
                response.put("message", "Tổng số lượng đổi phải bằng tổng số lượng đã chọn.");
                return ResponseEntity.badRequest().body(response);
            }

            String lyDoDay = reason + (description != null && !description.trim().isEmpty() ? ": " + description : "");
            BigDecimal tongTienGoc = BigDecimal.ZERO;
            BigDecimal tongTienThayThe = BigDecimal.ZERO;

            // Tính tổng tiền sản phẩm gốc - sử dụng giá thực tế khách hàng đã trả
            for (ChiTietDonHang chiTiet : chiTietDonHangs) {
                ChiTietSanPham chiTietSanPhamGoc = chiTiet.getChiTietSanPham();
                BigDecimal giaGocHienTai = chiTietSanPhamGoc.getGia(); // Giá gốc hiện tại

                // Kiểm tra và áp dụng giảm giá hiện tại cho sản phẩm gốc
                Optional<ChienDichGiamGia> activeCampaignGoc = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPhamGoc.getId());
                if (activeCampaignGoc.isPresent()) {
                    ChienDichGiamGia campaign = activeCampaignGoc.get();
                    BigDecimal discount = giaGocHienTai.multiply(campaign.getPhanTramGiam()).divide(BigDecimal.valueOf(100));
                    giaGocHienTai = giaGocHienTai.subtract(discount);
                    logger.info("Áp dụng giảm giá {}% cho sản phẩm gốc {}: {} -> {}",
                            campaign.getPhanTramGiam(), chiTietSanPhamGoc.getSanPham().getTenSanPham(),
                            chiTietSanPhamGoc.getGia(), giaGocHienTai);
                }

                tongTienGoc = tongTienGoc.add(giaGocHienTai.multiply(BigDecimal.valueOf(chiTiet.getSoLuong())));
            }

            // Lấy và validate sản phẩm thay thế
            List<ChiTietSanPham> sanPhamThayTheList = new ArrayList<>();
            for (int i = 0; i < replacementProducts.size(); i++) {
                UUID replacementProductId = replacementProducts.get(i);
                int currentSoLuong = soLuong.get(i);

                Optional<ChiTietSanPham> sanPhamThayTheOpt = chiTietSanPhamRepository.findById(replacementProductId);
                if (sanPhamThayTheOpt.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Sản phẩm thay thế không tồn tại với id: " + replacementProductId);
                    return ResponseEntity.badRequest().body(response);
                }

                ChiTietSanPham chiTietSanPhamThayThe = sanPhamThayTheOpt.get();
                if (chiTietSanPhamThayThe.getSoLuongTonKho() < currentSoLuong) {
                    response.put("success", false);
                    response.put("message", "Số lượng tồn kho không đủ cho sản phẩm: " + chiTietSanPhamThayThe.getSanPham().getTenSanPham());
                    return ResponseEntity.badRequest().body(response);
                }

                sanPhamThayTheList.add(chiTietSanPhamThayThe);

                // Tính giá sản phẩm thay thế (áp dụng giảm giá nếu có)
                BigDecimal giaThayThe = chiTietSanPhamThayThe.getGia();
                Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPhamThayThe.getId());
                if (activeCampaign.isPresent()) {
                    ChienDichGiamGia campaign = activeCampaign.get();
                    BigDecimal discount = giaThayThe.multiply(campaign.getPhanTramGiam()).divide(BigDecimal.valueOf(100));
                    giaThayThe = giaThayThe.subtract(discount);
                    logger.info("Áp dụng giảm giá {}% cho sản phẩm thay thế {}: {} -> {}",
                            campaign.getPhanTramGiam(), chiTietSanPhamThayThe.getSanPham().getTenSanPham(),
                            chiTietSanPhamThayThe.getGia(), giaThayThe);
                }

                tongTienThayThe = tongTienThayThe.add(giaThayThe.multiply(BigDecimal.valueOf(currentSoLuong)));
            }

            // Tính chênh lệch giá
            BigDecimal chenhLechGiaThucTe = tongTienThayThe.subtract(tongTienGoc);
            logger.info("Tính toán giá: Tổng tiền gốc = {}, Tổng tiền thay thế = {}, Chênh lệch = {}",
                    tongTienGoc, tongTienThayThe, chenhLechGiaThucTe);

            // Kiểm tra chênh lệch giá
            if (priceDifference != null) {
                BigDecimal tolerance = tongTienGoc.multiply(BigDecimal.valueOf(0.1)); // 10% tolerance
                BigDecimal difference = chenhLechGiaThucTe.subtract(priceDifference).abs();

                if (difference.compareTo(tolerance) > 0) {
                    logger.warn("Chênh lệch giá có sự khác biệt lớn: Đã nhập = {}, Tính toán = {}, Chênh lệch = {}",
                            priceDifference, chenhLechGiaThucTe, difference);
                }
            }

            // Cập nhật trạng thái chi tiết đơn hàng
            for (ChiTietDonHang chiTietDonHang : chiTietDonHangs) {
                chiTietDonHang.setTrangThaiDoiSanPham(true);
                chiTietDonHang.setLyDoDoiHang(lyDoDay);
                chiTietDonHangRepository.save(chiTietDonHang);
            }

            // Cập nhật trạng thái hóa đơn
            hoaDon.setTrangThai("Chờ xử lý đổi hàng");
            HoaDon savedHoaDon = hoaDonRepo.save(hoaDon);
            logger.info("Cập nhật trạng thái hóa đơn {} thành 'Chờ xử lý đổi hàng'", hoaDon.getId());

            // Tạo lịch sử hóa đơn
            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai("Chờ xử lý đổi hàng");
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Khách hàng yêu cầu đổi sản phẩm: " + lyDoDay +
                    (chenhLechGiaThucTe.compareTo(BigDecimal.ZERO) > 0 ? ", Chênh lệch: " + formatVND(chenhLechGiaThucTe.doubleValue()) : ""));
            lichSuHoaDonRepository.save(lichSuHoaDon);

            // Tạo bản ghi lịch sử đổi sản phẩm
            int totalAssigned = 0;
            for (ChiTietDonHang chiTietDonHangGoc : chiTietDonHangs) {
                int soLuongGoc = chiTietDonHangGoc.getSoLuong();
                int soLuongConLai = soLuongGoc;

                // Tính giá gốc hiện tại (có áp dụng giảm giá nếu có)
                ChiTietSanPham chiTietSanPhamGoc = chiTietDonHangGoc.getChiTietSanPham();
                BigDecimal giaGocHienTai = chiTietSanPhamGoc.getGia();
                Optional<ChienDichGiamGia> activeCampaignGoc = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPhamGoc.getId());
                if (activeCampaignGoc.isPresent()) {
                    ChienDichGiamGia campaign = activeCampaignGoc.get();
                    BigDecimal discount = giaGocHienTai.multiply(campaign.getPhanTramGiam()).divide(BigDecimal.valueOf(100));
                    giaGocHienTai = giaGocHienTai.subtract(discount);
                }

                while (soLuongConLai > 0 && totalAssigned < soLuong.size()) {
                    int soLuongDoi = Math.min(soLuongConLai, soLuong.get(totalAssigned));
                    ChiTietSanPham chiTietSanPhamThayThe = sanPhamThayTheList.get(totalAssigned);

                    // SỬA ĐỔI: Sử dụng giá hiện tại sau giảm giá thay vì giá cũ trong đơn hàng
                    BigDecimal giaGoc = giaGocHienTai; // Đã được tính toán ở trên
                    BigDecimal giaThayThe = chiTietSanPhamThayThe.getGia();

                    // Áp dụng giảm giá cho sản phẩm thay thế
                    Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPhamThayThe.getId());
                    if (activeCampaign.isPresent()) {
                        ChienDichGiamGia campaign = activeCampaign.get();
                        BigDecimal discount = giaThayThe.multiply(campaign.getPhanTramGiam()).divide(BigDecimal.valueOf(100));
                        giaThayThe = giaThayThe.subtract(discount);
                    }

                    BigDecimal tongTienHoan = giaGoc.multiply(BigDecimal.valueOf(soLuongDoi));
                    BigDecimal tongTienMoi = giaThayThe.multiply(BigDecimal.valueOf(soLuongDoi));
                    BigDecimal chenhLechCap = tongTienMoi.subtract(tongTienHoan);

                    LichSuDoiSanPham lichSuDoiSanPham = new LichSuDoiSanPham();
                    lichSuDoiSanPham.setChiTietDonHang(chiTietDonHangGoc);
                    lichSuDoiSanPham.setHoaDon(hoaDon);
                    lichSuDoiSanPham.setChiTietSanPhamThayThe(chiTietSanPhamThayThe);
                    lichSuDoiSanPham.setSoLuong(soLuongDoi);
                    lichSuDoiSanPham.setTongTienHoan(tongTienHoan);
                    lichSuDoiSanPham.setLyDoDoiHang(lyDoDay);
                    lichSuDoiSanPham.setThoiGianDoi(LocalDateTime.now());
                    lichSuDoiSanPham.setTrangThai("Chờ xử lý");
                    lichSuDoiSanPham.setChenhLechGia(chenhLechCap);

                    logger.info("Lưu lịch sử đổi - Gốc={} (giá hiện tại: {}) -> Thay thế={} (giá sau giảm: {}), Số lượng={}, Tiền hoàn={}, Tiền mới={}, Chênh lệch={}",
                            chiTietDonHangGoc.getTenSanPham(), giaGoc, chiTietSanPhamThayThe.getSanPham().getTenSanPham(),
                            giaThayThe, soLuongDoi, tongTienHoan, tongTienMoi, chenhLechCap);

                    lichSuDoiSanPhamRepository.save(lichSuDoiSanPham);
                    soLuongConLai -= soLuongDoi;
                    totalAssigned++;
                }

                if (soLuongConLai > 0) {
                    response.put("success", false);
                    response.put("message", "Số lượng sản phẩm thay thế không đủ để đổi cho sản phẩm gốc.");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Gửi thông báo và email
            try {
                String thongBaoContent = "Khách hàng " + user.getHoTen() + " yêu cầu đổi sản phẩm cho đơn hàng " + hoaDon.getDonHang().getMaDonHang();
                thongBaoService.taoThongBaoHeThong("admin", "Yêu cầu đổi sản phẩm mới", thongBaoContent, hoaDon.getDonHang());
            } catch (Exception e) {
                logger.warn("Không thể tạo thông báo: {}", e.getMessage());
            }

            try {
                String emailContent = "<h2>Xác nhận yêu cầu đổi sản phẩm</h2>" +
                        "<p>Xin chào " + user.getHoTen() + ",</p>" +
                        "<p>Yêu cầu đổi sản phẩm của bạn cho đơn hàng mã <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã được gửi thành công và đang chờ xử lý.</p>" +
                        "<p><strong>Sản phẩm đổi:</strong> " + chiTietDonHangs.stream().map(ChiTietDonHang::getTenSanPham).collect(Collectors.joining(", ")) + "</p>" +
                        "<p><strong>Số lượng:</strong> " + totalSelectedQuantity + "</p>" +
                        "<p><strong>Lý do:</strong> " + lyDoDay + "</p>" +
                        (chenhLechGiaThucTe.compareTo(BigDecimal.ZERO) > 0 ?
                                "<p><strong>Chênh lệch giá cần thanh toán:</strong> " + formatVND(chenhLechGiaThucTe.doubleValue()) + "</p>" +
                                        "<p>Vui lòng liên hệ đội ngũ hỗ trợ để hoàn tất thanh toán chênh lệch.</p>" : "") +
                        "<p>Chúng tôi sẽ xử lý yêu cầu trong thời gian sớm nhất.</p>" +
                        "<p>Trân trọng,<br>Đội ngũ ACV Store</p>";
                emailService.sendEmail(email, "Xác nhận yêu cầu đổi sản phẩm - ACV Store", emailContent);
            } catch (Exception e) {
                logger.warn("Không thể gửi email: {}", e.getMessage());
            }

            response.put("success", true);
            response.put("message", "Yêu cầu đổi sản phẩm đã được gửi thành công và đang chờ xử lý.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi xử lý yêu cầu đổi sản phẩm cho id {}: {}", id, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xử lý yêu cầu. Vui lòng thử lại sau. Chi tiết: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/api/replacement-products/{id}")
    public ResponseEntity<Map<String, Object>> getReplacementProducts(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) List<UUID> chiTietIds,
            @RequestParam(required = false) UUID currentProductId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        DecimalFormat formatter = new DecimalFormat("#,###");

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để tiếp tục.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            UUID nguoiDungId = getNguoiDungIdFromAuthentication(authentication);
            if (nguoiDungId == null) {
                response.put("success", false);
                response.put("message", "Không thể xác định người dùng.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<HoaDon> hoaDonOpt = hoaDonRepo.findById(id);
            if (hoaDonOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Hóa đơn không tồn tại.");
                return ResponseEntity.badRequest().body(response);
            }

            HoaDon hoaDon = hoaDonOpt.get();
            if (!hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDungId)) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền truy cập hóa đơn này.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // THAY ĐỔI CHÍNH: Sử dụng giá gốc thay vì giá sau giảm giá để tính minPrice
            BigDecimal minPriceOriginal = BigDecimal.ZERO; // Sử dụng giá gốc thấp nhất

            if (currentProductId != null) {
                Optional<ChiTietDonHang> currentProduct = chiTietDonHangRepository.findById(currentProductId);
                if (currentProduct.isPresent() &&
                        currentProduct.get().getDonHang().getId().equals(hoaDon.getDonHang().getId())) {
                    ChiTietDonHang chiTiet = currentProduct.get();
                    // Sử dụng giá gốc từ ChiTietSanPham thay vì giá đã giảm
                    minPriceOriginal = chiTiet.getChiTietSanPham().getGia();
                } else {
                    response.put("success", false);
                    response.put("message", "Sản phẩm hiện tại không hợp lệ.");
                    return ResponseEntity.badRequest().body(response);
                }
            } else if (chiTietIds != null && !chiTietIds.isEmpty()) {
                List<ChiTietDonHang> chiTietDonHangs = chiTietDonHangRepository.findAllById(chiTietIds).stream()
                        .filter(chiTiet -> chiTiet.getDonHang().getId().equals(hoaDon.getDonHang().getId()))
                        .collect(Collectors.toList());
                // Sử dụng giá gốc thấp nhất
                minPriceOriginal = chiTietDonHangs.stream()
                        .map(chiTiet -> chiTiet.getChiTietSanPham().getGia())
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
            } else {
                minPriceOriginal = chiTietDonHangRepository.findByDonHangId(hoaDon.getDonHang().getId())
                        .stream()
                        .map(chiTiet -> chiTiet.getChiTietSanPham().getGia())
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
            }

            // THAY ĐỔI: Giảm 50% giá tối thiểu để tăng tính linh hoạt
            BigDecimal adjustedMinPrice = minPriceOriginal.multiply(BigDecimal.valueOf(0.5));

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "gia"));
            Page<ChiTietSanPham> productPage;

            if (keyword.trim().isEmpty()) {
                productPage = chiTietSanPhamRepository.findBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqual(
                        0, adjustedMinPrice, pageable);
            } else {
                productPage = chiTietSanPhamRepository.findBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqualAndSanPham_TenSanPhamContainingIgnoreCase(
                        0, adjustedMinPrice, keyword, pageable);
            }

            List<Map<String, Object>> products = productPage.getContent().stream().map(product -> {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("id", product.getId().toString());
                productMap.put("tenSanPham", product.getSanPham().getTenSanPham());
                productMap.put("mauSac", product.getMauSac() != null ? product.getMauSac().getTenMau() : "Không xác định");
                productMap.put("kichCo", product.getKichCo() != null ? product.getKichCo().getTen() : "Không xác định");
                productMap.put("imageUrl", product.getHinhAnhSanPhams() != null && !product.getHinhAnhSanPhams().isEmpty()
                        ? product.getHinhAnhSanPhams().get(0).getUrlHinhAnh()
                        : "/images/default-product.jpg");
                productMap.put("soLuongTonKho", product.getSoLuongTonKho());

                // Tạo và gán ChiTietSanPhamDto mặc định
                ChiTietSanPhamDto chiTietSanPhamDto = new ChiTietSanPhamDto();
                chiTietSanPhamDto.setId(product.getId());
                BigDecimal giaGoc = product.getGia();
                chiTietSanPhamDto.setGia(giaGoc);
                chiTietSanPhamDto.setOldPrice(giaGoc);

                // Kiểm tra và áp dụng chiến dịch giảm giá
                Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(product.getId());
                if (activeCampaign.isPresent()) {
                    ChienDichGiamGia campaign = activeCampaign.get();
                    BigDecimal discountAmount = giaGoc.multiply(campaign.getPhanTramGiam()).divide(BigDecimal.valueOf(100));
                    chiTietSanPhamDto.setGia(giaGoc.subtract(discountAmount));
                    chiTietSanPhamDto.setDiscount(formatter.format(campaign.getPhanTramGiam()) + "%");
                    chiTietSanPhamDto.setDiscountCampaignName(campaign.getTen());
                    chiTietSanPhamDto.setDiscountPercentage(campaign.getPhanTramGiam());
                }

                // Sử dụng giá sau khi đã áp dụng giảm giá (nếu có)
                productMap.put("giaValue", chiTietSanPhamDto.getGia());
                productMap.put("gia", new DecimalFormat("#,### VNĐ").format(chiTietSanPhamDto.getGia()));

                if (chiTietSanPhamDto.getOldPrice() != null && activeCampaign.isPresent()) {
                    productMap.put("oldPrice", new DecimalFormat("#,### VNĐ").format(chiTietSanPhamDto.getOldPrice()));
                }
                if (chiTietSanPhamDto.getDiscount() != null) {
                    productMap.put("discount", chiTietSanPhamDto.getDiscount());
                }
                if (chiTietSanPhamDto.getDiscountCampaignName() != null) {
                    productMap.put("campaignName", chiTietSanPhamDto.getDiscountCampaignName());
                }

                return productMap;
            }).collect(Collectors.toList());

            response.put("success", true);
            response.put("products", products);
            response.put("currentPage", productPage.getNumber());
            response.put("totalPages", productPage.getTotalPages());
            response.put("minPrice", adjustedMinPrice);
            response.put("originalMinPrice", minPriceOriginal); // Thêm thông tin để debug

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách sản phẩm thay thế cho hóa đơn {}: {}", id, e.getMessage());
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}