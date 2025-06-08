package com.example.AsmGD1.repository;

import com.example.AsmGD1.entity.NguoiDung;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NguoiDungRepository extends JpaRepository<NguoiDung, UUID> {

    List<NguoiDung> findByVaiTro(String vaiTro);

    // Tìm kiếm KHÔNG phân trang
    @Query("SELECT u FROM NguoiDung u WHERE u.vaiTro = 'CUSTOMER' AND u.trangThai = true AND (" +
            "LOWER(u.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.soDienThoai) LIKE LOWER(CONCAT('%', :keyword, '%')) )")
    List<NguoiDung> searchByKeywordNoPaging(@Param("keyword") String keyword);

    // Tìm kiếm CÓ phân trang
    @Query("SELECT u FROM NguoiDung u WHERE u.vaiTro = 'CUSTOMER' AND u.trangThai = true AND (" +
            "LOWER(u.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.soDienThoai) LIKE LOWER(CONCAT('%', :keyword, '%')) )")
    Page<NguoiDung> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Lấy tất cả theo vai trò + trạng thái
    Page<NguoiDung> findByVaiTroAndTrangThaiTrue(String vaiTro, Pageable pageable);

    // Tìm theo họ tên + vai trò + trạng thái
    Page<NguoiDung> findByHoTenContainingIgnoreCaseAndVaiTroAndTrangThaiTrue(String hoTen, String vaiTro, Pageable pageable);
}
