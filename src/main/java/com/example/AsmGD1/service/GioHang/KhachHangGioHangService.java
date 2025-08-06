package com.example.AsmGD1.service.GioHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
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

        // Lấy ChiTietSanPham để kiểm tra tồn kho
        ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietSanPhamId)
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại với ID: " + chiTietSanPhamId));

        // Kiểm tra số lượng tồn kho
        if (chiTietSanPham.getSoLuongTonKho() < soLuong) {
            throw new RuntimeException("Số lượng tồn kho không đủ: " + chiTietSanPham.getSoLuongTonKho());
        }

        // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
        Optional<ChiTietGioHang> existingChiTiet = chiTietGioHangRepository.findByGioHangIdAndChiTietSanPhamId(gioHangId, chiTietSanPhamId);
        if (existingChiTiet.isPresent()) {
            ChiTietGioHang chiTiet = existingChiTiet.get();
            int newQuantity = chiTiet.getSoLuong() + soLuong;

            // Kiểm tra lại tồn kho cho số lượng mới
            if (chiTietSanPham.getSoLuongTonKho() < newQuantity) {
                throw new RuntimeException("Số lượng tồn kho không đủ sau khi cộng dồn: " + chiTietSanPham.getSoLuongTonKho());
            }

            chiTiet.setSoLuong(newQuantity);
            chiTietGioHangRepository.save(chiTiet);

            // Cập nhật tổng tiền giỏ hàng
            BigDecimal tongTienMoi = gioHang.getTongTien().add(chiTietSanPham.getGia().multiply(BigDecimal.valueOf(soLuong)));
            gioHang.setTongTien(tongTienMoi);
            gioHangRepository.save(gioHang);

            return chiTiet;
        }

        // Tạo mới ChiTietGioHang nếu sản phẩm chưa có trong giỏ
        ChiTietGioHang chiTiet = new ChiTietGioHang();
        chiTiet.setGioHang(gioHang);
        chiTiet.setChiTietSanPham(chiTietSanPham);
        chiTiet.setSoLuong(soLuong);
        chiTiet.setGia(chiTietSanPham.getGia());
        chiTiet.setTienGiam(BigDecimal.ZERO);
        chiTiet.setThoiGianThem(LocalDateTime.now());
        chiTiet.setTrangThai(true);
        chiTiet = chiTietGioHangRepository.save(chiTiet);

        // Cập nhật tổng tiền giỏ hàng
        BigDecimal tongTienMoi = gioHang.getTongTien().add(chiTietSanPham.getGia().multiply(BigDecimal.valueOf(soLuong)));
        gioHang.setTongTien(tongTienMoi);
        gioHangRepository.save(gioHang);

        return chiTiet;
    }

    public List<ChiTietGioHang> getGioHangChiTiets(UUID gioHangId) {
        List<ChiTietGioHang> chiTiets = chiTietGioHangRepository.findByGioHangIdWithHinhAnh(gioHangId);
        return chiTiets != null ? chiTiets : java.util.Collections.emptyList();
    }
}