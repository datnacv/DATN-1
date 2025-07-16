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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChienDichGiamGiaServiceImpl implements ChienDichGiamGiaService {

    private static final Logger logger = LoggerFactory.getLogger(ChienDichGiamGiaServiceImpl.class);

    @Autowired
    private ChienDichGiamGiaRepository chienDichRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Override
    @Transactional
    public void taoMoiChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham) {
        // Kiểm tra xem các chi tiết sản phẩm có đang tham gia chiến dịch khác không
        for (UUID chiTietId : danhSachChiTietSanPham) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + chiTietId));
            if (chiTiet.getChienDichGiamGia() != null) {
                throw new RuntimeException("Chi tiết sản phẩm " + chiTietId + " đã tham gia chiến dịch: " + chiTiet.getChienDichGiamGia().getTen());
            }
        }

        // Lưu chiến dịch
        ChienDichGiamGia saved = chienDichRepository.save(chienDich);

        // Gán chiến dịch cho các chi tiết sản phẩm
        for (UUID chiTietId : danhSachChiTietSanPham) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + chiTietId));
            chiTiet.setChienDichGiamGia(saved);
            chiTietSanPhamRepository.save(chiTiet);
        }
    }

    @Override
    @Transactional
    public void capNhatChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPhamMoi) {
        ChienDichGiamGia existing = chienDichRepository.findById(chienDich.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + chienDich.getId()));

        // Cập nhật thông tin chiến dịch
        existing.setMa(chienDich.getMa());
        existing.setTen(chienDich.getTen());
        existing.setPhanTramGiam(chienDich.getPhanTramGiam());
        existing.setNgayBatDau(chienDich.getNgayBatDau());
        existing.setNgayKetThuc(chienDich.getNgayKetThuc());
        existing.setHinhThucGiam(chienDich.getHinhThucGiam());
        existing.setSoLuong(chienDich.getSoLuong());
        chienDichRepository.save(existing);

        // Lấy danh sách chi tiết sản phẩm hiện tại của chiến dịch
        List<ChiTietSanPham> chiTietHienTai = chiTietSanPhamRepository.findByChienDichGiamGiaId(chienDich.getId());
        Set<UUID> idsHienTai = chiTietHienTai.stream()
                .map(ChiTietSanPham::getId)
                .collect(Collectors.toSet());

        Set<UUID> idsMoi = new HashSet<>(danhSachChiTietSanPhamMoi);

        // Tìm các ID cần xóa (các chi tiết sản phẩm không còn trong danh sách mới)
        Set<UUID> idsCanXoa = new HashSet<>(idsHienTai);
        idsCanXoa.removeAll(idsMoi);

        // Tìm các ID cần thêm (các chi tiết sản phẩm mới)
        Set<UUID> idsCanThem = new HashSet<>(idsMoi);
        idsCanThem.removeAll(idsHienTai);

        // Xóa chiến dịch khỏi các chi tiết sản phẩm không còn trong danh sách
        for (UUID chiTietId : idsCanXoa) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm: " + chiTietId));
            chiTiet.setChienDichGiamGia(null);
            chiTietSanPhamRepository.save(chiTiet);
        }

        // Thêm chiến dịch cho các chi tiết sản phẩm mới
        for (UUID chiTietId : idsCanThem) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm: " + chiTietId));
            if (chiTiet.getChienDichGiamGia() != null && !chiTiet.getChienDichGiamGia().getId().equals(existing.getId())) {
                throw new RuntimeException("Chi tiết sản phẩm " + chiTietId + " đã tham gia chiến dịch: " + chiTiet.getChienDichGiamGia().getTen());
            }
            chiTiet.setChienDichGiamGia(existing);
            chiTietSanPhamRepository.save(chiTiet);
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
        return chiTietSanPhamRepository.findAvailableBySanPhamId(idSanPham);
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
        // Tìm chiến dịch
        ChienDichGiamGia chienDich = chienDichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + id));

        // Kiểm tra trạng thái chiến dịch
        LocalDate today = LocalDate.now();
        if (!chienDich.getNgayBatDau().isAfter(today) && !chienDich.getNgayKetThuc().isBefore(today)) {
            logger.warn("Thử xóa chiến dịch đang diễn ra: ID = {}, Tên = {}", id, chienDich.getTen());
            throw new RuntimeException("Không thể xóa chiến dịch đang diễn ra.");
        }

        // Bỏ liên kết chiến dịch khỏi các chi tiết sản phẩm
        logger.info("Bắt đầu bỏ liên kết chiến dịch: ID = {}, Tên = {}", id, chienDich.getTen());
        chiTietSanPhamRepository.removeChienDichGiamGiaById(id);

        // Xóa chiến dịch
        chienDichRepository.delete(chienDich);
        logger.info("Đã xóa thành công chiến dịch: ID = {}, Tên = {}", id, chienDich.getTen());
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

    @Override
    public ChiTietSanPham layChiTietTheoId(UUID id) {
        return chiTietSanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + id));
    }

    @Override
    @Transactional
    public void truSoLuong(UUID idChienDich, int soLuongTru) {
        ChienDichGiamGia cdgg = chienDichRepository.findById(idChienDich)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch giảm giá với ID: " + idChienDich));
        if (cdgg.getSoLuong() != null && cdgg.getSoLuong() > 0) {
            int moi = Math.max(0, cdgg.getSoLuong() - soLuongTru);
            cdgg.setSoLuong(moi);
            chienDichRepository.save(cdgg);
        }
    }
}
