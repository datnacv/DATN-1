package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaService {

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;

    public List<PhieuGiamGia> layTatCa() {
        return phieuGiamGiaRepository.findAll();
    }

    public PhieuGiamGia layTheoId(UUID id) {
        return phieuGiamGiaRepository.findById(id).orElse(null);
    }

    public boolean tonTaiMa(String ma) {
        return phieuGiamGiaRepository.existsByMaIgnoreCase(ma);
    }

    public PhieuGiamGia luu(PhieuGiamGia phieu) {
        if (phieu.getThoiGianTao() == null) {
            phieu.setThoiGianTao(LocalDateTime.now());
        }
        return phieuGiamGiaRepository.save(phieu);
    }

    public void xoa(UUID id) {
        phieuGiamGiaRepository.deleteById(id);
    }

    public String tinhTrang(PhieuGiamGia v) {
        LocalDate homNay = LocalDate.now();

        if (v.getNgayBatDau() != null && v.getNgayKetThuc() != null) {
            if (homNay.isBefore(v.getNgayBatDau())) {
                return "Sắp diễn ra";
            } else if (!homNay.isAfter(v.getNgayKetThuc())) {
                return "Đang diễn ra";
            } else {
                return "Đã kết thúc";
            }
        }

        return "Không xác định";
    }
}