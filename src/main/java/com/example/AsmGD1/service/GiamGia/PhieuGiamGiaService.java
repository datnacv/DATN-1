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

    public PhieuGiamGia layTheoMa(String ma) {
        return phieuGiamGiaRepository.findByMaIgnoreCase(ma).orElse(null);
    }

    public boolean tonTaiMa(String ma) {
        return phieuGiamGiaRepository.existsByMaIgnoreCase(ma);
    }

    public PhieuGiamGia luu(PhieuGiamGia phieu) {
        if (phieu.getThoiGianTao() == null) {
            phieu.setThoiGianTao(LocalDateTime.now());
        }

        // Đảm bảo soLuong và gioiHanSuDung không null
        if ("ca_nhan".equalsIgnoreCase(phieu.getKieuPhieu())) {
            phieu.setSoLuong(1); // Phiếu cá nhân: soLuong = 1
            phieu.setGioiHanSuDung(1); // Phiếu cá nhân: gioiHanSuDung = 1
        } else {
            if (phieu.getSoLuong() == null) {
                phieu.setSoLuong(1); // Mặc định cho phiếu công khai
            }
            if (phieu.getGioiHanSuDung() == null) {
                phieu.setGioiHanSuDung(phieu.getSoLuong()); // gioiHanSuDung bằng soLuong
            }
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

    public boolean apDungPhieuGiamGia(UUID phieuId) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findById(phieuId).orElse(null);
        if (phieu == null) {
            return false;
        }
        // Kiểm tra trạng thái
        if (!"Đang diễn ra".equals(tinhTrang(phieu))) {
            return false;
        }
        // Kiểm tra lượt sử dụng
        Integer luotConLai = phieu.getGioiHanSuDung();
        if (luotConLai != null && luotConLai > 0) {
            phieu.setGioiHanSuDung(luotConLai - 1);
            phieuGiamGiaRepository.save(phieu);
            return true;
        }
        return false;
    }
}