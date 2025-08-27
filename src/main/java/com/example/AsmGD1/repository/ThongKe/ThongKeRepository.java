package com.example.AsmGD1.repository.ThongKe;

import com.example.AsmGD1.dto.ThongKe.SanPhamBanChayDTO;
import com.example.AsmGD1.dto.ThongKe.SanPhamTonKhoThapDTO;
import com.example.AsmGD1.entity.ThongKe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = """
WITH completed_orders AS (
  SELECT DISTINCT hd.id        AS id_hoa_don,
                  dh.id        AS id_don_hang
  FROM hoa_don hd
  JOIN don_hang dh       ON dh.id = hd.id_don_hang
  JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
  WHERE ls.thoi_gian >= :start
    AND ls.thoi_gian <  :end
    AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
),
merch AS (
  SELECT co.id_don_hang,
         COALESCE(SUM(ct.so_luong * ct.gia), 0) AS total_merch
  FROM chi_tiet_don_hang ct
  JOIN completed_orders co ON co.id_don_hang = ct.id_don_hang
  GROUP BY co.id_don_hang
),
order_disc AS (
  SELECT co.id_don_hang,
         COALESCE(SUM(v.gia_tri_giam), 0) AS order_disc
  FROM don_hang_phieu_giam_gia v
  JOIN completed_orders co ON co.id_don_hang = v.id_don_hang
  WHERE UPPER(v.loai_giam_gia) = N'ORDER'
  GROUP BY co.id_don_hang
)
SELECT COALESCE(SUM(m.total_merch - COALESCE(od.order_disc, 0)), 0)
FROM merch m
LEFT JOIN order_disc od ON od.id_don_hang = m.id_don_hang
""", nativeQuery = true)
    BigDecimal tinhDoanhThuTheoHoaDon(@Param("start") LocalDateTime start,
                                      @Param("end")   LocalDateTime end);



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
    Page<SanPhamBanChayDTO> laySanPhamBanChayTheoHoaDon(@Param("start") LocalDateTime start,
                                                        @Param("end")   LocalDateTime end,
                                                        Pageable pageable);

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
    Page<SanPhamTonKhoThapDTO> laySanPhamTonKhoThap(@Param("threshold") int threshold, Pageable pageable);

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

    @Query(value = """
        SELECT CAST(COUNT(DISTINCT hd.id) AS int)
        FROM hoa_don hd
        JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
        WHERE CAST(ls.thoi_gian AS date) = :date
          AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
    """, nativeQuery = true)
    Integer laySoHoaDonTheoNgay(@Param("date") LocalDate date);

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

    @Query(value = """
WITH completed_orders AS (
  SELECT DISTINCT CAST(ls.thoi_gian AS date) AS d,
                  hd.id AS id_hoa_don,
                  dh.id AS id_don_hang
  FROM hoa_don hd
  JOIN don_hang dh       ON dh.id = hd.id_don_hang
  JOIN lich_su_hoa_don ls ON ls.id_hoa_don = hd.id
  WHERE ls.thoi_gian >= :start
    AND ls.thoi_gian <  :end
    AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8, N'_', N''), N' ', N'')) = N'HOANTHANH'
),
merch AS (
  SELECT co.d,
         co.id_don_hang,
         COALESCE(SUM(ct.so_luong * ct.gia), 0) AS total_merch
  FROM chi_tiet_don_hang ct
  JOIN completed_orders co ON co.id_don_hang = ct.id_don_hang
  GROUP BY co.d, co.id_don_hang
),
order_disc AS (
  SELECT co.d,
         co.id_don_hang,
         COALESCE(SUM(v.gia_tri_giam), 0) AS order_disc
  FROM don_hang_phieu_giam_gia v
  JOIN completed_orders co ON co.id_don_hang = v.id_don_hang
  WHERE UPPER(v.loai_giam_gia) = N'ORDER'
  GROUP BY co.d, co.id_don_hang
)
SELECT m.d,
       COALESCE(SUM(m.total_merch - COALESCE(od.order_disc, 0)), 0) AS s
FROM merch m
LEFT JOIN order_disc od
  ON od.id_don_hang = m.id_don_hang AND od.d = m.d
GROUP BY m.d
ORDER BY m.d
""", nativeQuery = true)
    List<Object[]> thongKeDoanhThuTheoNgay(@Param("start") LocalDateTime start,
                                           @Param("end")   LocalDateTime end);


}