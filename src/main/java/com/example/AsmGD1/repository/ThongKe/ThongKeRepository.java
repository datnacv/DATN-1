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
WITH rev_ht AS (               -- tiền hàng gốc ở mốc Hoàn thành (không ship)
  SELECT
    dh.id AS id_don_hang,
    SUM(CAST(ct.so_luong AS decimal(18,2)) * CAST(ct.gia AS decimal(18,2))) AS merch,
    COALESCE(SUM(CAST(v.gia_tri_giam AS decimal(18,2))),0) AS order_disc
  FROM lich_su_hoa_don ls
  JOIN hoa_don hd ON hd.id = ls.id_hoa_don
  JOIN don_hang dh ON dh.id = hd.id_don_hang
  JOIN chi_tiet_don_hang ct ON ct.id_don_hang = dh.id
  LEFT JOIN don_hang_phieu_giam_gia v
    ON v.id_don_hang = dh.id AND UPPER(v.loai_giam_gia) = N'ORDER'
  WHERE ls.thoi_gian >= :start
    AND ls.thoi_gian <  :end
    AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8,N'_',N''),N' ',N'')) = N'HOANTHANH'
    AND (ct.trang_thai_hoan_tra = 0 OR ct.trang_thai_hoan_tra IS NULL)
    AND (hd.trang_thai IS NULL OR hd.trang_thai <> N'Hủy đơn hàng')
    AND NOT EXISTS (                         -- loại mọi hóa đơn có lịch sử ĐÃ TRẢ HÀNG
      SELECT 1 FROM lich_su_hoa_don ls2
      WHERE ls2.id_hoa_don = hd.id
        AND UPPER(REPLACE(REPLACE(ls2.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8,N'_',N''),N' ',N'')) = N'DATRAHANG'
    )
  GROUP BY dh.id
),
rev_ddh AS (              -- phần chênh lệch ở mốc ĐÃ ĐỔI HÀNG
  SELECT
    lsdsp.id_hoa_don AS id_hd,
    SUM(
      (CAST(ctsp_new.gia AS decimal(18,2)) - CAST(ctdh.gia AS decimal(18,2)))
      * CAST(lsdsp.so_luong AS decimal(18,2))
    ) AS diff
  FROM lich_su_hoa_don ls
  JOIN hoa_don hd ON hd.id = ls.id_hoa_don
  JOIN lich_su_doi_san_pham lsdsp ON lsdsp.id_hoa_don = hd.id
  JOIN chi_tiet_don_hang ctdh ON ctdh.id = lsdsp.id_chi_tiet_don_hang
  JOIN chi_tiet_san_pham ctsp_new ON ctsp_new.id = lsdsp.id_chi_tiet_san_pham_thay_the
  WHERE ls.thoi_gian >= :start
    AND ls.thoi_gian <  :end
    AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8,N'_',N''),N' ',N'')) = N'DADOIHANG'
    AND UPPER(REPLACE(REPLACE(lsdsp.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8,N'_',N''),N' ',N'')) IN (N'DAXACNHAN', N'DAXACNHANH')
    AND NOT EXISTS (                         -- nếu hóa đơn từng TRẢ HÀNG thì bỏ luôn chênh lệch
      SELECT 1 FROM lich_su_hoa_don ls2
      WHERE ls2.id_hoa_don = hd.id
        AND UPPER(REPLACE(REPLACE(ls2.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8,N'_',N''),N' ',N'')) = N'DATRAHANG'
    )
  GROUP BY lsdsp.id_hoa_don
)
SELECT
  COALESCE((SELECT SUM(merch - order_disc) FROM rev_ht), 0)
  + COALESCE((SELECT SUM(diff) FROM rev_ddh), 0)
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
  AND (ct.trang_thai_hoan_tra = 0 OR ct.trang_thai_hoan_tra IS NULL)
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
      AND (ct.trangThaiHoanTra IS NULL OR ct.trangThaiHoanTra = false)
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
  AND (ct.trang_thai_hoan_tra = 0 OR ct.trang_thai_hoan_tra IS NULL)
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
WITH rev_ht AS (
  SELECT
    CAST(ls.thoi_gian AS date) AS d,
    SUM(CAST(ct.so_luong AS decimal(18,2)) * CAST(ct.gia AS decimal(18,2))) AS merch,
    COALESCE(SUM(CAST(v.gia_tri_giam AS decimal(18,2))),0) AS order_disc
  FROM lich_su_hoa_don ls
  JOIN hoa_don hd ON hd.id = ls.id_hoa_don
  JOIN don_hang dh ON dh.id = hd.id_don_hang
  JOIN chi_tiet_don_hang ct ON ct.id_don_hang = dh.id
  LEFT JOIN don_hang_phieu_giam_gia v
    ON v.id_don_hang = dh.id AND UPPER(v.loai_giam_gia) = N'ORDER'
  WHERE ls.thoi_gian >= :start
    AND ls.thoi_gian <  :end
    AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8,N'_',N''),N' ',N'')) = N'HOANTHANH'
  GROUP BY CAST(ls.thoi_gian AS date)
),
rev_ddh AS (
  SELECT
    CAST(ls.thoi_gian AS date) AS d,
    SUM(
      (CAST(ctsp_new.gia AS decimal(18,2)) - CAST(ctdh.gia AS decimal(18,2)))
      * CAST(lsdsp.so_luong AS decimal(18,2))
    ) AS diff
  FROM lich_su_hoa_don ls
  JOIN hoa_don hd ON hd.id = ls.id_hoa_don
  JOIN lich_su_doi_san_pham lsdsp ON lsdsp.id_hoa_don = hd.id
  JOIN chi_tiet_don_hang ctdh ON ctdh.id = lsdsp.id_chi_tiet_don_hang
  JOIN chi_tiet_san_pham ctsp_new ON ctsp_new.id = lsdsp.id_chi_tiet_san_pham_thay_the
  WHERE ls.thoi_gian >= :start
    AND ls.thoi_gian <  :end
    AND UPPER(REPLACE(REPLACE(ls.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8,N'_',N''),N' ',N'')) = N'DADOIHANG'
    AND UPPER(REPLACE(REPLACE(lsdsp.trang_thai COLLATE Vietnamese_100_CI_AI_SC_UTF8,N'_',N''),N' ',N'')) IN (N'DAXACNHAN', N'DAXACNHANH')
  GROUP BY CAST(ls.thoi_gian AS date)
)
SELECT d, SUM(s) AS s
FROM (
  SELECT d, SUM(merch - order_disc) AS s FROM rev_ht GROUP BY d
  UNION ALL
  SELECT d, SUM(diff)             AS s FROM rev_ddh GROUP BY d
) U
GROUP BY d
ORDER BY d
""", nativeQuery = true)
    List<Object[]> thongKeDoanhThuTheoNgay(@Param("start") LocalDateTime start,
                                           @Param("end")   LocalDateTime end);







}