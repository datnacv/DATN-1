package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.TayAo;
import com.example.AsmGD1.repository.TayAoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TayAoService {
    @Autowired
    private TayAoRepository tayAoRepository;

    public List<TayAo> getAllTayAo() {
        return tayAoRepository.findAll();
    }

    public List<TayAo> searchTayAo(String tenTayAo) {
        return tayAoRepository.findByTenTayAoContainingIgnoreCase(tenTayAo);
    }

    public TayAo getTayAoById(UUID id) {
        return tayAoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TayAo not found with id: " + id));
    }

    public TayAo saveTayAo(TayAo tayAo) {
        return tayAoRepository.save(tayAo);
    }

    public void deleteTayAo(UUID id) {
        tayAoRepository.deleteById(id);
    }
}