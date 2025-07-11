package com.example.AsmGD1.service.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ThanhToan.CheckoutRequest;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.KHChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.KHDonHangRepository;
import com.example.AsmGD1.repository.BanHang.KHPhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.GiamGia.KHPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.HoaDon.KHHoaDonRepository;
import com.example.AsmGD1.repository.ThongBao.KHChiTietThongBaoNhomRepository;
import com.example.AsmGD1.repository.ThongBao.KHThongBaoNhomRepository;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangChiTietSanPhamRepository;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    @Autowired
    private KHDonHangRepository donHangRepository;

    @Autowired
    private KHChiTietDonHangRepository chiTietDonHangRepository;

    @Autowired
    private KhachHangChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private KHPhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private KHHoaDonRepository hoaDonRepository;

    @Autowired
    private KHThongBaoNhomRepository thongBaoNhomRepository;

    @Autowired
    private KHChiTietThongBaoNhomRepository chiTietThongBaoNhomRepository;

    @Autowired
    private KHPhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Autowired
    private HoaDonService hoaDonService;


    @Transactional
    public DonHang createOrder(NguoiDung nguoiDung, CheckoutRequest request) {
        // 1. Tạo đối tượng đơn hàng
        DonHang donHang = new DonHang();
        donHang.setNguoiDung(nguoiDung);
        donHang.setMaDonHang("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTrangThaiThanhToan(false);
        donHang.setPhuongThucBanHang("Giao hàng");
        donHang.setPhiVanChuyen(BigDecimal.valueOf("EXPRESS".equals(request.getShippingMethod()) ? 30000 : 15000));

        // 2. Lấy phương thức thanh toán từ repository
        if (request.getPaymentMethodId() != null) {
            PhuongThucThanhToan pttt = phuongThucThanhToanRepository
                    .findById(request.getPaymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không hợp lệ."));
            donHang.setPhuongThucThanhToan(pttt);
        }

        donHang.setDiaChiGiaoHang(request.getAddress());
        donHang.setGhiChu(request.getNotes());

        // 3. Tính tổng tiền và tạo chi tiết đơn hàng
        BigDecimal tongTien = BigDecimal.ZERO;
        List<ChiTietDonHang> chiTietList = new ArrayList<>();

        for (CheckoutRequest.OrderItem item : request.getOrderItems()) {
            ChiTietSanPham chiTietSP = chiTietSanPhamRepository.findById(item.getChiTietSanPhamId())
                    .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại."));

            BigDecimal gia = chiTietSP.getGia();
            int soLuong = item.getSoLuong();
            BigDecimal thanhTien = gia.multiply(BigDecimal.valueOf(soLuong));

            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(donHang); // ✅ Đây là dòng QUAN TRỌNG để tránh lỗi NULL
            chiTiet.setChiTietSanPham(chiTietSP);
            chiTiet.setGia(gia);
            chiTiet.setSoLuong(soLuong);
            chiTiet.setThanhTien(thanhTien);
            chiTiet.setTenSanPham(chiTietSP.getSanPham().getTenSanPham());
            chiTiet.setTrangThaiHoanTra(false);
            chiTiet.setGhiChu(request.getNotes()); // hoặc request.getNote() nếu có

            chiTietList.add(chiTiet);
            tongTien = tongTien.add(thanhTien);
        }

        // 4. Áp dụng giảm giá nếu có
        BigDecimal giamGia = BigDecimal.ZERO;
        if (request.getVoucher() != null && !request.getVoucher().isEmpty()) {
            PhieuGiamGia voucher = phieuGiamGiaRepository.findByMa(request.getVoucher())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));
            giamGia = voucher.getGiaTriGiam().min(tongTien);
            donHang.setPhieuGiamGia(voucher);
        }

        // 5. Gán tổng tiền
        donHang.setTienGiam(giamGia);
        donHang.setTongTien(tongTien.add(donHang.getPhiVanChuyen()).subtract(giamGia));

        // 6. Lưu đơn hàng và chi tiết
        donHangRepository.save(donHang);
        chiTietDonHangRepository.saveAll(chiTietList);

        hoaDonService.createHoaDonFromDonHang(donHang);


        return donHang;
    }


    public BigDecimal calculateDiscount(PhieuGiamGia phieuGiamGia) {
        if ("Phần trăm".equals(phieuGiamGia.getLoai())) {
            BigDecimal discount = phieuGiamGia.getGiaTriGiam();
            if (phieuGiamGia.getGiaTriGiamToiDa() != null && discount.compareTo(phieuGiamGia.getGiaTriGiamToiDa()) > 0) {
                return phieuGiamGia.getGiaTriGiamToiDa();
            }
            return discount;
        }
        return phieuGiamGia.getGiaTriGiam();
    }
}