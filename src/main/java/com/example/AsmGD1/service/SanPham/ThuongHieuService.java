package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.ThuongHieu;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.ThuongHieuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ThuongHieuService {
    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    public List<ThuongHieu> getAllThuongHieu() {
        return thuongHieuRepository.findAll();
    }

    public List<ThuongHieu> searchThuongHieu(String tenThuongHieu) {
        return thuongHieuRepository.findByTenThuongHieuContainingIgnoreCase(tenThuongHieu);
    }

    public ThuongHieu getThuongHieuById(UUID id) {
        return thuongHieuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ThuongHieu not found with id: " + id));
    }

    public ThuongHieu saveThuongHieu(ThuongHieu thuongHieu) throws IllegalArgumentException {
        if (thuongHieu.getTenThuongHieu() == null || thuongHieu.getTenThuongHieu().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống");
        }
        if (thuongHieuRepository.findByTenThuongHieuContainingIgnoreCase(thuongHieu.getTenThuongHieu())
                .stream()
                .anyMatch(t -> !t.getId().equals(thuongHieu.getId()) && t.getTenThuongHieu().equalsIgnoreCase(thuongHieu.getTenThuongHieu()))) {
            throw new IllegalArgumentException("Tên thương hiệu đã tồn tại");
        }
        return thuongHieuRepository.save(thuongHieu);
    }

    public void deleteThuongHieu(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByThuongHieuId(id)) {
            throw new IllegalStateException("Không thể xóa thương hiệu vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        thuongHieuRepository.deleteById(id);
    }
}