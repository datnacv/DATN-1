package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.ChiTietDonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/dsdon-mua")
public class KHDonMuaController {

    @Autowired
    private HoaDonRepository hoaDonRepo;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private HoaDonService hoaDonService;

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
            // Gọi phương thức cancelOrder của HoaDonService để đồng bộ với logic admin
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
}