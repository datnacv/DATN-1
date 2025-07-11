package com.example.AsmGD1.service.HoaDon;

import com.example.AsmGD1.dto.BanHang.GioHangItemDTO;
import com.example.AsmGD1.dto.HoaDonDTO;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuTraHangRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HoaDonService {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private LichSuTraHangRepository lichSuTraHangRepository;

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

        boolean isTaiQuay = "Tại quầy".equalsIgnoreCase(refreshedDonHang.getPhuongThucBanHang());
        hoaDon.setTrangThai(isTaiQuay ? "Hoàn thành" : "Chưa xác nhận");
        hoaDon.setNgayTao(refreshedDonHang.getThoiGianTao() != null ? refreshedDonHang.getThoiGianTao() : LocalDateTime.now());
        hoaDon.setGhiChu(isTaiQuay ? "Hoàn thành (Tại quầy)" : refreshedDonHang.getDiaChiGiaoHang() != null ? refreshedDonHang.getDiaChiGiaoHang() : "");

        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(hoaDon.getTrangThai());
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setGhiChu(isTaiQuay ? "Hoàn thành tự động (Tại quầy)" : "Hóa đơn được tạo");
        hoaDon.getLichSuHoaDons().add(lichSu);

        hoaDonRepository.saveAndFlush(hoaDon);
    }

    public Page<HoaDon> findAll(String search, String trangThai, String paymentMethod, String salesMethod, Pageable pageable) {
        Sort sort = Sort.by(Sort.Direction.DESC, "ngayTao");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return hoaDonRepository.searchByKeywordAndFilters(
                search != null && !search.isEmpty() ? search : null,
                trangThai != null && !trangThai.isEmpty() ? trangThai : null,
                paymentMethod != null && !paymentMethod.isEmpty() ? paymentMethod : null,
                salesMethod != null && !salesMethod.isEmpty() ? salesMethod : null,
                sortedPageable
        );
    }

    public HoaDonDTO getHoaDonDetail(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            HoaDon hoaDon = hoaDonRepository.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));

            String currentStatus = getCurrentStatus(hoaDon);
            if (("Đang vận chuyển".equals(currentStatus) || "Hoàn thành".equals(currentStatus)) && !"Hoàn thành".equals(hoaDon.getTrangThai())) {
                hoaDon.setTrangThai(currentStatus);
                hoaDon.setNgayThanhToan(LocalDateTime.now());
                save(hoaDon);
            }

            HoaDonDTO dto = new HoaDonDTO();
            dto.setId(hoaDon.getId());
            dto.setMaHoaDon(hoaDon.getDonHang().getMaDonHang());
            dto.setTenKhachHang(hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getHoTen() : "Khách lẻ");
            dto.setSoDienThoaiKhachHang(hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getSoDienThoai() : "Không rõ");
            dto.setDiaChi(hoaDon.getNguoiDung() != null && hoaDon.getNguoiDung().getDiaChi() != null ? hoaDon.getNguoiDung().getDiaChi() : hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "Không rõ");
            dto.setTongTienHang(hoaDon.getTongTien().add(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO));
            dto.setTongTien(hoaDon.getTongTien());
            dto.setPhiVanChuyen(hoaDon.getDonHang().getPhiVanChuyen() != null ? hoaDon.getDonHang().getPhiVanChuyen() : BigDecimal.ZERO);
            dto.setPhuongThucThanhToan(hoaDon.getPhuongThucThanhToan() != null ? hoaDon.getPhuongThucThanhToan().getTenPhuongThuc() : "Chưa chọn");
            dto.setTrangThaiThanhToan(hoaDon.getTrangThai());
            dto.setThoiGianTao(hoaDon.getNgayTao());
            dto.setNgayThanhToan(hoaDon.getNgayThanhToan());
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
                item.setTrangThaiHoanTra(chiTiet.getTrangThaiHoanTra() != null ? chiTiet.getTrangThaiHoanTra() : false);
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
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            BaseFont bf;
            try {
                bf = BaseFont.createFont("/fonts/DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                System.err.println("Không thể tải font DejaVuSans, sử dụng font Helvetica: " + e.getMessage());
                bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
            }
            Font fontTitle = new Font(bf, 18, Font.BOLD, BaseColor.BLACK);
            Font fontHeader = new Font(bf, 14, Font.BOLD, BaseColor.DARK_GRAY);
            Font fontNormal = new Font(bf, 12, Font.NORMAL, BaseColor.BLACK);
            Font fontBold = new Font(bf, 12, Font.BOLD, BaseColor.BLACK);
            Font fontFooter = new Font(bf, 10, Font.ITALIC, BaseColor.GRAY);

            try {
                Image logo = Image.getInstance("src/main/resources/static/images/acv-logo.png");
                logo.scaleToFit(100, 100);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
                document.add(Chunk.NEWLINE);
            } catch (Exception e) {
                System.err.println("Không thể tải logo: " + e.getMessage());
            }

            Paragraph storeInfo = new Paragraph();
            storeInfo.setAlignment(Element.ALIGN_CENTER);
            storeInfo.add(new Phrase("CỬA HÀNG ACV STORE\n", fontHeader));
            storeInfo.add(new Phrase("Địa chỉ: Thanh Oai, TP. Hà Nội\n", fontNormal));
            storeInfo.add(new Phrase("Hotline: 0866 716 384 | Email: datn.acv@gmail.com\n", fontNormal));
            document.add(storeInfo);
            document.add(Chunk.NEWLINE);

            Paragraph title = new Paragraph("HÓA ĐƠN BÁN HÀNG", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1, 3});
            infoTable.setSpacingAfter(10f);

            addInfoCell(infoTable, fontBold, fontNormal, "Mã hóa đơn:", donHang.getMaDonHang());
            addInfoCell(infoTable, fontBold, fontNormal, "Ngày tạo:", hoaDon.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            addInfoCell(infoTable, fontBold, fontNormal, "Khách hàng:", hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getHoTen() : "Khách lẻ");
            addInfoCell(infoTable, fontBold, fontNormal, "Số điện thoại:", hoaDon.getNguoiDung() != null ? hoaDon.getNguoiDung().getSoDienThoai() : "Không rõ");

            String address = "Không rõ";
            if ("Tại quầy".equalsIgnoreCase(donHang.getPhuongThucBanHang())) {
                address = hoaDon.getNguoiDung() != null && hoaDon.getNguoiDung().getDiaChi() != null
                        ? hoaDon.getNguoiDung().getDiaChi()
                        : "Mua tại quầy";
            } else {
                address = hoaDon.getNguoiDung().getChiTietDiaChi() != null && !hoaDon.getNguoiDung().getChiTietDiaChi().isEmpty()
                        ? hoaDon.getNguoiDung().getChiTietDiaChi()
                        : (donHang.getDiaChiGiaoHang() != null ? donHang.getDiaChiGiaoHang() : "Không rõ");
            }
            addInfoCell(infoTable, fontBold, fontNormal, "Địa chỉ:", address);

            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.5f, 1f, 2f, 1f, 1f, 1.5f, 1.5f});
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            addTableHeader(table, fontBold, BaseColor.LIGHT_GRAY, "STT", "Mã sản phẩm", "Sản phẩm", "Màu", "Kích cỡ", "Đơn giá", "Thành tiền");

            int index = 1;
            for (ChiTietDonHang chiTiet : donHang.getChiTietDonHangs()) {
                table.addCell(createCell(String.valueOf(index++), fontNormal, Element.ALIGN_CENTER));
                table.addCell(createCell(chiTiet.getChiTietSanPham().getSanPham().getMaSanPham() != null ? chiTiet.getChiTietSanPham().getSanPham().getMaSanPham() : "N/A", fontNormal, Element.ALIGN_CENTER));
                table.addCell(createCell(chiTiet.getTenSanPham(), fontNormal, Element.ALIGN_LEFT));
                table.addCell(createCell(chiTiet.getChiTietSanPham().getMauSac().getTenMau(), fontNormal, Element.ALIGN_CENTER));
                table.addCell(createCell(chiTiet.getChiTietSanPham().getKichCo().getTen(), fontNormal, Element.ALIGN_CENTER));
                table.addCell(createCell(formatCurrency(chiTiet.getGia()), fontNormal, Element.ALIGN_RIGHT));
                table.addCell(createCell(formatCurrency(chiTiet.getThanhTien()), fontNormal, Element.ALIGN_RIGHT));
            }
            document.add(table);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setSpacingBefore(10f);

            BigDecimal tongTienHang = hoaDon.getTongTien().add(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO);
            addSummaryCell(summaryTable, fontBold, fontNormal, "Tổng tiền hàng:", formatCurrency(tongTienHang));
            addSummaryCell(summaryTable, fontBold, fontNormal, "Phí vận chuyển:", formatCurrency(donHang.getPhiVanChuyen() != null ? donHang.getPhiVanChuyen() : BigDecimal.ZERO));
            addSummaryCell(summaryTable, fontBold, fontNormal, "Giảm giá:", formatCurrency(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO));
            addSummaryCell(summaryTable, fontBold, fontNormal, "Tổng tiền:", formatCurrency(hoaDon.getTongTien()));

            document.add(summaryTable);

            Paragraph footer = new Paragraph("Cảm ơn quý khách đã mua sắm tại ACV Store!\nVui lòng kiểm tra kỹ thông tin hóa đơn.", fontFooter);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20f);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("ID hóa đơn không hợp lệ: " + id);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo PDF hóa đơn: " + e.getMessage());
        }
    }

    private void addInfoCell(PdfPTable table, Font fontBold, Font fontNormal, String label, String value) {
        table.addCell(createCell(label, fontBold, Element.ALIGN_LEFT));
        table.addCell(createCell(value, fontNormal, Element.ALIGN_LEFT));
    }

    private void addSummaryCell(PdfPTable table, Font fontBold, Font fontNormal, String label, String value) {
        table.addCell(createCell(label, fontBold, Element.ALIGN_LEFT));
        table.addCell(createCell(value, fontNormal, Element.ALIGN_RIGHT));
    }

    private void addTableHeader(PdfPTable table, Font font, BaseColor bgColor, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(bgColor);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private PdfPCell createCell(String content, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        return cell;
    }

    private String formatCurrency(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0 VNĐ", symbols);
        df.setGroupingSize(3);
        return df.format(amount.setScale(0, RoundingMode.HALF_UP));
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
        if ("Tại quầy".equalsIgnoreCase(hoaDon.getDonHang().getPhuongThucBanHang())) {
            return "Hoàn thành";
        }
        if (hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Hoàn thành".equals(ls.getTrangThai()))) {
            return "Hoàn thành";
        } else if (hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Vận chuyển thành công".equals(ls.getTrangThai()))) {
            return "Vận chuyển thành công";
        } else if (hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Đang vận chuyển".equals(ls.getTrangThai()))) {
            return "Đang vận chuyển";
        } else if (hoaDon.getLichSuHoaDons().stream().anyMatch(ls -> "Đã xác nhận".equals(ls.getTrangThai()))) {
            return "Đã xác nhận";
        }
        return "Chưa xác nhận";
    }

    @Transactional
    public void processReturn(UUID hoaDonId, List<UUID> chiTietDonHangIds, String lyDoTraHang) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại với ID: " + hoaDonId));

        if (!"Hoàn thành".equals(hoaDon.getTrangThai())) {
            throw new IllegalStateException("Hóa đơn phải ở trạng thái 'Hoàn thành' để thực hiện trả hàng.");
        }

        if (chiTietDonHangIds == null || chiTietDonHangIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách sản phẩm trả không hợp lệ.");
        }

        BigDecimal tongTienHoan = BigDecimal.ZERO;

        for (UUID chiTietId : chiTietDonHangIds) {
            ChiTietDonHang chiTiet = chiTietDonHangRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Chi tiết đơn hàng không tồn tại với ID: " + chiTietId));

            if (Boolean.TRUE.equals(chiTiet.getTrangThaiHoanTra())) {
                throw new RuntimeException("Sản phẩm đã được trả trước đó: " + chiTiet.getTenSanPham());
            }

            chiTiet.setTrangThaiHoanTra(true);
            chiTiet.setLyDoTraHang(lyDoTraHang);
            chiTietDonHangRepository.save(chiTiet);

            chiTietSanPhamRepository.updateStock(chiTiet.getChiTietSanPham().getId(), chiTiet.getSoLuong());

            LichSuTraHang lichSu = new LichSuTraHang();
            lichSu.setHoaDon(hoaDon);
            lichSu.setChiTietDonHang(chiTiet);
            lichSu.setSoLuong(chiTiet.getSoLuong());
            lichSu.setTongTienHoan(chiTiet.getThanhTien());
            lichSu.setLyDoTraHang(lyDoTraHang);
            lichSu.setThoiGianTra(LocalDateTime.now());
            lichSu.setTrangThai("Đã trả");
            lichSuTraHangRepository.save(lichSu);

            tongTienHoan = tongTienHoan.add(chiTiet.getThanhTien());
        }

        String ghiChu = "Trả hàng: " + lyDoTraHang + ". Tổng tiền hoàn: " + formatCurrency(tongTienHoan);
        addLichSuHoaDon(hoaDon, "Trả hàng", ghiChu);
        hoaDon.setGhiChu(ghiChu);
        save(hoaDon);
    }

    public List<ChiTietDonHang> getReturnableItems(UUID hoaDonId) {
        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại."));
        return hoaDon.getDonHang().getChiTietDonHangs().stream()
                .filter(item -> item.getTrangThaiHoanTra() == null || !item.getTrangThaiHoanTra())
                .collect(Collectors.toList());
    }
}