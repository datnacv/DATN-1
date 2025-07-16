package com.example.AsmGD1.service.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ThanhToan.CheckoutRequest;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.KHChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.KHDonHangRepository;
import com.example.AsmGD1.repository.BanHang.KHPhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.GiamGia.KHPhieuGiamGiaRepository;
import com.example.AsmGD1.repository.GioHang.ChiTietGioHangRepository;
import com.example.AsmGD1.repository.GioHang.GioHangRepository;
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

    @Autowired
    private ChiTietGioHangRepository chiTietGioHangRepository;

    @Autowired
    private GioHangRepository gioHangRepository;


    @Transactional
    public DonHang createOrder(NguoiDung nguoiDung, CheckoutRequest request) {
        // 1. Tạo đơn hàng
        DonHang donHang = new DonHang();
        donHang.setNguoiDung(nguoiDung);
        donHang.setMaDonHang("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTrangThaiThanhToan(true); // Giả sử thanh toán ngay (COD, chuyển khoản...)
        donHang.setThoiGianThanhToan(LocalDateTime.now());
        donHang.setPhuongThucBanHang("Giao hàng");
        donHang.setPhiVanChuyen(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.valueOf(15000));
        donHang.setDiaChiGiaoHang(request.getAddress());
        donHang.setGhiChu(request.getNotes());

        // 2. Thiết lập phương thức thanh toán
        if (request.getPaymentMethodId() != null) {
            PhuongThucThanhToan pttt = phuongThucThanhToanRepository
                    .findById(request.getPaymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không hợp lệ."));
            donHang.setPhuongThucThanhToan(pttt);
        }

        // 3. Tính tiền và chi tiết
        BigDecimal tongTien = BigDecimal.ZERO;
        List<ChiTietDonHang> chiTietList = new ArrayList<>();

        for (CheckoutRequest.OrderItem item : request.getOrderItems()) {
            ChiTietSanPham chiTietSP = chiTietSanPhamRepository.findById(item.getChiTietSanPhamId())
                    .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại: " + item.getChiTietSanPhamId()));

            if (chiTietSP.getSoLuongTonKho() < item.getSoLuong()) {
                throw new RuntimeException("Sản phẩm " + chiTietSP.getSanPham().getTenSanPham() + " không đủ số lượng trong kho. Còn lại: " + chiTietSP.getSoLuongTonKho());
            }

            BigDecimal gia = chiTietSP.getGia();
            int soLuong = item.getSoLuong();
            BigDecimal thanhTien = gia.multiply(BigDecimal.valueOf(soLuong));

            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(donHang);
            chiTiet.setChiTietSanPham(chiTietSP);
            chiTiet.setGia(gia);
            chiTiet.setSoLuong(soLuong);
            chiTiet.setThanhTien(thanhTien);
            chiTiet.setTenSanPham(chiTietSP.getSanPham().getTenSanPham());
            chiTiet.setTrangThaiHoanTra(false);
            chiTiet.setGhiChu(request.getNotes());

            chiTietList.add(chiTiet);
            tongTien = tongTien.add(thanhTien);

            // Giảm tồn kho
            chiTietSP.setSoLuongTonKho(chiTietSP.getSoLuongTonKho() - soLuong);
            chiTietSanPhamRepository.save(chiTietSP);
            logger.info("Đã giảm tồn kho: {} -> {}", chiTietSP.getSanPham().getTenSanPham(), chiTietSP.getSoLuongTonKho());
        }

        // 4. Áp dụng mã giảm giá nếu có
        BigDecimal giamGia = BigDecimal.ZERO;
        if (request.getVoucher() != null && !request.getVoucher().isEmpty()) {
            PhieuGiamGia voucher = phieuGiamGiaRepository.findByMa(request.getVoucher())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));

            if (voucher.getSoLuong() <= 0 || voucher.getNgayKetThuc().isBefore(LocalDateTime.now().toLocalDate())) {
                throw new RuntimeException("Mã giảm giá đã hết hạn hoặc không còn số lượng");
            }

            giamGia = voucher.getGiaTriGiam().min(tongTien);
            donHang.setPhieuGiamGia(voucher);
            voucher.setSoLuong(voucher.getSoLuong() - 1); // giảm số lượng còn lại
            phieuGiamGiaRepository.save(voucher);
        }

        // 5. Gán tổng tiền, tiền giảm
        donHang.setTienGiam(giamGia);
        donHang.setTongTien(tongTien.add(donHang.getPhiVanChuyen()).subtract(giamGia));

        // 6. Lưu đơn hàng và chi tiết
        donHangRepository.save(donHang);
        chiTietDonHangRepository.saveAll(chiTietList);
        logger.info("Đã lưu đơn hàng: {}", donHang.getMaDonHang());

        // 7. Tạo hóa đơn
        hoaDonService.createHoaDonFromDonHang(donHang);

        // 8. Xoá sản phẩm khỏi giỏ hàng
        GioHang gioHang = gioHangRepository.findByNguoiDungId(nguoiDung.getId());
        if (gioHang != null) {
            for (CheckoutRequest.OrderItem item : request.getOrderItems()) {
                chiTietGioHangRepository.deleteByGioHangIdAndChiTietSanPhamId(gioHang.getId(), item.getChiTietSanPhamId());
                logger.info("Đã xoá sản phẩm {} khỏi giỏ hàng", item.getChiTietSanPhamId());
            }
        }

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