package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.KieuDang;
import com.example.AsmGD1.repository.KieuDangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KieuDangService {
    @Autowired
    private KieuDangRepository kieuDangRepository;

    public List<KieuDang> getAllKieuDang() {
        return kieuDangRepository.findAll();
    }

    public List<KieuDang> searchKieuDang(String tenKieuDang) {
        return kieuDangRepository.findByTenKieuDangContainingIgnoreCase(tenKieuDang);
    }

    public KieuDang getKieuDangById(UUID id) {
        return kieuDangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KieuDang not found with id: " + id));
    }

    public KieuDang saveKieuDang(KieuDang kieuDang) {
        return kieuDangRepository.save(kieuDang);
    }

    public void deleteKieuDang(UUID id) {
        kieuDangRepository.deleteById(id);
    }
}