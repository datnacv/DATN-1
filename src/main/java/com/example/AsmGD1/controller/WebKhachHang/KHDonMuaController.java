package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.ChiTietDonHang;
import com.example.AsmGD1.entity.DanhGia;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.WebKhachHang.DanhGiaRepository;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    private static final Logger logger = LoggerFactory.getLogger(KHDonMuaController.class);

    @Autowired
    private DanhGiaRepository danhGiaRepository;

    @Autowired
    private HoaDonRepository hoaDonRepo;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private HoaDonService hoaDonService;

    private final String UPLOAD_DIR;

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

    @GetMapping
    public String donMuaPage(@RequestParam(name = "status", defaultValue = "tat-ca") String status,
                             Model model,
                             Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
        model.addAttribute("user", nguoiDung);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        List<HoaDon> danhSachHoaDon;
        if ("tat-ca".equalsIgnoreCase(status)) {
            danhSachHoaDon = hoaDonRepo.findByDonHang_NguoiDungId(nguoiDung.getId());
        } else {
            String statusDb = switch (status) {
                case "cho-xac-nhan" -> "Chưa xác nhận";
                case "da-xac-nhan" -> "Đã xác nhận Online";
                case "dang-xu-ly-online" -> "Đang xử lý Online";
                case "dang-van-chuyen" -> "Đang vận chuyển";
                case "van-chuyen-thanh-cong" -> "Vận chuyển thành công";
                case "hoan-thanh" -> "Hoàn thành";
                case "da-huy" -> "Hủy đơn hàng";
                default -> "";
            };
            danhSachHoaDon = hoaDonRepo.findByDonHang_NguoiDungIdAndTrangThai(nguoiDung.getId(), statusDb);
        }

        for (HoaDon hoaDon : danhSachHoaDon) {
            hoaDon.setFormattedTongTien(hoaDon.getTongTien() != null ? formatter.format(hoaDon.getTongTien()) : "0");

            for (ChiTietDonHang chiTiet : hoaDon.getDonHang().getChiTietDonHangs()) {
                chiTiet.setFormattedGia(chiTiet.getGia() != null ? formatter.format(chiTiet.getGia()) : "0");
            }

            // Kiểm tra đã đánh giá chưa
            boolean daDanhGia = danhGiaRepository.existsByHoaDonIdAndNguoiDungId(
                    hoaDon.getId(), nguoiDung.getId()
            );
            hoaDon.setDaDanhGia(daDanhGia); // cần thêm field này trong Entity hoặc tạo DTO riêng
        }

        model.addAttribute("danhSachHoaDon", danhSachHoaDon);
        model.addAttribute("status", status);
        return "WebKhachHang/don-mua";
    }

    @GetMapping("/chi-tiet/{id}")
    public String chiTietDonHang(@PathVariable("id") UUID id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
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
        }

        String currentStatus = hoaDonService.getCurrentStatus(hoaDon);
        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("user", nguoiDung);
        model.addAttribute("currentStatus", currentStatus);
        model.addAttribute("statusHistory", hoaDon.getLichSuHoaDons() != null ? hoaDon.getLichSuHoaDons() : new ArrayList<>());

        return "WebKhachHang/chi-tiet-don-mua";
    }

    @GetMapping("/danh-gia/{id}")
    public String danhGiaPage(@PathVariable("id") UUID id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);
        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId()) || !"Hoàn thành".equals(hoaDon.getTrangThai())) {
            return "redirect:/dsdon-mua";
        }

        // Kiểm tra và xử lý nếu hoaDon.donHang là null
        if (hoaDon.getDonHang() == null || hoaDon.getDonHang().getChiTietDonHangs() == null) {
            model.addAttribute("error", "Không có sản phẩm trong hóa đơn để đánh giá.");
            return "error"; // Chuyển hướng đến trang lỗi tùy chỉnh
        }
        boolean alreadyRated = danhGiaRepository.existsByHoaDonIdAndNguoiDungId(id, nguoiDung.getId());
        if (alreadyRated) {
            return "redirect:/dsdon-mua"; // hoặc trả về trang thông báo
        }

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("user", nguoiDung);
        return "WebKhachHang/danh-gia";
    }

    @PostMapping("/api/orders/cancel/{id}")
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<?> cancelOrder(@PathVariable("id") UUID id, @RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để hủy đơn hàng.");
        }

        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
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
        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
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
            hoaDonService.addLichSuHoaDon(hoaDon, "Hoàn thành", "Khách hàng xác nhận đã nhận hàng");
            HoaDon savedHoaDon = hoaDonService.save(hoaDon);
            System.out.println("Trạng thái HoaDon đã lưu: " + savedHoaDon.getTrangThai() + ", ID: " + savedHoaDon.getId());
            if (!"Hoàn thành".equals(savedHoaDon.getTrangThai())) {
                throw new RuntimeException("Không thể cập nhật trạng thái thành 'Hoàn thành'.");
            }
            return ResponseEntity.ok(Map.of("message", "Đã xác nhận nhận hàng thành công."));
        } catch (ObjectOptimisticLockingFailureException e) {
            System.err.println("Xung đột đồng thời khi lưu hóa đơn: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi xung đột đồng thời khi xác nhận. Vui lòng thử lại.");
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

        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
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

        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
        if (!nguoiDung.getId().equals(userId)) {
            response.put("success", false);
            response.put("message", "Bạn không có quyền gửi đánh giá này.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        HoaDon hoaDon = hoaDonRepo.findById(hoaDonId).orElse(null);
        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(userId) || !"Hoàn thành".equals(hoaDon.getTrangThai())) {
            response.put("success", false);
            response.put("message", "Hóa đơn không hợp lệ hoặc chưa hoàn thành.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        boolean validProduct = hoaDon.getDonHang().getChiTietDonHangs().stream()
                .anyMatch(chiTiet -> chiTiet.getChiTietSanPham().getId().equals(chiTietSanPhamId));
        if (!validProduct) {
            response.put("success", false);
            response.put("message", "Sản phẩm không thuộc hóa đơn này.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        boolean alreadyRated = danhGiaRepository.existsByHoaDonIdAndChiTietSanPhamIdAndNguoiDungId(hoaDonId, chiTietSanPhamId, userId);
        if (alreadyRated) {
            response.put("success", false);
            response.put("message", "Bạn đã đánh giá sản phẩm này trong hóa đơn này rồi.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
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

            // Xử lý upload media với giới hạn 3 file
            if (media != null && media.length > 0) {
                if (media.length > 3) {
                    response.put("success", false);
                    response.put("message", "Chỉ được phép tải lên tối đa 3 hình ảnh.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                StringBuilder mediaUrls = new StringBuilder();
                for (MultipartFile file : media) {
                    if (file != null && !file.isEmpty() && file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
                        String url = uploadFile(file);
                        mediaUrls.append(url).append(",");
                    }
                }
                // Loại bỏ dấu phẩy cuối cùng nếu có
                if (mediaUrls.length() > 0) {
                    danhGia.setUrlHinhAnh(mediaUrls.toString().replaceAll(",$", ""));
                }
            }

            danhGiaRepository.save(danhGia);
            response.put("success", true);
            response.put("message", "Đánh giá đã được gửi thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi gửi đánh giá: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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
}