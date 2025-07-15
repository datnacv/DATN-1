package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.ChiTietDonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

        // Lấy người dùng từ phiên đăng nhập
        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
        model.addAttribute("user", nguoiDung); // -> Gán user cho model

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

        // Format dữ liệu
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
//    @GetMapping("/chi-tiet/{id}")
//    public String chiTietDonHang(@PathVariable("id") UUID id, Model model, Authentication authentication) {
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return "redirect:/dang-nhap";
//        }
//
//        NguoiDung nguoiDung = (NguoiDung) authentication.getPrincipal();
//
//        HoaDon hoaDon = hoaDonRepo.findById(id).orElse(null);
//        if (hoaDon == null || !hoaDon.getDonHang().getNguoiDung().getId().equals(nguoiDung.getId())) {
//            return "redirect:/dsdon-mua";
//        }
//
//        DecimalFormat formatter = new DecimalFormat("#,###");
//
//        hoaDon.setFormattedTongTien(hoaDon.getTongTien() != null ? formatter.format(hoaDon.getTongTien()) : "0");
//        for (ChiTietDonHang chiTiet : hoaDon.getDonHang().getChiTietDonHangs()) {
//            chiTiet.setFormattedGia(chiTiet.getGia() != null ? formatter.format(chiTiet.getGia()) : "0");
//        }
//
//        model.addAttribute("hoaDon", hoaDon);
//        model.addAttribute("user", nguoiDung);
//
//        return "WebKhachHang/chi-tiet-don-mua";
//    }

}
