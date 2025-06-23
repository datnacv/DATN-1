package com.example.AsmGD1.dto.BanHang;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class KetQuaDonHangDTO {
    private String maDonHang;
    private Map<UUID, Integer> soLuongTonKho;
    private BigDecimal changeAmount; // Thêm trường này
}
