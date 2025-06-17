package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.ChiTietSanPhamChienDichGiamGia;
import com.example.AsmGD1.entity.ChienDichGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ChiTietSanPhamChienDichGiamGiaRepository extends JpaRepository<ChiTietSanPhamChienDichGiamGia, UUID> {

    // Tìm theo đối tượng ChienDichGiamGia
    List<ChiTietSanPhamChienDichGiamGia> findByChienDichGiamGia(ChienDichGiamGia chienDichGiamGia);

    // Tìm theo ID của chiến dịch giảm giá
    List<ChiTietSanPhamChienDichGiamGia> findByChienDichGiamGiaId(UUID chienDichId);

    // Xóa theo đối tượng ChienDichGiamGia
    @Transactional
    void deleteByChienDichGiamGia(ChienDichGiamGia chienDichGiamGia);

    // Xóa theo ID của chiến dịch giảm giá
    @Transactional
    void deleteByChienDichGiamGiaId(UUID chienDichId);
    @org.springframework.data.jpa.repository.Query("""
    SELECT c FROM ChiTietSanPhamChienDichGiamGia c
    JOIN FETCH c.chiTietSanPham ctsp
    JOIN FETCH ctsp.sanPham
    JOIN FETCH ctsp.mauSac
    JOIN FETCH ctsp.kichCo
    WHERE c.chienDichGiamGia.id = :chienDichId
""")
    List<ChiTietSanPhamChienDichGiamGia> findWithDetailsByChienDichId(UUID chienDichId);
    @Modifying
    @Query("DELETE FROM ChiTietSanPhamChienDichGiamGia c WHERE c.chienDichGiamGia.id = :chienDichId AND c.chiTietSanPham.id IN :ids")
    void deleteByChienDichGiamGiaIdAndChiTietSanPhamIds(@Param("chienDichId") UUID chienDichId,
                                                        @Param("ids") Set<UUID> ids);

}