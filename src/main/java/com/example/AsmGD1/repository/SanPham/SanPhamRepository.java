package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, UUID> {

    @Query("SELECT sp FROM SanPham sp WHERE LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :searchName, '%')) AND (:trangThai IS NULL OR sp.trangThai = :trangThai)")
    Page<SanPham> findByTenSanPhamContainingIgnoreCaseAndTrangThai(@Param("searchName") String searchName, @Param("trangThai") Boolean trangThai, Pageable pageable);

    List<SanPham> findByTenSanPhamContainingIgnoreCase(String name);

    Page<SanPham> findByTenSanPhamContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT sp FROM SanPham sp WHERE sp.trangThai = :trangThai")
    Page<SanPham> findByTrangThai(@Param("trangThai") Boolean trangThai, Pageable pageable);

    Page<SanPham> findByTenSanPhamContainingIgnoreCaseOrMaSanPhamContainingIgnoreCase(String ten, String ma, Pageable pageable);

    @Query("SELECT sp FROM SanPham sp WHERE sp.trangThai = true")
    List<SanPham> findAllByTrangThai();

    boolean existsByDanhMucId(UUID danhMucId);

    @Query("""
    SELECT sp FROM SanPham sp
    WHERE sp.id <> :idSanPham
      AND sp.trangThai = true
      AND (
          sp.danhMuc.id = :idDanhMuc
          OR LOWER(sp.tenSanPham) LIKE %:tenSanPham%
      )
    ORDER BY sp.thoiGianTao DESC
    """)
    List<SanPham> findSanPhamLienQuan(
            @Param("idSanPham") UUID idSanPham,
            @Param("idDanhMuc") UUID idDanhMuc,
            @Param("tenSanPham") String tenSanPham,
            Pageable pageable
    );

}