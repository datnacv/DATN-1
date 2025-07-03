package com.example.AsmGD1.repository.GioHang;

import com.example.AsmGD1.entity.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GioHangRepository extends JpaRepository<GioHang, UUID> {
    /**
     * Tìm giỏ hàng dựa trên ID của người dùng
     * @param nguoiDungId ID của người dùng
     * @return Đối tượng GioHang hoặc null nếu không tìm thấy
     */
    GioHang findByNguoiDungId(UUID nguoiDungId);

    /**
     * Kiểm tra xem giỏ hàng có tồn tại cho người dùng không
     * @param nguoiDungId ID của người dùng
     * @return true nếu tồn tại, false nếu không
     */
    boolean existsByNguoiDungId(UUID nguoiDungId);
}