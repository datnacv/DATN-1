package com.example.AsmGD1.service.GioHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
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

    /**
     * Cập nhật số lượng sản phẩm trong chi tiết giỏ hàng
     * @param chiTietGioHangId ID của chi tiết giỏ hàng
     * @param soLuongMoi Số lượng mới
     * @return ChiTietGioHang đối tượng đã cập nhật
     * @throws RuntimeException nếu chi tiết giỏ hàng không tồn tại hoặc số lượng không đủ
     */
    public ChiTietGioHang updateSoLuong(UUID chiTietGioHangId, Integer soLuongMoi) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findById(chiTietGioHangId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại với ID: " + chiTietGioHangId));

        ChiTietSanPham chiTietSanPham = chiTiet.getChiTietSanPham();
        int khoChenhLech = soLuongMoi - chiTiet.getSoLuong();

        if (chiTietSanPham.getSoLuongTonKho() + chiTiet.getSoLuong() < soLuongMoi) {
            throw new RuntimeException("Số lượng tồn kho không đủ: " + chiTietSanPham.getSoLuongTonKho());
        }

        // Cập nhật số lượng trong chi tiết giỏ hàng
        chiTiet.setSoLuong(soLuongMoi);
        chiTiet = chiTietGioHangRepository.save(chiTiet);

        // Cập nhật số lượng tồn kho
        chiTietSanPham.setSoLuongTonKho(chiTietSanPham.getSoLuongTonKho() - khoChenhLech);
        chiTietSanPhamRepository.save(chiTietSanPham);

        // Cập nhật tổng tiền trong giỏ hàng
        GioHang gioHang = gioHangRepository.findById(chiTiet.getGioHang().getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        BigDecimal giaMoi = chiTiet.getGia().multiply(BigDecimal.valueOf(soLuongMoi)).subtract(chiTiet.getTienGiam());
        BigDecimal giaCu = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong())).subtract(chiTiet.getTienGiam());
        gioHang.setTongTien(gioHang.getTongTien().add(giaMoi).subtract(giaCu));
        gioHangRepository.save(gioHang);

        return chiTiet;
    }

    /**
     * Áp dụng mã giảm giá cho chi tiết giỏ hàng
     * @param chiTietGioHangId ID của chi tiết giỏ hàng
     * @param tienGiam Số tiền giảm giá
     * @return ChiTietGioHang đối tượng đã cập nhật
     * @throws RuntimeException nếu chi tiết giỏ hàng không tồn tại
     */
    public ChiTietGioHang applyDiscount(UUID chiTietGioHangId, BigDecimal tienGiam) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findById(chiTietGioHangId)
                .orElseThrow(() -> new RuntimeException("Chi tiết giỏ hàng không tồn tại với ID: " + chiTietGioHangId));

        if (tienGiam.compareTo(chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong()))) > 0) {
            throw new RuntimeException("Số tiền giảm giá không được vượt quá tổng giá sản phẩm");
        }

        chiTiet.setTienGiam(tienGiam);
        chiTiet = chiTietGioHangRepository.save(chiTiet);

        // Cập nhật tổng tiền trong giỏ hàng
        GioHang gioHang = gioHangRepository.findById(chiTiet.getGioHang().getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
        BigDecimal giaMoi = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong())).subtract(tienGiam);
        BigDecimal giaCu = chiTiet.getGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong())).subtract(chiTiet.getTienGiam().add(tienGiam.negate()));
        gioHang.setTongTien(gioHang.getTongTien().add(giaMoi).subtract(giaCu));
        gioHangRepository.save(gioHang);

        return chiTiet;
    }

    /**
     * Xóa chi tiết giỏ hàng
     * @param gioHangId ID của giỏ hàng
     * @param chiTietSanPhamId ID của chi tiết sản phẩm
     * @throws RuntimeException nếu chi tiết giỏ hàng không tồn tại
     */
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

        chiTietGioHangRepository.deleteByGioHangIdAndChiTietSanPhamId(gioHangId, chiTietSanPhamId);
    }

    /**
     * Lấy thông tin chi tiết giỏ hàng
     * @param chiTietGioHangId ID của chi tiết giỏ hàng
     * @return Optional chứa ChiTietGioHang nếu tìm thấy
     */
    public Optional<ChiTietGioHang> getChiTietGioHang(UUID chiTietGioHangId) {
        return chiTietGioHangRepository.findById(chiTietGioHangId);
    }
}