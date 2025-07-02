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
import java.util.UUID;

@Service("webKhachHangGioHangService")
public class KhachHangGioHangService {

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private ChiTietGioHangRepository chiTietGioHangRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    /**
     * Tạo hoặc lấy giỏ hàng cho người dùng
     * @param nguoiDungId ID của người dùng
     * @return Đối tượng GioHang
     */
    public GioHang getOrCreateGioHang(UUID nguoiDungId) {
        GioHang gioHang = gioHangRepository.findByNguoiDungId(nguoiDungId);
        if (gioHang == null) {
            gioHang = new GioHang();
            NguoiDung nguoiDung = new NguoiDung();
            nguoiDung.setId(nguoiDungId);
            gioHang.setNguoiDung(nguoiDung);
            gioHang.setMaGioHang("GH" + UUID.randomUUID().toString().substring(0, 8));
            gioHang.setTongTien(BigDecimal.ZERO);
            gioHang.setThoiGianTao(LocalDateTime.now());
            gioHang.setTrangThai(true);
            gioHang = gioHangRepository.save(gioHang);
        }
        return gioHang;
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     * @param gioHangId ID của giỏ hàng
     * @param chiTietSanPhamId ID của chi tiết sản phẩm
     * @param soLuong Số lượng sản phẩm
     * @return ChiTietGioHang đối tượng chi tiết vừa thêm
     * @throws RuntimeException nếu giỏ hàng hoặc sản phẩm không tồn tại
     */
    public ChiTietGioHang addToGioHang(UUID gioHangId, UUID chiTietSanPhamId, Integer soLuong) {
        GioHang gioHang = gioHangRepository.findById(gioHangId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại với ID: " + gioHangId));

        ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietSanPhamId)
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại với ID: " + chiTietSanPhamId));

        if (chiTietSanPham.getSoLuongTonKho() < soLuong) {
            throw new RuntimeException("Số lượng tồn kho không đủ: " + chiTietSanPham.getSoLuongTonKho());
        }

        if (chiTietGioHangRepository.existsByGioHangIdAndChiTietSanPhamId(gioHangId, chiTietSanPhamId)) {
            throw new RuntimeException("Sản phẩm đã tồn tại trong giỏ hàng");
        }

        ChiTietGioHang chiTiet = new ChiTietGioHang();
        chiTiet.setGioHang(gioHang);
        chiTiet.setChiTietSanPham(chiTietSanPham);
        chiTiet.setSoLuong(soLuong);
        chiTiet.setGia(chiTietSanPham.getGia());
        chiTiet.setTienGiam(BigDecimal.ZERO);
        chiTiet.setGhiChu(null);
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

    /**
     * Lấy danh sách chi tiết giỏ hàng
     * @param gioHangId ID của giỏ hàng
     * @return Danh sách ChiTietGioHang
     */
    public List<ChiTietGioHang> getGioHangChiTiets(UUID gioHangId) {
        return chiTietGioHangRepository.findByGioHangId(gioHangId);
    }

    /**
     * Xóa sản phẩm khỏi giỏ hàng
     * @param gioHangId ID của giỏ hàng
     * @param chiTietSanPhamId ID của chi tiết sản phẩm
     */
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

        chiTietGioHangRepository.deleteByGioHangIdAndChiTietSanPhamId(gioHangId, chiTietSanPhamId);
    }


}