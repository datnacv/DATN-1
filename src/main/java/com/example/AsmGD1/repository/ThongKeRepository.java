package com.example.AsmGD1.repository;

import com.example.AsmGD1.dto.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.entity.ThongKeDoanhThuChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThongKeRepository extends JpaRepository<ThongKeDoanhThuChiTiet, Long> {

    @Query("SELECT SUM(tk.doanhThu) " +
            "FROM ThongKeDoanhThuChiTiet tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate")
    BigDecimal tinhDoanhThuTheoKhoangThoiGian(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(DISTINCT tk.idHoaDon) " +
            "FROM ThongKeDoanhThuChiTiet tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate")
    Integer demDonHangTheoKhoangThoiGian(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(tk.soLuongDaBan) " +
            "FROM ThongKeDoanhThuChiTiet tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate")
    Integer demSanPhamTheoKhoangThoiGian(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT new com.example.AsmGD1.dto.SanPhamBanChayDTO(" +
            "tk.idChiTietSanPham, tk.tenSanPham, tk.mauSac, tk.kichCo, " +
            "CASE WHEN SUM(tk.soLuongDaBan) > 0 THEN SUM(tk.doanhThu) / SUM(tk.soLuongDaBan) ELSE 0 END, " +
            "SUM(tk.soLuongDaBan), tk.imageUrl) " +
            "FROM ThongKeDoanhThuChiTiet tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate " +
            "GROUP BY tk.idChiTietSanPham, tk.tenSanPham, tk.mauSac, tk.kichCo, tk.imageUrl " +
            "ORDER BY SUM(tk.soLuongDaBan) DESC")
    List<SanPhamBanChayDTO> laySanPhamBanChay(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT new com.example.AsmGD1.dto.SanPhamTonKhoThapDTO(" +
            "tk.idChiTietSanPham, tk.tenSanPham, tk.mauSac, tk.kichCo, " +
            "CASE WHEN tk.soLuongDaBan > 0 THEN tk.doanhThu / tk.soLuongDaBan ELSE 0 END, " +
            "tk.soLuongTonKho, tk.imageUrl) " +
            "FROM ThongKeDoanhThuChiTiet tk " +
            "WHERE tk.soLuongTonKho < :threshold")
    List<SanPhamTonKhoThapDTO> laySanPhamTonKhoThap(@Param("threshold") int threshold);

    @Query("SELECT COUNT(h) * 1.0 / (SELECT COUNT(h2) FROM HoaDon h2 WHERE h2.ngayTao BETWEEN :batDau AND :ketThuc) " +
            "FROM HoaDon h WHERE h.trangThai = :trangThai AND h.ngayTao BETWEEN :batDau AND :ketThuc")
    Double tinhPhanTramTrangThaiDonHang(@Param("batDau") LocalDateTime batDau,
                                        @Param("ketThuc") LocalDateTime ketThuc,
                                        @Param("trangThai") Boolean trangThai); // Đổi String -> Boolean



    @Query("SELECT tk.ngayThanhToan " +
            "FROM ThongKeDoanhThuChiTiet tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate " +
            "GROUP BY tk.ngayThanhToan " +
            "ORDER BY tk.ngayThanhToan")
    List<LocalDate> layNhanBieuDo(@Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(DISTINCT tk.idHoaDon) " +
            "FROM ThongKeDoanhThuChiTiet tk " +
            "WHERE tk.ngayThanhToan = :date")
    Integer layDonHangBieuDoTheoNgay(@Param("date") LocalDate date);

    @Query("SELECT SUM(tk.soLuongDaBan) " +
            "FROM ThongKeDoanhThuChiTiet tk " +
            "WHERE tk.ngayThanhToan = :date")
    Integer laySanPhamBieuDoTheoNgay(@Param("date") LocalDate date);
}
