package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.KieuDang;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.KieuDangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KieuDangService {
    @Autowired
    private KieuDangRepository kieuDangRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Lấy danh sách kiểu dáng với phân trang
    public Page<KieuDang> getAllKieuDang(Pageable pageable) {
        return kieuDangRepository.findAll(pageable);
    }

    // Lấy tất cả kiểu dáng (không phân trang)
    public List<KieuDang> getAllKieuDang() {
        return kieuDangRepository.findAll();
    }

    // Tìm kiếm kiểu dáng với phân trang
    public Page<KieuDang> searchKieuDang(String tenKieuDang, Pageable pageable) {
        return kieuDangRepository.findByTenKieuDangContainingIgnoreCase(tenKieuDang, pageable);
    }

    public KieuDang getKieuDangById(UUID id) {
        return kieuDangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KieuDang not found with id: " + id));
    }

    public KieuDang saveKieuDang(KieuDang kieuDang) throws IllegalArgumentException {
        if (kieuDang.getTenKieuDang() == null || kieuDang.getTenKieuDang().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên kiểu dáng không được để trống");
        }
        if (kieuDangRepository.findByTenKieuDangContainingIgnoreCase(kieuDang.getTenKieuDang())
                .stream()
                .anyMatch(k -> !k.getId().equals(kieuDang.getId()) && k.getTenKieuDang().equalsIgnoreCase(kieuDang.getTenKieuDang()))) {
            throw new IllegalArgumentException("Tên kiểu dáng đã tồn tại");
        }
        return kieuDangRepository.save(kieuDang);
    }

    public void deleteKieuDang(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByKieuDangId(id)) {
            throw new IllegalStateException("Không thể xóa kiểu dáng vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        kieuDangRepository.deleteById(id);
    }
}