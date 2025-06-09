package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, UUID> {
//    List<ChiTietSanPham> findBySanPhamId(UUID productId);

    @Query("SELECT ct FROM ChiTietSanPham ct " +
            "JOIN FETCH ct.sanPham sp " +
            "JOIN FETCH ct.kichCo kc " +
            "JOIN FETCH ct.mauSac ms " +
            "WHERE sp.id = :idSanPham")
    List<ChiTietSanPham> findBySanPhamId(@Param("idSanPham") UUID idSanPham);
}