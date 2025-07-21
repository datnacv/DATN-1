package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.ChiTietDonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/dsdon-mua")
public class KHDonMuaController {

    @Autowired
    private HoaDonRepository hoaDonRepo;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping
    public String donMuaPage(@RequestParam(name = "status", defaultValue = "tat-ca") String status,
                             Model model,
                             Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/dang-nhap";
        }

        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
        model.addAttribute("user", nguoiDung);

        DecimalFormat formatter = new DecimalFormat("#,###");

        List<HoaDon> danhSachHoaDon;
        if ("tat-ca".equalsIgnoreCase(status)) {
            danhSachHoaDon = hoaDonRepo.findByDonHang_NguoiDungId(nguoiDung.getId());
        } else {
            String statusDb = switch (status) {
                case "cho-xac-nhan" -> "CHO_XAC_NHAN";
                case "dang-giao" -> "DANG_GIAO";
                case "hoan-thanh" -> "HOAN_THANH";
                case "da-huy" -> "DA_HUY";
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

        DecimalFormat formatter = new DecimalFormat("#,###");

        hoaDon.setFormattedTongTien(hoaDon.getTongTien() != null ? formatter.format(hoaDon.getTongTien()) : "0");
        for (ChiTietDonHang chiTiet : hoaDon.getDonHang().getChiTietDonHangs()) {
            chiTiet.setFormattedGia(chiTiet.getGia() != null ? formatter.format(chiTiet.getGia()) : "0");
        }

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("user", nguoiDung);

        return "WebKhachHang/chi-tiet-don-mua";
    }

    @PostMapping("/api/orders/cancel/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable("id") UUID id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để hủy đơn hàng.");
        }

        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);

        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng hoặc bạn không có quyền hủy đơn hàng này.");
        }

        if (!hoaDon.getTrangThai().equals("CHO_XAC_NHAN")) {
            return ResponseEntity.badRequest().body("Chỉ có thể hủy đơn hàng ở trạng thái 'Chờ xác nhận'.");
        }

        try {
            hoaDon.setTrangThai("DA_HUY");
            hoaDonRepo.save(hoaDon);
            return ResponseEntity.ok("Đơn hàng đã được hủy thành công.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi hủy đơn hàng: " + e.getMessage());
        }
    }
    @PostMapping("/api/orders/confirm-received/{id}")
    public ResponseEntity<?> confirmReceivedOrder(@PathVariable("id") UUID id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để xác nhận.");
        }
        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);
        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng hoặc bạn không có quyền xác nhận.");
        }
        if (!hoaDon.getTrangThai().equals("Đã giao hàng")) {
            return ResponseEntity.badRequest().body("Chỉ có thể xác nhận khi đơn hàng ở trạng thái 'Đã giao hàng'.");
        }
        try {
            hoaDon.setTrangThai("Đã nhận hàng");
            hoaDonRepo.save(hoaDon);
            return ResponseEntity.ok("Đã xác nhận nhận hàng thành công.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi xác nhận: " + e.getMessage());
        }
    }
}