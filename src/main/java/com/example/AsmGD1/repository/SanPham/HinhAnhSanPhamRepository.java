package com.example.AsmGD1.repository.SanPham;

import com.example.AsmGD1.entity.HinhAnhSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HinhAnhSanPhamRepository extends JpaRepository<HinhAnhSanPham, UUID> {

}