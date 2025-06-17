package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ChienDichGiamGiaRepository extends JpaRepository<ChienDichGiamGia, UUID>, JpaSpecificationExecutor<ChienDichGiamGia> {

    // Kiểm tra mã đã tồn tại (dùng khi tạo mới)
    boolean existsByMa(String ma);

    // Kiểm tra mã đã tồn tại nhưng loại trừ theo ID (dùng khi sửa)
    boolean existsByMaAndIdNot(String ma, UUID id);

    // Kiểm tra tên đã tồn tại (không phân biệt hoa thường)
    boolean existsByTenIgnoreCase(String ten);
}
