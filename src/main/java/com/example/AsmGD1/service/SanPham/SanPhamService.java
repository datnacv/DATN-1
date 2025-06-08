package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.repository.SanPham.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SanPhamService {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    public List<SanPham> findAll() {
        return sanPhamRepository.findAll();
    }

    public Page<SanPham> findAllPaginated(Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        return sanPhamRepository.findAll(sortedByThoiGianTaoDesc);
    }

    public SanPham findById(UUID id) {
        return sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SanPham not found with id: " + id));
    }

    public void save(SanPham sanPham) {
        sanPhamRepository.save(sanPham);
    }

    public void deleteById(UUID id) {
        sanPhamRepository.deleteById(id);
    }

    public List<SanPham> findByTenSanPhamContaining(String name) {
        return sanPhamRepository.findByTenSanPhamContainingIgnoreCase(name);
    }

    public Page<SanPham> findByTenSanPhamContaining(String searchName, Boolean trangThai, Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        if (trangThai != null) {
            return sanPhamRepository.findByTenSanPhamContainingIgnoreCaseAndTrangThai(searchName, trangThai, sortedByThoiGianTaoDesc);
        }
        return sanPhamRepository.findByTenSanPhamContainingIgnoreCase(searchName, sortedByThoiGianTaoDesc);
    }

    public Page<SanPham> findByTrangThai(Boolean trangThai, Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        return sanPhamRepository.findByTrangThai(trangThai, sortedByThoiGianTaoDesc);
    }
}