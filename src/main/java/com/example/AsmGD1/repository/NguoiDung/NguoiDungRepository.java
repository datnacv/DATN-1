package com.example.AsmGD1.repository.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface NguoiDungRepository extends JpaRepository<NguoiDung, UUID> {
    @Query("SELECT u FROM NguoiDung u WHERE u.vaiTro = :vaiTro AND (u.hoTen LIKE %:keyword% OR u.email LIKE %:keyword% OR u.soDienThoai LIKE %:keyword%)")
    Page<NguoiDung> findByVaiTroAndKeyword(String vaiTro, String keyword, Pageable pageable);
}
