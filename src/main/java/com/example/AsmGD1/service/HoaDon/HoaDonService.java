package com.example.AsmGD1.service.HoaDon;

import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.ChiTietDonHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HoaDonService {

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    public byte[] taoHoaDon(String maDonHang) {
        DonHang donHang = donHangRepository.findByMaDonHang(maDonHang)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));

        HoaDon hoaDon = hoaDonRepository.findByDonHangId(donHang.getId())
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));

        NguoiDung khachHang = nguoiDungRepository.findById(donHang.getNguoiDung().getId())
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại."));

        List<ChiTietDonHang> chiTietDonHangs = chiTietDonHangRepository.findByDonHangId(donHang.getId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Tiêu đề hóa đơn
            Paragraph tieuDe = new Paragraph("HÓA ĐƠN BÁN HÀNG")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20)
                    .setBold();
            document.add(tieuDe);

            // Thông tin cửa hàng
            document.add(new Paragraph("Cửa hàng ACV Store")
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Địa chỉ: 123 Đường Láng, Đống Đa, Hà Nội")
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Số điện thoại: 0123 456 789")
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Thông tin hóa đơn
            document.add(new Paragraph("Mã hóa đơn: " + maDonHang));
            document.add(new Paragraph("Ngày tạo: " + hoaDon.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            document.add(new Paragraph("Khách hàng: " + khachHang.getHoTen()));
            document.add(new Paragraph("Số điện thoại: " + khachHang.getSoDienThoai()));
            document.add(new Paragraph("Phương thức thanh toán: " + (hoaDon.getPhuongThucThanhToan() != null ? hoaDon.getPhuongThucThanhToan().getTenPhuongThuc() : "Chưa xác định")));
            document.add(new Paragraph("Phương thức bán hàng: " + donHang.getPhuongThucBanHang()));
            document.add(new Paragraph("\n"));

            // Bảng chi tiết đơn hàng
            float[] columnWidths = {1, 3, 1, 1, 2};
            Table bangChiTiet = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();
            bangChiTiet.addHeaderCell(new Cell().add(new Paragraph("STT").setBold()));
            bangChiTiet.addHeaderCell(new Cell().add(new Paragraph("Sản phẩm").setBold()));
            bangChiTiet.addHeaderCell(new Cell().add(new Paragraph("Số lượng").setBold()));
            bangChiTiet.addHeaderCell(new Cell().add(new Paragraph("Đơn giá").setBold()));
            bangChiTiet.addHeaderCell(new Cell().add(new Paragraph("Thành tiền").setBold()));

            int stt = 1;
            for (ChiTietDonHang chiTiet : chiTietDonHangs) {
                bangChiTiet.addCell(new Cell().add(new Paragraph(String.valueOf(stt++))));
                bangChiTiet.addCell(new Cell().add(new Paragraph(chiTiet.getTenSanPham())));
                bangChiTiet.addCell(new Cell().add(new Paragraph(String.valueOf(chiTiet.getSoLuong()))));
                bangChiTiet.addCell(new Cell().add(new Paragraph(chiTiet.getGia().toString())));
                bangChiTiet.addCell(new Cell().add(new Paragraph(chiTiet.getThanhTien().toString())));
            }

            document.add(bangChiTiet);
            document.add(new Paragraph("\n"));

            // Thông tin tổng kết
            document.add(new Paragraph("Tổng tiền hàng: " + donHang.getTongTien().add(hoaDon.getTienGiam()).subtract(donHang.getPhiVanChuyen() != null ? donHang.getPhiVanChuyen() : BigDecimal.ZERO).toString()));
            if (hoaDon.getTienGiam() != null && hoaDon.getTienGiam().compareTo(BigDecimal.ZERO) > 0) {
                document.add(new Paragraph("Giảm giá: " + hoaDon.getTienGiam().toString()));
            }
            if (donHang.getPhiVanChuyen() != null && donHang.getPhiVanChuyen().compareTo(BigDecimal.ZERO) > 0) {
                document.add(new Paragraph("Phí vận chuyển: " + donHang.getPhiVanChuyen().toString()));
            }
            document.add(new Paragraph("Tổng cộng: " + hoaDon.getTongTien().toString()).setBold());
            if (donHang.getSoTienKhachDua() != null && donHang.getSoTienKhachDua().compareTo(BigDecimal.ZERO) > 0) {
                document.add(new Paragraph("Số tiền khách đưa: " + donHang.getSoTienKhachDua().toString()));
                document.add(new Paragraph("Tiền thừa: " + donHang.getSoTienKhachDua().subtract(hoaDon.getTongTien()).toString()));
            }

            // Chân trang
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Cảm ơn quý khách đã mua hàng!")
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo hóa đơn PDF: " + e.getMessage());
        }
    }
}
