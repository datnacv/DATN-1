package com.example.AsmGD1.repository.HoaDon;
import com.example.AsmGD1.entity.LichSuTraHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LichSuTraHangRepository extends JpaRepository<LichSuTraHang, UUID> {
}
