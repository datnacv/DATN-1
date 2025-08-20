package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.CoAo;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.CoAoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CoAoService {
    @Autowired
    private CoAoRepository coAoRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Lấy danh sách cổ áo với phân trang
    public Page<CoAo> getAllCoAo(Pageable pageable) {
        return coAoRepository.findAll(pageable);
    }

    // Lấy tất cả cổ áo (không phân trang)
    public List<CoAo> getAllCoAo() {
        return coAoRepository.findAll();
    }

    // Tìm kiếm cổ áo với phân trang
    public Page<CoAo> searchCoAo(String tenCoAo, Pageable pageable) {
        String keyword = (tenCoAo == null) ? "" : tenCoAo.trim();
        return coAoRepository.findByTenCoAoContainingIgnoreCase(keyword, pageable);
    }

    public CoAo getCoAoById(UUID id) {
        return coAoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CoAo not found with id: " + id));
    }

    public CoAo saveCoAo(CoAo coAo) throws IllegalArgumentException {
        if (coAo.getTenCoAo() == null || coAo.getTenCoAo().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên cổ áo không được để trống");
        }
        if (coAoRepository.findByTenCoAoContainingIgnoreCase(coAo.getTenCoAo())
                .stream()
                .anyMatch(c -> !c.getId().equals(coAo.getId()) && c.getTenCoAo().equalsIgnoreCase(coAo.getTenCoAo()))) {
            throw new IllegalArgumentException("Tên cổ áo đã tồn tại");
        }
        return coAoRepository.save(coAo);
    }

    public void deleteCoAo(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByCoAoId(id)) {
            throw new IllegalStateException("Không thể xóa cổ áo vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        coAoRepository.deleteById(id);
    }
}