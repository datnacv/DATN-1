package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, UUID> {
    List<ChiTietSanPham> findBySanPhamId(UUID productId);
}