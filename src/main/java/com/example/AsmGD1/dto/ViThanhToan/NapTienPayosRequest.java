package com.example.AsmGD1.dto.ViThanhToan;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class NapTienPayosRequest {
    private UUID idNguoiDung;
    private BigDecimal soTien;
    private String description;
}