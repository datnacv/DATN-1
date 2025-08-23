package com.example.AsmGD1.repository.WebKhachHang;

import com.example.AsmGD1.entity.LichSuDoiSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LichSuDoiSanPhamRepository extends JpaRepository<LichSuDoiSanPham, UUID> {
    boolean existsByHoaDonId(UUID hoaDonId);
    boolean existsByHoaDonIdAndTrangThai(UUID hoaDonId, String trangThai);

    @Query("SELECT l FROM LichSuDoiSanPham l WHERE " +
            "(:maDonHang = '' OR l.hoaDon.donHang.maDonHang LIKE %:maDonHang%) AND " +
            "(:hoTen = '' OR l.hoaDon.donHang.nguoiDung.hoTen LIKE %:hoTen%)")
    Page<LichSuDoiSanPham> findByMaDonHangOrHoTen(@Param("maDonHang") String maDonHang,
                                                  @Param("hoTen") String hoTen,
                                                  Pageable pageable);

}