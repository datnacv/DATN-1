package com.example.AsmGD1.service.GioHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.ChienDichGiamGia;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private ChienDichGiamGiaService chienDichGiamGiaService;

    public ChiTietGioHang updateSoLuong(UUID chiTietGioHangId, Integer soLuongMoi) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findById(chiTietGioHangId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại với ID: " + chiTietGioHangId));

        ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();
        int soLuongTonKho = chiTietSanPham.getSoLuongTonKho();

        if (soLuongMoi > soLuongTonKho) {
            throw new RuntimeException("Số lượng yêu cầu vượt quá tồn kho hiện tại: " + soLuongTonKho);
        }

        int soLuongCu = chiTiet.getSoLuong();

        // Cập nhật giảm giá
        BigDecimal giaGiam = chiTietSanPham.getGia();
        BigDecimal tienGiam = BigDecimal.ZERO;
        Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProduct(chiTietSanPham.getSanPham().getId());
        if (activeCampaign.isPresent()) {
            ChienDichGiamGia campaign = activeCampaign.get();
            if (campaign.getPhanTramGiam() != null && campaign.getSoLuong() != null && campaign.getSoLuong() >= soLuongMoi) {
                tienGiam = chiTietSanPham.getGia().multiply(campaign.getPhanTramGiam().divide(BigDecimal.valueOf(100)));
                giaGiam = chiTietSanPham.getGia().subtract(tienGiam);
                chienDichGiamGiaService.truSoLuong(campaign.getId(), soLuongMoi - soLuongCu);
            }
        }

        chiTiet.setSoLuong(soLuongMoi);
        chiTiet.setGia(giaGiam);
        chiTiet.setTienGiam(tienGiam.multiply(BigDecimal.valueOf(soLuongMoi)));
        chiTiet = chiTietGioHangRepository.save(chiTiet);

        BigDecimal thanhTienMoi = giaGiam.multiply(BigDecimal.valueOf(soLuongMoi));
        BigDecimal thanhTienCu = chiTiet.getGia().multiply(BigDecimal.valueOf(soLuongCu));

        GioHang gioHang = gioHangRepository.findById(chiTiet.getGioHang().getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        gioHang.setTongTien(gioHang.getTongTien().add(thanhTienMoi).subtract(thanhTienCu));
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

        // Cập nhật tổng tiền giỏ hàng
        BigDecimal tongTienGiam = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong())).subtract(chiTiet.getTienGiam());
        gioHang.setTongTien(gioHang.getTongTien().subtract(tongTienGiam));
        gioHangRepository.save(gioHang);

        // Xóa chi tiết giỏ hàng
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
    @Transactional
    public void clearGioHang(UUID gioHangId) {
        // Xóa tất cả chi tiết giỏ hàng
        chiTietGioHangRepository.deleteByGioHang_Id(gioHangId);

        // Cập nhật tổng tiền của giỏ hàng về 0
        GioHang gioHang = gioHangRepository.findById(gioHangId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        gioHang.setTongTien(BigDecimal.ZERO);
        gioHangRepository.save(gioHang);
    }
}
