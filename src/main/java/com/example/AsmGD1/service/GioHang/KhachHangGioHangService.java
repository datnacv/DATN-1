package com.example.AsmGD1.service.GioHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public GioHang getOrCreateGioHang(UUID nguoiDungId) {
        GioHang gioHang = gioHangRepository.findByNguoiDungId(nguoiDungId);
        if (gioHang == null) {
            gioHang = new GioHang();
            gioHang.setId(UUID.randomUUID());
            gioHang.setMaGioHang("GH" + UUID.randomUUID().toString().substring(0, 8));
            gioHang.setTongTien(BigDecimal.ZERO);
            gioHang.setThoiGianTao(LocalDateTime.now());
            gioHang.setTrangThai(true);
            // Gán nguoiDung dựa trên id từ Authentication
            NguoiDung nguoiDung = new NguoiDung();
            nguoiDung.setId(nguoiDungId); // Liên kết với người dùng hiện tại
            gioHang.setNguoiDung(nguoiDung);
            gioHang = gioHangRepository.save(gioHang);
        }
        return gioHang;
    }

    public ChiTietGioHang addToGioHang(UUID gioHangId, UUID chiTietSanPhamId, Integer soLuong) {
        GioHang gioHang = gioHangRepository.findById(gioHangId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại với ID: " + gioHangId));

        ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietSanPhamId)
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại với ID: " + chiTietSanPhamId));

        if (chiTietSanPham.getSoLuongTonKho() < soLuong) {
            throw new RuntimeException("Số lượng tồn kho không đủ: " + chiTietSanPham.getSoLuongTonKho());
        }

        Optional<ChiTietGioHang> existingChiTiet = chiTietGioHangRepository.findByGioHangIdAndChiTietSanPhamId(gioHangId, chiTietSanPhamId);
        if (existingChiTiet.isPresent()) {
            ChiTietGioHang chiTiet = existingChiTiet.get();
            int newQuantity = chiTiet.getSoLuong() + soLuong;
            if (chiTietSanPham.getSoLuongTonKho() + chiTiet.getSoLuong() < newQuantity) {
                throw new RuntimeException("Số lượng tồn kho không đủ sau khi cộng dồn");
            }
            chiTiet.setSoLuong(newQuantity);
            chiTietGioHangRepository.save(chiTiet);
            return chiTiet;
        }

        ChiTietGioHang chiTiet = new ChiTietGioHang();
        chiTiet.setId(UUID.randomUUID());
        chiTiet.setGioHang(gioHang);
        chiTiet.setChiTietSanPham(chiTietSanPham);
        chiTiet.setSoLuong(soLuong);
        chiTiet.setGia(chiTietSanPham.getGia());
        chiTiet.setTienGiam(BigDecimal.ZERO);
        chiTiet.setThoiGianThem(LocalDateTime.now());
        chiTiet.setTrangThai(true);
        chiTiet = chiTietGioHangRepository.save(chiTiet);

        BigDecimal tongTienMoi = gioHang.getTongTien().add(chiTietSanPham.getGia().multiply(BigDecimal.valueOf(soLuong)));
        gioHang.setTongTien(tongTienMoi);
        gioHangRepository.save(gioHang);

        chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() - soLuong);
        chiTietSanPhamRepository.save(chiTietSanPham);

        return chiTiet;
    }

    public List<ChiTietGioHang> getGioHangChiTiets(UUID gioHangId) {
        List<ChiTietGioHang> chiTiets = chiTietGioHangRepository.findByGioHangId(gioHangId);
        return chiTiets != null ? chiTiets : java.util.Collections.emptyList();
    }

    public void removeFromGioHang(UUID gioHangId, UUID chiTietSanPhamId) {
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
}