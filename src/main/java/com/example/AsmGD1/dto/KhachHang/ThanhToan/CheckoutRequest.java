package com.example.AsmGD1.dto.KhachHang.ThanhToan;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CheckoutRequest {
    private String fullName;
    private String phone;
    private String address;
    private String notes;
    private String shippingMethod;
    private UUID paymentMethodId;
    private String voucher;
    private List<OrderItem> orderItems;

    @Data
    public static class OrderItem {
        private UUID chiTietSanPhamId;
        private int soLuong;
    }
}