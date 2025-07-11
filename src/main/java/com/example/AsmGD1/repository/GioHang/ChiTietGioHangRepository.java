package com.example.AsmGD1.repository.GioHang;

import com.example.AsmGD1.entity.ChiTietGioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChiTietGioHangRepository extends JpaRepository<ChiTietGioHang, UUID> {
    List<ChiTietGioHang> findByGioHangId(UUID gioHangId);

    boolean existsByGioHangIdAndChiTietSanPhamId(UUID gioHangId, UUID chiTietSanPhamId);

    void deleteByGioHangIdAndChiTietSanPhamId(UUID gioHangId, UUID chiTietSanPhamId);

    Optional<ChiTietGioHang> findByGioHangIdAndChiTietSanPhamId(UUID gioHangId, UUID chiTietSanPhamId);
}