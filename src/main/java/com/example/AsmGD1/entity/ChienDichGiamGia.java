    package com.example.AsmGD1.entity;

    import com.fasterxml.jackson.annotation.JsonIgnore;
    import lombok.Data;
    import jakarta.persistence.*;
    import java.util.UUID;
    import java.math.BigDecimal;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.List;

    @Entity
    @Table(name = "chien_dich_giam_gia")
    @Data
    public class ChienDichGiamGia {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID id;

        @Column(name = "ten")
        private String ten;

        @Column(name = "ma", nullable = false, length = 50)
        private String ma;

        @Column(name = "hinh_thuc_giam")
        private String hinhThucGiam;

        @Column(name = "so_luong")
        private Integer soLuong;

        @Column(name = "phan_tram_giam")
        private BigDecimal phanTramGiam;

        @Column(name = "ngay_bat_dau")
        private LocalDate ngayBatDau;

        @Column(name = "ngay_ket_thuc")
        private LocalDate ngayKetThuc;

        @Column(name = "thoi_gian_tao")
        private LocalDateTime thoiGianTao;

        @OneToMany(mappedBy = "chienDichGiamGia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JsonIgnore
        private List<ChiTietSanPham> chiTietSanPhams; // Danh sách các chi tiết sản phẩm thuộc chiến dịch

        public String getStatus() {
            LocalDate today = LocalDate.now();
            if (today.isBefore(ngayBatDau)) {
                return "UPCOMING"; // Sắp diễn ra
            } else if (today.isAfter(ngayKetThuc)) {
                return "ENDED"; // Đã kết thúc
            } else {
                return "ONGOING"; // Đang diễn ra
            }
        }
    }