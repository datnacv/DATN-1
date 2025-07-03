package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.WebKhachHang.KhachhangSanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String home(Model model) {
        model.addAttribute("newProducts", khachhangSanPhamService.getNewProducts());
        model.addAttribute("summerProducts", khachhangSanPhamService.getNewProducts());
        model.addAttribute("bestsellerProducts", khachhangSanPhamService.getNewProducts());
        return "WebKhachHang/index";
    }

    @GetMapping("/chitietsanpham")
    public String productDetail(@RequestParam("id") UUID sanPhamId, Model model) {
        ChiTietSanPhamDto productDetail = khachhangSanPhamService.getProductDetail(sanPhamId);
        if (productDetail == null) {
            return "redirect:/"; // Chuyển hướng nếu không tìm thấy sản phẩm
        }
        model.addAttribute("productDetail", productDetail);
        model.addAttribute("productImages", productDetail.getHinhAnhList());
        return "WebKhachHang/chitietsanpham";
    }

    @GetMapping("/cart")
    public String cart(Model model) {
        // Sử dụng nguoiDungId có sẵn trong database
        UUID nguoiDungId = UUID.fromString("550e8400-e29b-41d4-a716-446655440014"); // Trần Thị Customer
        try {
            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
            model.addAttribute("gioHangId", gioHang.getId());
            model.addAttribute("chiTietGioHang", khachHangGioHangService.getGioHangChiTiets(gioHang.getId()));
            model.addAttribute("tongTien", gioHang.getTongTien());
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải giỏ hàng: " + e.getMessage());
            return "WebKhachHang/cart"; // Vẫn hiển thị trang với thông báo lỗi
        }
        return "WebKhachHang/cart";
    }
}