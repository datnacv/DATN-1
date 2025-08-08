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
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
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

    @Autowired
    private ViThanhToanService viThanhToanService;

    @Autowired
    private DiaChiNguoiDungRepository diaChiNguoiDungRepository;

    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungService phieuGiamGiaCuaNguoiDungService;
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
            PhieuGiamGia phieu = phieuGiamGiaRepository.findByMa(request.getVoucher())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại."));

            // Trạng thái phiếu: phải đang diễn ra
            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieu))) {
                throw new RuntimeException("Phiếu giảm giá không trong thời gian hiệu lực.");
            }

            // Đạt giá trị tối thiểu mới cho áp mã
            if (phieu.getGiaTriGiamToiThieu() != null && tongTien.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã.");
            }

            // Kiểm tra loại phiếu: cá nhân / công khai và TRỪ LƯỢT **tại đây** (chỉ 1 lần khi chốt đơn)
            boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(phieu.getKieuPhieu());
            boolean used = isCaNhan
                    ? phieuGiamGiaCuaNguoiDungService.suDungPhieu(nguoiDung.getId(), phieu.getId())
                    : phieuGiamGiaService.apDungPhieuGiamGia(phieu.getId());

            if (!used) {
                throw new RuntimeException(isCaNhan
                        ? "Mã giảm giá cá nhân không khả dụng hoặc đã hết lượt sử dụng."
                        : "Mã giảm giá công khai đã hết lượt sử dụng.");
            }

            // TÍNH GIẢM = cùng một hàm như bên KHThanhToanController
            giamGia = phieuGiamGiaService.tinhTienGiamGia(phieu, tongTien);

            donHang.setPhieuGiamGia(phieu);
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
            donHang.setTrangThaiThanhToan(true);
            donHang.setThoiGianThanhToan(LocalDateTime.now());
            donHangRepository.save(donHang);
        }

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
        return donHang;
    }


}