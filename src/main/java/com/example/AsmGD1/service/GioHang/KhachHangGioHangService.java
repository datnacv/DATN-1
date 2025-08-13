package com.example.AsmGD1.service.GioHang;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service("webKhachHangGioHangService")
public class KhachHangGioHangService {

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private ChiTietGioHangRepository chiTietGioHangRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private ChienDichGiamGiaService chienDichGiamGiaService;

    public GioHang getOrCreateGioHang(UUID nguoiDungId) {
        GioHang gioHang = gioHangRepository.findByNguoiDungId(nguoiDungId);
        if (gioHang == null) {
            gioHang = new GioHang();
//            gioHang.setId(UUID.randomUUID());
            gioHang.setMaGioHang("GH" + UUID.randomUUID().toString().substring(0, 8));
            gioHang.setTongTien(BigDecimal.ZERO);
            gioHang.setThoiGianTao(LocalDateTime.now());
            gioHang.setTrangThai(true);

            // Thay vì tạo đối tượng giả
            NguoiDung nguoiDung = nguoiDungRepository.findById(nguoiDungId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            gioHang.setNguoiDung(nguoiDung);

            gioHang = gioHangRepository.save(gioHang);
        }
        return gioHang;
    }


    @Transactional
    public ChiTietGioHang addToGioHang(UUID gioHangId, UUID chiTietSanPhamId, Integer soLuong) {
        GioHang gioHang = gioHangRepository.findById(gioHangId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại với ID: " + gioHangId));

        ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietSanPhamId)
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại với ID: " + chiTietSanPhamId));

        if (chiTietSanPham.getSoLuongTonKho() < soLuong) {
            throw new RuntimeException("Số lượng tồn kho không đủ: " + chiTietSanPham.getSoLuongTonKho());
        }

        // Kiểm tra chiến dịch giảm giá
        BigDecimal giaGiam = chiTietSanPham.getGia();
        BigDecimal tienGiam = BigDecimal.ZERO;
        Optional<ChienDichGiamGia> activeCampaign = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTietSanPhamId);
        if (activeCampaign.isPresent()) {
            ChienDichGiamGia campaign = activeCampaign.get();
            if (campaign.getPhanTramGiam() != null && campaign.getSoLuong() != null && campaign.getSoLuong() >= soLuong) {
                tienGiam = chiTietSanPham.getGia().multiply(campaign.getPhanTramGiam().divide(BigDecimal.valueOf(100)));
                giaGiam = chiTietSanPham.getGia().subtract(tienGiam);
                chienDichGiamGiaService.truSoLuong(campaign.getId(), soLuong);
            }
        }

        Optional<ChiTietGioHang> existingChiTiet = chiTietGioHangRepository.findByGioHangIdAndChiTietSanPhamId(gioHangId, chiTietSanPhamId);
        if (existingChiTiet.isPresent()) {
            ChiTietGioHang chiTiet = existingChiTiet.get();
            int newQuantity = chiTiet.getSoLuong() + soLuong;

            if (chiTietSanPham.getSoLuongTonKho() < newQuantity) {
                throw new RuntimeException("Số lượng tồn kho không đủ sau khi cộng dồn: " + chiTietSanPham.getSoLuongTonKho());
            }

            chiTiet.setSoLuong(newQuantity);
            chiTiet.setGia(giaGiam);
            chiTiet.setTienGiam(tienGiam.multiply(BigDecimal.valueOf(newQuantity)));
            chiTietGioHangRepository.save(chiTiet);

            BigDecimal tongTienMoi = gioHang.getTongTien().add(giaGiam.multiply(BigDecimal.valueOf(soLuong)));
            gioHang.setTongTien(tongTienMoi);
            gioHangRepository.save(gioHang);

            return chiTiet;
        }

        ChiTietGioHang chiTiet = new ChiTietGioHang();
        chiTiet.setGioHang(gioHang);
        chiTiet.setChiTietSanPham(chiTietSanPham);
        chiTiet.setSoLuong(soLuong);
        chiTiet.setGia(giaGiam);
        chiTiet.setTienGiam(tienGiam.multiply(BigDecimal.valueOf(soLuong)));
        chiTiet.setThoiGianThem(LocalDateTime.now());
        chiTiet.setTrangThai(true);
        chiTiet = chiTietGioHangRepository.save(chiTiet);

        BigDecimal tongTienMoi = gioHang.getTongTien().add(giaGiam.multiply(BigDecimal.valueOf(soLuong)));
        gioHang.setTongTien(tongTienMoi);
        gioHangRepository.save(gioHang);

        return chiTiet;
    }

    public List<ChiTietGioHang> getGioHangChiTiets(UUID gioHangId) {
        List<ChiTietGioHang> chiTiets = chiTietGioHangRepository.findByGioHangIdWithHinhAnh(gioHangId);
        return chiTiets != null ? chiTiets : java.util.Collections.emptyList();
    }
}