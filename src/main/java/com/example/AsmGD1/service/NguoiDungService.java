package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NguoiDungService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    public Page<NguoiDung> findUsersByVaiTro(String vaiTro, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return nguoiDungRepository.findByVaiTroAndKeyword(vaiTro, keyword != null ? keyword : "", pageable);
    }

    public NguoiDung findById(UUID id) {
        return nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));
    }

    public NguoiDung save(NguoiDung nguoiDung) {
        if (nguoiDung.getId() == null) {
            nguoiDung.setThoiGianTao(LocalDateTime.now());
            nguoiDung.setTrangThai(true);
        }
        return nguoiDungRepository.save(nguoiDung);
    }

    public void deleteById(UUID id) {
        nguoiDungRepository.deleteById(id);
    }
}