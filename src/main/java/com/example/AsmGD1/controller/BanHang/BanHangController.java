package com.example.AsmGD1.controller.BanHang;

import com.example.AsmGD1.dto.BanHang.DonHangDTO;
import com.example.AsmGD1.dto.BanHang.DonHangTamDTO;
import com.example.AsmGD1.dto.BanHang.GioHangDTO;
import com.example.AsmGD1.dto.BanHang.KetQuaDonHangDTO;
import com.example.AsmGD1.dto.BanHang.NguoiDungDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        List<ChiTietSanPham> chiTietSanPhams = chiTietSanPhamService.findAll();
        model.addAttribute("chiTietSanPhams", chiTietSanPhams);
        model.addAttribute("products", sanPhamService.findAll());
        model.addAttribute("customers", nguoiDungService.findUsersByVaiTro("customer", "", 0, 10).getContent());
        model.addAttribute("discountVouchers", phieuGiamGiaService.layTatCa().stream()
                .filter(v -> "Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(v)))
                .collect(Collectors.toList()));
        BigDecimal defaultShippingFee = BigDecimal.ZERO;
        model.addAttribute("gioHang", gioHangService.layGioHang(defaultShippingFee));
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        return "WebQuanLy/ban-hang";
    }

    @GetMapping("/product/{productId}/variants")
    public ResponseEntity<Map<String, Object>> getProductVariants(@PathVariable UUID productId) {
        List<MauSac> colors = chiTietSanPhamService.findColorsByProductId(productId);
        List<KichCo> sizes = chiTietSanPhamService.findSizesByProductId(productId);
        if (colors.isEmpty() && sizes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy thông tin biến thể"));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("colors", colors);
        response.put("sizes", sizes);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/variant")
    public ResponseEntity<Map<String, Object>> getVariant(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID colorId,
            @RequestParam(required = false) UUID sizeId,
            @RequestParam(required = false) UUID productDetailId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ChiTietSanPham variant;
            if (productDetailId != null) {
                variant = chiTietSanPhamService.findById(productDetailId);
            } else {
                variant = chiTietSanPhamService.findBySanPhamIdAndMauSacIdAndKichCoId(productId, colorId, sizeId);
            }
            if (variant != null) {
                result.put("stockQuantity", variant.getSoLuongTonKho());
                result.put("price", variant.getGia());
                result.put("productDetailId", variant.getId());
                return ResponseEntity.ok(result);
            } else {
                result.put("error", "Không tìm thấy biến thể.");
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IllegalArgumentException e) {
            result.put("error", "Định dạng UUID không hợp lệ: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/product-detail/{productDetailId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> layIdSanPhamTuChiTiet(@PathVariable UUID productDetailId) {
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
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/add")
    @ResponseBody
    public ResponseEntity<GioHangDTO> themVaoGioHang(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.containsKey("shippingFee") ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;

            GioHangDTO gioHang = gioHangService.themVaoGioHang(productDetailId, quantity, shippingFee);
            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges == null) tempStockChanges = new HashMap<>();
            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
            if (chiTiet != null) {
                tempStockChanges.put(productDetailId, Map.of("originalStock", chiTiet.getSoLuongTonKho(), "reservedQuantity", quantity));
                session.setAttribute("tempStockChanges", tempStockChanges);
            }
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/update")
    @ResponseBody
    public ResponseEntity<GioHangDTO> capNhatGioHang(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.containsKey("shippingFee") ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;

            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            // Kiểm tra số lượng tồn kho
            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges == null) {
                tempStockChanges = new HashMap<>();
                session.setAttribute("tempStockChanges", tempStockChanges);
            }
            int currentReserved = tempStockChanges.containsKey(productDetailId) ? (int) tempStockChanges.get(productDetailId).get("reservedQuantity") : 0;
            int availableStock = chiTiet.getSoLuongTonKho() - currentReserved;
            if (quantity > availableStock) {
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            // Cập nhật giỏ hàng
            GioHangDTO gioHang = gioHangService.capNhatGioHang(productDetailId, quantity, shippingFee);

            // Cập nhật tempStockChanges
            tempStockChanges.put(productDetailId, Map.of("originalStock", chiTiet.getSoLuongTonKho(), "reservedQuantity", quantity));
            session.setAttribute("tempStockChanges", tempStockChanges);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/remove")
    @ResponseBody
    public ResponseEntity<GioHangDTO> xoaKhoiGioHang(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            BigDecimal shippingFee = request.containsKey("shippingFee") ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;

            GioHangDTO gioHang = gioHangService.xoaKhoiGioHang(productDetailId, shippingFee);
            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges != null && tempStockChanges.containsKey(productDetailId)) {
                tempStockChanges.remove(productDetailId);
                session.setAttribute("tempStockChanges", tempStockChanges);
            }
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/clear")
    @ResponseBody
    public ResponseEntity<GioHangDTO> xoaTatCaGioHang() {
        try {
            GioHangDTO gioHang = gioHangService.xoaTatCaGioHang();
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/apply-voucher")
    @ResponseBody
    public ResponseEntity<GioHangDTO> apDungPhieuGiamGia(@RequestParam UUID voucherId, @RequestParam(required = false) BigDecimal shippingFee) {
        try {
            BigDecimal fee = shippingFee != null ? shippingFee : BigDecimal.ZERO;
            GioHangDTO gioHang = gioHangService.apDungPhieuGiamGia(voucherId, fee);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new GioHangDTO());
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
    public ResponseEntity<Map<String, Object>> giuDonHang(@RequestBody DonHangDTO donHangDTO, HttpSession session) {
        try {
            DonHangDTO donHangDaGiu = donHangService.giuDonHang(donHangDTO);
            BigDecimal shippingFee = donHangDTO.getPhiVanChuyen() != null ? donHangDTO.getPhiVanChuyen() : BigDecimal.ZERO;
            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges != null) {
                session.removeAttribute("tempStockChanges"); // Xóa tạm khi giữ đơn
            }
            return ResponseEntity.ok(Map.of("message", "Đơn hàng đã được giữ thành công!", "cart", gioHangService.layGioHang(shippingFee)));
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
    public ResponseEntity<DonHangTamDTO> layChiTietDonHangTam(@PathVariable UUID orderId) {
        try {
            DonHangTamDTO donHang = donHangService.layChiTietDonHangTam(orderId);
            return ResponseEntity.ok(donHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/restore-order")
    @ResponseBody
    public ResponseEntity<GioHangDTO> khoiPhucDonHang(@RequestParam UUID orderId) {
        try {
            GioHangDTO gioHang = donHangService.khoiPhucDonHang(orderId);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/delete-held-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaDonHangTam(@RequestParam UUID orderId) {
        try {
            donHangService.xoaDonHangTam(orderId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa đơn hàng tạm."));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/create-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> taoDonHang(@RequestBody DonHangDTO donHangDTO, HttpSession session) {
        try {
            KetQuaDonHangDTO ketQua = donHangService.taoDonHang(donHangDTO);
            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges != null) {
                for (Map.Entry<UUID, Map<String, Object>> entry : tempStockChanges.entrySet()) {
                    UUID productDetailId = entry.getKey();
                    int reservedQuantity = (int) entry.getValue().get("reservedQuantity");
                    ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
                    if (chiTiet != null) {
                        chiTiet.setSoLuongTonKho(chiTiet.getSoLuongTonKho() - reservedQuantity);
                        chiTietSanPhamService.save(chiTiet);
                    }
                }
                session.removeAttribute("tempStockChanges");
            }
            return ResponseEntity.ok(Map.of("message", "Đơn hàng " + ketQua.getMaDonHang() + " đã được tạo thành công!"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/cart/get-temp-stock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTempStock(@RequestParam UUID productDetailId, HttpSession session) {
        Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
        Map<String, Object> result = new HashMap<>();
        if (tempStockChanges != null && tempStockChanges.containsKey(productDetailId)) {
            result.put("reservedQuantity", tempStockChanges.get(productDetailId).get("reservedQuantity"));
        } else {
            result.put("reservedQuantity", 0);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/vnpay-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> taoThanhToanVNPay(@RequestBody DonHangDTO donHangDTO) {
        try {
            String urlThanhToan = vnPayService.createPaymentUrl(donHangDTO);
            return ResponseEntity.ok(Map.of("paymentUrl", urlThanhToan));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/download-receipt/{maDonHang}")
    public ResponseEntity<byte[]> taiHoaDon(@PathVariable String maDonHang) {
        try {
            byte[] pdfBytes = hoaDonService.taoHoaDon(maDonHang);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=hoa_don_" + maDonHang + ".pdf")
                    .body(pdfBytes);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}