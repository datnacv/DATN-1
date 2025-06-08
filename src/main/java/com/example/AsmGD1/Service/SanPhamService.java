package com.example.AsmGD1.Service;

import com.example.AsmGD1.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SanPhamService {
    Page<SanPham> getPagedProducts(Pageable pageable);
    List<SanPham> getAll();
    Optional<SanPham> findById(UUID id);
    Page<SanPham> searchByTenOrMa(String keyword, Pageable pageable);

}
