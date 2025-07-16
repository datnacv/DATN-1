package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.GioHang.ChiTietGioHangService;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
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
import java.util.List;

@Controller
@RequestMapping("/thanh-toan")
public class KHThanhToanController {

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
    public String datHang(@RequestParam("ptThanhToan") String ptThanhToan,
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
            redirect.addFlashAttribute("error", "Giỏ hàng của bạn hiện đang rỗng. Vui lòng thêm sản phẩm trước khi thanh toán.");
            return "redirect:/thanh-toan";
        }

        DonHang donHang = new DonHang();
        donHang.setNguoiDung(nguoiDung);
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTrangThai("CHO_XAC_NHAN");
        donHangRepo.save(donHang);

        BigDecimal tongTien = BigDecimal.ZERO;
        for (ChiTietGioHang item : chiTietList) {
            if (item.getGia() == null || item.getChiTietSanPham() == null || item.getChiTietSanPham().getSanPham() == null) {
                redirect.addFlashAttribute("error", "Dữ liệu giỏ hàng không hợp lệ.");
                return "redirect:/thanh-toan";
            }

            ChiTietDonHang ct = new ChiTietDonHang();
            ct.setDonHang(donHang);
            ct.setChiTietSanPham(item.getChiTietSanPham());
            ct.setSoLuong(item.getSoLuong());
            ct.setGia(item.getGia());
            ct.setTenSanPham(item.getChiTietSanPham().getSanPham().getTenSanPham());
            BigDecimal thanhTien = item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong()));
            ct.setThanhTien(thanhTien);
            chiTietDonHangRepo.save(ct);

            tongTien = tongTien.add(thanhTien);
        }

        HoaDon hoaDon = new HoaDon();
        hoaDon.setDonHang(donHang);
        hoaDon.setNgayTao(LocalDateTime.now());
        hoaDon.setTongTien(tongTien);
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