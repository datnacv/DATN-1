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
        Page<HoaDon> hoaDonPage = hoaDonService.findAll(search, null, pageable);
        model.addAttribute("hoaDonPage", hoaDonPage);
        model.addAttribute("search", search);

        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
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
            @RequestParam(required = false) Boolean trangThai) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<HoaDon> hoaDonPage = hoaDonService.findAll(search, trangThai, pageable);

            List<Map<String, Object>> hoaDonList = hoaDonPage.getContent().stream()
                    .map(row -> {
                        Map<String, Object> hoaDonInfo = new HashMap<>();
                        hoaDonInfo.put("id", row.getId());
                        hoaDonInfo.put("maHoaDon", row.getDonHang().getMaDonHang());
                        hoaDonInfo.put("tenKhachHang", row.getNguoiDung().getHoTen());
                        hoaDonInfo.put("tongTien", row.getTongTien());
                        hoaDonInfo.put("thoiGianTao", row.getNgayTao());
                        hoaDonInfo.put("phuongThucThanhToan", row.getPhuongThucThanhToan() != null ? row.getPhuongThucThanhToan().getTenPhuongThuc() : "Chưa chọn");
                        hoaDonInfo.put("trangThai", row.getTrangThai() != null ? row.getTrangThai() : false);
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
                    (hoaDon.getTrangThai() == null || !hoaDon.getTrangThai())) {
                hoaDon.setTrangThai(true);
                hoaDon.setNgayThanhToan(LocalDateTime.now());
                hoaDon.setGhiChu("Hoàn thành (Tại quầy)");
                hoaDonService.addLichSuHoaDon(hoaDon, "Hoàn thành", "Hoàn thành tự động (Tại quầy)");
                hoaDonService.save(hoaDon);
            }

            model.addAttribute("hoaDon", hoaDon);
            return "WebQuanLy/hoa-don-detail";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải chi tiết hóa đơn: " + e.getMessage());
            return "error";
        }
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

            // Validate request data
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

            // Update information
            hoaDon.getNguoiDung().setHoTen(hoTen);
            hoaDon.getNguoiDung().setSoDienThoai(soDienThoai);
            hoaDon.getNguoiDung().setTinhThanhPho(tinhThanhPho);
            hoaDon.getNguoiDung().setQuanHuyen(quanHuyen);
            hoaDon.getNguoiDung().setPhuongXa(phuongXa);
            hoaDon.getNguoiDung().setDiaChi(chiTietDiaChi);

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

            if (hoaDon.getTrangThai() != null && hoaDon.getTrangThai()) {
                throw new RuntimeException("Hóa đơn đã được xác nhận hoặc hoàn thành trước đó.");
            }

            String trangThai = "Tại quầy".equalsIgnoreCase(donHang.getPhuongThucBanHang()) ? "Hoàn thành" : "Đã xác nhận";
            hoaDon.setGhiChu(ghiChu);
            hoaDon.setTrangThai(true);
            hoaDon.setNgayThanhToan(LocalDateTime.now());

            donHang.setTrangThaiThanhToan(true);
            donHang.setThoiGianThanhToan(LocalDateTime.now());
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
            donHang = donHangRepository.findById(donHang.getId())
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tìm thấy trong cơ sở dữ liệu."));

            if (!"Giao hàng".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ áp dụng xác nhận giao hàng cho đơn hàng giao hàng.");
            }
            if (hoaDon.getTrangThai() == null || !hoaDon.getTrangThai()) {
                throw new RuntimeException("Đơn hàng chưa được xác nhận, không thể xác nhận giao hàng.");
            }
            if (hoaDon.getGhiChu() != null && hoaDon.getGhiChu().contains("Đang vận chuyển")) {
                throw new RuntimeException("Đơn hàng đã được xác nhận giao hàng trước đó.");
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            donHang.setThoiGianThanhToan(LocalDateTime.now());
            hoaDon.setDonHang(donHang);

            hoaDonService.addLichSuHoaDon(hoaDon, "Đang vận chuyển", ghiChu);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận giao hàng thành công.");
            response.put("currentStatus", "Đang vận chuyển");
            response.put("phuongThucBanHang", donHang.getPhuongThucBanHang());
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
            donHang = donHangRepository.findById(donHang.getId())
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tìm thấy trong cơ sở dữ liệu."));

            if (!"Giao hàng".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ áp dụng xác nhận vận chuyển thành công cho đơn hàng giao hàng.");
            }
            if (hoaDon.getGhiChu() == null || !hoaDon.getGhiChu().contains("Đang vận chuyển")) {
                throw new RuntimeException("Đơn hàng chưa được xác nhận đang vận chuyển.");
            }
            if (hoaDon.getGhiChu().contains("Vận chuyển thành công")) {
                throw new RuntimeException("Đơn hàng đã được xác nhận vận chuyển thành công trước đó.");
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            donHang.setThoiGianThanhToan(LocalDateTime.now());
            hoaDon.setDonHang(donHang);

            hoaDonService.addLichSuHoaDon(hoaDon, "Vận chuyển thành công", ghiChu);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận vận chuyển thành công.");
            response.put("currentStatus", "Vận chuyển thành công");
            response.put("phuongThucBanHang", donHang.getPhuongThucBanHang());
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
            donHang = donHangRepository.findById(donHang.getId())
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tìm thấy trong cơ sở dữ liệu."));

            if (!"Giao hàng".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                throw new RuntimeException("Chỉ áp dụng xác nhận hoàn thành cho đơn hàng giao hàng.");
            }
            if (hoaDon.getGhiChu() == null || !hoaDon.getGhiChu().contains("Vận chuyển thành công")) {
                throw new RuntimeException("Đơn hàng chưa được xác nhận vận chuyển thành công.");
            }
            if (hoaDon.getGhiChu().contains("Hoàn thành")) {
                throw new RuntimeException("Đơn hàng đã được xác nhận hoàn thành trước đó.");
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setNgayThanhToan(LocalDateTime.now());
            donHang.setThoiGianThanhToan(LocalDateTime.now());
            hoaDon.setDonHang(donHang);

            hoaDonService.addLichSuHoaDon(hoaDon, "Hoàn thành", ghiChu);
            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận hoàn thành thành công.");
            response.put("currentStatus", "Hoàn thành");
            response.put("phuongThucBanHang", donHang.getPhuongThucBanHang());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận hoàn thành: " + e.getMessage()));
        }
    }
}