package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaService {

    private static final Logger logger = LoggerFactory.getLogger(PhieuGiamGiaService.class);

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

        if ("ca_nhan".equalsIgnoreCase(phieu.getKieuPhieu())) {
            phieu.setSoLuong(1);
            phieu.setGioiHanSuDung(1);
        } else {
            if (phieu.getSoLuong() == null) {
                phieu.setSoLuong(1);
            }
            if (phieu.getGioiHanSuDung() == null) {
                phieu.setGioiHanSuDung(phieu.getSoLuong());
            }
        }

        return phieuGiamGiaRepository.save(phieu);
    }

    public void xoa(UUID id) {
        phieuGiamGiaRepository.deleteById(id);
    }

    public String tinhTrang(PhieuGiamGia v) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime batDau = v.getNgayBatDau();
        LocalDateTime ketThuc = v.getNgayKetThuc();

        if (batDau != null && ketThuc != null) {
            if (now.isBefore(batDau)) return "Sắp diễn ra";
            else if (!now.isAfter(ketThuc)) return "Đang diễn ra";
            else return "Đã kết thúc";
        }

        return "Không xác định";
    }

    @Transactional
    public boolean apDungPhieuGiamGia(UUID phieuId) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findById(phieuId).orElse(null);
        if (phieu == null) return false;
        if (!"Đang diễn ra".equals(tinhTrang(phieu))) return false;

        Integer luotConLai = phieu.getGioiHanSuDung();
        Integer soLuong = phieu.getSoLuong();

        if (luotConLai != null && luotConLai > 0 && soLuong != null && soLuong > 0) {
            phieu.setGioiHanSuDung(luotConLai - 1);
            phieu.setSoLuong(soLuong - 1);
            phieuGiamGiaRepository.save(phieu);
            phieuGiamGiaRepository.flush();
            return true;
        }
        return false;
    }

    public BigDecimal tinhTienGiamGia(PhieuGiamGia phieu, BigDecimal tongTien) {
        if (phieu == null || tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal giamGia = BigDecimal.ZERO;
        BigDecimal giaTriGiam = phieu.getGiaTriGiam();

        if (giaTriGiam == null || giaTriGiam.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        String loai = phieu.getLoai();

        if ("PERCENT".equalsIgnoreCase(loai) || "Phần trăm".equalsIgnoreCase(loai)) {
            // Lưu trong DB là phần trăm (VD: 10.00 nghĩa là 10%)
            giamGia = tongTien.multiply(giaTriGiam)
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.HALF_UP);

            if (phieu.getGiaTriGiamToiDa() != null &&
                    giamGia.compareTo(phieu.getGiaTriGiamToiDa()) > 0) {
                giamGia = phieu.getGiaTriGiamToiDa();
            }

        } else if ("FIXED".equalsIgnoreCase(loai) || "Tiền mặt".equalsIgnoreCase(loai)) {
            giamGia = giaTriGiam;
        }

        if (giamGia.compareTo(tongTien) > 0) {
            giamGia = tongTien;
        }

        return giamGia;
    }

}
