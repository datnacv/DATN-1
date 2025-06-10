package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.ChiTietSanPhamChienDichGiamGia;
import com.example.AsmGD1.repository.GiamGia.ChienDichGiamGiaRepository;
import com.example.AsmGD1.repository.GiamGia.ChiTietSanPhamChienDichGiamGiaRepository;
import com.example.AsmGD1.repository.GiamGia.ChienDichGiamGiaSpecification;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChienDichGiamGiaServiceImpl implements ChienDichGiamGiaService {

    @Autowired
    private ChienDichGiamGiaRepository chienDichRepository;

    @Autowired
    private ChiTietSanPhamChienDichGiamGiaRepository chiTietSanPhamChienDichGiamGiaRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Override
    @Transactional
    public void taoMoiChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham) {
        ChienDichGiamGia saved = chienDichRepository.save(chienDich);

        for (UUID chiTietId : danhSachChiTietSanPham) {
            ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + chiTietId));

            ChiTietSanPhamChienDichGiamGia lienKet = new ChiTietSanPhamChienDichGiamGia();
            lienKet.setChienDichGiamGia(saved);
            lienKet.setChiTietSanPham(chiTietSanPham);

            chiTietSanPhamChienDichGiamGiaRepository.save(lienKet);
        }
    }

    @Override
    public Page<ChienDichGiamGia> locChienDich(String keyword, LocalDate startDate, LocalDate endDate,
                                               String status, String discountLevel, Pageable pageable) {
        return chienDichRepository.findAll(
                ChienDichGiamGiaSpecification.buildFilter(keyword, startDate, endDate, status, discountLevel),
                pageable
        );
    }

    @Override
    public List<ChiTietSanPham> layChiTietTheoSanPham(UUID idSanPham) {
        return chiTietSanPhamRepository.findBySanPhamId(idSanPham);
    }

    @Override
    public List<ChiTietSanPham> layChiTietDaChonTheoChienDich(UUID idChienDich) {
        List<ChiTietSanPhamChienDichGiamGia> lienKets = chiTietSanPhamChienDichGiamGiaRepository.findWithDetailsByChienDichId(idChienDich);
        return lienKets.stream()
                .map(ChiTietSanPhamChienDichGiamGia::getChiTietSanPham)
                .toList();
    }

    @Override
    public Optional<ChienDichGiamGia> timTheoId(UUID id) {
        return chienDichRepository.findById(id);
    }

    @Override
    @Transactional
    public void capNhatChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPhamMoi) {
        ChienDichGiamGia existing = chienDichRepository.findById(chienDich.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));

        existing.setMa(chienDich.getMa());
        existing.setTen(chienDich.getTen());
        existing.setPhanTramGiam(chienDich.getPhanTramGiam());
        existing.setNgayBatDau(chienDich.getNgayBatDau());
        existing.setNgayKetThuc(chienDich.getNgayKetThuc());
        existing.setHinhThucGiam(chienDich.getHinhThucGiam());
        existing.setSoLuong(chienDich.getSoLuong());
        chienDichRepository.save(existing);

        List<ChiTietSanPhamChienDichGiamGia> lienKetsHienTai = chiTietSanPhamChienDichGiamGiaRepository
                .findWithDetailsByChienDichId(chienDich.getId());

        Set<UUID> idsHienTai = lienKetsHienTai.stream()
                .map(lk -> lk.getChiTietSanPham().getId())
                .collect(Collectors.toSet());

        Set<UUID> idsMoi = new HashSet<>(danhSachChiTietSanPhamMoi);

        Set<UUID> idsCanXoa = new HashSet<>(idsHienTai);
        idsCanXoa.removeAll(idsMoi);

        Set<UUID> idsCanThem = new HashSet<>(idsMoi);
        idsCanThem.removeAll(idsHienTai);

        if (!idsCanXoa.isEmpty()) {
            chiTietSanPhamChienDichGiamGiaRepository.deleteByChienDichGiamGiaIdAndChiTietSanPhamIds(chienDich.getId(), idsCanXoa);
        }

        for (UUID chiTietId : idsCanThem) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm: " + chiTietId));

            ChiTietSanPhamChienDichGiamGia lienKet = new ChiTietSanPhamChienDichGiamGia();
            lienKet.setChienDichGiamGia(existing);
            lienKet.setChiTietSanPham(chiTiet);
            chiTietSanPhamChienDichGiamGiaRepository.save(lienKet);
        }
    }

    @Override
    public void xoaChienDich(UUID id) {
        ChienDichGiamGia chienDich = chienDichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + id));

        chiTietSanPhamChienDichGiamGiaRepository.deleteByChienDichGiamGiaId(id);
        chienDichRepository.delete(chienDich);
        System.out.println("Đã xóa thành công chiến dịch: " + chienDich.getTen());
    }

    public boolean coTheXoa(UUID id) {
        String status = layStatusChienDich(id);
        return !"ONGOING".equals(status);
    }

    public void xoaChienDichEpBuoc(UUID id) {
        ChienDichGiamGia chienDich = chienDichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + id));

        chiTietSanPhamChienDichGiamGiaRepository.deleteByChienDichGiamGiaId(id);
        chienDichRepository.delete(chienDich);
        System.out.println("Đã xóa ép buộc chiến dịch: " + chienDich.getTen() + " (Status: " + chienDich.getStatus() + ")");
    }

    public String layStatusChienDich(UUID id) {
        ChienDichGiamGia chienDich = chienDichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + id));
        return chienDich.getStatus();
    }

    public boolean coTheChinhSua(UUID id) {
        String status = layStatusChienDich(id);
        return "UPCOMING".equals(status) || "ONGOING".equals(status);
    }

    @Override
    public List<ChiTietSanPhamChienDichGiamGia> layLienKetChiTietTheoChienDich(UUID idChienDich) {
        return chiTietSanPhamChienDichGiamGiaRepository.findWithDetailsByChienDichId(idChienDich);
    }

    @Override
    public boolean maDaTonTai(String ma, UUID excludeId) {
        if (excludeId == null) {
            return chienDichRepository.existsByMa(ma);
        } else {
            return chienDichRepository.existsByMaAndIdNot(ma, excludeId);
        }
    }

    @Override
    public boolean kiemTraMaTonTai(String ma) {
        return chienDichRepository.existsByMa(ma.trim());
    }

    @Override
    public boolean kiemTraTenTonTai(String ten) {
        return chienDichRepository.existsByTenIgnoreCase(ten.trim());
    }
}