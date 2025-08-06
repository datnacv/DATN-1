package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.DanhGia;
import com.example.AsmGD1.entity.GioHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.dto.KhachHang.ChiTietSanPhamDto;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.WebKhachHang.DanhGiaRepository;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangSanPhamRepository;
import com.example.AsmGD1.repository.WebKhachHang.LichSuTimKiemRepository;
import com.example.AsmGD1.service.GioHang.KhachHangGioHangService;
import com.example.AsmGD1.service.WebKhachHang.KhachhangSanPhamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Controller
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class); // Sửa logger để dùng đúng class

    private final KhachhangSanPhamService khachhangSanPhamService;
    private final KhachHangGioHangService khachHangGioHangService;
    @Autowired
    private KhachHangSanPhamRepository khachHangSanPhamRepository;
    @Autowired
    private NguoiDungRepository nguoiDungRepository; // Thêm repository

    @Autowired
    private LichSuTimKiemRepository lichSuTimKiemRepository;

    @Autowired
    private DanhGiaRepository danhGiaRepository;

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

        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        String giaFormatted = format.format(productDetail.getGia()) + " VNĐ";

        // 👇 Lấy sản phẩm liên quan
        List<SanPhamDto> sanPhamLienQuan = khachhangSanPhamService.getSanPhamLienQuan(sanPhamId, 6);

        model.addAttribute("giaFormatted", giaFormatted);
        model.addAttribute("productDetail", productDetail);
        model.addAttribute("sanPhamLienQuan", sanPhamLienQuan); // 👈 Gửi qua view

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

    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<SanPhamDto>> searchProducts(@RequestParam("keyword") String keyword) {
        try {
            List<SanPhamDto> results = khachhangSanPhamService.searchProducts(keyword);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Lỗi khi tìm kiếm sản phẩm với từ khóa {}: {}", keyword, e.getMessage());
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    @GetMapping("/api/search-with-history")
    @ResponseBody
    public ResponseEntity<List<SanPhamDto>> searchProductsWithHistory(@RequestParam("keyword") String keyword, Authentication authentication) {
        try {
            NguoiDung nguoiDung = null;
            if (authentication != null && authentication.isAuthenticated()) {
                String email = extractEmailFromAuthentication(authentication);
                if (email != null) {
                    nguoiDung = nguoiDungRepository.findByEmail(email)
                            .orElse(null);
                }
            }
            List<SanPhamDto> results = khachhangSanPhamService.searchProductsWithHistory(keyword, nguoiDung);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Lỗi khi tìm kiếm sản phẩm với từ khóa {}: {}", keyword, e.getMessage());
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    // Phương thức mới: Lấy lịch sử tìm kiếm
    @GetMapping("/api/search-history")
    @ResponseBody
    public ResponseEntity<List<String>> getSearchHistory(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            List<String> history = khachhangSanPhamService.getSearchHistory(nguoiDung.getId());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy lịch sử tìm kiếm: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    @GetMapping("/search")
    public String searchProducts(@RequestParam("keyword") String keyword, Model model, Authentication authentication) {
        try {
            NguoiDung nguoiDung = null;
            if (authentication != null && authentication.isAuthenticated()) {
                String email = extractEmailFromAuthentication(authentication);
                if (email != null) {
                    nguoiDung = nguoiDungRepository.findByEmail(email)
                            .orElse(null);
                    model.addAttribute("loggedInUser", nguoiDung);
                    model.addAttribute("user", nguoiDung);
                }
            } else {
                model.addAttribute("loggedInUser", null);
                model.addAttribute("user", null);
            }

            List<SanPhamDto> products = khachhangSanPhamService.searchProductsWithHistory(keyword, nguoiDung);
            model.addAttribute("products", products);
            model.addAttribute("keyword", keyword);
            return "WebKhachHang/search-results";
        } catch (Exception e) {
            logger.error("Lỗi khi tìm kiếm sản phẩm với từ khóa {}: {}", keyword, e.getMessage());
            model.addAttribute("error", "Lỗi khi tìm kiếm sản phẩm: " + e.getMessage());
            return "WebKhachHang/error";
        }
    }

    @DeleteMapping("/api/search-history")
    @ResponseBody
    public ResponseEntity<String> clearSearchHistory(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập!");
            }
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể xác định người dùng!");
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            lichSuTimKiemRepository.deleteByNguoiDungId(nguoiDung.getId());
            return ResponseEntity.ok("Đã xóa lịch sử tìm kiếm!");
        } catch (Exception e) {
            logger.error("Lỗi khi xóa lịch sử tìm kiếm: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi xóa lịch sử tìm kiếm!");
        }
    }

    // Trong HomeController.java
    @DeleteMapping("/api/search-history/delete")
    @ResponseBody
    public ResponseEntity<String> deleteSearchHistoryItem(@RequestParam("keyword") String keyword, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để xóa lịch sử tìm kiếm!");
            }
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể xác định người dùng!");
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
            lichSuTimKiemRepository.deleteByTuKhoaAndNguoiDungId(keyword, nguoiDung.getId());
            return ResponseEntity.ok("Đã xóa mục lịch sử tìm kiếm!");
        } catch (Exception e) {
            logger.error("Lỗi khi xóa mục lịch sử tìm kiếm: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi xóa mục lịch sử tìm kiếm!");
        }
    }

    @PostMapping("/api/save-search-history")
    @ResponseBody
    public ResponseEntity<String> saveSearchHistory(@RequestParam("keyword") String keyword, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthorized attempt to save search history");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để lưu lịch sử tìm kiếm!");
            }
            String email = extractEmailFromAuthentication(authentication);
            if (email == null) {
                logger.warn("Email extracted from authentication is null");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể xác định người dùng!");
            }
            NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                    .orElse(null);
            if (nguoiDung == null) {
                logger.warn("User not found for email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại!");
            }
            khachhangSanPhamService.saveSearchHistory(nguoiDung, keyword);
            return ResponseEntity.ok("Đã lưu lịch sử tìm kiếm!");
        } catch (Exception e) {
            logger.error("Lỗi khi lưu lịch sử tìm kiếm cho từ khóa '{}': {}", keyword, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lưu lịch sử tìm kiếm: " + e.getMessage());
        }
    }

    @GetMapping("/api/product/{sanPhamId}/ratings")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProductRatings(@PathVariable UUID sanPhamId, Authentication authentication) {
        List<DanhGia> allReviews = danhGiaRepository.findByChiTietSanPham_SanPham_IdOrderByThoiGianDanhGiaDesc(sanPhamId);

        UUID currentUserId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = extractEmailFromAuthentication(authentication);
            if (email != null) {
                currentUserId = nguoiDungRepository.findByEmail(email)
                        .map(NguoiDung::getId)
                        .orElse(null);
            }
        }

        List<Map<String, Object>> myReviews = new ArrayList<>();
        List<Map<String, Object>> otherReviews = new ArrayList<>();

        for (DanhGia dg : allReviews) {
            Map<String, Object> map = new HashMap<>();
            map.put("user", dg.getNguoiDung().getHoTen());
            map.put("rating", dg.getXepHang());
            map.put("content", dg.getNoiDung());
            map.put("date", dg.getThoiGianDanhGia());
            map.put("media", dg.getUrlHinhAnh());

            if (currentUserId != null && dg.getNguoiDung().getId().equals(currentUserId)) {
                myReviews.add(map);
            } else {
                otherReviews.add(map);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("myReviews", myReviews);
        response.put("otherReviews", otherReviews);
        return ResponseEntity.ok(response);
    }


}