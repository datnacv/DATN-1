package com.example.AsmGD1.repository.GiamGia;

import com.example.AsmGD1.entity.ChienDichGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ChienDichGiamGiaRepository extends JpaRepository<ChienDichGiamGia, UUID>, JpaSpecificationExecutor<ChienDichGiamGia> {
    boolean existsByMa(String ma);
    boolean existsByMaAndIdNot(String ma, UUID id);

}