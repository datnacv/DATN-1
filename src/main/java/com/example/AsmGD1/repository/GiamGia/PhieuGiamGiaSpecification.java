package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGia;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class PhieuGiamGiaSpecification {

    public static Specification<PhieuGiamGia> filter(String search, String type, String status, LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (search != null && !search.trim().isEmpty()) {
                String likeSearch = "%" + search.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("ten")), likeSearch),
                        cb.like(cb.lower(root.get("ma")), likeSearch)
                ));
            }

            if (type != null && !type.isEmpty()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("loai")), type.toLowerCase()));
            }

            if (fromDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("ngayBatDau"), fromDate));
            }

            if (toDate != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("ngayKetThuc"), toDate));
            }

            if (status != null) {
                LocalDateTime now = LocalDateTime.now();
                switch (status) {
                    case "upcoming" -> predicate = cb.and(predicate, cb.greaterThan(root.get("ngayBatDau"), now));
                    case "active" -> predicate = cb.and(predicate,
                            cb.lessThanOrEqualTo(root.get("ngayBatDau"), now),
                            cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), now));
                    case "expired" -> predicate = cb.and(predicate, cb.lessThan(root.get("ngayKetThuc"), now));
                }
            }

            return predicate;
        };
    }
}
