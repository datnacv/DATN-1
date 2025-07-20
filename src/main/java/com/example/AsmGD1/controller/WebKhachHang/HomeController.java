package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangSanPhamRepository;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.WebKhachHang.KhachhangSanPhamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class); // Sửa logger để dùng đúng class

    private final KhachhangSanPhamService khachhangSanPhamService;
    private final KhachHangGioHangService khachHangGioHangService;
    @Autowired
    private KhachHangSanPhamRepository khachHangSanPhamRepository;
    @Autowired
    private com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository nguoiDungRepository; // Thêm repository

    @Autowired
    public HomeController(KhachhangSanPhamService khachhangSanPhamService, KhachHangGioHangService khachHangGioHangService) {
        this.khachhangSanPhamService = khachhangSanPhamService;
        this.khachHangGioHangService = khachHangGioHangService;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        model.addAttribute("newProducts", khachhangSanPhamService.getNewProducts());
        model.addAttribute("summerProducts", khachhangSanPhamService.getNewProducts());
        model.addAttribute("bestsellerProducts", khachhangSanPhamService.getBestSellingProducts());

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                String email = extractEmailFromAuthentication(authentication);
                if (email != null) {
                    NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("Người dùng với email " + email + " không tồn tại"));

                    model.addAttribute("loggedInUser", nguoiDung);
                    model.addAttribute("user", nguoiDung); // ✅ Thêm dòng này

                    if ("customer".equals(nguoiDung.getVaiTro())) {
                        GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDung.getId());
                        model.addAttribute("gioHangId", gioHang.getId());
                        model.addAttribute("chiTietGioHang", khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) != null
                                ? khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) : java.util.Collections.emptyList());
                        model.addAttribute("tongTien", gioHang.getTongTien() != null ? gioHang.getTongTien() : BigDecimal.ZERO);
                    }
                }
            } catch (Exception e) {
                model.addAttribute("cartError", "Không thể tải giỏ hàng hoặc thông tin người dùng: " + e.getMessage());
            }
        } else {
            model.addAttribute("loggedInUser", null);
            model.addAttribute("user", null); // 👈 để tránh lỗi null trong navbar
        }

        return "WebKhachHang/index";
    }

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
    @GetMapping("/don-mua")
    public String donMuaPage(Model model, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof NguoiDung user) {
            model.addAttribute("user", user);
            // model.addAttribute("orders", donHangService.findByCustomerId(user.getId()));
            return "WebKhachHang/don-mua"; // trang hiển thị đơn hàng đã mua
        }
        return "redirect:/customers/login";
    }

    @GetMapping("/cart")
    public String cart(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("error", "Vui lòng đăng nhập để xem giỏ hàng!");
            return "WebKhachHang/cart";
        }

        try {
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                model.addAttribute("error", "Không thể xác định người dùng. Vui lòng thử lại!");
                return "WebKhachHang/cart";
            }

            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng với email " + email + " không tồn tại"));
            UUID nguoiDungId = nguoiDung.getId();
            GioHang gioHang = khachHangGioHangService.getOrCreateGioHang(nguoiDungId);
            model.addAttribute("gioHangId", gioHang.getId());
            model.addAttribute("chiTietGioHang", khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) != null
                    ? khachHangGioHangService.getGioHangChiTiets(gioHang.getId()) : java.util.Collections.emptyList());
            model.addAttribute("tongTien", gioHang.getTongTien() != null ? gioHang.getTongTien() : BigDecimal.ZERO);
            model.addAttribute("loggedInUser", nguoiDung);
        } catch (Exception e) {
            model.addAttribute("error", "Không thể tải giỏ hàng: " + e.getMessage());
        }
        return "WebKhachHang/cart";
    }

    @GetMapping("/api/getChiTietSanPham")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChiTietSanPhamId(
            @RequestParam("sanPhamId") UUID sanPhamId,
            @RequestParam("sizeId") UUID sizeId,
            @RequestParam("colorId") UUID colorId) {
        try {
            ChiTietSanPham chiTiet = khachHangSanPhamRepository.findBySanPhamIdAndSizeIdAndColorId(sanPhamId, sizeId, colorId);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sản phẩm không tồn tại"));
            }
            Map<String, Object> response = new HashMap<>();
            response.put("id", chiTiet.getId());
            response.put("gia", chiTiet.getGia());
            response.put("soLuongTonKho", chiTiet.getSoLuongTonKho());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy chi tiết sản phẩm với sanPhamId={}, sizeId={}, colorId={}: ", sanPhamId, sizeId, colorId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy chi tiết sản phẩm: " + e.getMessage()));
        }
    }

    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            return (String) oauthToken.getPrincipal().getAttributes().get("email");
        } else if (authentication.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) authentication.getPrincipal();
            return user.getEmail();
        }
        return null; // Trường hợp không xác định được email
    }
}