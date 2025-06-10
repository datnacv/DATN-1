package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.MauSac;
import com.example.AsmGD1.repository.SanPham.MauSacRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MauSacService {
    @Autowired
    private MauSacRepository mauSacRepository;

    public List<MauSac> getAllMauSac() {
        return mauSacRepository.findAll();
    }

    public List<MauSac> searchMauSac(String tenMau) {
        return mauSacRepository.findByTenMauContainingIgnoreCase(tenMau);
    }

    public MauSac getMauSacById(UUID id) {
        return mauSacRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MauSac not found with id: " + id));
    }

    public MauSac saveMauSac(MauSac mauSac) {
        return mauSacRepository.save(mauSac);
    }

    public void deleteMauSac(UUID id) {
        mauSacRepository.deleteById(id);
    }
}