package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.XuatXu;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.XuatXuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class XuatXuService {
    @Autowired
    private XuatXuRepository xuatXuRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    public List<XuatXu> getAllXuatXu() {
        return xuatXuRepository.findAll();
    }

    public List<XuatXu> searchXuatXu(String tenXuatXu) {
        return xuatXuRepository.findByTenXuatXuContainingIgnoreCase(tenXuatXu);
    }

    public XuatXu getXuatXuById(UUID id) {
        return xuatXuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("XuatXu not found with id: " + id));
    }

    public XuatXu saveXuatXu(XuatXu xuatXu) throws IllegalArgumentException {
        if (xuatXu.getTenXuatXu() == null || xuatXu.getTenXuatXu().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên xuất xứ không được để trống");
        }
        if (xuatXuRepository.findByTenXuatXuContainingIgnoreCase(xuatXu.getTenXuatXu())
                .stream()
                .anyMatch(x -> !x.getId().equals(xuatXu.getId()) && x.getTenXuatXu().equalsIgnoreCase(xuatXu.getTenXuatXu()))) {
            throw new IllegalArgumentException("Tên xuất xứ đã tồn tại");
        }
        return xuatXuRepository.save(xuatXu);
    }

    public void deleteXuatXu(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByXuatXuId(id)) {
            throw new IllegalStateException("Không thể xóa xuất xứ vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        xuatXuRepository.deleteById(id);
    }
}