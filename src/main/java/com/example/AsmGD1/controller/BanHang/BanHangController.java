package com.example.AsmGD1.controller.BanHang;

import com.example.AsmGD1.dto.BanHang.*;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.service.BanHang.DonHangService;
import com.example.AsmGD1.service.BanHang.GioHangService;
import com.example.AsmGD1.service.BanHang.QRCodeService;
import com.example.AsmGD1.service.BanHang.VNPayService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.ChiTietSanPhamService;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/banhang")
public class BanHangController {

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private ChiTietSanPhamService chiTietSanPhamService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungService phieuGiamGiaCuaNguoiDungService;

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private DonHangService donHangService;

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private QRCodeService maQRService;

    @GetMapping
    public String hienThiTrangBanHang(Model model) {
        model.addAttribute("products", sanPhamService.findAll());
        model.addAttribute("customers", nguoiDungService.findUsersByVaiTro("customer", "", 0, 10).getContent());
        model.addAttribute("discountVouchers", phieuGiamGiaService.layTatCa().stream()
                .filter(v -> phieuGiamGiaService.tinhTrang(v).equals("Đang diễn ra"))
                .collect(Collectors.toList()));
        // Lấy giỏ hàng hiện tại từ session với phí vận chuyển mặc định
        BigDecimal defaultShippingFee = BigDecimal.ZERO;
        model.addAttribute("gioHang", gioHangService.layGioHang(defaultShippingFee));
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        return "WebQuanLy/ban-hang";
    }

    @GetMapping("/product/{productId}/variants")
    @ResponseBody
    public ResponseEntity<?> layBienTheSanPham(@PathVariable UUID productId) {
        try {
            List<ChiTietSanPham> bienThes = chiTietSanPhamService.findByProductId(productId);
            if (bienThes.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy biến thể."));
            }

            List<Map<String, Object>> colors = bienThes.stream()
                    .map(bienThe -> {
                        Map<String, Object> color = new HashMap<>();
                        color.put("id", bienThe.getMauSac().getId());
                        color.put("name", bienThe.getMauSac().getTenMau());
                        return color;
                    })
                    .distinct()
                    .collect(Collectors.toList());

            List<Map<String, Object>> sizes = bienThes.stream()
                    .map(bienThe -> {
                        Map<String, Object> size = new HashMap<>();
                        size.put("id", bienThe.getKichCo().getId());
                        size.put("name", bienThe.getKichCo().getTen());
                        return size;
                    })
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("colors", colors);
            response.put("sizes", sizes);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi không tìm thấy biến thể: " + ex.getMessage()));
        }
    }

