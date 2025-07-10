package com.example.AsmGD1.service.WebKhachHang;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.WebKhachHang.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class KHCheckoutService {

    @Autowired
    private KHDonHangTamRepository donHangTamRepository;

    @Autowired
    private KHChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private KHPhieuGiamGiaCuaNguoiDungRepository phieuGiamGiaCuaNguoiDungRepository;

    @Autowired
    private KHPhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Autowired
    private KHDonHangRepository donHangRepository;

    @Autowired
    private KHNguoiDungRepository nguoiDungRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public DonHangTam createTempOrder(UUID chiTietSanPhamId, Integer soLuong) {
        NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new RuntimeException("Người dùng không tìm thấy"));

        ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietSanPhamId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tìm thấy"));

        if (chiTietSanPham.getSoLuongTonKho() < soLuong) {
            throw new RuntimeException("Số lượng tồn kho không đủ");
        }

        DonHangTam donHangTam = new DonHangTam();
        donHangTam.setId(UUID.randomUUID());
        donHangTam.setKhachHang(nguoiDung.getId());
        donHangTam.setMaDonHangTam("TEMP-" + System.currentTimeMillis());
        donHangTam.setSoDienThoaiKhachHang(nguoiDung.getSoDienThoai());
        donHangTam.setThoiGianTao(LocalDateTime.now());
        donHangTam.setPhuongThucBanHang("Giao hàng");

        List<ChiTietDonHang> items = new ArrayList<>();
        ChiTietDonHang item = new ChiTietDonHang();
        item.setId(UUID.randomUUID());
        item.setChiTietSanPham(chiTietSanPham);
        item.setSoLuong(soLuong);
        item.setGia(chiTietSanPham.getGia());
        item.setTenSanPham(chiTietSanPham.getSanPham().getTenSanPham());
        item.setThanhTien(chiTietSanPham.getGia().multiply(BigDecimal.valueOf(soLuong)));
        items.add(item);

        donHangTam.setTong(calculateTotal(items, BigDecimal.ZERO, null));
        try {
            donHangTam.setDanhSachSanPham(objectMapper.writeValueAsString(items));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi chuyển đổi danh sách sản phẩm sang JSON");
        }

        return donHangTamRepository.save(donHangTam);
    }

    public List<PhieuGiamGiaCuaNguoiDung> getAvailableVouchers() {
        NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new RuntimeException("Người dùng không tìm thấy"));
        return phieuGiamGiaCuaNguoiDungRepository.findAvailableVouchersByNguoiDungId(nguoiDung.getId());
    }

    public BigDecimal applyVoucher(String voucherId, String voucherCode, BigDecimal tongTienHang) {
        NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new RuntimeException("Người dùng không tìm thấy"));

        PhieuGiamGiaCuaNguoiDung voucher = null;
        if (voucherId != null && !voucherId.isEmpty()) {
            voucher = phieuGiamGiaCuaNguoiDungRepository.findById(UUID.fromString(voucherId))
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));
        } else if (voucherCode != null && !voucherCode.isEmpty()) {
            voucher = phieuGiamGiaCuaNguoiDungRepository.findByMaAndNguoiDungId(voucherCode, nguoiDung.getId())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));
        }

        if (voucher == null) {
            throw new RuntimeException("Mã giảm giá không hợp lệ");
        }

        PhieuGiamGia phieuGiamGia = voucher.getPhieuGiamGia();
        if (tongTienHang.compareTo(phieuGiamGia.getGiaTriGiamToiThieu()) < 0) {
            throw new RuntimeException("Tổng tiền hàng không đủ để áp dụng mã giảm giá");
        }

        BigDecimal giamGia = phieuGiamGia.getGiaTriGiam();
        if (phieuGiamGia.getGiaTriGiamToiDa() != null && giamGia.compareTo(phieuGiamGia.getGiaTriGiamToiDa()) > 0) {
            giamGia = phieuGiamGia.getGiaTriGiamToiDa();
        }

        return giamGia;
    }

    @Transactional
    public DonHang confirmOrder(String diaChiGiaoHang, String soDienThoaiKhachHang, String ghiChu,
                                String phuongThucVanChuyen, BigDecimal phiVanChuyen,
                                UUID phuongThucThanhToanId, UUID phieuGiamGiaId, UUID tempOrderId) {
        NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new RuntimeException("Người dùng không tìm thấy"));

        DonHangTam donHangTam = null;
        if (tempOrderId != null) {
            donHangTam = donHangTamRepository.findById(tempOrderId)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng tạm không tìm thấy"));
        }

        List<ChiTietDonHang> items;
        try {
            items = objectMapper.readValue(
                    donHangTam != null ? donHangTam.getDanhSachSanPham() : "[]",
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChiTietDonHang.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi đọc danh sách sản phẩm");
        }

        for (ChiTietDonHang item : items) {
            ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(item.getChiTietSanPham().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tìm thấy"));
            if (chiTietSanPham.getSoLuongTonKho() < item.getSoLuong()) {
                throw new RuntimeException("Số lượng tồn kho không đủ cho sản phẩm: " + chiTietSanPham.getSanPham().getTenSanPham());
            }
            chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() - item.getSoLuong());
            chiTietSanPhamRepository.save(chiTietSanPham);
        }

        PhuongThucThanhToan phuongThucThanhToan = phuongThucThanhToanRepository.findById(phuongThucThanhToanId)
                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không hợp lệ"));

        BigDecimal giamGia = BigDecimal.ZERO;
        PhieuGiamGia phieuGiamGia = null;
        if (phieuGiamGiaId != null) {
            PhieuGiamGiaCuaNguoiDung voucher = phieuGiamGiaCuaNguoiDungRepository.findById(phieuGiamGiaId)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));
            phieuGiamGia = voucher.getPhieuGiamGia();
            giamGia = applyVoucher(phieuGiamGiaId.toString(), null, calculateTotal(items, BigDecimal.ZERO, null));
            voucher.setSoLuotConLai(voucher.getSoLuotConLai() - 1);
            phieuGiamGiaCuaNguoiDungRepository.save(voucher);
        }

        DonHang donHang = new DonHang();
        donHang.setId(UUID.randomUUID());
        donHang.setNguoiDung(nguoiDung);
        donHang.setMaDonHang("DH-" + System.currentTimeMillis());
        donHang.setTrangThaiThanhToan(true);
        donHang.setPhiVanChuyen(phiVanChuyen);
        donHang.setPhuongThucThanhToan(phuongThucThanhToan);
        donHang.setSoTienKhachDua(calculateTotal(items, phiVanChuyen, giamGia));
        donHang.setThoiGianThanhToan(LocalDateTime.now());
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTienGiam(giamGia);
        donHang.setTongTien(calculateTotal(items, phiVanChuyen, giamGia));
        donHang.setPhuongThucBanHang("Giao hàng");
        donHang.setDiaChiGiaoHang(diaChiGiaoHang);

        for (ChiTietDonHang item : items) {
            item.setId(UUID.randomUUID());
            item.setDonHang(donHang);
            donHang.addChiTietDonHang(item);
        }

        donHangRepository.save(donHang);

        if (donHangTam != null) {
            donHangTamRepository.delete(donHangTam);
        }

        return donHang;
    }

    private BigDecimal calculateTotal(List<ChiTietDonHang> items, BigDecimal phiVanChuyen, BigDecimal giamGia) {
        BigDecimal total = items.stream()
                .map(ChiTietDonHang::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total = total.add(phiVanChuyen != null ? phiVanChuyen : BigDecimal.ZERO);
        total = total.subtract(giamGia != null ? giamGia : BigDecimal.ZERO);
        return total;
    }
}