package com.example.AsmGD1.controller.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.dto.ThongKe.ThongKeDoanhThuDTO;
import com.example.AsmGD1.service.ThongKe.ThongKeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/acvstore/thong-ke")
public class ThongKeController {

    @Autowired
    private ThongKeService thongKeDichVu;

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

        // Dữ liệu thống kê
        ThongKeDoanhThuDTO thongKe = thongKeDichVu.layThongKeDoanhThu(boLoc, ngayBatDau, ngayKetThuc);
        List<SanPhamBanChayDTO> sanPhamBanChay = thongKeDichVu.laySanPhamBanChay(boLoc, ngayBatDau, ngayKetThuc);
        List<SanPhamTonKhoThapDTO> sanPhamTonKhoThap = thongKeDichVu.laySanPhamTonKhoThap();

        // Trạng thái đơn hàng (truyền đúng kiểu String tương ứng với Enum hoặc định nghĩa trạng thái trong DB)
        model.addAttribute("successPercent", thongKeDichVu.layPhanTramTrangThaiDonHang(true, ngayBatDau, ngayKetThuc));
        model.addAttribute("failedPercent", thongKeDichVu.layPhanTramTrangThaiDonHang(false, ngayBatDau, ngayKetThuc));

        // Biểu đồ
        model.addAttribute("chartLabels", thongKeDichVu.layNhanBieuDo(ngayBatDau, ngayKetThuc));
        model.addAttribute("chartOrders", thongKeDichVu.layDonHangBieuDo(ngayBatDau, ngayKetThuc));
        model.addAttribute("chartProducts", thongKeDichVu.laySanPhamBieuDo(ngayBatDau, ngayKetThuc));

        // Gửi về view
        model.addAttribute("stats", thongKe);
        model.addAttribute("topSellingProducts", sanPhamBanChay);
        model.addAttribute("lowStockProducts", sanPhamTonKhoThap);
        model.addAttribute("filterStatus", trangThaiBoLoc);

        return "WebQuanLy/thong-ke";
    }
}
