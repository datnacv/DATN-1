package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;

import com.example.AsmGD1.service.WebKhachHang.KhachhangSanPhamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class HomeController {
    private final KhachhangSanPhamService khachhangSanPhamService;

    public HomeController(KhachhangSanPhamService khachhangSanPhamService) {
        this.khachhangSanPhamService = khachhangSanPhamService;
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
}
