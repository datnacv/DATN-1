package com.example.AsmGD1.repository.HoaDon;

import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, UUID> {
    Optional<HoaDon> findByDonHangId(UUID donHangId);

    // Tìm hóa đơn theo mã đơn hàng
    HoaDon findByDonHang_MaDonHang(String maDonHang);

    // Tìm kiếm hóa đơn theo nhiều tiêu chí (mã đơn hàng, tên khách hàng, số điện thoại)
    @Query("SELECT h FROM HoaDon h JOIN h.donHang d JOIN h.nguoiDung n " +
            "WHERE UPPER(d.maDonHang) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
            "OR UPPER(n.hoTen) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
            "OR UPPER(n.soDienThoai) LIKE UPPER(CONCAT('%', :keyword, '%'))")
    Page<HoaDon> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);


    Optional<HoaDon> findByDonHang(DonHang donHang);

    @Query("SELECT h FROM HoaDon h WHERE UPPER(h.donHang.maDonHang) LIKE UPPER(CONCAT('%', :maDonHang, '%'))")
    Page<HoaDon> findByDonHangMaDonHangContainingIgnoreCase(@Param("value") String maDonHang, Pageable pageable);

    Optional<HoaDon> findByDonHangMaDonHang(String maDonHang);
}