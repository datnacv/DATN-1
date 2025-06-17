package com.example.AsmGD1.entity;

import lombok.Data;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_giam_gia")
@Data
public class PhieuGiamGia {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "ten")
    private String ten;

    @Column(name = "loai")
    private String loai;

    @Column(name = "gia_tri_giam")
    private BigDecimal giaTriGiam;

    @Column(name = "gia_tri_giam_toi_thieu")
    private BigDecimal giaTriGiamToiThieu;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "gioi_han_su_dung")
    private Integer gioiHanSuDung;

//    @Column(name = "cong_khai")
//    private Boolean congKhai;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc;


    @Column(name = "thoi_gian_tao")
    private LocalDateTime thoiGianTao;
    @Column(name = "kieu_phieu")
    private String kieuPhieu; // "cong_khai" hoặc "ca_nhan"
    @Column(name = "gia_tri_giam_toi_da", precision = 10, scale = 2)
    private BigDecimal giaTriGiamToiDa;

    @Column(name="ma")
    private String ma;

    public String getGiaTriGiamToiThieuFormatted() {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return giaTriGiamToiThieu != null ? formatter.format(giaTriGiamToiThieu) + " VNĐ" : "0 VNĐ";
    }
}