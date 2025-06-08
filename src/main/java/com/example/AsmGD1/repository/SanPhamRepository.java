package com.example.AsmGD1.repository;

import com.example.AsmGD1.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SanPhamRepository extends JpaRepository<SanPham, UUID> {
    Page<SanPham> findByTenSanPhamContainingIgnoreCaseOrMaSanPhamContainingIgnoreCase(String ten, String ma, Pageable pageable);

}
