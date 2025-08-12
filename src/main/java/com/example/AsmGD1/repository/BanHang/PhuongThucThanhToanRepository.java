package com.example.AsmGD1.repository.BanHang;

import com.example.AsmGD1.entity.PhuongThucThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhuongThucThanhToanRepository extends JpaRepository<PhuongThucThanhToan, UUID> {
    Optional<PhuongThucThanhToan> findByTenPhuongThuc(String tenPhuongThuc);
    // Lấy danh sách ID phương thức thanh toán đã gắn với 1 phiếu
    @Query(value = """
        SELECT pttt_id 
        FROM phieu_giam_gia_phuong_thuc_thanh_toan 
        WHERE phieu_id = :voucherId
        """, nativeQuery = true)
    List<UUID> findSelectedPtttIdsByVoucherId(UUID voucherId);

    // (tuỳ chọn) Lấy full entity theo voucher
    @Query(value = """
        SELECT pttt.* 
        FROM phuong_thuc_thanh_toan pttt
        JOIN phieu_giam_gia_phuong_thuc_thanh_toan l 
             ON l.pttt_id = pttt.id
        WHERE l.phieu_id = :voucherId
        """, nativeQuery = true)
    List<PhuongThucThanhToan> findSelectedPtttByVoucherId(UUID voucherId);
}