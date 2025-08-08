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

    @Query("""
        SELECT COALESCE(SUM(hd.tongTien), 0)
        FROM HoaDon hd
        WHERE hd.trangThai = 'HOAN_THANH'
          AND hd.ngayThanhToan BETWEEN :start AND :end
    """)
    BigDecimal tinhDoanhThuTheoHoaDon(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("""
        SELECT COUNT(hd.id)
        FROM HoaDon hd
        WHERE hd.trangThai = 'HOAN_THANH'
          AND hd.ngayThanhToan BETWEEN :start AND :end
    """)
    Integer demHoaDonTheoKhoangThoiGian(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query("""
        SELECT COALESCE(SUM(ct.soLuong), 0)
        FROM ChiTietDonHang ct
        JOIN ct.donHang dh
        JOIN HoaDon hd ON hd.donHang = dh
        WHERE hd.trangThai = 'HOAN_THANH'
          AND hd.ngayThanhToan BETWEEN :start AND :end
    """)
    Integer demSanPhamTheoHoaDon(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

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
        WHERE hd.trangThai = 'HOAN_THANH'
          AND hd.ngayThanhToan BETWEEN :start AND :end
        GROUP BY ctp.id, sp.id, sp.tenSanPham, ms.tenMau, kc.ten
        ORDER BY SUM(ct.soLuong) DESC
    """)
    List<SanPhamBanChayDTO> laySanPhamBanChayTheoHoaDon(@Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

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

    // ======= 3 hàm theo NGÀY dùng native SQL Server để tránh lỗi 'date' =======

    @Query(value = """
        SELECT DISTINCT CONVERT(date, hd.ngay_thanh_toan) AS d
        FROM hoa_don hd
        WHERE hd.trang_thai = 'HOAN_THANH'
          AND hd.ngay_thanh_toan BETWEEN :start AND :end
        ORDER BY CONVERT(date, hd.ngay_thanh_toan)
    """, nativeQuery = true)
    List<LocalDate> layNhanBieuDoTheoHoaDon(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT CAST(COUNT(*) AS int)
        FROM hoa_don hd
        WHERE hd.trang_thai = 'HOAN_THANH'
          AND CONVERT(date, hd.ngay_thanh_toan) = :date
    """, nativeQuery = true)
    Integer laySoHoaDonTheoNgay(@Param("date") LocalDate date);

    @Query(value = """
        SELECT COALESCE(SUM(ct.so_luong), 0)
        FROM chi_tiet_don_hang ct
        JOIN don_hang dh ON dh.id = ct.id_don_hang
        JOIN hoa_don  hd ON hd.id_don_hang = dh.id
        WHERE hd.trang_thai = 'HOAN_THANH'
          AND CONVERT(date, hd.ngay_thanh_toan) = :date
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
                                                        @Param("end") LocalDateTime end);
}
