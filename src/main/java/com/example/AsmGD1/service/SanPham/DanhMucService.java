package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.repository.SanPham.DanhMucRepository;
import com.example.AsmGD1.repository.SanPham.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DanhMucService {
    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    public List<DanhMuc> getAllDanhMuc() {
        return danhMucRepository.findAll();
    }

    public List<DanhMuc> searchDanhMuc(String tenDanhMuc) {
        return danhMucRepository.findByTenDanhMucContainingIgnoreCase(tenDanhMuc);
    }

    public DanhMuc getDanhMucById(UUID id) {
        return danhMucRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DanhMuc not found with id: " + id));
    }

    public DanhMuc saveDanhMuc(DanhMuc danhMuc) throws IllegalArgumentException {
        if (danhMuc.getTenDanhMuc() == null || danhMuc.getTenDanhMuc().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }
        if (danhMucRepository.findByTenDanhMucContainingIgnoreCase(danhMuc.getTenDanhMuc())
                .stream()
                .anyMatch(d -> !d.getId().equals(danhMuc.getId()) && d.getTenDanhMuc().equalsIgnoreCase(danhMuc.getTenDanhMuc()))) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }
        return danhMucRepository.save(danhMuc);
    }

    public void deleteDanhMuc(UUID id) throws IllegalStateException {
        if (sanPhamRepository.existsByDanhMucId(id)) {
            throw new IllegalStateException("Không thể xóa danh mục vì đang có sản phẩm tham chiếu đến");
        }
        danhMucRepository.deleteById(id);
    }
}