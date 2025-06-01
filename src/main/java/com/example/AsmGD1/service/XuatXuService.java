package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.XuatXu;
import com.example.AsmGD1.repository.XuatXuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class XuatXuService {
    @Autowired
    private XuatXuRepository xuatXuRepository;

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

    public XuatXu saveXuatXu(XuatXu xuatXu) {
        return xuatXuRepository.save(xuatXu);
    }

    public void deleteXuatXu(UUID id) {
        xuatXuRepository.deleteById(id);
    }
}