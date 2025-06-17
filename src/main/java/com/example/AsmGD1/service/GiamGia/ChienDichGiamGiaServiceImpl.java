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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        // KHÔNG cần setId, Hibernate sẽ tự sinh ID
        ChienDichGiamGia saved = chienDichRepository.save(chienDich);

        for (UUID chiTietId : danhSachChiTietSanPham) {
            ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + chiTietId));

            // ✅ KHÔNG gán ID
            ChiTietSanPhamChienDichGiamGia lienKet = new ChiTietSanPhamChienDichGiamGia();
            lienKet.setChienDichGiamGia(saved);
            lienKet.setChiTietSanPham(chiTietSanPham);

            chiTietSanPhamChienDichGiamGiaRepository.save(lienKet);
        }
    }




    @Override
    public Page<ChienDichGiamGia> locChienDich(String keyword, LocalDate startDate, LocalDate endDate,
                                               String status, String discountLevel, Pageable pageable) {
        // Sử dụng ChienDichGiamGiaSpecification để tạo filter
        return chienDichRepository.findAll(
                ChienDichGiamGiaSpecification.buildFilter(keyword, startDate, endDate, status, discountLevel),
                pageable
        );
    }

    @Override
    public List<ChiTietSanPham> layChiTietTheoSanPham(UUID idSanPham) {
        // Lấy danh sách chi tiết sản phẩm theo ID sản phẩm
        return chiTietSanPhamRepository.findBySanPhamId(idSanPham);
    }

    @Override
    public List<ChiTietSanPham> layChiTietDaChonTheoChienDich(UUID idChienDich) {
        // Lấy danh sách chi tiết sản phẩm đã chọn cho chiến dịch
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
    public void capNhatChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham) {
        // Kiểm tra xem chiến dịch có tồn tại không
        ChienDichGiamGia existingChienDich = chienDichRepository.findById(chienDich.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + chienDich.getId()));

        // Cập nhật thông tin chiến dịch
        existingChienDich.setMa(chienDich.getMa());
        existingChienDich.setTen(chienDich.getTen());
        existingChienDich.setPhanTramGiam(chienDich.getPhanTramGiam());
        existingChienDich.setNgayBatDau(chienDich.getNgayBatDau());
        existingChienDich.setNgayKetThuc(chienDich.getNgayKetThuc());
        existingChienDich.setHinhThucGiam(chienDich.getHinhThucGiam());
        existingChienDich.setSoLuong(chienDich.getSoLuong());

        // THAY VÌ: existingChienDich.setStatus(chienDich.getStatus());
        // TA SỬ DỤNG: Log status hiện tại sau khi cập nhật (status sẽ tự động tính toán)
        String currentStatus = existingChienDich.getStatus();
        System.out.println("Status hiện tại của chiến dịch sau khi cập nhật: " + currentStatus);

        // Có thể thêm logic xử lý dựa trên status
        if ("ONGOING".equals(currentStatus)) {
            System.out.println("Chiến dịch đang diễn ra - có thể thực hiện các thao tác đặc biệt");
        } else if ("UPCOMING".equals(currentStatus)) {
            System.out.println("Chiến dịch sắp diễn ra - chuẩn bị kích hoạt");
        } else if ("ENDED".equals(currentStatus)) {
            System.out.println("Chiến dịch đã kết thúc - không thể chỉnh sửa");
        }

        chienDichRepository.save(existingChienDich);

        // Xóa các liên kết cũ
        chiTietSanPhamChienDichGiamGiaRepository.deleteByChienDichGiamGiaId(chienDich.getId());

        // Tạo liên kết mới với các chi tiết sản phẩm
        for (UUID chiTietId : danhSachChiTietSanPham) {
            ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findById(chiTietId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + chiTietId));

            ChiTietSanPhamChienDichGiamGia lienKet = new ChiTietSanPhamChienDichGiamGia();
            lienKet.setId(UUID.randomUUID());
            lienKet.setChienDichGiamGia(existingChienDich);
            lienKet.setChiTietSanPham(chiTietSanPham);
            chiTietSanPhamChienDichGiamGiaRepository.save(lienKet);
        }
    }

    @Override
    public void xoaChienDich(UUID id) {
        // Kiểm tra xem chiến dịch có tồn tại không
        ChienDichGiamGia chienDich = chienDichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + id));

        // (Đã bỏ kiểm tra trạng thái)

        // Xóa các bản ghi liên kết trong bảng chi_tiet_san_pham_chien_dich_giam_gia
        chiTietSanPhamChienDichGiamGiaRepository.deleteByChienDichGiamGiaId(id);

        // Xóa chiến dịch
        chienDichRepository.delete(chienDich);

        System.out.println("Đã xóa thành công chiến dịch: " + chienDich.getTen());
    }


    // Thêm method kiểm tra có thể xóa không
    public boolean coTheXoa(UUID id) {
        String status = layStatusChienDich(id);
        return !"ONGOING".equals(status);
    }

    // Method xóa ép buộc (dành cho admin)
    public void xoaChienDichEpBuoc(UUID id) {
        // Kiểm tra xem chiến dịch có tồn tại không
        ChienDichGiamGia chienDich = chienDichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + id));

        // Xóa các bản ghi liên kết
        chiTietSanPhamChienDichGiamGiaRepository.deleteByChienDichGiamGiaId(id);

        // Xóa chiến dịch bất kể status
        chienDichRepository.delete(chienDich);

        System.out.println("Đã xóa ép buộc chiến dịch: " + chienDich.getTen() + " (Status: " + chienDich.getStatus() + ")");
    }

    // Thêm method tiện ích để lấy status của chiến dịch
    public String layStatusChienDich(UUID id) {
        ChienDichGiamGia chienDich = chienDichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch với ID: " + id));
        return chienDich.getStatus();
    }

    // Thêm method kiểm tra chiến dịch có thể chỉnh sửa không
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

}