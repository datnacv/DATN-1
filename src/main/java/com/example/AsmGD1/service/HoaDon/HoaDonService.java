package com.example.AsmGD1.service.HoaDon;

import com.example.AsmGD1.dto.BanHang.GioHangItemDTO;
import com.example.AsmGD1.dto.HoaDonDTO;
import com.example.AsmGD1.entity.ChiTietDonHang;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.LichSuHoaDon;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class HoaDonService {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(
            value = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public HoaDon save(HoaDon hoaDon) {
        try {
            return hoaDonRepository.save(hoaDon);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("Xung đột đồng thời khi cập nhật hóa đơn. Vui lòng thử lại.");
        }
    }

    public void deleteHoaDon(UUID id) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));
        hoaDonRepository.delete(hoaDon);
    }

    public void createHoaDonFromDonHang(DonHang donHang) {
        DonHang refreshedDonHang = donHangRepository.findById(donHang.getId())
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại với ID: " + donHang.getId()));

        HoaDon hoaDon = new HoaDon();
        hoaDon.setDonHang(refreshedDonHang);
        hoaDon.setNguoiDung(refreshedDonHang.getNguoiDung());
        hoaDon.setTongTien(refreshedDonHang.getTongTien());
        hoaDon.setTienGiam(refreshedDonHang.getTienGiam() != null ? refreshedDonHang.getTienGiam() : BigDecimal.ZERO);
        hoaDon.setPhuongThucThanhToan(refreshedDonHang.getPhuongThucThanhToan());
        hoaDon.setTrangThai(refreshedDonHang.getTrangThaiThanhToan() != null ? refreshedDonHang.getTrangThaiThanhToan() :
                "Tại quầy".equalsIgnoreCase(refreshedDonHang.getPhuongThucBanHang()));
        hoaDon.setNgayTao(refreshedDonHang.getThoiGianTao() != null ? refreshedDonHang.getThoiGianTao() : LocalDateTime.now());
        hoaDon.setGhiChu(refreshedDonHang.getDiaChiGiaoHang() != null ? refreshedDonHang.getDiaChiGiaoHang() : "");

        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(hoaDon.getTrangThai() ? "Đã xác nhận" : "Chưa xác nhận");
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setGhiChu("Hóa đơn được tạo");
        hoaDon.getLichSuHoaDons().add(lichSu);

        hoaDonRepository.saveAndFlush(hoaDon);
    }

    public Page<HoaDon> findAll(String search, Boolean trangThai, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            if (trangThai != null) {
                return hoaDonRepository.findBySearchAndTrangThai(search, trangThai, pageable);
            }
            return hoaDonRepository.searchByKeyword(search, pageable);
        }
        if (trangThai != null) {
            return hoaDonRepository.findByTrangThai(trangThai, pageable);
        }
        return hoaDonRepository.findAll(pageable);
    }

    public HoaDonDTO getHoaDonDetail(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonRepository.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));

            if ("Tại quầy".equalsIgnoreCase(hoaDon.getDonHang().getPhuongThucBanHang()) &&
                    (hoaDon.getTrangThai() == null || !hoaDon.getTrangThai())) {
                hoaDon.setTrangThai(true);
                hoaDon.setNgayThanhToan(LocalDateTime.now());
                hoaDon.setGhiChu("Đã xác nhận tự động (Tại quầy)");
                addLichSuHoaDon(hoaDon, "Đã xác nhận", "Đã xác nhận tự động (Tại quầy)");
                save(hoaDon);
            }

            HoaDonDTO dto = new HoaDonDTO();
            dto.setId(hoaDon.getId());
            dto.setMaHoaDon(hoaDon.getDonHang().getMaDonHang());
            dto.setTenKhachHang(hoaDon.getNguoiDung().getHoTen());
            dto.setSoDienThoaiKhachHang(hoaDon.getNguoiDung().getSoDienThoai());
            dto.setTongTienHang(hoaDon.getTongTien().add(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO));
            dto.setTienGiam(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO);
            dto.setTongTien(hoaDon.getTongTien());
            dto.setPhiVanChuyen(hoaDon.getDonHang().getPhiVanChuyen() != null ? hoaDon.getDonHang().getPhiVanChuyen() : BigDecimal.ZERO);
            dto.setPhuongThucThanhToan(hoaDon.getPhuongThucThanhToan() != null ? hoaDon.getPhuongThucThanhToan().getTenPhuongThuc() : "Chưa chọn");
            dto.setTrangThaiThanhToan(hoaDon.getTrangThai());
            dto.setThoiGianTao(hoaDon.getNgayTao());
            dto.setGhiChu(hoaDon.getGhiChu());
            dto.setPhuongThucBanHang(hoaDon.getDonHang().getPhuongThucBanHang());

            List<GioHangItemDTO> danhSachSanPham = new ArrayList<>();
            DonHang donHang = donHangRepository.findByMaDonHang(hoaDon.getDonHang().getMaDonHang())
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
            for (ChiTietDonHang chiTiet : donHang.getChiTietDonHangs()) {
                GioHangItemDTO item = new GioHangItemDTO();
                item.setIdChiTietSanPham(chiTiet.getChiTietSanPham().getId());
                item.setTenSanPham(chiTiet.getTenSanPham());
                item.setMauSac(chiTiet.getChiTietSanPham().getMauSac().getTenMau());
                item.setKichCo(chiTiet.getChiTietSanPham().getKichCo().getTen());
                item.setSoLuong(chiTiet.getSoLuong());
                item.setGia(chiTiet.getGia());
                item.setThanhTien(chiTiet.getThanhTien());
                item.setGhiChu(chiTiet.getGhiChu());
                danhSachSanPham.add(item);
            }
            dto.setDanhSachSanPham(danhSachSanPham);

            List<HoaDonDTO.LichSuDTO> lichSuDTOs = new ArrayList<>();
            for (LichSuHoaDon lichSu : hoaDon.getLichSuHoaDons()) {
                HoaDonDTO.LichSuDTO lichSuDTO = new HoaDonDTO.LichSuDTO();
                lichSuDTO.setThoiGian(lichSu.getThoiGian());
                lichSuDTO.setTrangThai(lichSu.getTrangThai());
                lichSuDTO.setGhiChu(lichSu.getGhiChu());
                lichSuDTOs.add(lichSuDTO);
            }
            dto.setLichSuHoaDons(lichSuDTOs);

            return dto;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("ID hóa đơn không hợp lệ: " + id);
        }
    }

    public byte[] taoHoaDon(String maDonHang) {
        DonHang donHang = donHangRepository.findByMaDonHang(maDonHang)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
        HoaDon hoaDon = hoaDonRepository.findByDonHang(donHang)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));
        return generateHoaDonPDF(hoaDon.getId().toString());
    }

    public byte[] generateHoaDonPDF(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonRepository.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + id));
            DonHang donHang = hoaDon.getDonHang();
            if (donHang == null || donHang.getChiTietDonHangs() == null || donHang.getChiTietDonHangs().isEmpty()) {
                throw new RuntimeException("Không có dữ liệu chi tiết đơn hàng để tạo PDF cho ID: " + id);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            BaseFont bf;
            try {
                bf = BaseFont.createFont("/fonts/DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                System.err.println("Không thể tải font DejaVuSans, sử dụng font fallback Helvetica: " + e.getMessage());
                bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
            }
            Font fontTitle = new Font(bf, 16, Font.BOLD);
            Font fontNormal = new Font(bf, 12);
            Font fontBold = new Font(bf, 12, Font.BOLD);

            Paragraph title = new Paragraph("HÓA ĐƠN BÁN HÀNG", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Mã hóa đơn: " + donHang.getMaDonHang(), fontNormal));
            document.add(new Paragraph("Ngày tạo: " + hoaDon.getNgayTao().toString(), fontNormal));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Khách hàng: " + (hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getHoTen() : "Không rõ"), fontNormal));
            document.add(new Paragraph("Số điện thoại: " + (hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getSoDienThoai() : "Không rõ"), fontNormal));
            if (hoaDon.getGhiChu() != null && !hoaDon.getGhiChu().isEmpty()) {
                document.add(new Paragraph("Địa chỉ: " + hoaDon.getNguoiDung().getDiaChi(), fontNormal));
            }
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 3, 1, 2, 2});

            addTableHeader(table, fontBold, "STT", "Sản phẩm", "SL", "Đơn giá", "Thành tiền");
            int index = 1;
            for (ChiTietDonHang chiTiet : donHang.getChiTietDonHangs()) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(index++), fontNormal)));
                table.addCell(new PdfPCell(new Phrase(chiTiet.getTenSanPham() + " (" + (chiTiet.getGhiChu() != null ? chiTiet.getGhiChu() : "") + ")", fontNormal)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(chiTiet.getSoLuong()), fontNormal)));
                table.addCell(new PdfPCell(new Phrase(formatCurrency(chiTiet.getGia()), fontNormal)));
                table.addCell(new PdfPCell(new Phrase(formatCurrency(chiTiet.getThanhTien()), fontNormal)));
            }

            document.add(table);
            document.add(new Paragraph("\n"));

            BigDecimal tongTienHang = hoaDon.getTongTien().add(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO);
            document.add(new Paragraph("Tổng tiền hàng: " + formatCurrency(tongTienHang), fontNormal));
            document.add(new Paragraph("Phí vận chuyển: " + formatCurrency(donHang.getPhiVanChuyen() != null ? donHang.getPhiVanChuyen() : BigDecimal.ZERO), fontNormal));
            document.add(new Paragraph("Giảm giá: " + formatCurrency(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO), fontNormal));
            document.add(new Paragraph("Tổng tiền: " + formatCurrency(hoaDon.getTongTien()), fontBold));

            document.close();
            return baos.toByteArray();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("ID hóa đơn không hợp lệ: " + id);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo PDF hóa đơn: " + e.getMessage());
        }
    }

    // Phương thức định dạng tiền tệ với dấu chấm làm dấu phân cách nghìn và loại bỏ chữ số thập phân
    private String formatCurrency(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.'); // Sử dụng dấu chấm làm dấu phân cách nghìn
        symbols.setDecimalSeparator(','); // Không sử dụng dấu phẩy thập phân
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        df.setGroupingSize(3); // Định dạng theo nhóm 3 chữ số
        return df.format(amount.setScale(0, RoundingMode.HALF_UP)) + " VNĐ";
    }

    private void addTableHeader(PdfPTable table, Font font, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    public Optional<HoaDon> findById(UUID id) {
        return hoaDonRepository.findById(id);
    }

    public void addLichSuHoaDon(HoaDon hoaDon, String trangThai, String ghiChu) {
        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(trangThai);
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setGhiChu(ghiChu);
        lichSuHoaDonRepository.save(lichSu);
        hoaDon.getLichSuHoaDons().add(lichSu);
    }

    public String getCurrentStatus(HoaDon hoaDon) {
        if (hoaDon.getGhiChu() != null && hoaDon.getGhiChu().contains("Hoàn thành")) {
            return "Hoàn thành";
        } else if (hoaDon.getGhiChu() != null && hoaDon.getGhiChu().contains("Vận chuyển thành công")) {
            return "Vận chuyển thành công";
        } else if (hoaDon.getGhiChu() != null && hoaDon.getGhiChu().contains("Đang vận chuyển")) {
            return "Đang vận chuyển";
        } else if (hoaDon.getTrangThai() != null && hoaDon.getTrangThai()) {
            return "Đã xác nhận";
        } else {
            return "Chưa xác nhận";
        }
    }
}