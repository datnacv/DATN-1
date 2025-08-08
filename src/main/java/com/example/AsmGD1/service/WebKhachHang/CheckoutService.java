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
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import com.example.AsmGD1.repository.ThongBao.KHChiTietThongBaoNhomRepository;
import com.example.AsmGD1.repository.ThongBao.KHThongBaoNhomRepository;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangChiTietSanPhamRepository;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
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

    @Autowired
    private ViThanhToanService viThanhToanService;

    @Autowired
    private DiaChiNguoiDungRepository diaChiNguoiDungRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public DonHang createOrder(NguoiDung nguoiDung, CheckoutRequest request, UUID addressId) {
        DonHang donHang = new DonHang();
        donHang.setNguoiDung(nguoiDung);
        donHang.setMaDonHang("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTrangThaiThanhToan(false);
        donHang.setPhuongThucBanHang("Online");
        donHang.setPhiVanChuyen(request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.valueOf(15000));
        donHang.setGhiChu(request.getNotes() != null ? request.getNotes() + " | Người nhận: " + request.getFullName() + ", SĐT: " + request.getPhone() : "Người nhận: " + request.getFullName() + ", SĐT: " + request.getPhone());

        // Handle address
        if (addressId != null) {
            DiaChiNguoiDung selectedAddress = diaChiNguoiDungRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Địa chỉ không hợp lệ."));
            donHang.setDiaChi(selectedAddress);
            donHang.setDiaChiGiaoHang(selectedAddress.getChiTietDiaChi() + ", " +
                    selectedAddress.getPhuongXa() + ", " +
                    selectedAddress.getQuanHuyen() + ", " +
                    selectedAddress.getTinhThanhPho());
        } else if (request.getAddress() != null && !request.getAddress().isEmpty()) {
            donHang.setDiaChiGiaoHang(request.getAddress());
        } else {
            DiaChiNguoiDung defaultAddress = diaChiNguoiDungRepository.findByNguoiDung_IdAndMacDinhTrue(nguoiDung.getId())
                    .orElseThrow(() -> new RuntimeException("Vui lòng chọn hoặc nhập địa chỉ giao hàng."));
            donHang.setDiaChi(defaultAddress);
            donHang.setDiaChiGiaoHang(defaultAddress.getChiTietDiaChi() + ", " +
                    defaultAddress.getPhuongXa() + ", " +
                    defaultAddress.getQuanHuyen() + ", " +
                    defaultAddress.getTinhThanhPho());
        }

        PhuongThucThanhToan pttt = phuongThucThanhToanRepository
                .findById(request.getPaymentMethodId())
                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không hợp lệ."));
        donHang.setPhuongThucThanhToan(pttt);

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
            chiTietSP.setSoLuongTonKho(chiTietSP.getSoLuongTonKho() - soLuong);
            chiTietSanPhamRepository.save(chiTietSP);
            logger.info("Đã giảm tồn kho: {} -> {}", chiTietSP.getSanPham().getTenSanPham(), chiTietSP.getSoLuongTonKho());
        }

        BigDecimal giamGia = BigDecimal.ZERO;
        if (request.getVoucher() != null && !request.getVoucher().isEmpty()) {
            PhieuGiamGia voucher = phieuGiamGiaRepository.findByMa(request.getVoucher())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));
            if (voucher.getSoLuong() <= 0
                    || voucher.getNgayBatDau().isAfter(LocalDateTime.now())
                    || voucher.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Mã giảm giá không còn hiệu lực.");
            }
            giamGia = voucher.getGiaTriGiam().min(tongTien);
            donHang.setPhieuGiamGia(voucher);
            voucher.setSoLuong(voucher.getSoLuong() - 1);
            phieuGiamGiaRepository.save(voucher);
        }

        donHang.setTienGiam(giamGia);
        donHang.setTongTien(tongTien.add(donHang.getPhiVanChuyen()).subtract(giamGia));

        donHang = donHangRepository.save(donHang);
        UUID VI_PAYMENT_METHOD_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440019");
        if (VI_PAYMENT_METHOD_ID.equals(request.getPaymentMethodId())) {
            boolean paymentSuccess = viThanhToanService.thanhToanBangVi(
                    nguoiDung.getId(),
                    donHang.getId(),
                    donHang.getTongTien()
            );
            if (!paymentSuccess) {
                throw new RuntimeException("Số dư ví không đủ để thanh toán đơn hàng.");
            }
        }

        donHang.setTrangThaiThanhToan(true);
        donHang.setThoiGianThanhToan(LocalDateTime.now());
        donHangRepository.save(donHang);
        chiTietDonHangRepository.saveAll(chiTietList);
        logger.info("Đã lưu đơn hàng: {}", donHang.getMaDonHang());

        hoaDonService.createHoaDonFromDonHang(donHang);
        GioHang gioHang = gioHangRepository.findByNguoiDungId(nguoiDung.getId());
        if (gioHang != null) {
            for (CheckoutRequest.OrderItem item : request.getOrderItems()) {
                chiTietGioHangRepository.deleteByGioHangIdAndChiTietSanPhamId(gioHang.getId(), item.getChiTietSanPhamId());
                logger.info("Đã xoá sản phẩm {} khỏi giỏ hàng", item.getChiTietSanPhamId());
            }
        }

