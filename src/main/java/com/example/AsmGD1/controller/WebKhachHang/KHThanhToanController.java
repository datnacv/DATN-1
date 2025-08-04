package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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

    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungService phieuGiamGiaCuaNguoiDungService;

    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof NguoiDung) {
            return ((NguoiDung) authentication.getPrincipal()).getEmail();
        }
        return authentication.getName();
    }

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

            List<PhieuGiamGia> publicVouchers = phieuGiamGiaService.layTatCa().stream()
                    .filter(p -> "cong_khai".equalsIgnoreCase(p.getKieuPhieu()))
                    .filter(p -> "Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(p)))
                    .filter(p -> p.getGioiHanSuDung() != null && p.getGioiHanSuDung() > 0)
                    .toList();

            List<PhieuGiamGia> privateVouchers = phieuGiamGiaCuaNguoiDungService.layPhieuCaNhanConHan(nguoiDung.getId());

            List<PhieuGiamGia> allVouchers = new ArrayList<>();
            allVouchers.addAll(publicVouchers);
            allVouchers.addAll(privateVouchers);

            model.addAttribute("vouchers", allVouchers);
            logger.info("Tổng số voucher truyền ra view: {}", allVouchers.size());
            allVouchers.forEach(v -> logger.info(" - Voucher: {}", v.getMa()));

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

        String email = extractEmailFromAuthentication(authentication);
        if (email == null) {
            redirect.addFlashAttribute("error", "Không thể xác định email người dùng. Vui lòng đăng nhập lại.");
            return "redirect:/thanh-toan";
        }

        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email).orElse(null);
        if (nguoiDung == null) {
            redirect.addFlashAttribute("error", "Không tìm thấy người dùng với email: " + email);
            return "redirect:/thanh-toan";
        }

        GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
        List<ChiTietGioHang> chiTietList = chiTietGioHangService.getGioHangChiTietList(gioHang.getId());
        if (chiTietList.isEmpty()) {
            redirect.addFlashAttribute("error", "Giỏ hàng của bạn hiện đang rỗng.");
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
                .orElseThrow(() -> new RuntimeException("Phương thức thanh toán không hợp lệ."));
        donHang.setPhuongThucThanhToan(pttt);

        BigDecimal tongTien = BigDecimal.ZERO;
        List<ChiTietDonHang> chiTietListDH = new ArrayList<>();

        for (ChiTietGioHang item : chiTietList) {
            ChiTietSanPham chiTietSP = item.getChiTietSanPham();
            if (chiTietSP == null || chiTietSP.getSanPham() == null || item.getGia() == null) {
                redirect.addFlashAttribute("error", "Dữ liệu giỏ hàng không hợp lệ.");
                return "redirect:/thanh-toan";
            }

            if (chiTietSP.getSoLuongTonKho() < item.getSoLuong()) {
                redirect.addFlashAttribute("error", "Sản phẩm " + chiTietSP.getSanPham().getTenSanPham() + " không đủ số lượng.");
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
        if (voucher != null && !voucher.trim().isEmpty()) {
            PhieuGiamGia phieu = phieuGiamGiaService.layTatCa().stream()
                    .filter(p -> p.getMa() != null && p.getMa().equalsIgnoreCase(voucher.trim()))
                    .findFirst()
                    .orElse(null);

            if (phieu == null) {
                redirect.addFlashAttribute("error", "Mã giảm giá không tồn tại.");
                return "redirect:/thanh-toan";
            }

            if (!"Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(phieu))) {
                redirect.addFlashAttribute("error", "Phiếu giảm giá không trong thời gian hiệu lực.");
                return "redirect:/thanh-toan";
            }

            if (phieu.getGiaTriGiamToiThieu() != null &&
                    tongTien.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                redirect.addFlashAttribute("error", "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã.");
                return "redirect:/thanh-toan";
            }

            boolean isCaNhan = "CA_NHAN".equalsIgnoreCase(phieu.getKieuPhieu());
            boolean used = isCaNhan
                    ? phieuGiamGiaCuaNguoiDungService.suDungPhieu(nguoiDung.getId(), phieu.getId())
                    : phieuGiamGiaService.apDungPhieuGiamGia(phieu.getId());

            if (!used) {
                redirect.addFlashAttribute("error", isCaNhan
                        ? "Mã giảm giá cá nhân không khả dụng hoặc đã hết lượt sử dụng."
                        : "Mã giảm giá công khai đã hết lượt sử dụng.");
                return "redirect:/thanh-toan";
            }

            giamGia = phieuGiamGiaService.tinhTienGiamGia(phieu, tongTien);
            logger.info("🎯 Áp dụng mã tại /dat-hang - Mã: {}, Loại: {}, Giá trị giảm: {}, Tổng tiền: {}, Giảm giá tính được: {}",
                    phieu.getMa(), phieu.getLoai(), phieu.getGiaTriGiam(), tongTien, giamGia);
        }

        donHang.setTongTien(tongTien.add(shippingFee).subtract(giamGia));
        donHangRepo.save(donHang);
        chiTietDonHangRepo.saveAll(chiTietListDH);
        chiTietGioHangService.clearGioHang(gioHang.getId());

        redirect.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn hàng: " + donHang.getMaDonHang());
        return "redirect:/thanh-toan";
    }
}