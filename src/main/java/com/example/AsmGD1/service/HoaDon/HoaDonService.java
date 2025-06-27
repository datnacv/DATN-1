package com.example.AsmGD1.service.HoaDon;

import com.example.AsmGD1.dto.BanHang.GioHangItemDTO;
import com.example.AsmGD1.dto.HoaDonDTO;
import com.example.AsmGD1.entity.ChiTietDonHang;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
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
        hoaDon.setTrangThai(refreshedDonHang.getTrangThaiThanhToan() != null ? refreshedDonHang.getTrangThaiThanhToan() : false);
        hoaDon.setNgayTao(refreshedDonHang.getThoiGianTao() != null ? refreshedDonHang.getThoiGianTao() : LocalDateTime.now());
        hoaDon.setGhiChu(refreshedDonHang.getDiaChiGiaoHang() != null ? refreshedDonHang.getDiaChiGiaoHang() : "");

        hoaDonRepository.saveAndFlush(hoaDon);
    }

    public Page<HoaDon> findAll(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return hoaDonRepository.searchByKeyword(search, pageable);
        }
        return hoaDonRepository.findAll(pageable);
    }

    public HoaDonDTO getHoaDonDetail(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonRepository.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));
            HoaDonDTO dto = new HoaDonDTO();

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
            dto.setPhuongThucBanHang(hoaDon.getDonHang().getPhuongThucBanHang()); // Thêm phương thức bán hàng

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
                document.add(new Paragraph("Địa chỉ: " + hoaDon.getGhiChu(), fontNormal));
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
                table.addCell(new PdfPCell(new Phrase(chiTiet.getGia().toString() + " VNĐ", fontNormal)));
                table.addCell(new PdfPCell(new Phrase(chiTiet.getThanhTien().toString() + " VNĐ", fontNormal)));
            }

            document.add(table);
            document.add(new Paragraph("\n"));

            BigDecimal tongTienHang = hoaDon.getTongTien().add(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO);
            document.add(new Paragraph("Tổng tiền hàng: " + tongTienHang.toString() + " VNĐ", fontNormal));
            document.add(new Paragraph("Phí vận chuyển: " + (donHang.getPhiVanChuyen() != null ? donHang.getPhiVanChuyen().toString() : "0") + " VNĐ", fontNormal));
            document.add(new Paragraph("Giảm giá: " + (hoaDon.getTienGiam() != null ? hoaDon.getTienGiam().toString() : "0") + " VNĐ", fontNormal));
            document.add(new Paragraph("Tổng tiền: " + hoaDon.getTongTien().toString() + " VNĐ", fontBold));

            document.close();
            return baos.toByteArray();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("ID hóa đơn không hợp lệ: " + id);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo PDF hóa đơn: " + e.getMessage());
        }
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
}