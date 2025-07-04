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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/ban-hang")
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

    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> getCustomers(@RequestParam(required = false, defaultValue = "") String keyword) {
        try {
            List<NguoiDung> customers = nguoiDungRepository.searchByKeywordNoPaging(keyword);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customers", customers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Lỗi khi tải danh sách khách hàng: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/customer/{phone}/details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCustomerDetails(@PathVariable String phone) {
        try {
            NguoiDung nguoiDung = nguoiDungRepository.findBySoDienThoai(phone)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với số điện thoại: " + phone));
            List<String> addresses = (List<String>) session.getAttribute("customer_addresses_" + phone);
            if (addresses == null) {
                addresses = new ArrayList<>();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("hoTen", nguoiDung.getHoTen());
            response.put("email", nguoiDung.getEmail() != null ? nguoiDung.getEmail() : "");
            response.put("addresses", addresses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy chi tiết khách hàng: " + e.getMessage()));
        }
    }

    @GetMapping("/customer/{phone}/addresses")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCustomerAddresses(@PathVariable String phone) {
        try {
            NguoiDung nguoiDung = nguoiDungRepository.findBySoDienThoai(phone)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với số điện thoại: " + phone));

            List<String> addresses = new ArrayList<>();
            if (nguoiDung.getDiaChi() != null && !nguoiDung.getDiaChi().trim().isEmpty()) {
                addresses.add(nguoiDung.getDiaChi());
            }
            String detailedAddress = constructDetailedAddress(nguoiDung);
            if (detailedAddress != null && !addresses.contains(detailedAddress)) {
                addresses.add(detailedAddress);
            }
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
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy địa chỉ khách hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/customer/{phone}/add-address")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addCustomerAddress(@PathVariable String phone, @RequestBody Map<String, String> request) {
        try {
            NguoiDung nguoiDung = nguoiDungRepository.findBySoDienThoai(phone)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với số điện thoại: " + phone));
            String addressLine = request.get("addressLine");
            String ward = request.get("ward");
            String district = request.get("district");
            String city = request.get("city");
            if (addressLine != null && ward != null && district != null && city != null &&
                    !addressLine.trim().isEmpty() && !ward.trim().isEmpty() && !district.trim().isEmpty() && !city.trim().isEmpty()) {
                nguoiDung.setChiTietDiaChi(addressLine);
                nguoiDung.setPhuongXa(ward);
                nguoiDung.setQuanHuyen(district);
                nguoiDung.setTinhThanhPho(city);
                nguoiDungRepository.save(nguoiDung);

                List<String> sessionAddresses = (List<String>) session.getAttribute("customer_addresses_" + phone);
                if (sessionAddresses == null) {
                    sessionAddresses = new ArrayList<>();
                }
                String fullAddress = addressLine + ", " + ward + ", " + district + ", " + city;
                if (!sessionAddresses.contains(fullAddress)) {
                    sessionAddresses.add(fullAddress);
                }
                session.setAttribute("customer_addresses_" + phone, sessionAddresses);

                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Địa chỉ không hợp lệ"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Lỗi khi thêm địa chỉ: " + e.getMessage()));
        }
    }

    @GetMapping("/vouchers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getApplicableVouchers(@RequestParam String subTotal, @RequestParam(required = false) BigDecimal shippingFee, @RequestParam String tabId) {
        try {
            String cleanSubTotal = subTotal.replaceAll("[^0-9]", "");
            BigDecimal amount = new BigDecimal(cleanSubTotal.isEmpty() ? "0" : cleanSubTotal);
            System.out.println("Debug - Cleaned subTotal: " + amount + ", tabId: " + tabId);

            GioHangDTO gioHang = gioHangService.layGioHang(shippingFee != null ? shippingFee : BigDecimal.ZERO, tabId);
            gioHang.setTongTienHang(amount);

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
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy phiếu giảm giá: " + e.getMessage()));
        }
    }

    @PostMapping("/create-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody DonHangDTO donHangDTO) {
        try {
            System.out.println("=== [DEBUG] Nhận đơn hàng từ client ===");
            System.out.println("SĐT: " + donHangDTO.getSoDienThoaiKhachHang());
            System.out.println("SP: " + (donHangDTO.getDanhSachSanPham() != null ? donHangDTO.getDanhSachSanPham().size() : "null"));
            System.out.println("PTTT: " + donHangDTO.getPhuongThucThanhToan());
            System.out.println("Tiền khách đưa: " + donHangDTO.getSoTienKhachDua());
            System.out.println("Giao đến: " + donHangDTO.getDiaChiGiaoHang());
            System.out.println("TabId: " + donHangDTO.getTabId());

            if (donHangDTO.getDanhSachSanPham() == null || donHangDTO.getDanhSachSanPham().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh sách sản phẩm trống!"));
            }

            KetQuaDonHangDTO ketQua = donHangService.taoDonHang(donHangDTO);
            donHangTamRepository.deleteByTabId(donHangDTO.getTabId()); // Xóa đơn hàng tạm sau khi tạo đơn hàng chính thức
            session.removeAttribute("tempStockChanges"); // Xóa tồn kho tạm
            return ResponseEntity.ok(Map.of(
                    "message", "Tạo đơn hàng thành công!",
                    "maDonHang", ketQua.getMaDonHang()
            ));
        } catch (Exception e) {
            System.err.println("Lỗi tạo đơn hàng: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo đơn hàng: " + e.getMessage()));
        }
    }

    @GetMapping("/cart/apply-voucher")
    @ResponseBody
    public ResponseEntity<?> applyVoucher(@RequestParam UUID voucherId, @RequestParam BigDecimal shippingFee, @RequestParam String tabId) {
        System.out.println("Received voucherId: " + voucherId + ", shippingFee: " + shippingFee + ", tabId: " + tabId);
        try {
            GioHangDTO gioHangDTO = gioHangService.layGioHang(shippingFee != null ? shippingFee : BigDecimal.ZERO, tabId);
            GioHangDTO updatedGioHang = gioHangService.apDungPhieuGiamGia(voucherId, shippingFee, gioHangDTO, tabId);
            return ResponseEntity.ok(updatedGioHang);
        } catch (Exception e) {
            System.err.println("Error in applyVoucher: " + e.getMessage());
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
    public String hienThiTrangBanHang(Model model) {
        try {
            List<SanPham> products = sanPhamService.findAllByTrangThai();
            for (SanPham product : products) {
                product.setChiTietSanPhams(chiTietSanPhamService.findByProductId(product.getId()));
            }
            model.addAttribute("products", products);
            model.addAttribute("chiTietSanPhams", chiTietSanPhamService.findAllByTrangThai());
            model.addAttribute("customers", nguoiDungService.findUsersByVaiTro("customer", "", 0, 10).getContent());
            model.addAttribute("paymentMethods", phuongThucThanhToanService.findAll());
            model.addAttribute("discountVouchers", phieuGiamGiaService.layTatCa().stream()
                    .filter(v -> "Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(v)))
                    .collect(Collectors.toList()));
            BigDecimal defaultShippingFee = BigDecimal.ZERO;
            model.addAttribute("gioHang", gioHangService.layGioHang(defaultShippingFee, "default"));
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));

            List<DonHangTam> heldOrders = donHangTamRepository.findAll();
            Map<String, Object> tabs = new HashMap<>();
            for (DonHangTam order : heldOrders) {
                Map<String, Object> tabData = new HashMap<>();
                tabData.put("id", order.getId());
                tabData.put("tabId", order.getTabId());
                tabData.put("maDonHangTam", order.getMaDonHangTam());
                tabData.put("tenKhachHang", nguoiDungRepository.findById(order.getKhachHang())
                        .map(NguoiDung::getHoTen).orElse("Không rõ"));
                tabData.put("soDienThoaiKhachHang", order.getSoDienThoaiKhachHang());
                tabData.put("tong", order.getTong());
                tabData.put("phuongThucThanhToan", order.getPhuongThucThanhToan());
                tabData.put("phuongThucBanHang", order.getPhuongThucBanHang());
                tabData.put("phiVanChuyen", order.getPhiVanChuyen());
                tabData.put("idPhieuGiamGia", order.getPhieuGiamGia());
                try {
                    tabData.put("danhSachSanPham", objectMapper.readValue(order.getDanhSachSanPham(), new TypeReference<List<GioHangItemDTO>>(){}));
                } catch (Exception e) {
                    System.err.println("Lỗi parse danh sách sản phẩm cho tab " + order.getTabId() + ": " + e.getMessage());
                    tabData.put("danhSachSanPham", new ArrayList<>());
                }
                tabs.put(order.getTabId(), tabData);
            }
            model.addAttribute("orderTabs", tabs);
            model.addAttribute("orderCounter", heldOrders.size());

            return "WebQuanLy/ban-hang";
        } catch (Exception e) {
            System.err.println("Lỗi khi hiển thị trang bán hàng: " + e.getMessage());
            model.addAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            return "WebQuanLy/ban-hang";
        }
    }

    @GetMapping("/get-tabs")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTabs() {
        try {
            List<DonHangTam> heldOrders = donHangTamRepository.findAll();
            Map<String, Object> tabs = new HashMap<>();
            for (DonHangTam order : heldOrders) {
                Map<String, Object> tabData = new HashMap<>();
                tabData.put("id", order.getId());
                tabData.put("tabId", order.getTabId());
                tabData.put("maDonHangTam", order.getMaDonHangTam());
                tabData.put("tenKhachHang", nguoiDungRepository.findById(order.getKhachHang())
                        .map(NguoiDung::getHoTen).orElse("Không rõ"));
                tabData.put("soDienThoaiKhachHang", order.getSoDienThoaiKhachHang());
                tabData.put("tong", order.getTong());
                tabData.put("phuongThucThanhToan", order.getPhuongThucThanhToan());
                tabData.put("phuongThucBanHang", order.getPhuongThucBanHang());
                tabData.put("phiVanChuyen", order.getPhiVanChuyen());
                tabData.put("idPhieuGiamGia", order.getPhieuGiamGia());
                try {
                    tabData.put("danhSachSanPham", objectMapper.readValue(order.getDanhSachSanPham(), new TypeReference<List<GioHangItemDTO>>(){}));
                } catch (Exception e) {
                    System.err.println("Lỗi parse danh sách sản phẩm cho tab " + order.getTabId() + ": " + e.getMessage());
                    tabData.put("danhSachSanPham", new ArrayList<>());
                }
                tabs.put(order.getTabId(), tabData);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("orders", tabs);
            response.put("orderCounter", heldOrders.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách tab: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy danh sách tab: " + e.getMessage()));
        }
    }

    @PostMapping("/save-tabs")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveTabs(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> orders = (Map<String, Object>) request.get("orders");
            Integer orderCounter = (Integer) request.get("orderCounter");

            for (Map.Entry<String, Object> entry : orders.entrySet()) {
                String tabId = entry.getKey();
                Map<String, Object> tabData = (Map<String, Object>) entry.getValue();
                UUID khachHangId = tabData.get("khachHangId") != null ? UUID.fromString((String) tabData.get("khachHangId")) : null;
                String soDienThoai = (String) tabData.get("soDienThoaiKhachHang");
                GioHangDTO gioHangDTO = new GioHangDTO();
                gioHangDTO.setDanhSachSanPham(objectMapper.convertValue(tabData.get("danhSachSanPham"), new TypeReference<List<GioHangItemDTO>>(){}));
                gioHangDTO.setTong(new BigDecimal(tabData.get("tong").toString()));
                gioHangDTO.setPhiVanChuyen(tabData.get("phiVanChuyen") != null ? new BigDecimal(tabData.get("phiVanChuyen").toString()) : BigDecimal.ZERO);
                gioHangDTO.setPhuongThucThanhToan((String) tabData.get("phuongThucThanhToan"));
                gioHangDTO.setPhuongThucBanHang((String) tabData.get("phuongThucBanHang"));
                gioHangDTO.setIdPhieuGiamGia(tabData.get("idPhieuGiamGia") != null ? UUID.fromString((String) tabData.get("idPhieuGiamGia")) : null);
                gioHangService.saveCartToDonHangTam(gioHangDTO, tabId, khachHangId, soDienThoai);
            }

            session.setAttribute("orderTabs", orders);
            session.setAttribute("orderCounter", orderCounter);
            return ResponseEntity.ok(Map.of("success", "true"));
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu tab: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", "false", "error", "Lỗi khi lưu tab: " + e.getMessage()));
        }
    }

    @GetMapping("/product/{productId}/variants")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProductVariants(@PathVariable UUID productId) {
        try {
            List<ChiTietSanPham> variants = chiTietSanPhamService.findByProductId(productId).stream()
                    .filter(v -> v.getTrangThai() == true) // Chỉ lấy các biến thể có trạng thái = 1
                    .collect(Collectors.toList());
            if (variants.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy biến thể hợp lệ cho sản phẩm."));
            }

            List<Map<String, Object>> variantList = variants.stream().map(variant -> {
                Map<String, Object> variantMap = new HashMap<>();
                variantMap.put("colorId", variant.getMauSac().getId());
                variantMap.put("sizeId", variant.getKichCo().getId());
                variantMap.put("mauSac", variant.getMauSac().getTenMau());
                variantMap.put("kichCo", variant.getKichCo().getTen());
                variantMap.put("productDetailId", variant.getId());
                variantMap.put("price", variant.getGia());
                variantMap.put("stockQuantity", variant.getSoLuongTonKho());
                return variantMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("variants", variantList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy biến thể sản phẩm: " + e.getMessage()));
        }
    }

    @GetMapping("/product/variant")
    @ResponseBody
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
                // Lấy số lượng tồn kho thực tế (có thể trừ đi số lượng đã được giữ tạm thời)
                int actualStock = variant.getSoLuongTonKho();

                // Kiểm tra số lượng đã được giữ tạm thời trong session
                @SuppressWarnings("unchecked")
                Map<UUID, Map<String, Integer>> tempStockChanges =
                        (Map<UUID, Map<String, Integer>>) session.getAttribute("tempStockChanges");

                if (tempStockChanges != null && tempStockChanges.containsKey(variant.getId())) {
                    int reservedQuantity = tempStockChanges.get(variant.getId()).getOrDefault("reservedQuantity", 0);
                    actualStock = Math.max(0, actualStock - reservedQuantity);
                }

                result.put("stockQuantity", variant.getSoLuongTonKho()); // Tồn kho gốc
                result.put("availableStock", actualStock); // Tồn kho khả dụng
                result.put("price", variant.getGia());
                result.put("productDetailId", variant.getId());
                result.put("tenSanPham", variant.getSanPham().getTenSanPham());
                result.put("mauSac", variant.getMauSac().getTenMau());
                result.put("kichCo", variant.getKichCo().getTen());
                result.put("hinhAnh", variant.getSanPham().getUrlHinhAnh());
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

    @GetMapping("/products")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        try {
            List<ChiTietSanPham> products = chiTietSanPhamService.findAllByTrangThai();
            List<Map<String, Object>> productList = products.stream().map(product -> {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("idChiTietSanPham", product.getId());
                productMap.put("tenSanPham", product.getSanPham().getTenSanPham());
                productMap.put("mauSanPham", product.getMauSac());
                productMap.put("kichSanPham", product.getKichCo());
                productMap.put("price", product.getGia());
                return productMap;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("products", productList));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy danh sách sản phẩm: " + e.getMessage()));
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy chi tiết sản phẩm: " + e.getMessage()));
        }
    }

    @GetMapping("/cart/get")
    @ResponseBody
    public ResponseEntity<GioHangDTO> layGioHang(@RequestParam(required = false) BigDecimal shippingFee, @RequestParam String tabId) {
        System.out.println("Fetching cart với ID: " + tabId + ", shippingFee: " + shippingFee);
        try {
            BigDecimal fee = shippingFee != null ? shippingFee : BigDecimal.ZERO;
            GioHangDTO gioHang = gioHangService.layGioHang(fee, tabId);
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
        } catch (Exception e) {
            System.err.println("Exception in cart/get: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/add")
    @ResponseBody
    public ResponseEntity<GioHangDTO> themVaoGioHang(@RequestBody Map<String, Object> request) {
        System.out.println("Request received for /cart/add: " + request);
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.get("shippingFee") != null ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;
            String tabId = (String) request.get("tabId");

            System.out.println("Processing: productDetailId=" + productDetailId + ", quantity=" + quantity + ", shippingFee=" + shippingFee + ", tabId=" + tabId);

            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
            if (chiTiet == null) {
                System.err.println("ChiTietSanPham không tìm thấy với ID: " + productDetailId);
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            @SuppressWarnings("unchecked")
            Map<UUID, Map<String, Integer>> tempStockChanges = (Map<UUID, Map<String, Integer>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges == null) {
                tempStockChanges = new HashMap<>();
            }

            int availableStock = chiTiet.getSoLuongTonKho();
            if (tempStockChanges.containsKey(productDetailId)) {
                int reserved = tempStockChanges.get(productDetailId).getOrDefault("reservedQuantity", 0);
                availableStock -= reserved;
            }
            if (availableStock < quantity) {
                System.err.println("Số lượng tồn kho không đủ: available=" + availableStock + ", requested=" + quantity);
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            GioHangDTO gioHang = gioHangService.themVaoGioHang(productDetailId, quantity, shippingFee, tabId);
            tempStockChanges.put(productDetailId, Map.of("originalStock", chiTiet.getSoLuongTonKho(), "reservedQuantity", tempStockChanges.getOrDefault(productDetailId, Map.of("reservedQuantity", 0)).getOrDefault("reservedQuantity", 0) + quantity));
            session.setAttribute("tempStockChanges", tempStockChanges);

            System.out.println("Successfully added to cart: " + gioHang);
            return ResponseEntity.ok(gioHang);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID or data format: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        } catch (Exception e) {
            System.err.println("Exception in cart/add: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/update")
    @ResponseBody
    public ResponseEntity<GioHangDTO> capNhatGioHang(@RequestBody Map<String, Object> request) {
        System.out.println("Request received for /cart/update: " + request);
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.get("shippingFee") != null ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;
            String tabId = (String) request.get("tabId");

            System.out.println("Processing: productDetailId=" + productDetailId + ", quantity=" + quantity + ", shippingFee=" + shippingFee + ", tabId=" + tabId);

            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
            if (chiTiet == null) {
                System.err.println("ChiTietSanPham không tìm thấy với ID: " + productDetailId);
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            @SuppressWarnings("unchecked")
            Map<UUID, Map<String, Integer>> tempStockChanges = (Map<UUID, Map<String, Integer>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges == null) {
                tempStockChanges = new HashMap<>();
            }

            // Lấy số lượng hiện tại trong giỏ hàng
            GioHangDTO currentCart = gioHangService.layGioHang(shippingFee, tabId);
            int currentQuantity = currentCart.getDanhSachSanPham().stream()
                    .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                    .findFirst()
                    .map(GioHangItemDTO::getSoLuong)
                    .orElse(0);

            // Tính số lượng đã giữ tạm thời (reservedQuantity)
            int currentReserved = tempStockChanges.containsKey(productDetailId) ? tempStockChanges.get(productDetailId).getOrDefault("reservedQuantity", 0) : 0;
            int totalReserved = currentReserved - currentQuantity + quantity; // Tổng số lượng mới
            int availableStock = chiTiet.getSoLuongTonKho() - totalReserved;

            if (quantity < 0 || availableStock < 0) {
                System.err.println("Số lượng tồn kho không đủ để cập nhật: available=" + availableStock + ", requested=" + quantity);
                return ResponseEntity.badRequest().body(new GioHangDTO());
            }

            // Cập nhật giỏ hàng
            GioHangDTO gioHang = gioHangService.capNhatGioHang(productDetailId, quantity, shippingFee, tabId);
            // Cập nhật reservedQuantity với tổng số lượng mới
            tempStockChanges.put(productDetailId, Map.of("originalStock", chiTiet.getSoLuongTonKho(), "reservedQuantity", totalReserved));
            session.setAttribute("tempStockChanges", tempStockChanges);

            System.out.println("Successfully updated cart: " + gioHang);
            return ResponseEntity.ok(gioHang);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID or data format: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        } catch (Exception e) {
            System.err.println("Exception in cart/update: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/remove")
    @ResponseBody
    public ResponseEntity<GioHangDTO> xoaKhoiGioHang(@RequestBody Map<String, String> request) {
        try {
            UUID productDetailId = UUID.fromString(request.get("productDetailId"));
            BigDecimal shippingFee = request.get("shippingFee") != null ? new BigDecimal(request.get("shippingFee")) : BigDecimal.ZERO;
            String tabId = request.get("tabId");

            System.out.println("Processing remove: productDetailId=" + productDetailId + ", shippingFee=" + shippingFee + ", tabId=" + tabId);

            GioHangDTO gioHang = gioHangService.xoaKhoiGioHang(productDetailId, shippingFee, tabId);
            @SuppressWarnings("unchecked")
            Map<UUID, Map<String, Integer>> tempStockChanges = (Map<UUID, Map<String, Integer>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges != null && tempStockChanges.containsKey(productDetailId)) {
                tempStockChanges.remove(productDetailId);
                session.setAttribute("tempStockChanges", tempStockChanges);
            }
            return ResponseEntity.ok(gioHang);
        } catch (Exception e) {
            System.err.println("Exception in cart/remove: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/clear")
    @ResponseBody
    public ResponseEntity<GioHangDTO> xoaTatCaGioHang(@RequestParam String tabId) {
        try {
            System.out.println("Clearing cart for tabId: " + tabId);
            GioHangDTO gioHang = gioHangService.xoaTatCaGioHang(tabId);
            donHangTamRepository.deleteByTabId(tabId);
            session.removeAttribute("tempStockChanges");
            return ResponseEntity.ok(gioHang);
        } catch (Exception e) {
            System.err.println("Exception in cart/clear: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/add-customer")
    public String themKhachHang(@ModelAttribute NguoiDungDTO khachHangDTO) {
        try {
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
                return "redirect:/acvstore/ban-hang?error=Số điện thoại đã tồn tại";
            }
            nguoiDungService.save(nguoiDung);
            return "redirect:/acvstore/ban-hang";
        } catch (Exception e) {
            System.err.println("Lỗi khi thêm khách hàng: " + e.getMessage());
            return "redirect:/acvstore/ban-hang?error=Lỗi hệ thống";
        }
    }

    @PostMapping("/hold-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> giuDonHang(@RequestBody DonHangDTO donHangDTO) {
        try {
            System.out.println("Holding order for tabId: " + donHangDTO.getTabId());
            DonHangDTO donHangDaGiu = donHangService.giuDonHang(donHangDTO);
            BigDecimal shippingFee = donHangDTO.getPhiVanChuyen() != null ? donHangDTO.getPhiVanChuyen() : BigDecimal.ZERO;
            String tabId = donHangDTO.getTabId();

            GioHangDTO gioHangDTO = gioHangService.layGioHang(shippingFee, tabId);
            gioHangService.saveCartToDonHangTam(gioHangDTO, tabId, donHangDTO.getKhachHangId(), donHangDTO.getSoDienThoaiKhachHang());

            session.removeAttribute("tempStockChanges");
            gioHangService.xoaTatCaGioHang(tabId);

            GioHangDTO gioHangRong = gioHangService.layGioHang(BigDecimal.ZERO, tabId);
            return ResponseEntity.ok(Map.of("message", "Đơn hàng đã được giữ thành công!", "cart", gioHangRong));
        } catch (Exception e) {
            System.err.println("Lỗi khi giữ đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi giữ đơn hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/delete-all-held-orders")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaTatCaDonHangTam() {
        try {
            donHangTamRepository.deleteAll();
            session.removeAttribute("tempStockChanges");
            return ResponseEntity.ok(Map.of("message", "Đã xóa tất cả đơn hàng tạm!"));
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa tất cả đơn hàng tạm: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa đơn hàng tạm: " + e.getMessage()));
        }
    }

    @GetMapping("/held-orders")
    @ResponseBody
    public ResponseEntity<List<DonHangTamDTO>> layDonHangTam() {
        try {
            List<DonHangTam> heldOrders = donHangTamRepository.findAll();
            List<DonHangTamDTO> dtos = heldOrders.stream().map(order -> {
                DonHangTamDTO dto = new DonHangTamDTO(order, objectMapper);
                return dto;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách đơn hàng tạm: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    @GetMapping("/held-order-details/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chiTietDonHangTam(@PathVariable UUID orderId) {
        try {
            DonHangTam donHangTam = donHangTamRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng tạm không tồn tại với ID: " + orderId));
            System.out.println("Tìm thấy đơn hàng tạm với ID: " + orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", donHangTam.getId());
            response.put("tabId", donHangTam.getTabId());
            response.put("maDonHangTam", donHangTam.getMaDonHangTam());
            response.put("tenKhachHang", nguoiDungRepository.findById(donHangTam.getKhachHang())
                    .map(NguoiDung::getHoTen).orElse("Không rõ"));
            response.put("soDienThoaiKhachHang", donHangTam.getSoDienThoaiKhachHang());
            response.put("tong", donHangTam.getTong());
            response.put("thoiGianTao", donHangTam.getThoiGianTao());
            response.put("phuongThucThanhToan", donHangTam.getPhuongThucThanhToan());
            response.put("phuongThucBanHang", donHangTam.getPhuongThucBanHang());
            response.put("phiVanChuyen", donHangTam.getPhiVanChuyen());
            response.put("idPhieuGiamGia", donHangTam.getPhieuGiamGia());

            if (donHangTam.getDanhSachSanPham() != null) {
                try {
                    System.out.println("Parse danh sách sản phẩm cho đơn hàng tạm: " + orderId);
                    List<GioHangItemDTO> danhSachSanPham = objectMapper.readValue(
                            donHangTam.getDanhSachSanPham(),
                            new TypeReference<List<GioHangItemDTO>>(){}
                    );
                    response.put("danhSachSanPham", danhSachSanPham);
                } catch (Exception e) {
                    System.err.println("Lỗi parse danh sách sản phẩm: " + e.getMessage());
                    throw new RuntimeException("Lỗi khi parse danh_sachSanPham: " + e.getMessage());
                }
            } else {
                System.out.println("Không có danh sách sản phẩm cho đơn hàng tạm: " + orderId);
                response.put("danhSachSanPham", new ArrayList<>());
            }

            System.out.println("Trả về chi tiết đơn hàng tạm: " + response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy chi tiết đơn hàng tạm: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi lấy chi tiết: " + e.getMessage()));
        }
    }

    @PostMapping("/restore-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreOrder(@RequestParam UUID orderId) {
        try {
            DonHangTam donHangTam = donHangTamRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng tạm với ID: " + orderId));
            System.out.println("Khôi phục đơn hàng tạm với ID: " + orderId);

            String tabId = donHangTam.getTabId();
            gioHangService.xoaTatCaGioHang(tabId);

            List<GioHangItemDTO> danhSachSanPham = new ArrayList<>();
            if (donHangTam.getDanhSachSanPham() != null) {
                System.out.println("Parse danh sách sản phẩm từ đơn hàng tạm");
                List<GioHangItemDTO> items = objectMapper.readValue(
                        donHangTam.getDanhSachSanPham(),
                        new TypeReference<List<GioHangItemDTO>>(){}
                );
                for (GioHangItemDTO item : items) {
                    UUID productDetailId = item.getIdChiTietSanPham();
                    ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
                    if (chiTiet != null) {
                        int quantity = item.getSoLuong();
                        if (chiTiet.getSoLuongTonKho() < quantity) {
                            throw new RuntimeException("Số lượng tồn kho không đủ cho sản phẩm: " + chiTiet.getSanPham().getTenSanPham());
                        }
                        gioHangService.themVaoGioHang(productDetailId, quantity, BigDecimal.ZERO, tabId);
                        danhSachSanPham.add(item);
                    }
                }
            } else {
                System.out.println("Không có danh sách sản phẩm trong đơn hàng tạm: " + orderId);
            }

            if (donHangTam.getPhieuGiamGia() != null) {
                GioHangDTO gioHang = gioHangService.layGioHang(donHangTam.getPhiVanChuyen() != null ? donHangTam.getPhiVanChuyen() : BigDecimal.ZERO, tabId);
                gioHangService.apDungPhieuGiamGia(donHangTam.getPhieuGiamGia(), donHangTam.getPhiVanChuyen(), gioHang, tabId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", donHangTam.getId());
            response.put("tabId", donHangTam.getTabId());
            response.put("maDonHangTam", donHangTam.getMaDonHangTam());
            response.put("soDienThoaiKhachHang", donHangTam.getSoDienThoaiKhachHang());
            response.put("phiVanChuyen", donHangTam.getPhiVanChuyen() != null ? donHangTam.getPhiVanChuyen() : BigDecimal.ZERO);
            response.put("phuongThucThanhToan", donHangTam.getPhuongThucThanhToan());
            response.put("phuongThucBanHang", donHangTam.getPhuongThucBanHang());
            response.put("idPhieuGiamGia", donHangTam.getPhieuGiamGia());
            response.put("tong", donHangTam.getTong() != null ? donHangTam.getTong() : BigDecimal.ZERO);
            response.put("danhSachSanPham", danhSachSanPham);

            donHangTamRepository.deleteById(orderId);
            session.removeAttribute("tempStockChanges");

            System.out.println("Khôi phục đơn hàng thành công: " + response);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.err.println("Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Lỗi server khi khôi phục đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @PostMapping("/payment-methods")
    @ResponseBody
    public ResponseEntity<List<PhuongThucThanhToan>> getPaymentMethods() {
        try {
            List<PhuongThucThanhToan> paymentMethods = phuongThucThanhToanService.findAll();
            System.out.println("Lấy danh sách phương thức thanh toán: " + paymentMethods.size());
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy phương thức thanh toán: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ArrayList<>());
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
            if (hex.length() == 1) {
                hash.append('0');
            }
            hash.append(hex);
        }
        return hash.toString();
    }


    @PostMapping("/delete-held-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteHeldOrder(@RequestParam UUID orderId) {
        try {
            System.out.println("Xóa đơn hàng tạm với ID: " + orderId);
            donHangTamRepository.deleteById(orderId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa đơn hàng tạm thành công."));
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa đơn hàng tạm: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa đơn hàng tạm: " + e.getMessage()));
        }
    }
}