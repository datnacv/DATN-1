package com.example.AsmGD1.repository.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.entity.ThongKe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ThongKeRepository extends JpaRepository<ThongKe, UUID> {

    @Query("SELECT SUM(tk.doanhThu) " +
            "FROM ThongKe tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate")
    BigDecimal tinhDoanhThuTheoKhoangThoiGian(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(DISTINCT tk.idChiTietDonHang) " +
            "FROM ThongKe tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate")
    Integer demDonHangTheoKhoangThoiGian(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(tk.soLuongDaBan) " +
            "FROM ThongKe tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate")
    Integer demSanPhamTheoKhoangThoiGian(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT new com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO(" +
            "tk.idChiTietSanPham, tk.idSanPham, tk.tenSanPham, tk.mauSac, tk.kichCo, " +
            "CASE WHEN SUM(tk.soLuongDaBan) > 0 THEN SUM(tk.doanhThu) / SUM(tk.soLuongDaBan) ELSE 0 END, " +
            "SUM(tk.soLuongDaBan), tk.imageUrl) " +
            "FROM ThongKe tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate " +
            "GROUP BY tk.idChiTietSanPham, tk.idSanPham, tk.tenSanPham, tk.mauSac, tk.kichCo, tk.imageUrl " +
            "ORDER BY SUM(tk.soLuongDaBan) DESC")
    List<SanPhamBanChayDTO> laySanPhamBanChay(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);


    @Query("SELECT new com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO(" +
            "tk.idChiTietDonHang, tk.idChiTietSanPham, tk.idSanPham, tk.tenSanPham, tk.mauSac, tk.kichCo, " +
            "CASE WHEN tk.soLuongDaBan > 0 THEN tk.doanhThu / tk.soLuongDaBan ELSE 0 END, " +
            "tk.soLuongTonKho, tk.imageUrl) " +
            "FROM ThongKe tk " +
            "WHERE tk.soLuongTonKho < :threshold")
    List<SanPhamTonKhoThapDTO> laySanPhamTonKhoThap(@Param("threshold") int threshold);

    // ✅ Sửa lỗi chia cho 0 trong truy vấn phần trăm trạng thái đơn hàng
    @Query("""
        SELECT 
            CASE 
                WHEN (SELECT COUNT(ctdh2) FROM ChiTietDonHang ctdh2 
                      JOIN ctdh2.donHang dh2 
                      WHERE dh2.thoiGianTao BETWEEN :batDau AND :ketThuc) = 0 
                THEN 0.0 
                ELSE 
                    (COUNT(ctdh) * 1.0 / 
                    (SELECT COUNT(ctdh2) FROM ChiTietDonHang ctdh2 
                     JOIN ctdh2.donHang dh2 
                     WHERE dh2.thoiGianTao BETWEEN :batDau AND :ketThuc)) * 100 
            END
        FROM ChiTietDonHang ctdh 
        JOIN ctdh.donHang dh 
        WHERE ctdh.trangThaiHoanTra = :trangThai AND dh.thoiGianTao BETWEEN :batDau AND :ketThuc
    """)
    Double tinhPhanTramTrangThaiDonHang(@Param("batDau") LocalDateTime batDau,
                                        @Param("ketThuc") LocalDateTime ketThuc,
                                        @Param("trangThai") Boolean trangThai);

    @Query("SELECT tk.ngayThanhToan " +
            "FROM ThongKe tk " +
            "WHERE tk.ngayThanhToan BETWEEN :startDate AND :endDate " +
            "GROUP BY tk.ngayThanhToan " +
            "ORDER BY tk.ngayThanhToan")
    List<LocalDate> layNhanBieuDo(@Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(DISTINCT tk.idChiTietDonHang) " +
            "FROM ThongKe tk " +
            "WHERE tk.ngayThanhToan = :date")
    Integer layDonHangBieuDoTheoNgay(@Param("date") LocalDate date);

    @Query("SELECT SUM(tk.soLuongDaBan) " +
            "FROM ThongKe tk " +
            "WHERE tk.ngayThanhToan = :date")
    Integer laySanPhamBieuDoTheoNgay(@Param("date") LocalDate date);
}
