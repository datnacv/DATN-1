package com.example.AsmGD1.service.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.dto.ThongKe.ThongKeDoanhThuDTO;
import com.example.AsmGD1.repository.SanPham.HinhAnhSanPhamRepository;
import com.example.AsmGD1.repository.ThongKe.ThongKeRepository;
import com.example.AsmGD1.service.BanHang.DonHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ThongKeService {

    @Autowired
    private ThongKeRepository thongKeRepository;

    @Autowired
    private HinhAnhSanPhamRepository hinhAnhSanPhamRepository;
    @Autowired
    private DonHangService donHangService;
    public ThongKeDoanhThuDTO layThongKeDoanhThu(String boLoc, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        ThongKeDoanhThuDTO thongKe = new ThongKeDoanhThuDTO();
        LocalDate homNay = LocalDate.now();

        switch (boLoc) {
            case "day" -> { ngayBatDau = homNay; ngayKetThuc = homNay; }
            case "7days" -> { ngayBatDau = homNay.minusDays(6); ngayKetThuc = homNay; }
            case "month" -> { ngayBatDau = homNay.withDayOfMonth(1); ngayKetThuc = homNay; }
            case "year" -> { ngayBatDau = homNay.withDayOfYear(1); ngayKetThuc = homNay; }
            case "custom_range" -> {
                if (ngayBatDau == null || ngayKetThuc == null)
                    throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được để trống.");
                if (ngayBatDau.isAfter(ngayKetThuc))
                    throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            }
        }

        // mốc thời gian dùng cho HÓA ĐƠN Hoàn thành
        LocalDateTime dayStart   = homNay.atStartOfDay();
        LocalDateTime dayEnd     = homNay.atTime(LocalTime.MAX);
        LocalDateTime monthStart = homNay.withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart  = homNay.withDayOfYear(1).atStartOfDay();

        BigDecimal doanhThuNgay  = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoHoaDon(dayStart, dayEnd));
        BigDecimal doanhThuThang = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoHoaDon(monthStart, dayEnd));
        BigDecimal doanhThuNam   = defaultBigDecimal(thongKeRepository.tinhDoanhThuTheoHoaDon(yearStart, dayEnd));

        Integer donHangNgay  = defaultInteger(thongKeRepository.demHoaDonTheoKhoangThoiGian(dayStart, dayEnd));
        Integer donHangThang = defaultInteger(thongKeRepository.demHoaDonTheoKhoangThoiGian(monthStart, dayEnd));
        Integer sanPhamThang = defaultInteger(thongKeRepository.demSanPhamTheoHoaDon(monthStart, dayEnd));

        // so sánh tăng trưởng
        BigDecimal dtNgayTruoc = defaultBigDecimal(
                thongKeRepository.tinhDoanhThuTheoHoaDon(
                        dayStart.minusDays(1),
                        homNay.minusDays(1).atTime(LocalTime.MAX)));

        LocalDate prevMonth = homNay.minusMonths(1);
        BigDecimal dtThangTruoc = defaultBigDecimal(
                thongKeRepository.tinhDoanhThuTheoHoaDon(
                        prevMonth.withDayOfMonth(1).atStartOfDay(),
                        prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()).atTime(LocalTime.MAX)));

        LocalDate prevYear = homNay.minusYears(1);
        BigDecimal dtNamTruoc = defaultBigDecimal(
                thongKeRepository.tinhDoanhThuTheoHoaDon(
                        prevYear.withDayOfYear(1).atStartOfDay(),
                        prevYear.withDayOfYear(prevYear.lengthOfYear()).atTime(LocalTime.MAX)));

        Integer spThangTruoc = defaultInteger(
                thongKeRepository.demSanPhamTheoHoaDon(
                        prevMonth.withDayOfMonth(1).atStartOfDay(),
                        prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()).atTime(LocalTime.MAX)));

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
            case "day" -> { ngayBatDau = homNay; ngayKetThuc = homNay; }
            case "7days" -> { ngayBatDau = homNay.minusDays(6); ngayKetThuc = homNay; }
            case "month" -> { ngayBatDau = homNay.withDayOfMonth(1); ngayKetThuc = homNay; }
            case "year" -> { ngayBatDau = homNay.withDayOfYear(1); ngayKetThuc = homNay; }
            case "custom_range" -> {
                if (ngayBatDau == null || ngayKetThuc == null)
                    throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được để trống.");
                if (ngayBatDau.isAfter(ngayKetThuc))
                    throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            }
            default -> throw new IllegalArgumentException("Bộ lọc không hợp lệ.");
        }

        LocalDateTime start = ngayBatDau.atStartOfDay();
        LocalDateTime end   = ngayKetThuc.atTime(LocalTime.MAX);

        List<SanPhamBanChayDTO> list = thongKeRepository.laySanPhamBanChayTheoHoaDon(start, end);
        for (SanPhamBanChayDTO dto : list) {
            String img = hinhAnhSanPhamRepository
                    .findFirstImageByChiTietSanPham(dto.getIdChiTietSanPham())
                    .orElse("/img/default.png");
            dto.setImageUrl(img);
        }
        return list;
    }


    public List<SanPhamTonKhoThapDTO> laySanPhamTonKhoThap() {
        List<SanPhamTonKhoThapDTO> list = thongKeRepository.laySanPhamTonKhoThap(30);
        for (SanPhamTonKhoThapDTO dto : list) {
            String img = hinhAnhSanPhamRepository
                    .findFirstImageByChiTietSanPham(dto.getIdChiTietSanPham())
                    .orElse("/img/default.png"); // ảnh mặc định nếu không có
            dto.setImageUrl(img);
        }
        return list;
    }




    public List<String> layNhanBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        List<LocalDate> days = thongKeRepository.layNhanBieuDoTheoHoaDon(
                ngayBatDau.atStartOfDay(), ngayKetThuc.atTime(LocalTime.MAX));
        return days.stream().map(LocalDate::toString).collect(Collectors.toList());
    }

    public List<Integer> layDonHangBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        List<LocalDate> days = thongKeRepository.layNhanBieuDoTheoHoaDon(
                ngayBatDau.atStartOfDay(), ngayKetThuc.atTime(LocalTime.MAX));
        List<Integer> result = new ArrayList<>();
        for (LocalDate day : days) {
            Integer count = thongKeRepository.laySoHoaDonTheoNgay(day);
            result.add(count != null ? count : 0);
        }
        return result;
    }

    public List<Integer> laySanPhamBieuDo(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        List<LocalDate> days = thongKeRepository.layNhanBieuDoTheoHoaDon(
                ngayBatDau.atStartOfDay(), ngayKetThuc.atTime(LocalTime.MAX));
        List<Integer> result = new ArrayList<>();
        for (LocalDate day : days) {
            Integer count = thongKeRepository.laySoSanPhamTheoNgay(day);
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
    public Map<String, Map<String, Integer>> thongKeChiTietPhuongThucVaTrangThai(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        LocalDateTime start = ngayBatDau.atStartOfDay();
        LocalDateTime end = ngayKetThuc.plusDays(1).atStartOfDay(); // không bỏ sót cuối ngày
        return donHangService.thongKeChiTietTheoPhuongThucVaTrangThai(start, end);
    }
    private String normalize(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase().trim();

        // bỏ dấu cơ bản
        lower = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        // thay khoảng trắng thành underscore để dễ so khớp
        lower = lower.replaceAll("\\s+", "_");
        return lower;
    }

    private String toDisplayLabel(String raw) {
        String n = normalize(raw);

        // gom nhóm "thành công" / "hoàn thành"
        if (n.equals("thanh_cong") || n.equals("hoan_thanh")) return "Thành công";

        // các trạng thái hay gặp (bạn có thể bổ sung nếu hệ thống có thêm)
        return switch (n) {
            case "cho_xac_nhan", "cho_duyet" -> "Chờ xác nhận";
            case "dang_chuan_bi", "chuan_bi" -> "Đang chuẩn bị";
            case "dang_giao", "giao_hang" -> "Đang giao";
            case "da_huy", "huy" -> "Đã hủy";
            case "tra_hang", "hoan_tra" -> "Trả hàng";
            case "thanh_toan_that_bai" -> "Thanh toán thất bại";
            // nếu không map được, trả lại bản gốc (viết hoa chữ cái đầu)
            default -> {
                String original = raw == null ? "" : raw.trim();
                yield original.isEmpty() ? "Khác" :
                        original.substring(0,1).toUpperCase() + original.substring(1);
            }
        };
    }


    public Map<String, Double> layPhanTramTatCaTrangThaiDonHang(LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        LocalDateTime start = ngayBatDau.atStartOfDay();
        LocalDateTime end = ngayKetThuc.atTime(LocalTime.MAX);

        List<Object[]> rawData = thongKeRepository.thongKePhanTramTatCaTrangThaiDonHang(start, end);

        // Tính tổng bản ghi lịch sử (để làm mẫu số)
        int tong = rawData.stream()
                .mapToInt(r -> ((Long) r[1]).intValue())
                .sum();

        // Gom và cộng dồn theo nhãn hiển thị
        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            String rawStatus = (String) row[0];
            int count = ((Long) row[1]).intValue();

            String label = toDisplayLabel(rawStatus);
            double percent = (tong == 0) ? 0.0 : (count * 100.0 / tong);

            result.merge(label, percent, Double::sum);
        }

        return result;
    }





}
