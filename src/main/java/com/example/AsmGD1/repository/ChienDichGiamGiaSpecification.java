package com.example.AsmGD1.repository;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ChienDichGiamGiaSpecification {
    public static Specification<ChienDichGiamGia> buildFilter(
            String keyword, LocalDate startDate, LocalDate endDate,
            String status, String discountLevel
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            // Lọc theo từ khóa (tên hoặc mã)
            if (keyword != null && !keyword.isEmpty()) {
                predicate = cb.and(predicate,
                        cb.or(
                                cb.like(cb.lower(root.get("ten")), "%" + keyword.toLowerCase() + "%"),
                                cb.like(cb.lower(root.get("ma")), "%" + keyword.toLowerCase() + "%")
                        )
                );
            }

            // Lọc theo ngày bắt đầu
            if (startDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("ngayBatDau"), startDate));
            }

            // Lọc theo ngày kết thúc
            if (endDate != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("ngayKetThuc"), endDate));
            }

            // Lọc theo trạng thái chiến dịch
            if (status != null && !status.isEmpty()) {
                final LocalDate today = LocalDate.now(); // ✅ bắt buộc thêm final

                switch (status) {
                    case "ONGOING":
                        predicate = cb.and(predicate,
                                cb.lessThanOrEqualTo(root.get("ngayBatDau"), today),
                                cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), today));
                        break;
                    case "UPCOMING":
                        predicate = cb.and(predicate,
                                cb.greaterThan(root.get("ngayBatDau"), today));
                        break;
                    case "ENDED":
                        predicate = cb.and(predicate,
                                cb.lessThan(root.get("ngayKetThuc"), today));
                        break;
                }
            }

            // Lọc theo mức giảm giá
            if (discountLevel != null && !discountLevel.isEmpty()) {
                try {
                    double level = Double.parseDouble(discountLevel);
                    predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("phanTramGiam"), level));
                } catch (NumberFormatException ignored) {}
            }

            return predicate;
        };
    }
}
