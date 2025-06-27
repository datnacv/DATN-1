package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.ChiTietSanPham;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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

    @Query("SELECT ct FROM ChiTietSanPham ct " +
            "WHERE ct.sanPham.id = :productId AND ct.mauSac.id = :mauSacId AND ct.kichCo.id = :kichCoId")
    ChiTietSanPham findBySanPhamIdAndMauSacIdAndKichCoId(
            @Param("productId") UUID productId,
            @Param("mauSacId") UUID mauSacId,
            @Param("kichCoId") UUID kichCoId);


    // hoadon
    @Query("SELECT c FROM ChiTietSanPham c WHERE c.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChiTietSanPham> findById(@Param("id") UUID id, LockModeType lockMode);

    Optional<ChiTietSanPham> findById(UUID id);
}