// Định dạng tổng tiền thành tiền tệ
        DecimalFormat formatter = new DecimalFormat("#,###.###");
        String formattedTongTien = formatter.format(donHang.getTongTien()) + " VNĐ";

// Lấy thông tin người nhận từ request
        String fullName = request.getFullName();  // Tên người nhận từ request
        String phone = request.getPhone();  // Số điện thoại người nhận từ request
        String shippingAddress = donHang.getDiaChiGiaoHang();  // Địa chỉ giao hàng từ đơn hàng

// Gửi email cho khách hàng
        String subject = "Xác nhận đơn hàng #" + donHang.getMaDonHang();
        String paymentMethodName = donHang.getPhuongThucThanhToan().getTenPhuongThuc();
        String text = "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;" +
                "background-color: #f8fafc;" +
                "color: #1e3a8a;" +
                "margin: 0;" +
                "padding: 20px;" +
                "line-height: 1.5;" +
                "}" +
                ".container {" +
                "max-width: 600px;" +
                "margin: 0 auto;" +
                "background-color: #ffffff;" +
                "border-radius: 12px;" +
                "box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);" +
                "padding: 30px;" +
                "}" +
                "h1 {" +
                "color: #2563eb;" +
                "text-align: center;" +
                "font-size: 24px;" +
                "font-weight: 600;" +
                "margin-bottom: 20px;" +
                "}" +
                "p {" +
                "font-size: 16px;" +
                "color: #334155;" +
                "margin-bottom: 16px;" +
                "}" +
                ".order-details {" +
                "background-color: #f1f5f9;" +
                "padding: 20px;" +
                "border-radius: 8px;" +
                "margin-bottom: 20px;" +
                "border: 1px solid #e2e8f0;" +
                "}" +
                ".order-details p {" +
                "margin: 8px 0;" +
                "font-size: 15px;" +
                "}" +
                ".order-details strong {" +
                "color: #1e3a8a;" +
                "font-weight: 500;" +
                "}" +
                ".cta-button {" +
                "text-align: center;" +
                "margin: 20px 0;" +
                "}" +
                ".cta-button a {" +
                "display: inline-block;" +
                "padding: 12px 24px;" +
                "background-color: #2563eb;" +
                "color: #ffffff;" +
                "text-decoration: none;" +
                "border-radius: 6px;" +
                "font-size: 16px;" +
                "font-weight: 500;" +
                "transition: background-color 0.3s ease;" +
                "}" +
                ".cta-button a:hover {" +
                "background-color: #1e40af;" +
                "}" +
                ".footer {" +
                "text-align: center;" +
                "font-size: 14px;" +
                "color: #64748b;" +
                "margin-top: 30px;" +
                "padding-top: 20px;" +
                "border-top: 1px solid #e2e8f0;" +
                "}" +
                ".footer a {" +
                "color: #2563eb;" +
                "text-decoration: none;" +
                "}" +
                ".footer a:hover {" +
                "text-decoration: underline;" +
                "}" +
                "@media (max-width: 600px) {" +
                ".container {" +
                "padding: 20px;" +
                "}" +
                "h1 {" +
                "font-size: 20px;" +
                "}" +
                ".cta-button a {" +
                "padding: 10px 20px;" +
                "font-size: 14px;" +
                "}" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<h1>Cảm ơn bạn đã mua hàng tại ACV Store!</h1>" +
                "<p>Đơn hàng của bạn đã được đặt thành công! Chúng tôi sẽ nhanh chóng xử lý và gửi đơn hàng đến bạn trong thời gian sớm nhất. Dưới đây là thông tin chi tiết về đơn hàng:</p>" +
                "<div class='order-details'>" +
                "<p><strong>Mã đơn hàng:</strong> " + donHang.getMaDonHang() + "</p>" +
                "<p><strong>Tên người nhận:</strong> " + fullName + "</p>" +
                "<p><strong>Số điện thoại người nhận:</strong> " + phone + "</p>" +
                "<p><strong>Địa chỉ giao hàng:</strong> " + shippingAddress + "</p>" +
                "<p><strong>Tổng tiền:</strong> " + formattedTongTien + "</p>" +
                "<p><strong>Phương thức thanh toán:</strong> " + paymentMethodName + "</p>" +
                "</div>" +
                "<div class='cta-button'>" +
                "<a href='https://acvstore.site/dsdon-mua/chi-tiet/" + donHang.getId() + "'>Xem trạng thái đơn hàng</a>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>ACV Store - Cảm ơn bạn đã tin tưởng và mua sắm tại cửa hàng của chúng tôi!</p>" +
                "<p><a href='https://acvstore.site'>Truy cập ACV Store</a> | <a href='mailto:datn.acv@gmail.com'>Liên hệ hỗ trợ</a></p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        emailService.sendEmail(nguoiDung.getEmail(), subject, text);
        return donHang;
    }


}