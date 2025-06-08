package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.ThuongHieu;
import com.example.AsmGD1.repository.ThuongHieuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ThuongHieuService {
    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

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

    public ThuongHieu saveThuongHieu(ThuongHieu thuongHieu) {
        return thuongHieuRepository.save(thuongHieu);
    }

    public void deleteThuongHieu(UUID id) {
        thuongHieuRepository.deleteById(id);
    }
}