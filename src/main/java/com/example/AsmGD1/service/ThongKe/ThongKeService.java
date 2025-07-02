package com.example.AsmGD1.service.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.dto.ThongKe.ThongKeDoanhThuDTO;
import com.example.AsmGD1.repository.ThongKe.ThongKeRepository;
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

        // Áp dụng defaultBigDecimal / defaultInteger để tránh null
        BigDecimal doanhThuNgay = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay, homNay));
        BigDecimal doanhThuThang = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay.withDayOfMonth(1), homNay));
        BigDecimal doanhThuNam = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay.withDayOfYear(1), homNay));

        Integer donHangNgay = defaultInteger(thongKeRepository.demDonHangTheoKhoangThoiGian(homNay, homNay));
        Integer donHangThang = defaultInteger(thongKeRepository.demDonHangTheoKhoangThoiGian(homNay.withDayOfMonth(1), homNay));
        Integer sanPhamThang = defaultInteger(thongKeRepository.demSanPhamTheoKhoangThoiGian(homNay.withDayOfMonth(1), homNay));

        BigDecimal dtNgayTruoc = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(homNay.minusDays(1), homNay.minusDays(1)));
        BigDecimal dtThangTruoc = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(
                homNay.minusMonths(1).withDayOfMonth(1),
                homNay.minusMonths(1).withDayOfMonth(homNay.minusMonths(1).lengthOfMonth())
        ));
        BigDecimal dtNamTruoc = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoKhoangThoiGian(
                homNay.minusYears(1).withDayOfYear(1),
                homNay.minusYears(1).withDayOfYear(homNay.minusYears(1).lengthOfYear())
        ));
        Integer spThangTruoc = defaultInteger(thongKeRepository.demSanPhamTheoKhoangThoiGian(
                homNay.minusMonths(1).withDayOfMonth(1),
                homNay.minusMonths(1).withDayOfMonth(homNay.minusMonths(1).lengthOfMonth())
        ));

        // Gán lại vào DTO
        thongKe.setDoanhThuNgay(doanhThuNgay);
        thongKe.setDoanhThuThang(doanhThuThang);
        thongKe.setDoanhThuNam(doanhThuNam);
        thongKe.setSoDonHangNgay(donHangNgay);
        thongKe.setSoDonHangThang(donHangThang);
        thongKe.setSoSanPhamThang(sanPhamThang);

        thongKe.setTangTruongNgay(tinhTangTruong(doanhThuNgay, dtNgayTruoc));
        thongKe.setTangTruongThang(tinhTangTruong(doanhThuThang, dtThangTruoc));
        thongKe.setTangTruongNam(tinhTangTruong(doanhThuNam, dtNamTruoc));
        thongKe.setTangTruongSanPhamThang(tinhTangTruong(sanPhamThang, spThangTruoc));

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
        return thongKeRepository.laySanPhamTonKhoThap(30);
    }

    public Double layPhanTramTrangThaiDonHang(Boolean trangThai, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        LocalDateTime startDateTime = ngayBatDau.atStartOfDay();
        LocalDateTime endDateTime = ngayKetThuc.atTime(LocalTime.MAX);
        Double percent = thongKeRepository.tinhPhanTramTrangThaiDonHang(startDateTime, endDateTime, trangThai);
        return percent != null ? percent : 0.0;
    }

    public List<String> layNhanBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        return thongKeRepository.layNhanBieuDo(ngayBatDau, ngayKetThuc)
                .stream()
                .map(LocalDate::toString)
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

    private Double tinhTangTruong(BigDecimal hienTai, BigDecimal truocDo) {
        if (truocDo == null || truocDo.compareTo(BigDecimal.ZERO) == 0)
            return hienTai != null && hienTai.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        if (hienTai == null) return -100.0;

        return hienTai.subtract(truocDo)
                .divide(truocDo, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private Double tinhTangTruong(Integer hienTai, Integer truocDo) {
        if (truocDo == null || truocDo == 0)
            return hienTai != null && hienTai > 0 ? 100.0 : 0.0;
        if (hienTai == null) return -100.0;

        return ((double) (hienTai - truocDo) / truocDo) * 100;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Integer defaultInteger(Integer value) {
        return value != null ? value : 0;
    }
}
