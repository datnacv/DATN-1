package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.repository.GiamGia.ChienDichGiamGiaRepository;
import com.example.AsmGD1.repository.GiamGia.ChienDichGiamGiaSpecification;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChienDichGiamGiaServiceImpl implements ChienDichGiamGiaService {

    private static final Logger logger = LoggerFactory.getLogger(ChienDichGiamGiaServiceImpl.class);

    @Autowired
    private ChienDichGiamGiaRepository chienDichRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupEndedCampaigns() {
        chiTietSanPhamRepository.detachEndedCampaigns(LocalDateTime.now());
    }

    private String getStatus(ChienDichGiamGia chienDich) {
        LocalDateTime now = LocalDateTime.now();
        if (chienDich.getNgayBatDau().isAfter(now)) return "UPCOMING";
        if (!chienDich.getNgayKetThuc().isAfter(now)) return "ENDED";
        return "ONGOING";
    }

    @Override
    @Transactional
    public void taoMoiChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham) {
        for (UUID chiTietId : danhSachChiTietSanPham) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm: " + chiTietId));
            if (chiTiet.getChienDichGiamGia() != null) {
                String status = getStatus(chiTiet.getChienDichGiamGia());
                if (!"ENDED".equals(status)) {
                    throw new RuntimeException("Chi tiết " + chiTietId + " đã thuộc chiến dịch: "
                            + chiTiet.getChienDichGiamGia().getTen());
                } else {
                    chiTiet.setChienDichGiamGia(null);
                    chiTietSanPhamRepository.save(chiTiet);
                }
            }
        }

        ChienDichGiamGia saved = chienDichRepository.save(chienDich);

        for (UUID chiTietId : danhSachChiTietSanPham) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm: " + chiTietId));
            chiTiet.setChienDichGiamGia(saved);
            chiTietSanPhamRepository.save(chiTiet);
        }
    }

    @Override
    @Transactional
    public void capNhatChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPhamMoi) {
        ChienDichGiamGia existing = chienDichRepository.findById(chienDich.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch: " + chienDich.getId()));

        existing.setMa(chienDich.getMa());
        existing.setTen(chienDich.getTen());
        existing.setPhanTramGiam(chienDich.getPhanTramGiam());
        existing.setNgayBatDau(chienDich.getNgayBatDau());
        existing.setNgayKetThuc(chienDich.getNgayKetThuc());
        existing.setHinhThucGiam(chienDich.getHinhThucGiam());
        existing.setSoLuong(chienDich.getSoLuong());
        chienDichRepository.save(existing);

        List<ChiTietSanPham> chiTietHienTai = chiTietSanPhamRepository.findByChienDichGiamGiaId(chienDich.getId());
        Set<UUID> idsHienTai = chiTietHienTai.stream().map(ChiTietSanPham::getId).collect(Collectors.toSet());
        Set<UUID> idsMoi = new HashSet<>(danhSachChiTietSanPhamMoi);

        Set<UUID> idsCanXoa = new HashSet<>(idsHienTai);
        idsCanXoa.removeAll(idsMoi);

        Set<UUID> idsCanThem = new HashSet<>(idsMoi);
        idsCanThem.removeAll(idsHienTai);

        for (UUID chiTietId : idsCanXoa) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết: " + chiTietId));
            chiTiet.setChienDichGiamGia(null);
            chiTietSanPhamRepository.save(chiTiet);
        }

        for (UUID chiTietId : idsCanThem) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết: " + chiTietId));
            if (chiTiet.getChienDichGiamGia() != null && !chiTiet.getChienDichGiamGia().getId().equals(existing.getId())) {
                String status = getStatus(chiTiet.getChienDichGiamGia());
                if (!"ENDED".equals(status)) {
                    throw new RuntimeException("Chi tiết " + chiTietId + " đã thuộc chiến dịch: "
                            + chiTiet.getChienDichGiamGia().getTen());
                } else {
                    chiTiet.setChienDichGiamGia(null);
                    chiTietSanPhamRepository.save(chiTiet);
                }
            }
            chiTiet.setChienDichGiamGia(existing);
            chiTietSanPhamRepository.save(chiTiet);
        }
    }

    @Override
    public Page<ChienDichGiamGia> locChienDich(String keyword, LocalDateTime startDate, LocalDateTime endDate,
                                               String status, String discountLevel, Pageable pageable) {
        return chienDichRepository.findAll(
                ChienDichGiamGiaSpecification.buildFilter(keyword, startDate, endDate, status, discountLevel),
                pageable
        );
    }

    @Override
    public List<ChiTietSanPham> layChiTietTheoSanPham(UUID idSanPham) {
        return chiTietSanPhamRepository.findAvailableBySanPhamId(idSanPham, LocalDateTime.now());
    }

    @Override
    public List<ChiTietSanPham> layChiTietDaChonTheoChienDich(UUID idChienDich) {
        return chiTietSanPhamRepository.findByChienDichGiamGiaId(idChienDich);
    }

    @Override
    public Optional<ChienDichGiamGia> timTheoId(UUID id) {
        return chienDichRepository.findById(id);
    }

    @Override
    @Transactional
    public void xoaChienDich(UUID id) {
        ChienDichGiamGia chienDich = chienDichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch: " + id));

        LocalDateTime now = LocalDateTime.now();
        if (!chienDich.getNgayBatDau().isAfter(now) && !chienDich.getNgayKetThuc().isBefore(now)) {
            throw new RuntimeException("Không thể xóa chiến dịch đang diễn ra.");
        }

        chiTietSanPhamRepository.removeChienDichGiamGiaById(id);
        chienDichRepository.delete(chienDich);
    }

    @Override
    public boolean maDaTonTai(String ma, UUID excludeId) {
        return excludeId == null ? chienDichRepository.existsByMa(ma)
                : chienDichRepository.existsByMaAndIdNot(ma, excludeId);
    }

    @Override
    public boolean kiemTraMaTonTai(String ma) {
        return chienDichRepository.existsByMa(ma.trim());
    }

    @Override
    public boolean kiemTraTenTonTai(String ten) {
        return chienDichRepository.existsByTenIgnoreCase(ten.trim());
    }

    @Override
    public ChiTietSanPham layChiTietTheoId(UUID id) {
        return chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm: " + id));
    }

    @Override
    @Transactional
    public void truSoLuong(UUID idChienDich, int soLuongTru) {
        ChienDichGiamGia cdgg = chienDichRepository.findById(idChienDich)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch giảm giá: " + idChienDich));
        if (cdgg.getSoLuong() != null && cdgg.getSoLuong() > 0) {
            cdgg.setSoLuong(Math.max(0, cdgg.getSoLuong() - soLuongTru));
            chienDichRepository.save(cdgg);
        }
    }

    @Override
    public Optional<ChienDichGiamGia> getActiveCampaignForProduct(UUID sanPhamId) {
        return chienDichRepository.findBySanPhamIdAndActive(sanPhamId, LocalDateTime.now())
                .stream().findFirst();
    }

    @Override
    public Optional<ChienDichGiamGia> getActiveCampaignForProductDetail(UUID chiTietSanPhamId) {
        ChiTietSanPham ct = chiTietSanPhamRepository.findById(chiTietSanPhamId).orElse(null);
        if (ct == null || ct.getChienDichGiamGia() == null) return Optional.empty();
        LocalDateTime now = LocalDateTime.now();
        ChienDichGiamGia cd = ct.getChienDichGiamGia();
        boolean active = !cd.getNgayBatDau().isAfter(now) && !cd.getNgayKetThuc().isBefore(now);
        return active ? Optional.of(cd) : Optional.empty();
    }

    @Override
    public List<ChiTietSanPham> layChiTietConTrongTheoSanPham(List<UUID> productIds, UUID excludeCampaignId) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyList();
        return chiTietSanPhamRepository.findAvailableByProductIds(productIds, LocalDateTime.now(), excludeCampaignId);
    }
}
