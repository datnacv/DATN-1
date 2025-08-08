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

    // ---- TỔNG DOANH THU (Hoàn thành) theo THỜI ĐIỂM HOÀN THÀNH trong khoảng [start, end)
    @Query(value = """
        SELECT COALESCE(SUM(hd.tong_tien), 0)
        FROM hoa_don hd
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE ls.thoi_gian >= :start
          AND ls.thoi_gian <  :end
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
    """, nativeQuery = true)
    BigDecimal tinhDoanhThuTheoHoaDon(@Param("start") LocalDateTime start,
                                      @Param("end")   LocalDateTime end);

    // ---- ĐẾM HÓA ĐƠN (Hoàn thành) theo THỜI ĐIỂM HOÀN THÀNH trong khoảng [start, end)
    @Query(value = """
        SELECT CAST(COUNT(DISTINCT hd.id) AS int)
        FROM hoa_don hd
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE ls.thoi_gian >= :start
          AND ls.thoi_gian <  :end
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
    """, nativeQuery = true)
    Integer demHoaDonTheoKhoangThoiGian(@Param("start") LocalDateTime start,
                                        @Param("end")   LocalDateTime end);

    // ---- ĐẾM SẢN PHẨM THEO HÓA ĐƠN (Hoàn thành) theo THỜI ĐIỂM HOÀN THÀNH trong khoảng [start, end)
    @Query(value = """
        SELECT COALESCE(SUM(ct.so_luong), 0)
        FROM chi_tiet_don_hang ct
        JOIN don_hang dh ON dh.id = ct.id_don_hang
        JOIN hoa_don  hd ON hd.id_don_hang = dh.id
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE ls.thoi_gian >= :start
          AND ls.thoi_gian <  :end
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
    """, nativeQuery = true)
    Integer demSanPhamTheoHoaDon(@Param("start") LocalDateTime start,
                                 @Param("end")   LocalDateTime end);

    // ---- TOP SẢN PHẨM BÁN CHẠY (JPQL) — CHỈ đổi bộ lọc thời gian theo lịch sử hoàn thành
    @Query("""
        SELECT new com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO(
            ctp.id,
            sp.id,
            sp.tenSanPham,
            COALESCE(ms.tenMau, 'Không xác định'),
            COALESCE(kc.ten, 'Không xác định'),
            CASE WHEN SUM(ct.soLuong) > 0
                 THEN COALESCE(SUM(ct.thanhTien), 0) / SUM(ct.soLuong)
                 ELSE 0 END,
            SUM(ct.soLuong)
        )
        FROM ChiTietDonHang ct
        JOIN ct.chiTietSanPham ctp
        JOIN ctp.sanPham sp
        LEFT JOIN ctp.mauSac ms
        LEFT JOIN ctp.kichCo kc
        JOIN ct.donHang dh
        JOIN HoaDon hd ON hd.donHang = dh
        JOIN LichSuHoaDon ls ON ls.hoaDon = hd
        WHERE ls.trangThai = 'Hoàn thành'
          AND ls.thoiGian >= :start
          AND ls.thoiGian <  :end
        GROUP BY ctp.id, sp.id, sp.tenSanPham, ms.tenMau, kc.ten
        ORDER BY SUM(ct.soLuong) DESC
    """)
    List<SanPhamBanChayDTO> laySanPhamBanChayTheoHoaDon(@Param("start") LocalDateTime start,
                                                        @Param("end")   LocalDateTime end);

    // ---- SẢN PHẨM TỒN KHO THẤP (JPQL) — GIỮ NGUYÊN (không liên quan thời gian)
    @Query("""
        SELECT new com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO(
            NULL,
            pd.id,
            p.id,
            p.tenSanPham,
            COALESCE(ms.tenMau, 'Không xác định'),
            COALESCE(kc.ten, 'Không xác định'),
            COALESCE(pd.gia, 0),
            pd.soLuongTonKho
        )
        FROM ChiTietSanPham pd
        JOIN pd.sanPham p
        LEFT JOIN pd.mauSac ms
        LEFT JOIN pd.kichCo kc
        WHERE pd.soLuongTonKho < :threshold
        ORDER BY pd.soLuongTonKho ASC, p.tenSanPham ASC
    """)
    List<SanPhamTonKhoThapDTO> laySanPhamTonKhoThap(@Param("threshold") int threshold);

    // ---- NHÃN TRỤC NGÀY cho biểu đồ (Hoàn thành) theo THỜI ĐIỂM HOÀN THÀNH
    @Query(value = """
        SELECT DISTINCT CAST(ls.thoi_gian AS date) AS d
        FROM lich_su_hoa_don ls
        WHERE ls.thoi_gian >= :start
          AND ls.thoi_gian <  :end
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
        ORDER BY d
    """, nativeQuery = true)
    List<LocalDate> layNhanBieuDoTheoHoaDon(@Param("start") LocalDateTime start,
                                            @Param("end")   LocalDateTime end);

    // ---- SỐ HĐ THEO NGÀY CỤ THỂ (Hoàn thành) theo THỜI ĐIỂM HOÀN THÀNH
    @Query(value = """
        SELECT CAST(COUNT(DISTINCT hd.id) AS int)
        FROM hoa_don hd
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE CAST(ls.thoi_gian AS date) = :date
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
    """, nativeQuery = true)
    Integer laySoHoaDonTheoNgay(@Param("date") LocalDate date);

    // ---- SỐ SẢN PHẨM THEO NGÀY CỤ THỂ (Hoàn thành) theo THỜI ĐIỂM HOÀN THÀNH
    @Query(value = """
        SELECT COALESCE(SUM(ct.so_luong), 0)
        FROM chi_tiet_don_hang ct
        JOIN don_hang dh ON dh.id = ct.id_don_hang
        JOIN hoa_don  hd ON hd.id_don_hang = dh.id
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE CAST(ls.thoi_gian AS date) = :date
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
    """, nativeQuery = true)
    Integer laySoSanPhamTheoNgay(@Param("date") LocalDate date);

    // ---- PHẦN TRĂM TẤT CẢ TRẠNG THÁI DỰA TRÊN LỊCH SỬ (JPQL) — GIỮ NGUYÊN
    @Query("""
        SELECT ls.trangThai, COUNT(ls)
        FROM LichSuHoaDon ls
        JOIN ls.hoaDon hd
        WHERE ls.thoiGian BETWEEN :start AND :end
          AND ls.trangThai IS NOT NULL
        GROUP BY ls.trangThai
    """)
    List<Object[]> thongKePhanTramTatCaTrangThaiDonHang(@Param("start") LocalDateTime start,
                                                        @Param("end")   LocalDateTime end);

    // ---- TỔNG SỐ HÓA ĐƠN THEO TỪNG NGÀY (Hoàn thành) theo THỜI ĐIỂM HOÀN THÀNH
    @Query(value = """
        SELECT 
          CAST(ls.thoi_gian AS date) AS d,
          CAST(COUNT(DISTINCT hd.id) AS int) AS c,
          SUM(CAST(COUNT(DISTINCT hd.id) AS int)) OVER() AS total
        FROM hoa_don hd
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE ls.thoi_gian >= :start
          AND ls.thoi_gian <  :end
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
        GROUP BY CAST(ls.thoi_gian AS date)
        ORDER BY d
    """, nativeQuery = true)
    List<Object[]> thongKeSoHoaDonTheoNgay(@Param("start") LocalDateTime start,
                                           @Param("end")   LocalDateTime end);

    // ---- TỔNG DOANH THU THEO TỪNG NGÀY (Hoàn thành) theo THỜI ĐIỂM HOÀN THÀNH
    @Query(value = """
        SELECT CAST(ls.thoi_gian AS date) AS d,
               COALESCE(SUM(hd.tong_tien), 0)  AS s
        FROM hoa_don hd
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE ls.thoi_gian >= :start
          AND ls.thoi_gian <  :end
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
        GROUP BY CAST(ls.thoi_gian AS date)
        ORDER BY d
    """, nativeQuery = true)
    List<Object[]> thongKeDoanhThuTheoNgay(@Param("start") LocalDateTime start,
                                           @Param("end")   LocalDateTime end);
}
