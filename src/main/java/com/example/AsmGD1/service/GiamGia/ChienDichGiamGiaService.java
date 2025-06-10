package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.ChiTietSanPhamChienDichGiamGia;
import com.example.AsmGD1.entity.ChienDichGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChienDichGiamGiaService {

    // Tạo mới chiến dịch + gán chi tiết sản phẩm
    void taoMoiChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham);

    // Cập nhật chiến dịch + chi tiết
    void capNhatChienDichKemChiTiet(ChienDichGiamGia chienDich, List<UUID> danhSachChiTietSanPham);

    // Lọc chiến dịch theo bộ lọc
    Page<ChienDichGiamGia> locChienDich(String keyword, LocalDate startDate, LocalDate endDate,
                                        String status, String discountLevel, Pageable pageable);

    // Tìm chi tiết sản phẩm theo sản phẩm
    List<ChiTietSanPham> layChiTietTheoSanPham(UUID idSanPham);

    // Tìm chi tiết đã chọn cho chiến dịch
    List<ChiTietSanPham> layChiTietDaChonTheoChienDich(UUID idChienDich);

    // Tìm theo ID
    Optional<ChienDichGiamGia> timTheoId(UUID id);

    // Lấy liên kết chi tiết theo chiến dịch (bao gồm join thông tin màu sắc, kích cỡ, sản phẩm)
    List<ChiTietSanPhamChienDichGiamGia> layLienKetChiTietTheoChienDich(UUID idChienDich);

    // Xóa chiến dịch
    void xoaChienDich(UUID id);

    // ✅ Kiểm tra mã đã tồn tại (khi sửa — bỏ qua 1 ID cụ thể)
    boolean maDaTonTai(String ma, UUID excludeId);

    // ✅ Kiểm tra mã đã tồn tại (khi tạo)
    boolean kiemTraMaTonTai(String ma);

    // ✅ Kiểm tra tên đã tồn tại
    boolean kiemTraTenTonTai(String ten);
}
