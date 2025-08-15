package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import com.example.AsmGD1.entity.ChiTietSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChienDichGiamGiaService {

    // Tạo mới chiến dịch và gán chi tiết sản phẩm
    void taoMoiChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham);

    // Cập nhật chiến dịch và chi tiết sản phẩm
    void capNhatChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham);

    // Lọc chiến dịch theo bộ lọc
    Page<ChienDichGiamGia> locChienDich(String keyword, LocalDateTime startDate, LocalDateTime endDate,
                                        String status, String discountLevel, Pageable pageable);

    // Tìm chi tiết sản phẩm theo sản phẩm
    List<ChiTietSanPham> layChiTietTheoSanPham(UUID idSanPham);

    // Tìm chi tiết sản phẩm đã chọn cho chiến dịch
    List<ChiTietSanPham> layChiTietDaChonTheoChienDich(UUID idChienDich);

    // Tìm theo ID
    Optional<ChienDichGiamGia> timTheoId(UUID id);

    // Xóa chiến dịch
    void xoaChienDich(UUID id);

    // Kiểm tra mã đã tồn tại (khi sửa — bỏ qua ID cụ thể)
    boolean maDaTonTai(String ma, UUID excludeId);

    // Kiểm tra mã đã tồn tại (khi tạo)
    boolean kiemTraMaTonTai(String ma);

    // Kiểm tra tên đã tồn tại
    boolean kiemTraTenTonTai(String ten);

    // Tìm chi tiết sản phẩm theo ID
    ChiTietSanPham layChiTietTheoId(UUID id);

    void truSoLuong(UUID idChienDich, int soLuongTru);

    // Tìm chiến dịch giảm giá đang hoạt động cho sản phẩm
    Optional<ChienDichGiamGia> getActiveCampaignForProduct(UUID sanPhamId);

    // ChienDichGiamGiaService.java
    Optional<ChienDichGiamGia> getActiveCampaignForProductDetail(UUID chiTietSanPhamId);
}