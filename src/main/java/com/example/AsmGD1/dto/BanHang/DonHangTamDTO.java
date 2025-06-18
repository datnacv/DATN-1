package com.example.AsmGD1.dto.BanHang;

import com.example.AsmGD1.entity.DonHangTam;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Data
public class DonHangTamDTO {
    private UUID id;
    private String maDonHangTam;
    private String tenKhachHang;
    private BigDecimal tong;
    private LocalDateTime thoiGianTao;
    private String phuongThucThanhToan;
    private String phuongThucBanHang;
    private BigDecimal phiVanChuyen;
    private UUID idPhieuGiamGia;
    private List<GioHangItemDTO> danhSachSanPham;

    @JsonProperty("danh_sach_item")
    private String danhSachItem;

    public DonHangTamDTO() {
        this.danhSachSanPham = new ArrayList<>(); // Khởi tạo mặc định
    }

    public DonHangTamDTO(DonHangTam entity, ObjectMapper objectMapper) {
        this.id = entity.getId();
        this.tenKhachHang = (entity.getNguoiDung() != null) ? entity.getNguoiDung().getHoTen() : null;
        this.tong = entity.getTong();
        this.thoiGianTao = entity.getThoiGianTao();
        this.phuongThucThanhToan = entity.getPhuongThucThanhToan();
        this.phuongThucBanHang = entity.getPhuongThucBanHang();
        this.phiVanChuyen = entity.getPhiVanChuyen();
        this.idPhieuGiamGia = entity.getPhieuGiamGia();
        this.danhSachItem = entity.getDanhSachSanPham();
        parseDanhSachSanPham(objectMapper); // Gọi parse ngay khi khởi tạo
    }

    public void parseDanhSachSanPham(ObjectMapper objectMapper) {
        if (danhSachItem != null && !danhSachItem.isEmpty()) {
            try {
                this.danhSachSanPham = objectMapper.readValue(danhSachItem, new TypeReference<List<GioHangItemDTO>>(){});
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                System.err.println("Lỗi parse JSON: " + e.getMessage() + ", Dữ liệu: " + danhSachItem);
                this.danhSachSanPham = new ArrayList<>(); // Gán rỗng nếu lỗi
            }
        } else {
            this.danhSachSanPham = new ArrayList<>();
        }
    }
}