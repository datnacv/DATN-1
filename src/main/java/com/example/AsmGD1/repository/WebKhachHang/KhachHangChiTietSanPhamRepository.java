package com.example.AsmGD1.repository.WebKhachHang;


import com.example.AsmGD1.entity.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KhachHangChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, UUID> {

}

