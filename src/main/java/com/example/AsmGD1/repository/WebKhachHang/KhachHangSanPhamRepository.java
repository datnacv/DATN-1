package com.example.AsmGD1.repository.WebKhachHang;

import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KhachHangSanPhamRepository extends JpaRepository<SanPham, UUID> {
    // Web Khách hàng
    @Query("SELECT p FROM SanPham p JOIN p.danhMuc d WHERE p.trangThai = true")
    List<SanPham> findActiveProducts();

    @Query("SELECT c FROM ChiTietSanPham c " +
            "JOIN c.sanPham p " +
            "JOIN p.danhMuc d " +
            "JOIN c.kichCo k " +
            "JOIN c.mauSac m " +
            "JOIN c.chatLieu cl " +
            "JOIN c.xuatXu x " +
            "JOIN c.tayAo ta " +
            "JOIN c.coAo ca " +
            "JOIN c.kieuDang kd " +
            "JOIN c.thuongHieu th " +
            "WHERE p.id = :sanPhamId AND c.trangThai = true")
    List<ChiTietSanPham> findActiveProductDetailsBySanPhamId(UUID sanPhamId);

    @Query("SELECT h.urlHinhAnh FROM HinhAnhSanPham h WHERE h.chiTietSanPham.id = :chiTietSanPhamId")
    List<String> findProductImagesByChiTietSanPhamId(UUID chiTietSanPhamId);
}
