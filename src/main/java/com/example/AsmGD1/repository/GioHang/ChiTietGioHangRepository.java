package com.example.AsmGD1.repository.GioHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChiTietGioHangRepository extends JpaRepository<ChiTietGioHang, UUID> {
    /**
     * Tìm danh sách chi tiết giỏ hàng dựa trên ID của giỏ hàng
     * @param gioHangId ID của giỏ hàng
     * @return Danh sách ChiTietGioHang
     */
    List<ChiTietGioHang> findByGioHangId(UUID gioHangId);

    /**
     * Kiểm tra xem chi tiết giỏ hàng có tồn tại dựa trên ID giỏ hàng và ID chi tiết sản phẩm
     * @param gioHangId ID của giỏ hàng
     * @param chiTietSanPhamId ID của chi tiết sản phẩm
     * @return true nếu tồn tại, false nếu không
     */
    boolean existsByGioHangIdAndChiTietSanPhamId(UUID gioHangId, UUID chiTietSanPhamId);

    /**
     * Xóa chi tiết giỏ hàng dựa trên ID giỏ hàng và ID chi tiết sản phẩm
     * @param gioHangId ID của giỏ hàng
     * @param chiTietSanPhamId ID của chi tiết sản phẩm
     */
    void deleteByGioHangIdAndChiTietSanPhamId(UUID gioHangId, UUID chiTietSanPhamId);

    /**
     * Tìm chi tiết giỏ hàng dựa trên ID giỏ hàng và ID chi tiết sản phẩm
     * @param gioHangId ID của giỏ hàng
     * @param chiTietSanPhamId ID của chi tiết sản phẩm
     * @return Optional chứa ChiTietGioHang nếu tìm thấy, hoặc empty nếu không
     */
    Optional<ChiTietGioHang> findByGioHangIdAndChiTietSanPhamId(UUID gioHangId, UUID chiTietSanPhamId);
}