    @GetMapping("/product/variant")
    public ResponseEntity<Map<String, Object>> getVariant(
            @RequestParam UUID productId,
            @RequestParam UUID colorId,
            @RequestParam UUID sizeId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ChiTietSanPham variant = chiTietSanPhamService.findBySanPhamIdAndMauSacIdAndKichCoId(productId, colorId, sizeId);
            if (variant != null) {
                System.out.println("Variant found: " + variant.getId() + ", Price: " + variant.getGia());
                result.put("stockQuantity", variant.getSoLuongTonKho());
                result.put("price", variant.getGia());
                result.put("productDetailId", variant.getId());
                return ResponseEntity.ok(result);
            } else {
                result.put("error", "Không tìm thấy biến thể");
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            result.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/product-detail/{productDetailId}")
    @ResponseBody
    public ResponseEntity<?> layIdSanPhamTuChiTiet(@PathVariable UUID productDetailId) {
        try {
            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mã QR không hợp lệ."));
            }
            return ResponseEntity.ok(Map.of("productId", chiTiet.getSanPham().getId()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/cart/get")
    @ResponseBody
    public ResponseEntity<GioHangDTO> layGioHang(@RequestParam(required = false) BigDecimal shippingFee) {
        try {
            BigDecimal fee = shippingFee != null ? shippingFee : BigDecimal.ZERO;
            GioHangDTO gioHang = gioHangService.layGioHang(fee);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            System.err.println("Lỗi khi lấy giỏ hàng: " + ex.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/add")
    @ResponseBody
    public ResponseEntity<?> themVaoGioHang(@RequestBody Map<String, Object> request) {
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.containsKey("shippingFee") ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;
            GioHangDTO gioHang = gioHangService.themVaoGioHang(productDetailId, quantity, shippingFee);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/cart/update")
    @ResponseBody
    public ResponseEntity<?> capNhatGioHang(@RequestBody Map<String, Object> request) {
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.containsKey("shippingFee") ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;

            List<GioHangItemDTO> currentCart = gioHangService.getCurrentCart();
            GioHangItemDTO itemToUpdate = currentCart.stream()
                    .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                    .findFirst()
                    .orElse(null);

            if (itemToUpdate != null) {
                itemToUpdate.setSoLuong(quantity);
                itemToUpdate.setThanhTien(chiTietSanPhamService.findById(productDetailId).getGia().multiply(BigDecimal.valueOf(quantity)));
            } else if (quantity > 0) {
                gioHangService.themVaoGioHang(productDetailId, quantity, shippingFee);
            }

            GioHangDTO gioHang = gioHangService.layGioHang(shippingFee);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/cart/remove")
    @ResponseBody
    public ResponseEntity<?> xoaKhoiGioHang(@RequestBody Map<String, Object> request) {
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            BigDecimal shippingFee = request.containsKey("shippingFee") ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;
            GioHangDTO gioHang = gioHangService.xoaKhoiGioHang(productDetailId, shippingFee);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/cart/clear")
    @ResponseBody
    public ResponseEntity<?> xoaTatCaGioHang() {
        GioHangDTO gioHang = gioHangService.xoaTatCaGioHang();
        return ResponseEntity.ok(gioHang);
    }

    @PostMapping("/cart/apply-voucher")
    @ResponseBody
    public ResponseEntity<?> apDungPhieuGiamGia(@RequestParam UUID voucherId, @RequestParam(required = false) BigDecimal shippingFee) {
        try {
            BigDecimal fee = shippingFee != null ? shippingFee : BigDecimal.ZERO;
            GioHangDTO gioHang = gioHangService.apDungPhieuGiamGia(voucherId, fee);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/add-customer")
    public String themKhachHang(@ModelAttribute NguoiDungDTO khachHangDTO) {
        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setHoTen(khachHangDTO.getHoTen());
        nguoiDung.setSoDienThoai(khachHangDTO.getSoDienThoai());
        nguoiDung.setEmail(khachHangDTO.getEmail());
        nguoiDung.setVaiTro("customer");
        nguoiDung.setTenDangNhap(khachHangDTO.getSoDienThoai());
        nguoiDung.setMatKhau("123456");
        nguoiDung.setTrangThai(true);
        nguoiDung.setThoiGianTao(LocalDateTime.now());
        if (nguoiDungService.existsByPhone(khachHangDTO.getSoDienThoai())) {
            return "redirect:/acvstore/banhang?error=Số điện thoại đã tồn tại";
        }
        nguoiDungService.save(nguoiDung);
        return "redirect:/acvstore/banhang";
    }

    @PostMapping("/hold-order")
    @ResponseBody
    public ResponseEntity<?> giuDonHang(@RequestBody DonHangDTO donHangDTO) {
        try {
            DonHangDTO donHangDaGiu = donHangService.giuDonHang(donHangDTO);
            BigDecimal shippingFee = donHangDTO.getPhiVanChuyen() != null ? donHangDTO.getPhiVanChuyen() : BigDecimal.ZERO;
            return ResponseEntity.ok(Map.of(
                    "message", "Đơn hàng đã được giữ thành công!",
                    "cart", gioHangService.layGioHang(shippingFee)
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/held-orders")
    @ResponseBody
    public ResponseEntity<List<DonHangTamDTO>> layDonHangTam() {
        return ResponseEntity.ok(donHangService.layDanhSachDonHangTam());
    }

    @GetMapping("/held-order-details/{orderId}")
    @ResponseBody
    public ResponseEntity<?> layChiTietDonHangTam(@PathVariable UUID orderId) {
        try {
            DonHangTamDTO donHang = donHangService.layChiTietDonHangTam(orderId);
            return ResponseEntity.ok(donHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy đơn hàng."));
        }
    }

    @PostMapping("/restore-order")
    @ResponseBody
    public ResponseEntity<?> khoiPhucDonHang(@RequestParam UUID orderId) {
        try {
            GioHangDTO gioHang = donHangService.khoiPhucDonHang(orderId);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/delete-held-order")
    @ResponseBody
    public ResponseEntity<?> xoaDonHangTam(@RequestParam UUID orderId) {
        try {
            donHangService.xoaDonHangTam(orderId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa đơn hàng tạm."));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/create-order")
    @ResponseBody
    public ResponseEntity<?> taoDonHang(@RequestBody DonHangDTO donHangDTO) {
        try {
            KetQuaDonHangDTO ketQua = donHangService.taoDonHang(donHangDTO);
            return ResponseEntity.ok(Map.of(
                    "message", "Đơn hàng " + ketQua.getMaDonHang() + " đã được tạo thành công!",
                    "stockQuantities", ketQua.getSoLuongTonKho()
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/vnpay-payment")
    @ResponseBody
    public ResponseEntity<?> taoThanhToanVNPay(@RequestBody DonHangDTO donHangDTO) {
        try {
            String urlThanhToan = vnPayService.createPaymentUrl(donHangDTO);
            return ResponseEntity.ok(Map.of("paymentUrl", urlThanhToan));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/download-receipt/{maDonHang}")
    public ResponseEntity<?> taiHoaDon(@PathVariable String maDonHang) {
        try {
            byte[] pdfBytes = hoaDonService.taoHoaDon(maDonHang);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=hoa_don_" + maDonHang + ".pdf")
                    .body(pdfBytes);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không thể tạo hóa đơn."));
        }
    }

//    @GetMapping("/product/qr-file/{productDetailId}")
//    public ResponseEntity<?> layMaQR(@PathVariable UUID productDetailId) {
//        try {
//            byte[] maQR = maQRService.taoMaQR(productDetailId.toString());
//            return ResponseEntity.ok()
//                    .header("Content-Type", "image/png")
//                    .body(maQR);
//        } catch (Exception ex) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Không thể tạo mã QR."));
//        }
//    }
}