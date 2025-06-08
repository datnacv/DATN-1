package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.KichCo;
import com.example.AsmGD1.repository.SanPham.KichCoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KichCoService {
    @Autowired
    private KichCoRepository kichCoRepository;

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

    public KichCo saveKichCo(KichCo kichCo) {
        return kichCoRepository.save(kichCo);
    }

    public void deleteKichCo(UUID id) {
        kichCoRepository.deleteById(id);
    }
}