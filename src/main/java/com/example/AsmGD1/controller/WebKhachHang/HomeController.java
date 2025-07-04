package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.WebKhachHang.KhachhangSanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class HomeController {

    private final KhachhangSanPhamService khachhangSanPhamService;
    private final KhachHangGioHangService khachHangGioHangService;

    @Autowired
    public HomeController(KhachhangSanPhamService khachhangSanPhamService, KhachHangGioHangService khachHangGioHangService) {
        this.khachhangSanPhamService = khachhangSanPhamService;
        this.khachHangGioHangService = khachHangGioHangService;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        // Thêm sản phẩm
        model.addAttribute("newProducts", khachhangSanPhamService.getNewProducts());
        model.addAttribute("summerProducts", khachhangSanPhamService.getNewProducts()); // Có thể thay đổi logic cho danh mục mùa hè
        model.addAttribute("bestsellerProducts", khachhangSanPhamService.getBestSellingProducts());

        // Thêm thông tin người dùng nếu đã đăng nhập
        if (authentication != null && authentication.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) authentication.getPrincipal();
            model.addAttribute("loggedInUser", user);
            if ("customer".equals(user.getVaiTro())) {
                try {
                    GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(user.getId());
                    model.addAttribute("gioHangId", gioHang.getId());
                    model.addAttribute("chiTietGioHang", khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) != null
                            ? khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) : java.util.Collections.emptyList());
                    model.addAttribute("tongTien", gioHang.getTongTien() != null ? gioHang.getTongTien() : BigDecimal.ZERO);
                } catch (Exception e) {
                    model.addAttribute("cartError", "Không thể tải giỏ hàng: " + e.getMessage());
                }
            }
        } else {
            model.addAttribute("loggedInUser", null); // Đảm bảo có giá trị mặc định
        }
        return "WebKhachHang/index";
    }

    // Các phương thức khác giữ nguyên
    @GetMapping("/chitietsanpham")
    public String productDetail(@RequestParam("id") UUID sanPhamId, Model model) {
        ChiTietSanPhamDto productDetail = khachhangSanPhamService.getProductDetail(sanPhamId);
        if (productDetail == null) {
            model.addAttribute("error", "Sản phẩm không tồn tại hoặc đã bị xóa!");
            return "WebKhachHang/error";
        }
        model.addAttribute("productDetail", productDetail);
        return "WebKhachHang/chitietsanpham";
    }

    @GetMapping("/cart")
    public String cart(Model model, Authentication authentication) {
        UUID nguoiDungId = (authentication != null && authentication.getPrincipal() instanceof NguoiDung)
                ? ((NguoiDung) authentication.getPrincipal()).getId()
                : null;
        if (nguoiDungId == null) {
            model.addAttribute("error", "Vui lòng đăng nhập để xem giỏ hàng!");
            return "WebKhachHang/cart";
        }

        try {
            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
            model.addAttribute("gioHangId", gioHang.getId());
            model.addAttribute("chiTietGioHang", khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) != null
                    ? khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) : java.util.Collections.emptyList());
            model.addAttribute("tongTien", gioHang.getTongTien() != null ? gioHang.getTongTien() : BigDecimal.ZERO);
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải giỏ hàng: " + e.getMessage());
        }
        return "WebKhachHang/cart";
    }
}