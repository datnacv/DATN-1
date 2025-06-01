package com.example.AsmGD1.repository;

import com.example.AsmGD1.entity.ThuongHieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, UUID> {
    @Query("SELECT t FROM ThuongHieu t WHERE LOWER(t.tenThuongHieu) LIKE LOWER(CONCAT('%', :tenThuongHieu, '%'))")
    List<ThuongHieu> findByTenThuongHieuContainingIgnoreCase(String tenThuongHieu);
}