package com.example.AsmGD1.controller.HoaDon;

import com.example.AsmGD1.dto.HoaDonDTO;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public String hienThiTrangHoaDon(Model model,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "5") int size,
                                     @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<HoaDon> hoaDonPage = hoaDonService.findAll(search, pageable);

        model.addAttribute("hoaDonPage", hoaDonPage);
        model.addAttribute("search", search);
        return "WebQuanLy/hoa-don";
    }

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHoaDonList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<HoaDon> hoaDonPage = hoaDonService.findAll(search, pageable);

            List<Map<String, Object>> hoaDonList = hoaDonPage.getContent().stream()
                    .map(row -> {
                        Map<String, Object> hoaDonInfo = new HashMap<>();
                        hoaDonInfo.put("id", row.getId());
                        hoaDonInfo.put("maHoaDon", row.getDonHang().getMaDonHang());
                        hoaDonInfo.put("tenKhachHang", row.getNguoiDung().getHoTen());
                        hoaDonInfo.put("tongTien", row.getTongTien());
                        hoaDonInfo.put("thoiGianTao", row.getNgayTao());
                        hoaDonInfo.put("phuongThucThanhToan", row.getPhuongThucThanhToan() != null ? row.getPhuongThucThanhToan().getTenPhuongThuc() : "Chưa chọn");
                        hoaDonInfo.put("trangThai", row.getTrangThai() != null ? row.getTrangThai() : false); // Gửi Boolean
                        hoaDonInfo.put("phuongThucBanHang", row.getDonHang().getPhuongThucBanHang()); // Thêm phương thức bán hàng
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHoaDonDetail(@PathVariable String id) {
        try {
            HoaDonDTO hoaDonDTO = hoaDonService.getHoaDonDetail(id);
            Map<String, Object> response = new HashMap<>();
            response.put("hoaDon", hoaDonDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteHoaDon(@PathVariable String id) {
        try {
            UUID uuid = UUID.fromString(id);
            hoaDonService.deleteHoaDon(uuid);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Hóa đơn đã được xóa thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa hóa đơn: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadHoaDon(@PathVariable String id) {
        try {
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
            UUID uuid = UUID.fromString(id);
            String ghiChu = request.get("ghiChu");
            HoaDon hoaDon = hoaDonService.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));

            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null) {
                throw new RuntimeException("Đơn hàng liên quan không tồn tại.");
            }
            donHang = donHangRepository.findById(donHang.getId())
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tìm thấy trong cơ sở dữ liệu."));

            if (hoaDon.getTrangThai() != null && hoaDon.getTrangThai()) {
                throw new RuntimeException("Hóa đơn đã được xác nhận trước đó.");
            }

            hoaDon.setGhiChu(ghiChu);
            hoaDon.setTrangThai(true); // Xác nhận đơn hàng
            hoaDon.setNgayThanhToan(LocalDateTime.now());

            donHang.setTrangThaiThanhToan(true);
            donHang.setThoiGianThanhToan(LocalDateTime.now());
            hoaDon.setDonHang(donHang);

            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận thành công.");
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
            // Kiểm tra nếu id rỗng hoặc null
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "ID hóa đơn không được để trống."));
            }

            // Chuyển đổi id thành UUID và xử lý ngoại lệ
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "ID hóa đơn không hợp lệ: " + id));
            }

            String ghiChu = request.get("ghiChu");
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
            if (hoaDon.getGhiChu() != null && hoaDon.getGhiChu().contains("Đã giao hàng")) {
                throw new RuntimeException("Đơn hàng đã được xác nhận giao hàng trước đó.");
            }

            hoaDon.setGhiChu(ghiChu != null ? ghiChu + " - Đã giao hàng" : "Đã giao hàng");
            hoaDon.setNgayThanhToan(LocalDateTime.now());

            donHang.setThoiGianThanhToan(LocalDateTime.now());
            hoaDon.setDonHang(donHang);

            hoaDonService.save(hoaDon);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được xác nhận giao hàng thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xác nhận giao hàng: " + e.getMessage()));
        }
    }
}