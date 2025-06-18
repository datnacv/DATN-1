package com.example.AsmGD1.service;

import com.example.AsmGD1.dto.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.dto.ThongKeDoanhThuDTO;
import com.example.AsmGD1.repository.ThongKeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThongKeService {

    @Autowired
    private ThongKeRepository thongKeRepository;

    public ThongKeDoanhThuDTO layThongKeDoanhThu(String boLoc, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        ThongKeDoanhThuDTO thongKe = new ThongKeDoanhThuDTO();
        LocalDate homNay = LocalDate.now();

        // Xác định khoảng thời gian dựa vào bộ lọc
        switch (boLoc) {
            case "day" -> {
                ngayBatDau = homNay;
                ngayKetThuc = homNay;
            }
            case "7days" -> {
                ngayBatDau = homNay.minusDays(6);
                ngayKetThuc = homNay;
            }
            case "month" -> {
                ngayBatDau = homNay.withDayOfMonth(1);
                ngayKetThuc = homNay;
            }
            case "year" -> {
                ngayBatDau = homNay.withDayOfYear(1);
                ngayKetThuc = homNay;
            }
            case "custom_range" -> {
                if (ngayBatDau == null || ngayKetThuc == null)
                    throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được để trống.");
                if (ngayBatDau.isAfter(ngayKetThuc))
                    throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            }
        }

        // Doanh thu
        thongKe.setDoanhThuNgay(thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay, homNay));
        thongKe.setDoanhThuThang(thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay.withDayOfMonth(1), homNay));
        thongKe.setDoanhThuNam(thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay.withDayOfYear(1), homNay));

        // Số đơn hàng
        thongKe.setSoDonHangNgay(thongKeRepository.demDonHangTheoKhoangThoiGian(homNay, homNay));
        thongKe.setSoDonHangThang(thongKeRepository.demDonHangTheoKhoangThoiGian(homNay.withDayOfMonth(1), homNay));

        // Sản phẩm bán
        thongKe.setSoSanPhamThang(thongKeRepository.demSanPhamTheoKhoangThoiGian(homNay.withDayOfMonth(1), homNay));

        // Tăng trưởng
        BigDecimal dtNgayTruoc = thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay.minusDays(1), homNay.minusDays(1));
        BigDecimal dtThangTruoc = thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay.minusMonths(1).withDayOfMonth(1), homNay.minusMonths(1));
        BigDecimal dtNamTruoc = thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay.minusYears(1).withDayOfYear(1), homNay.minusYears(1));
        Integer spThangTruoc = thongKeRepository.demSanPhamTheoKhoangThoiGian(homNay.minusMonths(1).withDayOfMonth(1), homNay.minusMonths(1));

        thongKe.setTangTruongNgay(tinhTangTruong(thongKe.getDoanhThuNgay(), dtNgayTruoc));
        thongKe.setTangTruongThang(tinhTangTruong(thongKe.getDoanhThuThang(), dtThangTruoc));
        thongKe.setTangTruongNam(tinhTangTruong(thongKe.getDoanhThuNam(), dtNamTruoc));
        thongKe.setTangTruongSanPhamThang(tinhTangTruong(thongKe.getSoSanPhamThang(), spThangTruoc));

        return thongKe;
    }

    public List<SanPhamBanChayDTO> laySanPhamBanChay(String boLoc, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        LocalDate homNay = LocalDate.now();

        switch (boLoc) {
            case "day" -> {
                ngayBatDau = homNay;
                ngayKetThuc = homNay;
            }
            case "7days" -> {
                ngayBatDau = homNay.minusDays(6);
                ngayKetThuc = homNay;
            }
            case "month" -> {
                ngayBatDau = homNay.withDayOfMonth(1);
                ngayKetThuc = homNay;
            }
            case "year" -> {
                ngayBatDau = homNay.withDayOfYear(1);
                ngayKetThuc = homNay;
            }
            case "custom_range" -> {
                if (ngayBatDau == null || ngayKetThuc == null)
                    throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được để trống.");
                if (ngayBatDau.isAfter(ngayKetThuc))
                    throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            }
        }

        return thongKeRepository.laySanPhamBanChay(ngayBatDau, ngayKetThuc);
    }

    public List<SanPhamTonKhoThapDTO> laySanPhamTonKhoThap() {
        return thongKeRepository.laySanPhamTonKhoThap(50); // ngưỡng 50 sản phẩm
    }

    public Double layPhanTramTrangThaiDonHang(Boolean trangThai, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        LocalDateTime startDateTime = ngayBatDau.atStartOfDay();
        LocalDateTime endDateTime = ngayKetThuc.atTime(LocalTime.MAX);
        return thongKeRepository.tinhPhanTramTrangThaiDonHang(startDateTime, endDateTime, trangThai);
    }


    public List<String> layNhanBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        return thongKeRepository.layNhanBieuDo(ngayBatDau, ngayKetThuc)
                .stream()
                .map(date -> date.toString()) // đảm bảo không ép sai kiểu
                .collect(Collectors.toList());
    }


    public List<Integer> layDonHangBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        List<LocalDate> days = thongKeRepository.layNhanBieuDo(ngayBatDau, ngayKetThuc);
        List<Integer> result = new ArrayList<>();
        for (LocalDate day : days) {
            Integer count = thongKeRepository.layDonHangBieuDoTheoNgay(day);
            result.add(count != null ? count : 0);
        }
        return result;
    }

    public List<Integer> laySanPhamBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        List<LocalDate> days = thongKeRepository.layNhanBieuDo(ngayBatDau, ngayKetThuc);
        List<Integer> result = new ArrayList<>();
        for (LocalDate day : days) {
            Integer count = thongKeRepository.laySanPhamBieuDoTheoNgay(day);
            result.add(count != null ? count : 0);
        }
        return result;
    }

    // Tính % tăng trưởng doanh thu
    private Double tinhTangTruong(BigDecimal hienTai, BigDecimal truocDo) {
        if (truocDo == null || truocDo.compareTo(BigDecimal.ZERO) == 0)
            return hienTai != null && hienTai.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        if (hienTai == null) return -100.0;

        return hienTai.subtract(truocDo)
                .divide(truocDo, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    // Tính % tăng trưởng sản phẩm
    private Double tinhTangTruong(Integer hienTai, Integer truocDo) {
        if (truocDo == null || truocDo == 0)
            return hienTai != null && hienTai > 0 ? 100.0 : 0.0;
        if (hienTai == null) return -100.0;

        return ((double) (hienTai - truocDo) / truocDo) * 100;
    }
}
