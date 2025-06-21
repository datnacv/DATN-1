package com.example.AsmGD1.controller.BanHang;

import com.example.AsmGD1.config.VNPayConfig;
import com.example.AsmGD1.dto.BanHang.*;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.DonHangTamRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.BanHang.DonHangService;
import com.example.AsmGD1.service.BanHang.GioHangService;
import com.example.AsmGD1.service.BanHang.QRCodeService;
import com.example.AsmGD1.service.BanHang.VNPayService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaService;
import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.PhuongThucThanhToan.PhuongThucThanhToanService;
import com.example.AsmGD1.service.SanPham.ChiTietSanPhamService;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private PhuongThucThanhToanService phuongThucThanhToanService;

    @Autowired
    private DonHangTamRepository donHangTamRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private HttpSession session;

    private static final DecimalFormat currencyFormat = new DecimalFormat("#,##0 VNĐ");

    private String formatCurrency(BigDecimal amount) {
        return amount != null ? currencyFormat.format(amount) : "0 VNĐ";
    }

    @GetMapping("/customers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCustomers() {
        try {
            List<NguoiDung> customers = nguoiDungService.findUsersByVaiTro("customer", "", 0, 10).getContent();
            List<Map<String, Object>> customerList = customers.stream()
                    .map(customer -> {
                        Map<String, Object> customerInfo = new HashMap<>();
                        customerInfo.put("hoTen", customer.getHoTen());
                        customerInfo.put("soDienThoai", customer.getSoDienThoai());
                        customerInfo.put("email", customer.getEmail());
                        return customerInfo;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("customers", customerList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/customer/{phone}/details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCustomerDetails(@PathVariable String phone) {
        try {
            NguoiDung nguoiDung = nguoiDungRepository.findBySoDienThoai(phone)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
            List<String> addresses = (List<String>) session.getAttribute("customer_addresses_" + phone) != null
                    ? (List<String>) session.getAttribute("customer_addresses_" + phone)
                    : new ArrayList<>();

            Map<String, Object> response = new HashMap<>();
            response.put("hoTen", nguoiDung.getHoTen());
            response.put("email", nguoiDung.getEmail() != null ? nguoiDung.getEmail() : ""); // Ensure null handling
            response.put("addresses", addresses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/customer/{phone}/addresses")
    public ResponseEntity<Map<String, Object>> getCustomerAddresses(@PathVariable String phone) {
        try {
            NguoiDung nguoiDung = nguoiDungRepository.findBySoDienThoai(phone)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

            List<String> addresses = new ArrayList<>();
            // Thêm địa chỉ chính
            if (nguoiDung.getDiaChi() != null && !nguoiDung.getDiaChi().trim().isEmpty()) {
                addresses.add(nguoiDung.getDiaChi());
            }
            // Thêm địa chỉ chi tiết
            String detailedAddress = constructDetailedAddress(nguoiDung);
            if (detailedAddress != null && !addresses.contains(detailedAddress)) {
                addresses.add(detailedAddress);
            }
            // Thêm địa chỉ từ session
            List<String> sessionAddresses = (List<String>) session.getAttribute("customer_addresses_" + phone);
            if (sessionAddresses != null) {
                addresses.addAll(sessionAddresses);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("addresses", addresses);
            response.put("hoTen", nguoiDung.getHoTen());
            response.put("email", nguoiDung.getEmail() != null ? nguoiDung.getEmail() : "");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/customer/{phone}/add-address")
    public ResponseEntity<Map<String, Object>> addCustomerAddress(@PathVariable String phone, @RequestBody Map<String, String> request) {
        try {
            NguoiDung nguoiDung = nguoiDungRepository.findBySoDienThoai(phone)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
            String addressLine = request.get("addressLine");
            String ward = request.get("ward");
            String district = request.get("district");
            String city = request.get("city");
            if (addressLine != null && ward != null && district != null && city != null &&
                    !addressLine.trim().isEmpty() && !ward.trim().isEmpty() && !district.trim().isEmpty() && !city.trim().isEmpty()) {
                // Update NguoiDung entity with detailed fields
                nguoiDung.setChiTietDiaChi(addressLine);
                nguoiDung.setPhuongXa(ward);
                nguoiDung.setQuanHuyen(district);
                nguoiDung.setTinhThanhPho(city);
                nguoiDungRepository.save(nguoiDung);

                // Optionally store in session for addresses list
                List<String> sessionAddresses = (List<String>) session.getAttribute("customer_addresses_" + phone);
                if (sessionAddresses == null) {
                    sessionAddresses = new ArrayList<>();
                }
                sessionAddresses.add(request.get("address"));
                session.setAttribute("customer_addresses_" + phone, sessionAddresses);

                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Địa chỉ không hợp lệ"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/vouchers")
    public ResponseEntity<Map<String, Object>> getApplicableVouchers(@RequestParam String subTotal, @RequestParam(required = false) BigDecimal shippingFee) {
        try {
            String cleanSubTotal = subTotal.replaceAll("[^0-9]", "");
            BigDecimal amount = new BigDecimal(cleanSubTotal.isEmpty() ? "0" : cleanSubTotal);
            System.out.println("Debug - Cleaned subTotal: " + amount);

            // Lấy giỏ hàng hiện tại
            GioHangDTO gioHang = gioHangService.layGioHang(shippingFee != null ? shippingFee : BigDecimal.ZERO);

            // Cập nhật TongTienHangValue từ subTotal
            gioHang.setTongTienHangValue(amount);
            gioHang.setTongTienHang(gioHangService.formatCurrency(amount));

            // Tạo danh sách sản phẩm giả lập nếu giỏ trống
            if (gioHang.getDanhSachSanPham() == null || gioHang.getDanhSachSanPham().isEmpty()) {
                GioHangItemDTO dummyItem = new GioHangItemDTO();
                dummyItem.setIdChiTietSanPham(UUID.randomUUID());
                dummyItem.setTenSanPham("Sản phẩm tạm");
                dummyItem.setSoLuong(1);
                dummyItem.setGia(amount);
                dummyItem.setThanhTien(amount);
                gioHang.setDanhSachSanPham(Collections.singletonList(dummyItem));
                System.out.println("Debug - Added dummy item to cart with amount: " + amount);
            }

            // Lấy danh sách voucher khả dụng
            List<PhieuGiamGia> applicableVouchers = phieuGiamGiaRepository.findAll().stream()
                    .filter(v -> "Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(v)))
                    .filter(v -> amount.compareTo(v.getGiaTriGiamToiThieu()) >= 0)
                    .collect(Collectors.toList());
            System.out.println("Debug - Applicable vouchers count: " + applicableVouchers.size());

            Map<String, Object> response = new HashMap<>();
            response.put("gioHang", gioHang);
            response.put("vouchers", applicableVouchers);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Debug - Error in getApplicableVouchers: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody DonHangDTO donHangDTO) {
        try {
            KetQuaDonHangDTO ketQua = donHangService.taoDonHang(donHangDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đơn hàng đã được tạo thành công!");
            response.put("maDonHang", ketQua.getMaDonHang());
            response.put("soLuongTonKho", ketQua.getSoLuongTonKho());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cart/apply-voucher")
    public ResponseEntity<?> applyVoucher(@RequestParam UUID voucherId, @RequestParam BigDecimal shippingFee) {
        try {
            // Lấy giỏ hàng hiện tại
            GioHangDTO gioHangDTO = gioHangService.layGioHang(shippingFee != null ? shippingFee : BigDecimal.ZERO);
            // Áp dụng voucher với giỏ hàng hiện tại
            GioHangDTO updatedGioHang = gioHangService.apDungPhieuGiamGia(voucherId, shippingFee, gioHangDTO);
            return ResponseEntity.ok(updatedGioHang);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi áp dụng phiếu giảm giá: " + e.getMessage()));
        }
    }

    private String constructDetailedAddress(NguoiDung nguoiDung) {
        StringBuilder address = new StringBuilder();
        if (nguoiDung.getChiTietDiaChi() != null && !nguoiDung.getChiTietDiaChi().trim().isEmpty()) {
            address.append(nguoiDung.getChiTietDiaChi());
        }
        if (nguoiDung.getPhuongXa() != null && !nguoiDung.getPhuongXa().trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(nguoiDung.getPhuongXa());
        }
        if (nguoiDung.getQuanHuyen() != null && !nguoiDung.getQuanHuyen().trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(nguoiDung.getQuanHuyen());
        }
        if (nguoiDung.getTinhThanhPho() != null && !nguoiDung.getTinhThanhPho().trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(nguoiDung.getTinhThanhPho());
        }
        return address.length() > 0 ? address.toString() : null;
    }

    @GetMapping
    public String hienThiTrangBanHang(Model model, HttpSession session) {
        List<SanPham> products = sanPhamService.findAll();
        for (SanPham product : products) {
            product.setChiTietSanPhams(chiTietSanPhamService.findByProductId(product.getId()));
        }
        model.addAttribute("products", products);
        model.addAttribute("chiTietSanPhams", chiTietSanPhamService.findAll());
        model.addAttribute("customers", nguoiDungService.findUsersByVaiTro("customer", "", 0, 10).getContent());
        model.addAttribute("paymentMethods", phuongThucThanhToanService.findAll());
        model.addAttribute("discountVouchers", phieuGiamGiaService.layTatCa().stream()
                .filter(v -> "Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(v)))
                .collect(Collectors.toList()));
        BigDecimal defaultShippingFee = BigDecimal.ZERO;
        model.addAttribute("gioHang", gioHangService.layGioHang(defaultShippingFee));
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));

        // Load tabs from session
        Map<String, Object> tabs = (Map<String, Object>) session.getAttribute("orderTabs");
        model.addAttribute("orderTabs", tabs != null ? tabs : new HashMap<>());
        model.addAttribute("orderCounter", session.getAttribute("orderCounter") != null ? session.getAttribute("orderCounter") : 0);

        return "WebQuanLy/ban-hang";
    }

    @GetMapping("/get-tabs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTabs(HttpSession session) {
        Map<String, Object> tabs = (Map<String, Object>) session.getAttribute("orderTabs");
        Integer orderCounter = (Integer) session.getAttribute("orderCounter");
        if (tabs == null) {
            tabs = new HashMap<>();
            orderCounter = 0;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("orders", tabs);
        response.put("orderCounter", orderCounter);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-tabs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveTabs(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            Map<String, Object> orders = (Map<String, Object>) request.get("orders");
            Integer orderCounter = (Integer) request.get("orderCounter");
            session.setAttribute("orderTabs", orders);
            session.setAttribute("orderCounter", orderCounter);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
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
            } else if (productId != null && colorId != null && sizeId != null) {
                variant = chiTietSanPhamService.findBySanPhamIdAndMauSacIdAndKichCoId(productId, colorId, sizeId);
            } else {
                result.put("error", "Thiếu tham số cần thiết (productId, colorId, sizeId hoặc productDetailId).");
                return ResponseEntity.badRequest().body(result);
            }

            if (variant != null) {
                result.put("stockQuantity", variant.getSoLuongTonKho());
                result.put("price", variant.getGia());
                result.put("productDetailId", variant.getId());
                result.put("tenSanPham", variant.getSanPham().getTenSanPham());
                result.put("mauSac", variant.getMauSac().getTenMau());
                result.put("kichCo", variant.getKichCo().getTen());
                result.put("hinhAnh", variant.getSanPham().getUrlHinhAnh()); // Add image URL
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
//
//    @GetMapping("/vouchers")
//    @ResponseBody
//    public List<PhieuGiamGia> getVouchers() {
//        return phieuGiamGiaService.layTatCa(); // Trả về tất cả phiếu giảm giá
//    }

    @GetMapping("/products")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        try {
            List<ChiTietSanPham> products = chiTietSanPhamService.findAll();
            List<Map<String, Object>> productList = products.stream().map(product -> {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("idChiTietSanPham", product.getId());
                productMap.put("tenSanPham", product.getSanPham().getTenSanPham());
                productMap.put("mauSac", product.getMauSac());
                productMap.put("kichCo", product.getKichCo());
                productMap.put("price", product.getGia());
                return productMap;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("products", productList));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/product-detail/{productDetailId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> layIdSanPhamTuChiTiet(@PathVariable String productDetailId) {
        try {
            UUID id = UUID.fromString(productDetailId);
            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(id);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy sản phẩm."));
            }
            Map<String, Object> response = new HashMap<>();
            response.put("productId", chiTiet.getSanPham().getId());
            response.put("tenSanPham", chiTiet.getSanPham().getTenSanPham());
            response.put("hinhAnh", chiTiet.getSanPham().getUrlHinhAnh());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "UUID không hợp lệ."));
        }
    }

    @GetMapping("/cart/get")
    @ResponseBody
    public ResponseEntity<GioHangDTO> layGioHang(@RequestParam(required = false) BigDecimal shippingFee) {
        System.out.println("Fetching cart with shippingFee: " + shippingFee);
        try {
            BigDecimal fee = shippingFee != null ? shippingFee : BigDecimal.ZERO;
            GioHangDTO gioHang = gioHangService.layGioHang(fee);
            if (gioHang.getDanhSachSanPham() != null) {
                for (GioHangItemDTO item : gioHang.getDanhSachSanPham()) {
                    ChiTietSanPham chiTiet = chiTietSanPhamService.findById(item.getIdChiTietSanPham());
                    if (chiTiet != null) {
                        item.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
                        item.setMauSac(chiTiet.getMauSac().getTenMau());
                        item.setKichCo(chiTiet.getKichCo().getTen());
                    }
                }
            }
            System.out.println("Cart fetched: " + gioHang);
            return ResponseEntity.ok(gioHang);
        } catch (Exception ex) {
            System.err.println("Exception in cart/get: " + ex.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/add")
    @ResponseBody
    public ResponseEntity<GioHangDTO> themVaoGioHang(@RequestBody Map<String, Object> request, HttpSession session) {
        System.out.println("Request received for /cart/add: " + request);
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.containsKey("shippingFee") ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;

            System.out.println("Processing: productDetailId=" + productDetailId + ", quantity=" + quantity + ", shippingFee=" + shippingFee);

            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
            if (chiTiet == null) {
                System.err.println("ChiTietSanPham not found for ID: " + productDetailId);
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            // Kiểm tra tồn kho
            int availableStock = chiTiet.getSoLuongTonKho();
            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges != null && tempStockChanges.containsKey(productDetailId)) {
                int reserved = (int) tempStockChanges.get(productDetailId).get("reservedQuantity");
                availableStock -= reserved;
            }
            if (availableStock < quantity) {
                System.err.println("Insufficient stock for productDetailId: " + productDetailId +
                        ", available: " + availableStock + ", requested: " + quantity);
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            GioHangDTO gioHang = gioHangService.themVaoGioHang(productDetailId, quantity, shippingFee);
            if (tempStockChanges == null) tempStockChanges = new HashMap<>();
            tempStockChanges.put(productDetailId, Map.of("originalStock", chiTiet.getSoLuongTonKho(), "reservedQuantity", quantity));
            session.setAttribute("tempStockChanges", tempStockChanges);

            System.out.println("Successfully added to cart: " + gioHang);
            return ResponseEntity.ok(gioHang);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID or data format: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        } catch (Exception ex) {
            System.err.println("Exception in cart/add: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/update")
    @ResponseBody
    public ResponseEntity<GioHangDTO> capNhatGioHang(@RequestBody Map<String, Object> request, HttpSession session) {
        System.out.println("Request received for /cart/update: " + request);
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.containsKey("shippingFee") ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;

            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
            if (chiTiet == null) {
                System.err.println("ChiTietSanPham not found for ID: " + productDetailId);
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges == null) {
                tempStockChanges = new HashMap<>();
                session.setAttribute("tempStockChanges", tempStockChanges);
            }

            int currentReserved = tempStockChanges.containsKey(productDetailId) ? (int) tempStockChanges.get(productDetailId).get("reservedQuantity") : 0;
            int availableStock = chiTiet.getSoLuongTonKho() - currentReserved + (quantity > currentReserved ? 0 : currentReserved - quantity);
            if (quantity > availableStock) {
                System.err.println("Insufficient stock for update: " + productDetailId +
                        ", available: " + availableStock + ", requested: " + quantity);
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            GioHangDTO gioHang = gioHangService.capNhatGioHang(productDetailId, quantity, shippingFee);
            tempStockChanges.put(productDetailId, Map.of("originalStock", chiTiet.getSoLuongTonKho(), "reservedQuantity", quantity));
            session.setAttribute("tempStockChanges", tempStockChanges);

            System.out.println("Successfully updated cart: " + gioHang);
            return ResponseEntity.ok(gioHang);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID or data format: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        } catch (Exception ex) {
            System.err.println("Exception in cart/update: " + ex.getMessage());
            ex.printStackTrace();
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

//    @PostMapping("/cart/apply-voucher")
//    @ResponseBody
//    public ResponseEntity<GioHangDTO> apDungPhieuGiamGia(@RequestParam UUID voucherId, @RequestParam(required = false) BigDecimal shippingFee) {
//        try {
//            BigDecimal fee = shippingFee != null ? shippingFee : BigDecimal.ZERO;
//            GioHangDTO gioHang = gioHangService.apDungPhieuGiamGia(voucherId, fee);
//            return ResponseEntity.ok(gioHang);
//        } catch (Exception ex) {
//            return ResponseEntity.badRequest().body(new GioHangDTO());
//        }
//    }

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

            // Xóa tempStockChanges và reset giỏ hàng trên server
            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges != null) {
                session.removeAttribute("tempStockChanges");
            }
            gioHangService.xoaTatCaGioHang(); // Reset giỏ hàng trên server

            // Trả về giỏ hàng rỗng để đồng bộ với client
            GioHangDTO gioHangRong = gioHangService.layGioHang(BigDecimal.ZERO);
            return ResponseEntity.ok(Map.of("message", "Đơn hàng đã được giữ thành công!", "cart", gioHangRong));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // Thêm endpoint để render trang danh sách đơn hàng tạm
    @GetMapping("/don-hang-tam-thoi")
    public String hienThiTrangDonHangTam(Model model) {
        model.addAttribute("heldOrders", donHangService.layDanhSachDonHangTam());
        return "WebQuanLy/don-hang-tam-thoi"; // Trả về tên file HTML (không cần đuôi .html nếu dùng Thymeleaf)
    }

    // Thêm endpoint để xóa tất cả đơn hàng tạm
    @PostMapping("/delete-all-held-orders")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaTatCaDonHangTam() {
        try {
            donHangTamRepository.deleteAll();
            return ResponseEntity.ok(Map.of("message", "Đã xóa tất cả đơn hàng tạm!"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/held-orders")
    @ResponseBody
    public ResponseEntity<List<DonHangTamDTO>> layDonHangTam(HttpSession session) {
        try {
            List<DonHangTam> heldOrders = donHangTamRepository.findAll();
            List<DonHangTamDTO> dtos = heldOrders.stream().map(order -> {
                DonHangTamDTO dto = new DonHangTamDTO();
                dto.setId(order.getId());
                dto.setMaDonHangTam(order.getMaDonHangTam()); // Đảm bảo ánh xạ trường này
                if (order.getKhachHang() != null) {
                    NguoiDung nguoiDung = nguoiDungService.findById(order.getKhachHang());
                    dto.setTenKhachHang(nguoiDung != null ? nguoiDung.getHoTen() : "Không rõ");
                } else {
                    dto.setTenKhachHang("Không rõ");
                }
                dto.setTong(order.getTong() != null ? order.getTong() : BigDecimal.ZERO);
                dto.setThoiGianTao(order.getThoiGianTao());
                dto.setPhuongThucThanhToan(order.getPhuongThucThanhToan() != null ? order.getPhuongThucThanhToan() : "Chưa chọn");
                dto.setPhuongThucBanHang(order.getPhuongThucBanHang() != null ? order.getPhuongThucBanHang() : "Chưa chọn");
                dto.setPhiVanChuyen(order.getPhiVanChuyen() != null ? order.getPhiVanChuyen() : BigDecimal.ZERO);
                dto.setIdPhieuGiamGia(order.getPhieuGiamGia());

                if (order.getDanhSachSanPham() != null) {
                    try {
                        dto.setDanhSachSanPham(objectMapper.readValue(order.getDanhSachSanPham(), new TypeReference<List<GioHangItemDTO>>(){}));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    @GetMapping("/held-order-details/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chiTietDonHangTam(@PathVariable UUID orderId) {
        try {
            DonHangTam donHangTam = donHangTamRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng tạm không tồn tại."));

            Map<String, Object> response = new HashMap<>();
            response.put("id", donHangTam.getId());
            response.put("maDonHangTam", donHangTam.getMaDonHangTam());
            response.put("tenKhachHang", nguoiDungRepository.findById(donHangTam.getKhachHang())
                    .map(NguoiDung::getHoTen).orElse("Không rõ"));
            response.put("tong", donHangTam.getTong());
            response.put("thoiGianTao", donHangTam.getThoiGianTao());
            response.put("phuongThucThanhToan", donHangTam.getPhuongThucThanhToan());
            response.put("phuongThucBanHang", donHangTam.getPhuongThucBanHang());
            response.put("idPhieuGiamGia", donHangTam.getPhieuGiamGia());

            if (donHangTam.getDanhSachSanPham() != null) {
                try {
                    List<GioHangItemDTO> danhSachSanPham = objectMapper.readValue(
                            donHangTam.getDanhSachSanPham(),
                            new TypeReference<List<GioHangItemDTO>>(){}
                    );
                    response.put("danhSachSanPham", danhSachSanPham);
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi khi parse danh_sach_item: " + e.getMessage());
                }
            } else {
                response.put("danhSachSanPham", new ArrayList<>());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/restore-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreOrder(@RequestParam UUID orderId, HttpSession session) {
        try {
            // Tìm đơn hàng tạm thời theo ID
            DonHangTam donHangTam = donHangTamRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng tạm với ID: " + orderId));

            // Xóa giỏ hàng hiện tại
            gioHangService.xoaTatCaGioHang();

            // Khôi phục giỏ hàng từ đơn hàng tạm
            List<GioHangItemDTO> danhSachSanPham = new ArrayList<>();
            if (donHangTam.getDanhSachSanPham() != null) {
                List<Map<String, Object>> items = objectMapper.readValue(
                        donHangTam.getDanhSachSanPham(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                for (Map<String, Object> item : items) {
                    UUID productDetailId = UUID.fromString((String) item.get("idChiTietSanPham"));
                    ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
                    if (chiTiet != null) {
                        int soLuong = ((Number) item.get("soLuong")).intValue();
                        BigDecimal gia = new BigDecimal(item.get("gia").toString());
                        BigDecimal thanhTien = new BigDecimal(item.get("thanhTien").toString());
                        GioHangItemDTO gioHangItem = new GioHangItemDTO();
                        gioHangItem.setIdChiTietSanPham(productDetailId);
                        gioHangItem.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
                        gioHangItem.setMauSac(chiTiet.getMauSac().getTenMau());
                        gioHangItem.setKichCo(chiTiet.getKichCo().getTen());
                        gioHangItem.setSoLuong(soLuong);
                        gioHangItem.setGia(gia);
                        gioHangItem.setThanhTien(thanhTien);
                        danhSachSanPham.add(gioHangItem);
                        // Cập nhật giỏ hàng
                        gioHangService.themVaoGioHang(productDetailId, soLuong, BigDecimal.ZERO);
                    }
                }
            }

            // Chuẩn bị dữ liệu trả về
            Map<String, Object> response = new HashMap<>();
            response.put("id", donHangTam.getId());
            response.put("soDienThoaiKhachHang", donHangTam.getSoDienThoaiKhachHang());
            response.put("phiVanChuyen", donHangTam.getPhiVanChuyen() != null ? donHangTam.getPhiVanChuyen() : BigDecimal.ZERO);
            response.put("phuongThucThanhToan", donHangTam.getPhuongThucThanhToan());
            response.put("phuongThucBanHang", donHangTam.getPhuongThucBanHang());
            response.put("idPhieuGiamGia", donHangTam.getPhieuGiamGia());
            response.put("tong", donHangTam.getTong() != null ? donHangTam.getTong() : BigDecimal.ZERO);
            response.put("danhSachSanPham", danhSachSanPham);

            // Xóa đơn hàng tạm sau khi khôi phục
            donHangTamRepository.delete(donHangTam);

            // Cập nhật session nếu cần
            session.setAttribute("tempStockChanges", new HashMap<>());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi server: " + e.getMessage()));
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

//    @PostMapping("/create-order")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> taoDonHang(@RequestBody DonHangDTO donHangDTO, HttpSession session) {
//        try {
//            System.out.println("Dữ liệu donHangDTO: " + donHangDTO);
//            System.out.println("soDienThoaiKhachHang: " + donHangDTO.getSoDienThoaiKhachHang());
//            System.out.println("phiVanChuyen: " + donHangDTO.getPhiVanChuyen());
//            System.out.println("phuongThucThanhToan: " + donHangDTO.getPhuongThucThanhToan());
//            System.out.println("soTienKhachDua: " + donHangDTO.getSoTienKhachDua());
//            System.out.println("danhSachSanPham: " + (donHangDTO.getDanhSachSanPham() != null ? donHangDTO.getDanhSachSanPham() : "null"));
//            if (donHangDTO.getDanhSachSanPham() != null) {
//                donHangDTO.getDanhSachSanPham().forEach(item -> {
//                    System.out.println("  - idChiTietSanPham: " + item.getIdChiTietSanPham() + ", soLuong: " + item.getSoLuong());
//                });
//            }
//
//            KetQuaDonHangDTO ketQua = donHangService.taoDonHang(donHangDTO);
//            session.removeAttribute("tempStockChanges");
//            return ResponseEntity.ok(Map.of("message", "Đơn hàng " + ketQua.getMaDonHang() + " đã được tạo thành công!"));
//        } catch (Exception ex) {
//            System.err.println("Lỗi khi tạo đơn hàng: " + ex.getMessage());
//            ex.printStackTrace();
//            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
//        }
//    }

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

    @GetMapping("/payment-methods")
    @ResponseBody
    public List<PhuongThucThanhToan> getPaymentMethods() {
        return phuongThucThanhToanService.findAll(); // Giả định service có phương thức này
    }

    @PostMapping("/vnpay-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createVNPayPayment(@RequestBody DonHangDTO donHangDTO, HttpSession session) {
        try {
            if (donHangDTO.getPhuongThucThanhToan() == null || !donHangDTO.getPhuongThucThanhToan().equals("VNPay")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phương thức thanh toán phải là VNPay."));
            }

            BigDecimal subTotal = donHangDTO.getDanhSachSanPham().stream()
                    .map(item -> item.getGia().multiply(BigDecimal.valueOf(item.getSoLuong())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal discount = (BigDecimal) session.getAttribute("discount") != null ? (BigDecimal) session.getAttribute("discount") : BigDecimal.ZERO;
            BigDecimal total = subTotal.subtract(discount).add(donHangDTO.getPhiVanChuyen());
            long amount = total.multiply(BigDecimal.valueOf(100)).longValue();

            String orderCode = "HD" + System.currentTimeMillis();
            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", VNPayConfig.VNP_TMNCODE);
            vnpParams.put("vnp_Amount", String.valueOf(amount));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", orderCode);
            vnpParams.put("vnp_OrderInfo", "Thanh toán đơn hàng: " + orderCode);
            vnpParams.put("vnp_OrderType", "billpayment");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", VNPayConfig.VNP_RETURN_URL);
            vnpParams.put("vnp_IpAddr", "127.0.0.1");
            vnpParams.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            String hashData = vnpParams.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            String vnpSecureHash = hmacSHA512(VNPayConfig.VNP_HASHSECRET, hashData);
            vnpParams.put("vnp_SecureHash", vnpSecureHash);

            String paymentUrl = VNPayConfig.VNP_URL + "?" + vnpParams.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            Map<String, Object> pendingOrder = new HashMap<>();
            pendingOrder.put("donHangDTO", donHangDTO);
            pendingOrder.put("discount", discount);
            pendingOrder.put("orderCode", orderCode);
            session.setAttribute("pendingVNPayOrder", pendingOrder);

            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi tạo URL thanh toán: " + e.getMessage()));
        }
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512_HMAC.init(secretKey);
        byte[] bytes = sha512_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hash.append('0');
            hash.append(hex);
        }
        return hash.toString();
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