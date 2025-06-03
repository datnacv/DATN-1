package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.repository.DanhMucRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DanhMucService {
    @Autowired
    private DanhMucRepository danhMucRepository;

    public List<DanhMuc> getAllDanhMuc() {
        return danhMucRepository.findAll();
    }

    public List<DanhMuc> searchDanhMuc(String tenDanhMuc) {
        return danhMucRepository.findByTenDanhMucContainingIgnoreCase(tenDanhMuc);
    }

    public DanhMuc getDanhMucById(UUID id) {
        return danhMucRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DanhMuc not found with id: " + id));
    }

    public DanhMuc saveDanhMuc(DanhMuc danhMuc) {
        return danhMucRepository.save(danhMuc);
    }

    public void deleteDanhMuc(UUID id) {
        danhMucRepository.deleteById(id);
    }
}