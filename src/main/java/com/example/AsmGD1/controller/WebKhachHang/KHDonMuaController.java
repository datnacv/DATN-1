package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
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
    public String chiTietDonHang(@PathVariable("id") UUID id, Model model, Authentication authentication) {
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
        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
            return "redirect:/dsdon-mua";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

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
        }

        String currentStatus = hoaDonService.getCurrentStatus(hoaDon);
        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("user", nguoiDung);
        model.addAttribute("currentStatus", currentStatus);
        model.addAttribute("statusHistory", hoaDon.getLichSuHoaDons() != null ? hoaDon.getLichSuHoaDons() : new ArrayList<>());

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

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("user", nguoiDung);
        return "WebKhachHang/tra-hang";
    }

    @PostMapping("/api/orders/return/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> submitReturnRequest(
            @PathVariable("id") UUID id,
            @RequestParam("selectedProducts") List<UUID> selectedProductIds,
            @RequestParam("reason") String reason,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("email") String email,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để gửi yêu cầu trả hàng.");
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

        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);
        if (hoaDon == null || !hoaDon.getNguoiDung().getId().equals(nguoiDung.getId())) {
            response.put("success", false);
            response.put("message", "Hóa đơn không hợp lệ hoặc không thuộc về bạn.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!"Vận chuyển thành công".equals(hoaDon.getTrangThai())) {
            response.put("success", false);
            response.put("message", "Chỉ có thể gửi yêu cầu trả hàng khi đơn hàng ở trạng thái 'Vận chuyển thành công'.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (selectedProductIds == null || selectedProductIds.isEmpty()) {
            response.put("success", false);
            response.put("message", "Vui lòng chọn ít nhất một sản phẩm để trả hàng.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (selectedProductIds.size() != hoaDon.getDonHang().getChiTietDonHangs().size()) {
            response.put("success", false);
            response.put("message", "Phải trả toàn bộ sản phẩm trong đơn hàng.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            double totalReturnAmount = 0;
            for (UUID chiTietId : selectedProductIds) {
                ChiTietDonHang chiTiet = hoaDon.getDonHang().getChiTietDonHangs().stream()
                        .filter(ct -> ct.getId().equals(chiTietId))
                        .findFirst().orElse(null);
                if (chiTiet == null || chiTiet.getTrangThaiHoanTra()) {
                    response.put("success", false);
                    response.put("message", "Sản phẩm đã được trả hoặc không hợp lệ.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                // Tính giá sau giảm nếu có
                BigDecimal finalPrice = chiTiet.getChiTietSanPhamDto() != null
                        ? chiTiet.getChiTietSanPhamDto().getGia()
                        : chiTiet.getGia();

                BigDecimal amount = finalPrice.multiply(BigDecimal.valueOf(chiTiet.getSoLuong()));
                totalReturnAmount += amount.doubleValue();

                // Tạo bản ghi lịch sử trả hàng
                LichSuTraHang traHang = new LichSuTraHang();
                traHang.setChiTietDonHang(chiTiet);
                traHang.setHoaDon(hoaDon);
                traHang.setSoLuong(chiTiet.getSoLuong());
                traHang.setTongTienHoan(amount);
                traHang.setLyDoTraHang(description != null ? reason + ": " + description : reason);
                traHang.setThoiGianTra(LocalDateTime.now());
                traHang.setTrangThai("Chờ xử lý");

                // Lưu bản ghi trả hàng
                lichSuTraHangRepository.save(traHang);

                // Cập nhật trạng thái hoàn trả của chi tiết đơn hàng
                chiTiet.setTrangThaiHoanTra(true);
                chiTiet.setLyDoTraHang(description != null ? reason + ": " + description : reason);
                chiTietDonHangRepository.save(chiTiet);

                // Cập nhật số lượng tồn kho
                ChiTietSanPham sanPham = chiTiet.getChiTietSanPham();
                sanPham.setSoLuongTonKho(sanPham.getSoLuongTonKho() + chiTiet.getSoLuong());
                chiTietSanPhamRepository.save(sanPham);
            }



            hoaDon.setTrangThai("Đã trả hàng");
            hoaDonService.addLichSuHoaDon(hoaDon, "Đã trả hàng", "Khách hàng yêu cầu trả hàng: " + reason);
            hoaDonService.save(hoaDon);

            // Gửi thông báo hệ thống
            thongBaoService.taoThongBaoHeThong(
                    "admin",
                    "Yêu cầu trả hàng",
                    "Khách hàng yêu cầu trả hàng cho đơn hàng mã " + hoaDon.getDonHang().getMaDonHang() + ". Lý do: " + reason,
                    hoaDon.getDonHang()
            );

            // Gửi email thông báo
            String emailContent = "<h2>Thông báo yêu cầu trả hàng</h2>" +
                    "<p>Xin chào " + nguoiDung.getHoTen() + ",</p>" +
                    "<p>Yêu cầu trả hàng của bạn cho đơn hàng mã <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã được gửi thành công.</p>" +
                    "<p><strong>Trạng thái:</strong> Đã trả hàng</p>" +
                    "<p><strong>Lý do:</strong> " + reason + "</p>" +
                    "<p><strong>Mô tả:</strong> " + (description != null ? description : "Không có") + "</p>" +
                    "<p><strong>Tổng tiền hoàn:</strong> " + formatVND(totalReturnAmount) + "</p>" +
                    "<p>Chúng tôi sẽ xử lý yêu cầu của bạn trong thời gian sớm nhất.</p>" +
                    "<p>Trân trọng,<br>Đội ngũ ACV Store</p>";
            emailService.sendEmail(email, "Yêu cầu trả hàng - ACV Store", emailContent);

            response.put("success", true);
            response.put("message", "Yêu cầu trả hàng đã được gửi thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi gửi yêu cầu trả hàng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String formatVND(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(value) + " VNĐ";
    }

    @GetMapping("/tra-doi-san-pham/{id}")
    public String showExchangeForm(@PathVariable UUID id, Model model, Authentication authentication) {
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
        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId()) || !"Vận chuyển thành công".equals(hoaDon.getTrangThai())) {
            model.addAttribute("hoaDon", null);
            return "WebKhachHang/tra-doi-san-pham";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        // Xử lý danh sách sản phẩm trong đơn hàng
        List<ChiTietDonHang> chiTietDonHangs = chiTietDonHangRepository.findByDonHangId(hoaDon.getDonHang().getId());
        for (ChiTietDonHang chiTiet : chiTietDonHangs) {
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
        }

        // Xử lý danh sách sản phẩm thay thế
        // Lấy giá tối thiểu sau giảm từ các sản phẩm trong đơn hàng
        BigDecimal minPriceAfterDiscount = chiTietDonHangs.stream()
                .map(chiTiet -> chiTiet.getChiTietSanPhamDto() != null ? chiTiet.getChiTietSanPhamDto().getGia() : chiTiet.getGia())
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Lấy tất cả sản phẩm từ kho để tính giá sau giảm
        List<ChiTietSanPham> allProducts = chiTietSanPhamRepository.findAll();
        List<ChiTietSanPham> replacementProducts = new ArrayList<>();

        for (ChiTietSanPham chiTietSanPham : allProducts) {
            Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPham.getId());
            BigDecimal replacementPriceAfterDiscount = chiTietSanPham.getGia(); // Giá mặc định là giá gốc

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
                replacementPriceAfterDiscount = chiTietSanPham.getGia().subtract(discount);
                chiTietSanPhamDto.setGia(replacementPriceAfterDiscount);
                chiTietSanPhamDto.setDiscount(formatter.format(campaign.getPhanTramGiam()) + "%");

                // Gán DTO vào chiTietSanPham
                chiTietSanPham.setChiTietSanPhamDto(chiTietSanPhamDto);
            } else {
                // Nếu không có giảm giá, giữ nguyên giá gốc
                ChiTietSanPhamDto chiTietSanPhamDto = new ChiTietSanPhamDto();
                chiTietSanPhamDto.setId(chiTietSanPham.getId());
                chiTietSanPhamDto.setGia(chiTietSanPham.getGia());
                chiTietSanPhamDto.setOldPrice(chiTietSanPham.getGia());
                chiTietSanPham.setChiTietSanPhamDto(chiTietSanPhamDto);
            }

            // Chỉ thêm sản phẩm thay thế có giá sau giảm lớn hơn hoặc bằng minPriceAfterDiscount và tồn kho > 0
            if (chiTietSanPham.getChiTietSanPhamDto().getGia().compareTo(minPriceAfterDiscount) >= 0 && chiTietSanPham.getSoLuongTonKho() > 0) {
                replacementProducts.add(chiTietSanPham);
            }
        }

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("replacementProducts", replacementProducts);
        model.addAttribute("user", nguoiDung);
        model.addAttribute("keyword", "");
        model.addAttribute("cartCount", 0);
        return "WebKhachHang/tra-doi-san-pham";
    }

    @PostMapping("/api/orders/exchange/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> processExchange(
            @PathVariable UUID id,
            @RequestParam List<UUID> selectedProducts,
            @RequestParam List<UUID> replacementProducts,
            @RequestParam int soLuong,
            @RequestParam String reason,
            @RequestParam(required = false) String description,
            @RequestParam String email,
            @RequestParam BigDecimal priceDifference,
            HttpSession session,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

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
                response.put("success", false);
                response.put("message", "Hóa đơn không tồn tại.");
                return ResponseEntity.badRequest().body(response);
            }

            HoaDon hoaDon = hoaDonOpt.get();
            if (!hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDungId)) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền đổi đơn hàng này.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            if (!"Vận chuyển thành công".equals(hoaDon.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Chỉ có thể đổi sản phẩm khi hóa đơn ở trạng thái 'Vận chuyển thành công'.");
                return ResponseEntity.badRequest().body(response);
            }

            if (selectedProducts == null || selectedProducts.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một sản phẩm để đổi.");
                return ResponseEntity.badRequest().body(response);
            }

            if (replacementProducts == null || replacementProducts.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất một sản phẩm thay thế.");
                return ResponseEntity.badRequest().body(response);
            }

            if (selectedProducts.size() != replacementProducts.size()) {
                response.put("success", false);
                response.put("message", "Số lượng sản phẩm đổi và sản phẩm thay thế phải bằng nhau.");
                return ResponseEntity.badRequest().body(response);
            }

            if (soLuong <= 0) {
                response.put("success", false);
                response.put("message", "Số lượng phải lớn hơn 0.");
                return ResponseEntity.badRequest().body(response);
            }

            // Tính toán chênh lệch giá
            BigDecimal totalOriginalPrice = BigDecimal.ZERO;
            BigDecimal totalReplacementPrice = BigDecimal.ZERO;

            for (int i = 0; i < selectedProducts.size(); i++) {
                UUID selectedProductId = selectedProducts.get(i);
                UUID replacementProductId = replacementProducts.get(i);

                Optional<ChiTietDonHang> chiTietOpt = chiTietDonHangRepository.findById(selectedProductId);
                if (chiTietOpt.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Không tìm thấy sản phẩm trong đơn hàng.");
                    return ResponseEntity.badRequest().body(response);
                }

                ChiTietDonHang chiTietDonHang = chiTietOpt.get();
                if (!chiTietDonHang.getDonHang().getId().equals(hoaDon.getDonHang().getId())) {
                    response.put("success", false);
                    response.put("message", "Sản phẩm không thuộc hóa đơn này.");
                    return ResponseEntity.badRequest().body(response);
                }

                if (Boolean.TRUE.equals(chiTietDonHang.getTrangThaiHoanTra()) ||
                        Boolean.TRUE.equals(chiTietDonHang.getTrangThaiDoiSanPham())) {
                    response.put("success", false);
                    response.put("message", "Sản phẩm đã được trả hoặc đổi trước đó.");
                    return ResponseEntity.badRequest().body(response);
                }

                if (soLuong > chiTietDonHang.getSoLuong()) {
                    response.put("success", false);
                    response.put("message", "Số lượng đổi không được vượt quá số lượng đã mua.");
                    return ResponseEntity.badRequest().body(response);
                }

                Optional<ChiTietSanPham> sanPhamThayTheOpt = chiTietSanPhamRepository.findById(replacementProductId);
                if (sanPhamThayTheOpt.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Sản phẩm thay thế không tồn tại.");
                    return ResponseEntity.badRequest().body(response);
                }

                ChiTietSanPham chiTietSanPhamThayThe = sanPhamThayTheOpt.get();
                if (chiTietSanPhamThayThe.getSoLuongTonKho() < soLuong) {
                    response.put("success", false);
                    response.put("message", "Sản phẩm thay thế không đủ tồn kho.");
                    return ResponseEntity.badRequest().body(response);
                }

                // Lấy giá từ ChiTietSanPhamDto nếu có giảm giá
                BigDecimal originalPrice = chiTietDonHang.getChiTietSanPhamDto() != null
                        ? chiTietDonHang.getChiTietSanPhamDto().getGia()
                        : chiTietDonHang.getGia();
                BigDecimal replacementPrice = chiTietSanPhamThayThe.getChiTietSanPhamDto() != null
                        ? chiTietSanPhamThayThe.getChiTietSanPhamDto().getGia()
                        : chiTietSanPhamThayThe.getGia();

                totalOriginalPrice = totalOriginalPrice.add(originalPrice.multiply(BigDecimal.valueOf(soLuong)));
                totalReplacementPrice = totalReplacementPrice.add(replacementPrice.multiply(BigDecimal.valueOf(soLuong)));

                // Tạo lịch sử đổi sản phẩm
                BigDecimal tongTienHoan = originalPrice.multiply(BigDecimal.valueOf(soLuong));
                String lyDoDay = reason + (description != null && !description.trim().isEmpty() ? ": " + description : "");

                LichSuDoiSanPham lichSuDoiSanPham = new LichSuDoiSanPham();
                lichSuDoiSanPham.setChiTietDonHang(chiTietDonHang);
                lichSuDoiSanPham.setHoaDon(hoaDon);
                lichSuDoiSanPham.setChiTietSanPhamThayThe(chiTietSanPhamThayThe);
                lichSuDoiSanPham.setSoLuong(soLuong);
                lichSuDoiSanPham.setTongTienHoan(tongTienHoan);
                lichSuDoiSanPham.setLyDoDoiHang(lyDoDay);
                lichSuDoiSanPham.setThoiGianDoi(LocalDateTime.now());
                lichSuDoiSanPham.setTrangThai("Chờ xử lý");
                if (priceDifference.compareTo(BigDecimal.ZERO) > 0) {
                    lichSuDoiSanPham.setChenhLechGia(priceDifference);
                }
                lichSuDoiSanPhamRepository.save(lichSuDoiSanPham);

                // Cập nhật trạng thái chi tiết đơn hàng
                chiTietDonHang.setTrangThaiDoiSanPham(true);
                chiTietDonHang.setLyDoDoiHang(lyDoDay);
                chiTietDonHangRepository.save(chiTietDonHang);
            }

            // Cập nhật trạng thái hóa đơn
            hoaDon.setTrangThai("Chờ xử lý đổi hàng");
            logger.info("Cập nhật trạng thái hóa đơn {} thành 'Chờ xử lý đổi hàng'", hoaDon.getId());
            HoaDon savedHoaDon = hoaDonRepo.save(hoaDon);
            logger.info("Trạng thái hóa đơn sau khi lưu: {}", savedHoaDon.getTrangThai());

            // Thêm lịch sử hóa đơn
            String lyDoDay = reason + (description != null && !description.trim().isEmpty() ? ": " + description : "");
            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai("Chờ xử lý đổi hàng");
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Khách hàng yêu cầu đổi sản phẩm: " + lyDoDay +
                    (priceDifference.compareTo(BigDecimal.ZERO) > 0 ? ", Chênh lệch: " + formatVND(priceDifference.doubleValue()) : ""));
            lichSuHoaDonRepository.save(lichSuHoaDon);

            // Tạo thông báo cho admin
            try {
                String thongBaoContent = "Khách hàng " + user.getHoTen() + " yêu cầu đổi sản phẩm cho đơn hàng " + hoaDon.getDonHang().getMaDonHang();
                thongBaoService.taoThongBaoHeThong(
                        "admin",
                        "Yêu cầu đổi sản phẩm mới",
                        thongBaoContent,
                        hoaDon.getDonHang()
                );
            } catch (Exception e) {
                logger.warn("Không thể tạo thông báo: {}", e.getMessage());
            }

            // Gửi email xác nhận
            try {
                String emailContent = "<h2>Xác nhận yêu cầu đổi sản phẩm</h2>" +
                        "<p>Xin chào " + user.getHoTen() + ",</p>" +
                        "<p>Yêu cầu đổi sản phẩm của bạn cho đơn hàng mã <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã được gửi thành công và đang chờ xử lý.</p>" +
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
            logger.error("Lỗi khi xử lý yêu cầu đổi sản phẩm: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xử lý yêu cầu. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}