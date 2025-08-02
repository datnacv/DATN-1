package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.MauSac;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.MauSacRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MauSacService {
    @Autowired
    private MauSacRepository mauSacRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Lấy danh sách màu sắc với phân trang
    public Page<MauSac> getAllMauSac(Pageable pageable) {
        return mauSacRepository.findAll(pageable);
    }

    // Lấy tất cả màu sắc (không phân trang)
    public List<MauSac> getAllMauSac() {
        return mauSacRepository.findAll();
    }

    // Tìm kiếm màu sắc với phân trang
    public Page<MauSac> searchMauSac(String tenMau, Pageable pageable) {
        return mauSacRepository.findByTenMauContainingIgnoreCase(tenMau, pageable);
    }

    public MauSac getMauSacById(UUID id) {
        return mauSacRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MauSac not found with id: " + id));
    }

    public MauSac saveMauSac(MauSac mauSac) throws IllegalArgumentException {
        if (mauSac.getTenMau() == null || mauSac.getTenMau().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên màu sắc không được để trống");
        }
        if (mauSacRepository.findByTenMauContainingIgnoreCase(mauSac.getTenMau())
                .stream()
                .anyMatch(m -> !m.getId().equals(mauSac.getId()) && m.getTenMau().equalsIgnoreCase(mauSac.getTenMau()))) {
            throw new IllegalArgumentException("Tên màu sắc đã tồn tại");
        }
        return mauSacRepository.save(mauSac);
    }

    public void deleteMauSac(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByMauSacId(id)) {
            throw new IllegalStateException("Không thể xóa màu sắc vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        mauSacRepository.deleteById(id);
    }
}