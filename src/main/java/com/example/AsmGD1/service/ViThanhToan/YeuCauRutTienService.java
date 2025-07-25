package com.example.AsmGD1.service.ViThanhToan;

import com.example.AsmGD1.entity.ViThanhToan;
import com.example.AsmGD1.entity.YeuCauRutTien;
import com.example.AsmGD1.repository.ViThanhToan.ViThanhToanRepository;
import com.example.AsmGD1.repository.ViThanhToan.YeuCauRutTienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class YeuCauRutTienService {

    @Autowired
    private YeuCauRutTienRepository yeuCauRutTienRepository;
    @Autowired
    private ViThanhToanRepository viThanhToanRepository;

    public YeuCauRutTien taoYeuCauRutTien(UUID idNguoiDung, BigDecimal soTien) {
        ViThanhToan vi = viThanhToanRepository.findByIdNguoiDung(idNguoiDung)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        if (vi.getSoDu().compareTo(soTien) < 0) {
            throw new RuntimeException("Số dư không đủ để rút");
        }

        YeuCauRutTien yeuCau = new YeuCauRutTien();
        yeuCau.setViThanhToan(vi);
        yeuCau.setSoTien(soTien);
        // các trường còn lại tự set trong @PrePersist

        return yeuCauRutTienRepository.save(yeuCau);
    }
}
