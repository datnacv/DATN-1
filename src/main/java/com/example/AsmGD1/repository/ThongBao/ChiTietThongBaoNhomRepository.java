package com.example.AsmGD1.repository.ThongBao;

import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChiTietThongBaoNhomRepository extends JpaRepository<ChiTietThongBaoNhom, UUID> {
    List<ChiTietThongBaoNhom> findByNguoiDungIdOrderByThongBaoNhom_ThoiGianTaoDesc(UUID nguoiDungId);
    long countByNguoiDungIdAndDaXemFalse(UUID nguoiDungId);
    Optional<ChiTietThongBaoNhom> findByThongBaoNhom_IdAndNguoiDung_Id(UUID thongBaoId, UUID nguoiDungId);
}
