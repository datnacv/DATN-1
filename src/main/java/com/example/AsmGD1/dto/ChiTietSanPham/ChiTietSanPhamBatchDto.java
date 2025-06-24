package com.example.AsmGD1.dto.ChiTietSanPham;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class ChiTietSanPhamBatchDto {
    private UUID productId;
    private UUID originId;
    private UUID materialId;
    private UUID styleId;
    private UUID sleeveId;
    private UUID collarId;
    private UUID brandId;
    private String gender;
    private List<ChiTietSanPhamVariationDto> variations;
}