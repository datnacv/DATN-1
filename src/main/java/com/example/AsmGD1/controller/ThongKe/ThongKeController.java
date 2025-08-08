package com.example.AsmGD1.controller.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.dto.ThongKe.ThongKeDoanhThuDTO;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.BanHang.DonHangService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.ThongKe.ThongKeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/acvstore/thong-ke")
public class ThongKeController {
    @Autowired
    private DonHangService donHangService;

    @Autowired
    private ThongKeService thongKeDichVu;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping
    public String layThongKe(
            @RequestParam(defaultValue = "month") String boLoc,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayBatDau,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayKetThuc,
            Model model) {

        LocalDate homNay = LocalDate.now();
        String trangThaiBoLoc;

        switch (boLoc) {
            case "day" -> {
                ngayBatDau = homNay;
                ngayKetThuc = homNay;
                trangThaiBoLoc = "Hôm nay: " + homNay;
            }
            case "7days" -> {
                ngayBatDau = homNay.minusDays(6);
                ngayKetThuc = homNay;
                trangThaiBoLoc = "7 ngày: " + ngayBatDau + " đến " + ngayKetThuc;
            }
            case "month" -> {
                ngayBatDau = homNay.withDayOfMonth(1);
                ngayKetThuc = homNay;
                trangThaiBoLoc = "Tháng: " + homNay.getMonthValue() + "/" + homNay.getYear();
            }
            case "year" -> {
                ngayBatDau = homNay.withDayOfYear(1);
                ngayKetThuc = homNay;
                trangThaiBoLoc = "Năm: " + homNay.getYear();
            }
            case "custom_range" -> {
                if (ngayBatDau == null || ngayKetThuc == null) {
                    model.addAttribute("error", "Vui lòng chọn ngày bắt đầu và kết thúc.");
                    return "thongke";
                }
                if (ngayBatDau.isAfter(ngayKetThuc)) {
                    model.addAttribute("error", "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.");
                    return "thongke";
                }
                trangThaiBoLoc = "Tùy chỉnh: " + ngayBatDau + " đến " + ngayKetThuc;
            }
            default -> {
                model.addAttribute("error", "Bộ lọc không hợp lệ.");
                return "thongke";
            }
        }

        ThongKeDoanhThuDTO thongKe = thongKeDichVu.layThongKeDoanhThu(boLoc, ngayBatDau, ngayKetThuc);
        List<SanPhamBanChayDTO> sanPhamBanChay = thongKeDichVu.laySanPhamBanChay(boLoc, ngayBatDau, ngayKetThuc);
        List<SanPhamTonKhoThapDTO> sanPhamTonKhoThap = thongKeDichVu.laySanPhamTonKhoThap();

        model.addAttribute("chartLabels",  thongKeDichVu.layNhanBieuDoLienTuc(ngayBatDau, ngayKetThuc));
        model.addAttribute("chartOrders",  thongKeDichVu.layDonHangBieuDoLienTuc(ngayBatDau, ngayKetThuc));
        model.addAttribute("chartRevenue", thongKeDichVu.layDoanhThuBieuDoLienTuc(ngayBatDau, ngayKetThuc));


        model.addAttribute("stats", thongKe);
        model.addAttribute("topSellingProducts", sanPhamBanChay);
        model.addAttribute("lowStockProducts", sanPhamTonKhoThap);
        model.addAttribute("filterStatus", trangThaiBoLoc);
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
        }
        LocalDateTime startDateTime = ngayBatDau.atStartOfDay();
        LocalDateTime endDateTime = ngayKetThuc.atTime(LocalTime.MAX);
        Map<String, Integer> tongDonHangTheoPhuongThuc = donHangService.demDonHangTheoPhuongThuc(startDateTime, endDateTime);
        model.addAttribute("tongDonTheoPhuongThuc", tongDonHangTheoPhuongThuc);
        Map<String, Double> trangThaiPercentMap = thongKeDichVu.layPhanTramTatCaTrangThaiDonHang(ngayBatDau, ngayKetThuc);
        model.addAttribute("trangThaiPercentMap", trangThaiPercentMap);


        return "WebQuanLy/thong-ke";
    }

}