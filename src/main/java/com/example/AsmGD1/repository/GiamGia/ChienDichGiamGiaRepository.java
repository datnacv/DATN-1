package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChienDichGiamGiaRepository extends JpaRepository<ChienDichGiamGia, UUID>, JpaSpecificationExecutor<ChienDichGiamGia> {

    // Kiểm tra mã đã tồn tại (dùng khi tạo mới)
    boolean existsByMa(String ma);

    // Kiểm tra mã đã tồn tại nhưng loại trừ theo ID (dùng khi sửa)
    boolean existsByMaAndIdNot(String ma, UUID id);

    // Kiểm tra tên đã tồn tại (không phân biệt hoa thường)
    boolean existsByTenIgnoreCase(String ten);

    // Tìm chiến dịch giảm giá đang hoạt động cho một sản phẩm thông qua ChiTietSanPham
    @Query("SELECT c FROM ChienDichGiamGia c JOIN ChiTietSanPham ct ON ct.chienDichGiamGia.id = c.id " +
            "WHERE ct.sanPham.id = :sanPhamId AND c.ngayBatDau <= :now AND c.ngayKetThuc >= :now")
    Optional<ChienDichGiamGia> findActiveCampaignByProductId(@Param("sanPhamId") UUID sanPhamId, @Param("now") LocalDateTime now);


    @Query("SELECT c FROM ChienDichGiamGia c JOIN c.chiTietSanPhams ct WHERE ct.sanPham.id = :sanPhamId")
    List<ChienDichGiamGia> findBySanPhamIdAndActive(@Param("sanPhamId") UUID sanPhamId, LocalDateTime now);
}
