package com.example.AsmGD1.controller.HoaDon;

import com.example.AsmGD1.dto.HoaDonDTO;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/hoa-don")
public class HoaDonController {

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping
    public String hienThiTrangHoaDon(Model model,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "5") int size,
                                     @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<HoaDon> hoaDonPage = hoaDonService.findAll(search, null, null, null, pageable);
        model.addAttribute("hoaDonPage", hoaDonPage);
        model.addAttribute("search", search);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String tenDangNhap = authentication.getName();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
        }

        return "WebQuanLy/hoa-don";
    }

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHoaDonList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String salesMethod) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<HoaDon> hoaDonPage = hoaDonService.findAll(search, trangThai, paymentMethod, salesMethod, pageable);

            List<Map<String, Object>> hoaDonList = hoaDonPage.getContent().stream()
                    .map(row -> {
                        Map<String, Object> hoaDonInfo = new HashMap<>();
                        hoaDonInfo.put("id", row.getId());
                        hoaDonInfo.put("maHoaDon", row.getDonHang().getMaDonHang());
                        hoaDonInfo.put("tenKhachHang", row.getNguoiDung() != null ? row.getNguoiDung().getHoTen() : "Khách lẻ");
                        hoaDonInfo.put("tenNhanVien", row.getNhanVien() != null ? row.getNhanVien().getHoTen() : "Không rõ");
                        hoaDonInfo.put("tongTien", row.getTongTien());
                        hoaDonInfo.put("thoiGianTao", row.getNgayTao());
                        hoaDonInfo.put("phuongThucThanhToan", row.getPhuongThucThanhToan() != null ? row.getPhuongThucThanhToan().getTenPhuongThuc() : "Chưa chọn");
                        hoaDonInfo.put("trangThai", hoaDonService.getCurrentStatus(row));
                        hoaDonInfo.put("phuongThucBanHang", row.getDonHang().getPhuongThucBanHang());
                        return hoaDonInfo;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("hoaDon", hoaDonList);
            response.put("totalPages", hoaDonPage.getTotalPages());
            response.put("totalElements", hoaDonPage.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy danh sách hóa đơn: " + e.getMessage()));
        }
    }

    @GetMapping("/detail/{id}")
    public String getHoaDonDetail(@PathVariable String id, Model model) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            // Kiểm tra và tự động hoàn thành nếu phương thức bán hàng là "Tại quầy"
            if ("Tại quầy".equalsIgnoreCase(hoaDon.getDonHang().getPhuongThucBanHang()) &&
                    !"Hoàn thành".equals(hoaDon.getTrangThai())) {
                hoaDon.setGhiChu("Hoàn thành (Tại quầy)");
                hoaDon.setTrangThai("Hoàn thành");
                hoaDonService.addLichSuHoaDon(hoaDon, "Hoàn thành", "Hoàn thành tự động (Tại quầy)");
                hoaDonService.save(hoaDon);
            }

            model.addAttribute("hoaDon", hoaDon);
            model.addAttribute("currentStatus", hoaDonService.getCurrentStatus(hoaDon));
            return "WebQuanLy/hoa-don-detail";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải chi tiết hóa đơn: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getOrderStatus(@PathVariable UUID id) {
        HoaDon hoaDon = hoaDonService.findById(id)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));
        Map<String, String> response = new HashMap<>();
        response.put("currentStatus", hoaDonService.getCurrentStatus(hoaDon));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteHoaDon(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            hoaDonService.deleteHoaDon(uuid);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Hóa đơn đã được xóa thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa hóa đơn: " + e.getMessage()));
        }
    }

    @PostMapping("/update/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> updateHoaDon(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            String hoTen = request.get("hoTen");
            String soDienThoai = request.get("soDienThoai");
            String tinhThanhPho = request.get("tinhThanhPho");
            String quanHuyen = request.get("quanHuyen");
            String phuongXa = request.get("phuongXa");
            String chiTietDiaChi = request.get("chiTietDiaChi");
            if (hoTen == null || hoTen.trim().isEmpty() || soDienThoai == null || soDienThoai.trim().isEmpty() ||
                    tinhThanhPho == null || tinhThanhPho.trim().isEmpty() ||
                    quanHuyen == null || quanHuyen.trim().isEmpty() ||
                    phuongXa == null || phuongXa.trim().isEmpty() ||
                    chiTietDiaChi == null || chiTietDiaChi.trim().isEmpty()) {
                throw new IllegalArgumentException("Thông tin khách hàng không được để trống.");
            }

            hoaDon.getNguoiDung().setHoTen(hoTen);
            hoaDon.getNguoiDung().setSoDienThoai(soDienThoai);
            hoaDon.getNguoiDung().setTinhThanhPho(tinhThanhPho);
            hoaDon.getNguoiDung().setQuanHuyen(quanHuyen);
            hoaDon.getNguoiDung().setPhuongXa(phuongXa);
            hoaDon.getNguoiDung().setChiTietDiaChi(chiTietDiaChi);

            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Thông tin hóa đơn đã được cập nhật thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi cập nhật hóa đơn: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadHoaDon(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            byte[] pdfBytes = hoaDonService.generateHoaDonPDF(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=hoa_don_" + id + ".pdf")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Lỗi khi tải PDF: " + e.getMessage()).getBytes());
        }
    }

    @PostMapping("/cancel/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            // Gọi service để hủy đơn hàng
            hoaDonService.cancelOrder(uuid, ghiChu);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được hủy thành công.");
            response.put("currentStatus", "Hủy đơn hàng");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi hủy đơn hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmHoaDon(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            donHang = donHangRepository.findById(donHang.getId())
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tìm thấy trong cơ sở dữ liệu."));

            // Kiểm tra trạng thái hiện tại
            if (!"Chưa xác nhận".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Chưa xác nhận' để xác nhận.");
            }

            String trangThai;
            if ("Tại quầy".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                trangThai = "Hoàn thành";
                ghiChu = "Hoàn thành (Tại quầy)";
                donHang.setTrangThaiThanhToan(true); // Thanh toán tại quầy
            } else if ("Online".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                trangThai = "Đã xác nhận Online";
                ghiChu = "Đã xác nhận đơn hàng Online";
                // Kiểm tra nếu thanh toán bằng ví điện tử
                if ("Ví Thanh Toán".equalsIgnoreCase(donHang.getPhuongThucThanhToan().getTenPhuongThuc())) {
                    donHang.setTrangThaiThanhToan(true);
                    donHang.setThoiGianThanhToan(LocalDateTime.now());
                }
            } else {
                trangThai = "Đã xác nhận";
                ghiChu = "Đã xác nhận đơn hàng Giao hàng";
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            hoaDon.setTrangThai(trangThai);

            donHangRepository.save(donHang); // Lưu trạng thái thanh toán của DonHang
            hoaDon.setDonHang(donHang);

            hoaDonService.addLichSuHoaDon(hoaDon, trangThai, ghiChu);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được " + trangThai.toLowerCase() + " thành công.");
            response.put("currentStatus", trangThai);
            response.put("phuongThucBanHang", donHang.getPhuongThucBanHang());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận hóa đơn: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-delivery/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmDelivery(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Giao hàng".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Giao hàng' mới có thể chuyển sang trạng thái 'Đang vận chuyển'.");
            }
            if (!"Đã xác nhận".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đã xác nhận' để chuyển sang 'Đang vận chuyển'.");
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setTrangThai("Đang vận chuyển");
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            hoaDonService.addLichSuHoaDon(hoaDon, "Đang vận chuyển", ghiChu);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận giao hàng thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận giao hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-delivered/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmDelivered(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Giao hàng".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Giao hàng' mới có thể chuyển sang trạng thái 'Vận chuyển thành công'.");
            }
            if (!"Đang vận chuyển".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đang vận chuyển' để chuyển sang 'Vận chuyển thành công'.");
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setTrangThai("Vận chuyển thành công");
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            hoaDonService.addLichSuHoaDon(hoaDon, "Vận chuyển thành công", ghiChu);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận vận chuyển thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận vận chuyển thành công: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-online-processing/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmOnlineProcessing(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Online".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Online' mới có thể chuyển sang trạng thái 'Đang xử lý Online'.");
            }
            if (!"Đã xác nhận Online".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đã xác nhận Online' để chuyển sang 'Đang xử lý Online'.");
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setTrangThai("Đang xử lý Online");
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            hoaDonService.addLichSuHoaDon(hoaDon, "Đang xử lý Online", ghiChu);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận đang xử lý Online thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận xử lý Online: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-online-shipping/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmOnlineShipping(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Online".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Online' mới có thể chuyển sang trạng thái 'Đang vận chuyển'.");
            }
            if (!"Đang xử lý Online".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đang xử lý Online' để chuyển sang 'Đang vận chuyển'.");
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setTrangThai("Đang vận chuyển");
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            hoaDonService.addLichSuHoaDon(hoaDon, "Đang vận chuyển", ghiChu);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận đang vận chuyển thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận đang vận chuyển: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-online-delivered/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> confirmOnlineDelivered(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Online".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ các đơn hàng có phương thức 'Online' mới có thể chuyển sang trạng thái 'Vận chuyển thành công'.");
            }
            if (!"Đang vận chuyển".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Đang vận chuyển' để chuyển sang 'Vận chuyển thành công'.");
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setTrangThai("Vận chuyển thành công");
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            hoaDonService.addLichSuHoaDon(hoaDon, "Vận chuyển thành công", ghiChu);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận vận chuyển thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận vận chuyển thành công: " + e.getMessage()));
        }
    }



    @PostMapping("/complete/{id}")
    @ResponseBody
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<Map<String, Object>> completeOrder(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("ID hóa đơn không được để trống.");
            }
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            if (ghiChu == null || ghiChu.trim().isEmpty()) {
                throw new IllegalArgumentException("Ghi chú không được để trống.");
            }
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            if (!"Vận chuyển thành công".equals(hoaDon.getTrangThai())) {
                throw new RuntimeException("Hóa đơn phải ở trạng thái 'Vận chuyển thành công' để chuyển sang 'Hoàn thành'.");
            }

            // Kiểm tra nếu trạng thái đã được khách hàng xác nhận
            throw new RuntimeException("Trạng thái 'Hoàn thành' chỉ có thể được cập nhật bởi khách hàng sau khi xác nhận nhận hàng.");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}