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
    public ResponseEntity<?> confirmReceivedOrder(@PathVariable("id") UUID id, Authentication authentication) {
        System.out.println("Received confirmReceivedOrder request for id: " + id);
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để xác nhận.");
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng hoặc bạn không có quyền xác nhận.");
        }

        if (!"Vận chuyển thành công".equals(hoaDon.getTrangThai())) {
            return ResponseEntity.badRequest().body("Chỉ có thể xác nhận khi đơn hàng ở trạng thái 'Vận chuyển thành công'.");
        }

        try {
            hoaDon.setTrangThai("Hoàn thành");
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            hoaDon.setGhiChu("Khách hàng xác nhận đã nhận hàng");

            DonHang donHang = hoaDon.getDonHang();
            donHang.setTrangThai("THANH_CONG");
            donHangRepository.save(donHang);

            hoaDonService.addLichSuHoaDon(hoaDon, "Hoàn thành", "Khách hàng xác nhận đã nhận hàng");
            HoaDon savedHoaDon = hoaDonService.save(hoaDon);

            thongBaoService.taoThongBaoHeThong(
                    "admin",
                    "Đơn hàng đã hoàn thành",
                    "Đơn hàng mã " + hoaDon.getDonHang().getMaDonHang() + " đã được khách hàng xác nhận hoàn thành.",
                    hoaDon.getDonHang()
            );

            String emailContent = "<h2>Thông báo hoàn thành đơn hàng</h2>" +
                    "<p>Xin chào " + nguoiDung.getHoTen() + ",</p>" +
                    "<p>Đơn hàng của bạn với mã <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã được xác nhận hoàn thành.</p>" +
                    "<p>Cảm ơn bạn đã mua sắm tại ACV Store! Chúng tôi mong được phục vụ bạn trong tương lai.</p>" +
                    "<p>Trân trọng,<br>Đội ngũ ACV Store</p>";
            emailService.sendEmail(nguoiDung.getEmail(), "Hoàn thành đơn hàng - ACV Store", emailContent);

            return ResponseEntity.ok(Map.of("message", "Đã xác nhận nhận hàng thành công và gửi thông báo cho admin."));
        } catch (Exception e) {
            System.err.println("Lỗi khi xác nhận đơn hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi xác nhận: " + e.getMessage());
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
            hoaDonService.processReturn(id, selectedProductIds, lyDoGop);

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
            @RequestParam(required = false) UUID chiTietId, // Thêm tham số chiTietId
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
        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId()) || !List.of("Vận chuyển thành công", "Chờ xử lý đổi hàng", "Đã đổi hàng", "Hoàn thành").contains(hoaDon.getTrangThai())) {
            model.addAttribute("hoaDon", null);
            return "WebKhachHang/tra-doi-san-pham";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        // Lấy chi tiết đơn hàng cụ thể nếu chiTietId được cung cấp
        List<ChiTietDonHang> chiTietDonHangs;
        if (chiTietId != null) {
            chiTietDonHangs = chiTietDonHangRepository.findById(chiTietId)
                    .filter(chiTiet -> chiTiet.getDonHang().getId().equals(hoaDon.getDonHang().getId()))
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else {
            chiTietDonHangs = chiTietDonHangRepository.findByDonHangId(hoaDon.getDonHang().getId());
        }

        // Kiểm tra nếu sản phẩm đã được đổi
        if (!chiTietDonHangs.isEmpty() && chiTietDonHangs.get(0).getTrangThaiDoiSanPham()) {
            model.addAttribute("hoaDon", null);
            model.addAttribute("message", "Sản phẩm này đã được yêu cầu đổi trước đó.");
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
        model.addAttribute("chiTietDonHangs", chiTietDonHangs); // Thêm danh sách chi tiết đơn hàng
        model.addAttribute("replacementProducts", replacementProducts);
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("keyword", keyword);
        model.addAttribute("user", nguoiDung);
        model.addAttribute("cartCount", 0);
        model.addAttribute("chiTietId", chiTietId); // Thêm chiTietId vào model
        return "WebKhachHang/tra-doi-san-pham";
    }

    @PostMapping("/api/orders/exchange/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> processExchange(
            @PathVariable UUID id,
            @RequestParam List<UUID> selectedProducts,
            @RequestParam List<UUID> replacementProducts,
            @RequestParam List<Integer> soLuong, // Thêm danh sách số lượng
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
            if (!hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDungId)) {
                logger.error("Người dùng {} không có quyền đổi hóa đơn {}", nguoiDungId, id);
                response.put("success", false);
                response.put("message", "Bạn không có quyền đổi đơn hàng này.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            List<String> allowedStatuses = Arrays.asList("Vận chuyển thành công", "Chờ xử lý đổi hàng", "Đã đổi hàng", "Hoàn thành");
            if (!allowedStatuses.contains(hoaDon.getTrangThai())) {
                logger.error("Trạng thái hóa đơn {} không cho phép đổi: {}", id, hoaDon.getTrangThai());
                response.put("success", false);
                response.put("message", "Chỉ có thể đổi sản phẩm trong các trạng thái cho phép.");
                return ResponseEntity.badRequest().body(response);
            }

            if (selectedProducts == null || selectedProducts.size() != 1) {
                logger.error("Số lượng sản phẩm được chọn không hợp lệ: {}", selectedProducts);
                response.put("success", false);
                response.put("message", "Vui lòng chọn đúng một sản phẩm để đổi.");
                return ResponseEntity.badRequest().body(response);
            }

            if (replacementProducts == null || replacementProducts.isEmpty()) {
                logger.error("Số lượng sản phẩm thay thế không hợp lệ: {}", replacementProducts);
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một sản phẩm thay thế.");
                return ResponseEntity.badRequest().body(response);
            }

            if (soLuong == null || soLuong.isEmpty() || soLuong.size() != replacementProducts.size()) {
                logger.error("Số lượng không khớp với sản phẩm thay thế: soLuong={}, replacementProducts={}", soLuong, replacementProducts);
                response.put("success", false);
                response.put("message", "Số lượng không hợp lệ.");
                return ResponseEntity.badRequest().body(response);
            }

            UUID selectedProductId = selectedProducts.get(0);
            Optional<ChiTietDonHang> chiTietOpt = chiTietDonHangRepository.findById(selectedProductId);
            if (chiTietOpt.isEmpty()) {
                logger.error("Không tìm thấy sản phẩm trong đơn hàng với id: {}", selectedProductId);
                response.put("success", false);
                response.put("message", "Không tìm thấy sản phẩm trong đơn hàng.");
                return ResponseEntity.badRequest().body(response);
            }

            ChiTietDonHang chiTietDonHang = chiTietOpt.get();
            if (!chiTietDonHang.getDonHang().getId().equals(hoaDon.getDonHang().getId())) {
                logger.error("Sản phẩm {} không thuộc hóa đơn {}", selectedProductId, id);
                response.put("success", false);
                response.put("message", "Sản phẩm không thuộc hóa đơn này.");
                return ResponseEntity.badRequest().body(response);
            }

            int totalSoLuong = soLuong.stream().mapToInt(Integer::intValue).sum();
            if (totalSoLuong > chiTietDonHang.getSoLuong()) {
                logger.error("Tổng số lượng đổi {} vượt quá số lượng đã mua {}", totalSoLuong, chiTietDonHang.getSoLuong());
                response.put("success", false);
                response.put("message", "Tổng số lượng đổi không được vượt quá số lượng đã mua.");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra tồn kho và tạo bản ghi cho từng sản phẩm thay thế
            String lyDoDay = reason + (description != null && !description.trim().isEmpty() ? ": " + description : "");
            chiTietDonHang.setTrangThaiDoiSanPham(true);
            chiTietDonHang.setLyDoDoiHang(lyDoDay);
            chiTietDonHangRepository.save(chiTietDonHang);

            hoaDon.setTrangThai("Chờ xử lý đổi hàng");
            HoaDon savedHoaDon = hoaDonRepo.save(hoaDon);
            logger.info("Cập nhật trạng thái hóa đơn {} thành 'Chờ xử lý đổi hàng'", hoaDon.getId());

            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai("Chờ xử lý đổi hàng");
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Khách hàng yêu cầu đổi sản phẩm: " + lyDoDay +
                    (priceDifference.compareTo(BigDecimal.ZERO) > 0 ? ", Chênh lệch: " + formatVND(priceDifference.doubleValue()) : ""));
            lichSuHoaDonRepository.save(lichSuHoaDon);

            // Tạo bản ghi LichSuDoiSanPham cho từng sản phẩm thay thế
            BigDecimal originalPrice = chiTietDonHang.getChiTietSanPhamDto() != null
                    ? chiTietDonHang.getChiTietSanPhamDto().getGia()
                    : chiTietDonHang.getGia();
            for (int i = 0; i < replacementProducts.size(); i++) {
                UUID replacementProductId = replacementProducts.get(i);
                int currentSoLuong = soLuong.get(i);

                Optional<ChiTietSanPham> sanPhamThayTheOpt = chiTietSanPhamRepository.findById(replacementProductId);
                if (sanPhamThayTheOpt.isEmpty()) {
                    logger.error("Sản phẩm thay thế không tồn tại với id: {}", replacementProductId);
                    continue; // Bỏ qua sản phẩm lỗi, không dừng toàn bộ quá trình
                }

                ChiTietSanPham chiTietSanPhamThayThe = sanPhamThayTheOpt.get();
                if (chiTietSanPhamThayThe.getSoLuongTonKho() < currentSoLuong) {
                    logger.error("Số lượng tồn kho {} không đủ cho số lượng yêu cầu {} của sản phẩm {}", chiTietSanPhamThayThe.getSoLuongTonKho(), currentSoLuong, replacementProductId);
                    continue; // Bỏ qua sản phẩm không đủ tồn kho
                }

                BigDecimal replacementPrice = chiTietSanPhamThayThe.getChiTietSanPhamDto() != null
                        ? chiTietSanPhamThayThe.getChiTietSanPhamDto().getGia()
                        : chiTietSanPhamThayThe.getGia();
                BigDecimal totalOriginalPrice = originalPrice.multiply(BigDecimal.valueOf(currentSoLuong));
                BigDecimal totalReplacementPrice = replacementPrice.multiply(BigDecimal.valueOf(currentSoLuong));
                BigDecimal currentPriceDifference = totalReplacementPrice.subtract(totalOriginalPrice);

                LichSuDoiSanPham lichSuDoiSanPham = new LichSuDoiSanPham();
                lichSuDoiSanPham.setChiTietDonHang(chiTietDonHang);
                lichSuDoiSanPham.setHoaDon(hoaDon);
                lichSuDoiSanPham.setChiTietSanPhamThayThe(chiTietSanPhamThayThe);
                lichSuDoiSanPham.setSoLuong(currentSoLuong);
                lichSuDoiSanPham.setTongTienHoan(totalOriginalPrice);
                lichSuDoiSanPham.setLyDoDoiHang(lyDoDay);
                lichSuDoiSanPham.setThoiGianDoi(LocalDateTime.now());
                lichSuDoiSanPham.setTrangThai("Chờ xử lý");
                if (currentPriceDifference.compareTo(BigDecimal.ZERO) > 0) {
                    lichSuDoiSanPham.setChenhLechGia(currentPriceDifference);
                }
                lichSuDoiSanPhamRepository.save(lichSuDoiSanPham);
            }

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
                        "<p><strong>Sản phẩm đổi:</strong> " + chiTietDonHang.getTenSanPham() + "</p>" +
                        "<p><strong>Số lượng:</strong> " + totalSoLuong + "</p>" +
                        "<p><strong>Lý do:</strong> " + lyDoDay + "</p>" +
                        (priceDifference.compareTo(BigDecimal.ZERO) > 0 ?
                                "<p><strong>Chênh lệch giá cần thanh toán:</strong> " + formatVND(priceDifference.doubleValue()) + "</p>" +
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

    // Trong KHDonMuaController.java

    @GetMapping("/api/replacement-products/{id}")
    public ResponseEntity<Map<String, Object>> getReplacementProducts(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) UUID chiTietId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        DecimalFormat formatter = new DecimalFormat("#,###"); // Khai báo formatter

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

            BigDecimal minPriceAfterDiscount;
            if (chiTietId != null) {
                Optional<ChiTietDonHang> chiTietOpt = chiTietDonHangRepository.findById(chiTietId);
                if (chiTietOpt.isEmpty() || !chiTietOpt.get().getDonHang().getId().equals(hoaDon.getDonHang().getId())) {
                    response.put("success", false);
                    response.put("message", "Sản phẩm không hợp lệ.");
                    return ResponseEntity.badRequest().body(response);
                }
                ChiTietDonHang chiTiet = chiTietOpt.get();
                minPriceAfterDiscount = chiTiet.getChiTietSanPhamDto() != null
                        ? chiTiet.getChiTietSanPhamDto().getGia()
                        : chiTiet.getGia();
            } else {
                minPriceAfterDiscount = chiTietDonHangRepository.findByDonHangId(hoaDon.getDonHang().getId())
                        .stream()
                        .map(chiTiet -> chiTiet.getChiTietSanPhamDto() != null ? chiTiet.getChiTietSanPhamDto().getGia() : chiTiet.getGia())
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "gia"));
            Page<ChiTietSanPham> productPage;
            if (keyword.trim().isEmpty()) {
                productPage = chiTietSanPhamRepository.findBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqual(0, minPriceAfterDiscount, pageable);
            } else {
                productPage = chiTietSanPhamRepository.findBySoLuongTonKhoGreaterThanAndGiaGreaterThanEqualAndSanPham_TenSanPhamContainingIgnoreCase(
                        0, minPriceAfterDiscount, keyword, pageable);
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
                BigDecimal giaGoc = product.getGia(); // Giá gốc từ sản phẩm
                chiTietSanPhamDto.setGia(giaGoc);
                chiTietSanPhamDto.setOldPrice(giaGoc); // Giá gốc mặc định

                // Kiểm tra và áp dụng chiến dịch giảm giá
                Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(product.getId());
                if (activeCampaign.isPresent()) {
                    ChienDichGiamGia campaign = activeCampaign.get();
                    BigDecimal discountAmount = giaGoc.multiply(campaign.getPhanTramGiam()).divide(BigDecimal.valueOf(100));
                    chiTietSanPhamDto.setGia(giaGoc.subtract(discountAmount));
                    chiTietSanPhamDto.setDiscount(formatter.format(campaign.getPhanTramGiam()) + "%");
                    chiTietSanPhamDto.setDiscountCampaignName(campaign.getTen());
                }

                productMap.put("giaValue", chiTietSanPhamDto.getGia());
                productMap.put("gia", new DecimalFormat("#,### VNĐ").format(chiTietSanPhamDto.getGia()));
                if (chiTietSanPhamDto.getOldPrice() != null) {
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
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}