package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.TayAo;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.TayAoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TayAoService {
    @Autowired
    private TayAoRepository tayAoRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Lấy danh sách tay áo với phân trang
    public Page<TayAo> getAllTayAo(Pageable pageable) {
        return tayAoRepository.findAll(pageable);
    }

    // Lấy tất cả tay áo (không phân trang)
    public List<TayAo> getAllTayAo() {
        return tayAoRepository.findAll();
    }

    // Tìm kiếm tay áo với phân trang
    public Page<TayAo> searchTayAo(String tenTayAo, Pageable pageable) {
        return tayAoRepository.findByTenTayAoContainingIgnoreCase(tenTayAo, pageable);
    }

    public TayAo getTayAoById(UUID id) {
        return tayAoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TayAo not found with id: " + id));
    }

    public TayAo saveTayAo(TayAo tayAo) throws IllegalArgumentException {
        if (tayAo.getTenTayAo() == null || tayAo.getTenTayAo().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tay áo không được để trống");
        }
        if (tayAoRepository.findByTenTayAoContainingIgnoreCase(tayAo.getTenTayAo())
                .stream()
                .anyMatch(t -> !t.getId().equals(tayAo.getId()) && t.getTenTayAo().equalsIgnoreCase(tayAo.getTenTayAo()))) {
            throw new IllegalArgumentException("Tên tay áo đã tồn tại");
        }
        return tayAoRepository.save(tayAo);
    }

    public void deleteTayAo(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByTayAoId(id)) {
            throw new IllegalStateException("Không thể xóa tay áo vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        tayAoRepository.deleteById(id);
    }
}