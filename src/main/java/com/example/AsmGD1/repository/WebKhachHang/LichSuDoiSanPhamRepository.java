package com.example.AsmGD1.repository.WebKhachHang;

import com.example.AsmGD1.entity.LichSuDoiSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LichSuDoiSanPhamRepository extends JpaRepository<LichSuDoiSanPham, UUID> {
    boolean existsByHoaDonId(UUID hoaDonId);
    boolean existsByHoaDonIdAndTrangThai(UUID hoaDonId, String trangThai);

}