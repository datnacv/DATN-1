package com.example.AsmGD1.Service;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.entity.PhieuGiamGiaCuaNguoiDung;
import com.example.AsmGD1.repository.NguoiDungRepository;
import com.example.AsmGD1.repository.PhieuGiamGiaCuaNguoiDungRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaCuaNguoiDungService {

    private final NguoiDungRepository nguoiDungRepository;
    private final PhieuGiamGiaCuaNguoiDungRepository phieuGiamGiaCuaNguoiDungRepository;

    public List<NguoiDung> layTatCaKhachHang() {
        return nguoiDungRepository.findByVaiTro("CUSTOMER").stream()
                .filter(Objects::nonNull)
                .filter(kh -> kh.getHoTen() != null)
                .toList();
    }

    public Page<NguoiDung> layTatCaKhachHangPhanTrang(Pageable pageable) {
        return nguoiDungRepository.findByVaiTroAndTrangThaiTrue("CUSTOMER", pageable);
    }

    public List<NguoiDung> layNguoiDungTheoIds(List<UUID> ids) {
        return nguoiDungRepository.findAllById(ids);
    }

    public void ganPhieuChoNguoiDung(NguoiDung nguoiDung, PhieuGiamGia phieuGiamGia) {
        boolean daTonTai = phieuGiamGiaCuaNguoiDungRepository.findAll().stream()
                .anyMatch(item ->
                        item.getNguoiDung().getId().equals(nguoiDung.getId()) &&
                                item.getPhieuGiamGia().getId().equals(phieuGiamGia.getId())
                );

        if (!daTonTai) {
            PhieuGiamGiaCuaNguoiDung entity = new PhieuGiamGiaCuaNguoiDung();
            entity.setNguoiDung(nguoiDung);
            entity.setPhieuGiamGia(phieuGiamGia);
            phieuGiamGiaCuaNguoiDungRepository.save(entity);
        }
    }

    public List<UUID> layIdKhachHangDaGanPhieu(UUID phieuId) {
        return phieuGiamGiaCuaNguoiDungRepository.findByPhieuGiamGia_Id(phieuId).stream()
                .map(p -> p.getNguoiDung().getId())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<NguoiDung> layNguoiDungTheoPhieu(UUID phieuId) {
        return phieuGiamGiaCuaNguoiDungRepository.findByPhieuGiamGia_Id(phieuId).stream()
                .map(PhieuGiamGiaCuaNguoiDung::getNguoiDung)
                .collect(Collectors.toList());
    }

    @Transactional
    public void xoaTatCaGanKetTheoPhieu(UUID phieuId) {
        List<PhieuGiamGiaCuaNguoiDung> list = phieuGiamGiaCuaNguoiDungRepository.findByPhieuGiamGia_Id(phieuId);
        phieuGiamGiaCuaNguoiDungRepository.deleteAll(list);
    }

    public List<NguoiDung> timKiemKhachHang(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return layTatCaKhachHang();
        }
        return nguoiDungRepository.searchByKeywordNoPaging(keyword.trim());
    }

    public Page<NguoiDung> timKiemKhachHangPhanTrang(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return layTatCaKhachHangPhanTrang(pageable);
        }
        return nguoiDungRepository.findByHoTenContainingIgnoreCaseAndVaiTroAndTrangThaiTrue(keyword.trim(), "CUSTOMER", pageable);
    }
}
