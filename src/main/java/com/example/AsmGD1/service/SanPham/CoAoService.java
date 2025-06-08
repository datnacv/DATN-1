package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.CoAo;
import com.example.AsmGD1.repository.SanPham.CoAoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CoAoService {
    @Autowired
    private CoAoRepository coAoRepository;

    public List<CoAo> getAllCoAo() {
        return coAoRepository.findAll();
    }

    public List<CoAo> searchCoAo(String tenCoAo) {
        return coAoRepository.findByTenCoAoContainingIgnoreCase(tenCoAo);
    }

    public CoAo getCoAoById(UUID id) {
        return coAoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CoAo not found with id: " + id));
    }

    public CoAo saveCoAo(CoAo coAo) {
        return coAoRepository.save(coAo);
    }

    public void deleteCoAo(UUID id) {
        coAoRepository.deleteById(id);
    }
}