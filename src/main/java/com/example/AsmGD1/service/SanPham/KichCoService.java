package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.KichCo;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.KichCoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KichCoService {
    @Autowired
    private KichCoRepository kichCoRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    public List<KichCo> getAllKichCo() {
        return kichCoRepository.findAll();
    }

    public List<KichCo> searchKichCo(String ten) {
        return kichCoRepository.findByTenContainingIgnoreCase(ten);
    }

    public KichCo getKichCoById(UUID id) {
        return kichCoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KichCo not found with id: " + id));
    }

    public KichCo saveKichCo(KichCo kichCo) throws IllegalArgumentException {
        if (kichCo.getTen() == null || kichCo.getTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên kích cỡ không được để trống");
        }
        if (kichCoRepository.findByTenContainingIgnoreCase(kichCo.getTen())
                .stream()
                .anyMatch(k -> !k.getId().equals(kichCo.getId()) && k.getTen().equalsIgnoreCase(kichCo.getTen()))) {
            throw new IllegalArgumentException("Tên kích cỡ đã tồn tại");
        }
        return kichCoRepository.save(kichCo);
    }

    public void deleteKichCo(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByKichCoId(id)) {
            throw new IllegalStateException("Không thể xóa kích cỡ vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        kichCoRepository.deleteById(id);
    }
}