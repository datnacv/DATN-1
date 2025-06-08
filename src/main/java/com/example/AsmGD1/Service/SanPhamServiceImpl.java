package com.example.AsmGD1.Service;

import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SanPhamServiceImpl implements SanPhamService {

    private final SanPhamRepository sanPhamRepository;

    @Override
    public Page<SanPham> getPagedProducts(Pageable pageable) {
        return sanPhamRepository.findAll(pageable);
    }

    @Override
    public List<SanPham> getAll() {
        return sanPhamRepository.findAll();
    }

    @Override
    public Optional<SanPham> findById(UUID id) {
        return sanPhamRepository.findById(id);
    }
    @Override
    public Page<SanPham> searchByTenOrMa(String keyword, Pageable pageable) {
        return sanPhamRepository.findByTenSanPhamContainingIgnoreCaseOrMaSanPhamContainingIgnoreCase(keyword, keyword, pageable);
    }

}
