package com.example.AsmGD1.Service;

import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.ChiTietSanPhamChienDichGiamGia;
import com.example.AsmGD1.entity.ChienDichGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChienDichGiamGiaService {
    void taoMoiChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham);

    Page<ChienDichGiamGia> locChienDich(String keyword, LocalDate startDate, LocalDate endDate,
                                        String status, String discountLevel, Pageable pageable);

    List<ChiTietSanPham> layChiTietTheoSanPham(UUID idSanPham);

    List<ChiTietSanPham> layChiTietDaChonTheoChienDich(UUID idChienDich);

    Optional<ChienDichGiamGia> timTheoId(UUID id);

    void capNhatChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham);

    void xoaChienDich(UUID id);
    List<ChiTietSanPhamChienDichGiamGia> layLienKetChiTietTheoChienDich(UUID idChienDich);
    boolean maDaTonTai(String ma, UUID excludeId);

}