package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/thanh-toan")
public class KHThanhToanController {

    private static final Logger logger = LoggerFactory.getLogger(KHThanhToanController.class);

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private KhachHangGioHangService khachHangGioHangService;

    @Autowired
    private ChiTietGioHangService chiTietGioHangService;

    @Autowired
    private DonHangRepository donHangRepo;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepo;

    @Autowired
    private HoaDonRepository hoaDonRepo;

    @Autowired
    private LichSuHoaDonRepository lichSuRepo;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucRepo;

    @Autowired
    private ViThanhToanService viThanhToanService;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @GetMapping
    public String showCheckoutPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("error", "Vui lòng đăng nhập để thanh toán!");
            return "WebKhachHang/thanh-toan";
        }

        try {
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                model.addAttribute("error", "Không thể xác định người dùng. Vui lòng thử lại!");
                return "WebKhachHang/thanh-toan";
            }

            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng với email " + email + " không tồn tại"));

            model.addAttribute("loggedInUser", nguoiDung);
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải thông tin người dùng: " + e.getMessage());
        }

        return "WebKhachHang/thanh-toan";
    }

    @PostMapping("/dat-hang")
    public String datHang(
            @RequestParam("ptThanhToan") String ptThanhToan,
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            @RequestParam("address") String address,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "voucher", required = false) String voucher,
            @RequestParam(value = "shippingFee", required = false, defaultValue = "15000") BigDecimal shippingFee,
            Authentication authentication,
            RedirectAttributes redirect) {
        logger.info("Processing order: ptThanhToan={}, fullName={}, phone={}, address={}, voucher={}, shippingFee={}",
                ptThanhToan, fullName, phone, address, voucher, shippingFee);
        logger.info("Phương thức thanh toán người dùng chọn: {}", ptThanhToan);


        String email = extractEmailFromAuthentication(authentication);
        if (email == null) {
            redirect.addFlashAttribute("error", "Không thể xác định email người dùng. Vui lòng đăng nhập lại.");
            logger.error("Authentication email is null");
            return "redirect:/thanh-toan";
        }

        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email).orElse(null);
        if (nguoiDung == null) {
            redirect.addFlashAttribute("error", "Không tìm thấy người dùng với email: " + email);
            logger.error("User not found for email: {}", email);
            return "redirect:/thanh-toan";
        }

        GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
        List<ChiTietGioHang> chiTietList = chiTietGioHangService.getGioHangChiTietList(gioHang.getId());
        if (chiTietList.isEmpty()) {
            redirect.addFlashAttribute("error", "Giỏ hàng của bạn hiện đang rỗng. Vui lòng thêm sản phẩm trước khi thanh toán.");
            logger.warn("Cart is empty for userId: {}", nguoiDung.getId());
            return "redirect:/thanh-toan";
        }

        DonHang donHang = new DonHang();
        donHang.setNguoiDung(nguoiDung);
        donHang.setMaDonHang("DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTrangThai("CHO_XAC_NHAN");
        donHang.setPhuongThucBanHang("Online");
        donHang.setPhiVanChuyen(shippingFee);
        donHang.setDiaChiGiaoHang(address);
        donHang.setGhiChu(note);

        PhuongThucThanhToan pttt = phuongThucRepo.findById(UUID.fromString(ptThanhToan))
                .orElseThrow(() -> {
                    logger.error("Invalid payment method: {}", ptThanhToan);
                    return new RuntimeException("Phương thức thanh toán không hợp lệ.");
                });
        donHang.setPhuongThucThanhToan(pttt);

        BigDecimal tongTien = BigDecimal.ZERO;
        List<ChiTietDonHang> chiTietListDH = new ArrayList<>();
        for (ChiTietGioHang item : chiTietList) {
            ChiTietSanPham chiTietSP = item.getChiTietSanPham();
            if (chiTietSP == null || chiTietSP.getSanPham() == null || item.getGia() == null) {
                redirect.addFlashAttribute("error", "Dữ liệu giỏ hàng không hợp lệ.");
                logger.error("Invalid cart item data: chiTietSanPham={}, gia={}", chiTietSP, item.getGia());
                return "redirect:/thanh-toan";
            }

            if (chiTietSP.getSoLuongTonKho() < item.getSoLuong()) {
                redirect.addFlashAttribute("error", "Sản phẩm " + chiTietSP.getSanPham().getTenSanPham() + " không đủ số lượng trong kho.");
                logger.warn("Insufficient stock for product: {}, requested: {}, available: {}",
                        chiTietSP.getSanPham().getTenSanPham(), item.getSoLuong(), chiTietSP.getSoLuongTonKho());
                return "redirect:/thanh-toan";
            }

            BigDecimal gia = item.getGia();
            int soLuong = item.getSoLuong();
            BigDecimal thanhTien = gia.multiply(BigDecimal.valueOf(soLuong));

            ChiTietDonHang ct = new ChiTietDonHang();
            ct.setDonHang(donHang);
            ct.setChiTietSanPham(chiTietSP);
            ct.setSoLuong(soLuong);
            ct.setGia(gia);
            ct.setTenSanPham(chiTietSP.getSanPham().getTenSanPham());
            ct.setThanhTien(thanhTien);
            chiTietListDH.add(ct);

            chiTietSP.setSoLuongTonKho(chiTietSP.getSoLuongTonKho() - soLuong);
            chiTietSanPhamRepository.save(chiTietSP);
            tongTien = tongTien.add(thanhTien);
        }

        BigDecimal giamGia = BigDecimal.ZERO;
        donHang.setTongTien(tongTien.add(shippingFee).subtract(giamGia));

        donHangRepo.save(donHang);
        chiTietDonHangRepo.saveAll(chiTietListDH);

        if (ptThanhToan.equals("550E8400-E29B-41D4-A716-446655440019")) {
            try {
                boolean paymentSuccess = viThanhToanService.thanhToanBangVi(
                        nguoiDung.getId(),
                        donHang.getId(),
                        donHang.getTongTien()
                );
                if (!paymentSuccess) {
                    redirect.addFlashAttribute("error", "Số dư ví không đủ để thanh toán đơn hàng.");
                    logger.warn("Wallet payment failed: insufficient balance for userId: {}, orderId: {}, amount: {}",
                            nguoiDung.getId(), donHang.getId(), donHang.getTongTien());
                    return "redirect:/thanh-toan";
                }
                logger.info("Wallet payment successful: userId={}, orderId={}, amount={}",
                        nguoiDung.getId(), donHang.getId(), donHang.getTongTien());
            } catch (Exception e) {
                redirect.addFlashAttribute("error", "Lỗi khi thanh toán bằng ví: " + e.getMessage());
                logger.error("Wallet payment error: userId={}, orderId={}, error={}",
                        nguoiDung.getId(), donHang.getId(), e.getMessage());
                return "redirect:/thanh-toan";
            }
        }

        HoaDon hoaDon = new HoaDon();
        hoaDon.setDonHang(donHang);
        hoaDon.setNguoiDung(nguoiDung);
        hoaDon.setNgayTao(LocalDateTime.now());
        hoaDon.setTongTien(donHang.getTongTien());
        hoaDon.setTienGiam(giamGia);
        hoaDon.setPhuongThucThanhToan(pttt);
        hoaDon.setTrangThai("CHO_XAC_NHAN");
        hoaDonRepo.save(hoaDon);

        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setThoiGian(LocalDateTime.now());
        lichSu.setTrangThai("CHO_XAC_NHAN");
        lichSu.setGhiChu("Đơn hàng được tạo");
        lichSuRepo.save(lichSu);

        chiTietGioHangService.clearGioHang(gioHang.getId());

        redirect.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn hàng: " + donHang.getMaDonHang());
        logger.info("Order created successfully: orderId={}, maDonHang={}", donHang.getId(), donHang.getMaDonHang());
        return "redirect:/don-mua";
    }

    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return (String) oauthToken.getPrincipal().getAttributes().get("email");
        } else if (authentication.getPrincipal() instanceof NguoiDung user) {
            return user.getEmail();
        }
        return null;
    }
}