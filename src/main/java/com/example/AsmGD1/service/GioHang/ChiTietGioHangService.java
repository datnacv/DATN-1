package com.example.AsmGD1.service.GioHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChiTietGioHangService {

    @Autowired
    private ChiTietGioHangRepository chiTietGioHangRepository;

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    public ChiTietGioHang updateSoLuong(UUID chiTietGioHangId, Integer soLuongMoi) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findById(chiTietGioHangId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại với ID: " + chiTietGioHangId));

        ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();
        int khoChenhLech = soLuongMoi - chiTiet.getSoLuong();

        if (chiTietSanPham.getSoLuongTonKho() < khoChenhLech) {
            throw new RuntimeException("Số lượng tồn kho không đủ: " + chiTietSanPham.getSoLuongTonKho());
        }

        chiTiet.setSoLuong(soLuongMoi);
        chiTiet = chiTietGioHangRepository.save(chiTiet);

        chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() - khoChenhLech);
        chiTietSanPhamRepository.save(chiTietSanPham);

        GioHang gioHang = gioHangRepository.findById(chiTiet.getGioHang().getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        BigDecimal giaMoi = chiTiet.getGia().multiply(BigDecimal.valueOf(soLuongMoi)).subtract(chiTiet.getTienGiam());
        BigDecimal giaCu = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong() - khoChenhLech)).subtract(chiTiet.getTienGiam());
        gioHang.setTongTien(gioHang.getTongTien().add(giaMoi).subtract(giaCu));
        gioHangRepository.save(gioHang);

        return chiTiet;
    }

    public ChiTietGioHang applyDiscount(UUID chiTietGioHangId, BigDecimal tienGiam) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findById(chiTietGioHangId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại với ID: " + chiTietGioHangId));

        BigDecimal maxGiam = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong()));
        if (tienGiam.compareTo(maxGiam) > 0) {
            throw new RuntimeException("Số tiền giảm giá không được vượt quá: " + maxGiam);
        }

        chiTiet.setTienGiam(tienGiam);
        chiTiet = chiTietGioHangRepository.save(chiTiet);

        GioHang gioHang = gioHangRepository.findById(chiTiet.getGioHang().getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        BigDecimal giaMoi = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong())).subtract(tienGiam);
        BigDecimal giaCu = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong())).subtract(chiTiet.getTienGiam().add(tienGiam.negate()));
        gioHang.setTongTien(gioHang.getTongTien().add(giaMoi).subtract(giaCu));
        gioHangRepository.save(gioHang);

        return chiTiet;
    }

    public void removeChiTietGioHang(UUID gioHangId, UUID chiTietSanPhamId) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findByGioHangIdAndChiTietSanPhamId(gioHangId, chiTietSanPhamId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại"));
        GioHang gioHang = gioHangRepository.findById(gioHangId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();

        chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() + chiTiet.getSoLuong());
        chiTietSanPhamRepository.save(chiTietSanPham);

        BigDecimal tongTienGiam = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong())).subtract(chiTiet.getTienGiam());
        gioHang.setTongTien(gioHang.getTongTien().subtract(tongTienGiam));
        gioHangRepository.save(gioHang);

        chiTietGioHangRepository.delete(chiTiet);
    }

    public Optional<ChiTietGioHang> getChiTietGioHang(UUID chiTietGioHangId) {
        return chiTietGioHangRepository.findById(chiTietGioHangId);
    }

    // ✅ THÊM MỚI: Lấy danh sách chi tiết giỏ hàng theo ID giỏ hàng
    public List<ChiTietGioHang> getGioHangChiTietList(UUID gioHangId) {
        return chiTietGioHangRepository.findByGioHangId(gioHangId);
    }

    // ✅ THÊM MỚI: Xóa toàn bộ chi tiết giỏ hàng theo ID giỏ hàng
    public void clearGioHang(UUID gioHangId) {
        chiTietGioHangRepository.deleteByGioHang_Id(gioHangId);
    }
}
