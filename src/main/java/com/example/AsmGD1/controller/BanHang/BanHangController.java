package com.example.AsmGD1.controller.BanHang;

import com.example.AsmGD1.config.VNPayConfig;
import com.example.AsmGD1.dto.BanHang.*;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.DonHangTamRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.BanHang.DonHangService;
import com.example.AsmGD1.service.BanHang.GioHangService;
import com.example.AsmGD1.service.BanHang.QRCodeService;
import com.example.AsmGD1.service.BanHang.VNPayService;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
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
    private ChienDichGiamGiaService chienDichGiamGiaService;

    @Autowired
    private DiaChiNguoiDungRepository diaChiNguoiDungRepository;


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

            // Thêm địa chỉ mặc định từ NguoiDung
            String defaultAddress = constructDetailedAddress(nguoiDung);
            if (defaultAddress != null && !defaultAddress.isBlank()) {
                addresses.add("[Mặc định] " + defaultAddress);  // Gắn tag nếu muốn
            }

            // Thêm các địa chỉ khác từ bảng DiaChiNguoiDung
            List<DiaChiNguoiDung> diaChiKhac = diaChiNguoiDungRepository.findByNguoiDung_Id(nguoiDung.getId());
            for (DiaChiNguoiDung diaChi : diaChiKhac) {
                String otherAddr = constructDetailedAddress(diaChi);
                if (otherAddr != null && !addresses.contains(otherAddr)) {
                    addresses.add(otherAddr);
                }
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

            String chiTietDiaChi = request.get("chiTietDiaChi");
            String phuongXa = request.get("phuongXa");
            String quanHuyen = request.get("quanHuyen");
            String tinhThanhPho = request.get("tinhThanhPho");

            if (chiTietDiaChi != null && phuongXa != null && quanHuyen != null && tinhThanhPho != null &&
                    !chiTietDiaChi.trim().isEmpty() && !phuongXa.trim().isEmpty() && !quanHuyen.trim().isEmpty() && !tinhThanhPho.trim().isEmpty()) {
                nguoiDung.setChiTietDiaChi(chiTietDiaChi);
                nguoiDung.setPhuongXa(phuongXa);
                nguoiDung.setQuanHuyen(quanHuyen);
                nguoiDung.setTinhThanhPho(tinhThanhPho);
                nguoiDungRepository.save(nguoiDung);

                List<String> sessionAddresses = (List<String>) session.getAttribute("customer_addresses_" + phone);
                if (sessionAddresses == null) {
                    sessionAddresses = new ArrayList<>();
                }
                String fullAddress = chiTietDiaChi + ", " + phuongXa + ", " + quanHuyen + ", " + tinhThanhPho;
                if (!sessionAddresses.contains(fullAddress)) {
                    sessionAddresses.add(fullAddress);
                    session.setAttribute("customer_addresses_" + phone, sessionAddresses);
                }

                return ResponseEntity.ok(Map.of("success", true, "message", "Đã thêm địa chỉ thành công"));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Địa chỉ không hợp lệ"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Lỗi khi thêm địa chỉ: " + e.getMessage()));
        }
    }

        @GetMapping("/vouchers")
        @ResponseBody
        public ResponseEntity<Map<String, Object>> getApplicableVouchers(
                @RequestParam String subTotal,
                @RequestParam(required = false) BigDecimal shippingFee,
                @RequestParam String tabId,
                @RequestParam(required = false) UUID customerId // <- thêm dòng này
        ) {
            try {
                String cleanSubTotal = subTotal.replaceAll("[^0-9]", "");
                BigDecimal amount = new BigDecimal(cleanSubTotal.isEmpty() ? "0" : cleanSubTotal);
                System.out.println("Debug - Cleaned subTotal: " + amount + ", tabId: " + tabId);

                GioHangDTO gioHang = gioHangService.layGioHang(
                        shippingFee != null ? shippingFee : BigDecimal.ZERO,
                        tabId
                );
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

                // Lấy phiếu công khai hợp lệ
                List<PhieuGiamGia> congKhai = phieuGiamGiaService.layTatCa().stream()
                        .filter(v -> "cong_khai".equalsIgnoreCase(v.getKieuPhieu()))
                        .filter(v -> "Đang diễn ra".equals(phieuGiamGiaService.tinhTrang(v)))
                        .filter(v -> amount.compareTo(v.getGiaTriGiamToiThieu()) >= 0)
                        .collect(Collectors.toList());

                List<PhieuGiamGia> caNhan = new ArrayList<>();
                if (customerId != null) {
                    caNhan = phieuGiamGiaCuaNguoiDungService.layPhieuCaNhanConHan(customerId).stream()
                            .filter(v -> amount.compareTo(v.getGiaTriGiamToiThieu()) >= 0)
                            .collect(Collectors.toList());
                }

                List<PhieuGiamGia> applicableVouchers = new ArrayList<>();
                applicableVouchers.addAll(congKhai);
                applicableVouchers.addAll(caNhan);

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
            if (donHangDTO.getDanhSachSanPham() == null || donHangDTO.getDanhSachSanPham().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh sách sản phẩm trống!"));
            }

            // Tạo đơn hàng
            KetQuaDonHangDTO ketQua = donHangService.taoDonHang(donHangDTO);

            // Cập nhật số lượng chiến dịch nếu có
            for (GioHangItemDTO item : donHangDTO.getDanhSachSanPham()) {
                ChiTietSanPham chiTiet = chiTietSanPhamService.findById(item.getIdChiTietSanPham());
                if (chiTiet != null) {
                    ChienDichGiamGia cdgg = chiTiet.getChienDichGiamGia();
                    if (cdgg != null && "ONGOING".equals(cdgg.getStatus())) {
                        chienDichGiamGiaService.truSoLuong(cdgg.getId(), item.getSoLuong());
                    }
                }
            }

            // Xóa tab hiện tại
            donHangTamRepository.deleteByTabId(donHangDTO.getTabId());
            gioHangService.xoaTatCaGioHang(donHangDTO.getTabId());

            // Kiểm tra số lượng tab còn lại
            List<DonHangTam> remainingTabs = donHangTamRepository.findAll();
            String newTabId = null;
            if (remainingTabs.isEmpty()) {
                // Tạo tab mới với ID "1"
                GioHangDTO newCart = gioHangService.xoaTatCaGioHang("1");
                newTabId = "1";
            } else {
                // Lấy tab cuối cùng làm tab hiện tại
                newTabId = remainingTabs.get(remainingTabs.size() - 1).getTabId();
            }

            // Dọn dẹp dữ liệu tạm
            session.removeAttribute("tempStockChanges");
            session.removeAttribute("pendingOrder_" + donHangDTO.getTabId());

            // Tạo hóa đơn PDF
            byte[] pdfData = hoaDonService.taoHoaDon(ketQua.getMaDonHang());
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfData);

            // Trả về kết quả
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tạo đơn hàng thành công!");
            response.put("maDonHang", ketQua.getMaDonHang());
            response.put("pdfData", pdfBase64);
            response.put("tabId", donHangDTO.getTabId());
            response.put("newTabId", newTabId);
            response.put("remainingTabs", remainingTabs.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi tạo đơn hàng: " + e.getMessage()));
        }
    }



    private int getAvailableStock(UUID productDetailId, int originalStock) {
        @SuppressWarnings("unchecked")
        Map<UUID, Map<String, Integer>> tempStockChanges = (Map<UUID, Map<String, Integer>>) session.getAttribute("tempStockChanges");
        if (tempStockChanges != null && tempStockChanges.containsKey(productDetailId)) {
            int reservedQuantity = tempStockChanges.get(productDetailId).getOrDefault("reservedQuantity", 0);
            return Math.max(0, originalStock - reservedQuantity);
        }
        return originalStock;
    }

    private void updateTempStock(UUID productDetailId, int quantityChange, boolean isAdd) {
        @SuppressWarnings("unchecked")
        Map<UUID, Map<String, Integer>> tempStockChanges = (Map<UUID, Map<String, Integer>>) session.getAttribute("tempStockChanges");
        if (tempStockChanges == null) {
            tempStockChanges = new HashMap<>();
        }
        Map<String, Integer> stockInfo = tempStockChanges.getOrDefault(productDetailId, new HashMap<>());
        int currentReserved = stockInfo.getOrDefault("reservedQuantity", 0);
        int newReserved = isAdd ? currentReserved + quantityChange : Math.max(0, currentReserved + quantityChange);
        stockInfo.put("reservedQuantity", newReserved);
        ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
        if (chiTiet != null) {
            stockInfo.put("originalStock", chiTiet.getSoLuongTonKho());
        }
        tempStockChanges.put(productDetailId, stockInfo);
        session.setAttribute("tempStockChanges", tempStockChanges);
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
        StringBuilder sb = new StringBuilder();
        if (nguoiDung.getChiTietDiaChi() != null && !nguoiDung.getChiTietDiaChi().isBlank()) sb.append(nguoiDung.getChiTietDiaChi());
        if (nguoiDung.getPhuongXa() != null && !nguoiDung.getPhuongXa().isBlank()) sb.append(", ").append(nguoiDung.getPhuongXa());
        if (nguoiDung.getQuanHuyen() != null && !nguoiDung.getQuanHuyen().isBlank()) sb.append(", ").append(nguoiDung.getQuanHuyen());
        if (nguoiDung.getTinhThanhPho() != null && !nguoiDung.getTinhThanhPho().isBlank()) sb.append(", ").append(nguoiDung.getTinhThanhPho());
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String constructDetailedAddress(DiaChiNguoiDung diaChi) {
        StringBuilder sb = new StringBuilder();
        if (diaChi.getChiTietDiaChi() != null && !diaChi.getChiTietDiaChi().isBlank()) sb.append(diaChi.getChiTietDiaChi());
        if (diaChi.getPhuongXa() != null && !diaChi.getPhuongXa().isBlank()) sb.append(", ").append(diaChi.getPhuongXa());
        if (diaChi.getQuanHuyen() != null && !diaChi.getQuanHuyen().isBlank()) sb.append(", ").append(diaChi.getQuanHuyen());
        if (diaChi.getTinhThanhPho() != null && !diaChi.getTinhThanhPho().isBlank()) sb.append(", ").append(diaChi.getTinhThanhPho());
        return sb.length() > 0 ? sb.toString() : null;
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
                NguoiDung user = (NguoiDung) auth.getPrincipal();
                model.addAttribute("user", user);
            }

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
                    .filter(v -> v.getTrangThai() == true)
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
                BigDecimal giaSauGiam = tinhGiaSauGiam(variant); // Tính giá sau giảm
                variantMap.put("price", variant.getGia());
                variantMap.put("discountedPrice", giaSauGiam); // Giá sau giảm
                variantMap.put("phanTramGiam", variant.getChienDichGiamGia() != null && "ONGOING".equals(variant.getChienDichGiamGia().getStatus())
                        ? variant.getChienDichGiamGia().getPhanTramGiam() : BigDecimal.ZERO);
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
                int actualStock = variant.getSoLuongTonKho();
                BigDecimal giaSauGiam = tinhGiaSauGiam(variant); // Tính giá sau giảm

                @SuppressWarnings("unchecked")
                Map<UUID, Map<String, Integer>> tempStockChanges =
                        (Map<UUID, Map<String, Integer>>) session.getAttribute("tempStockChanges");

                if (tempStockChanges != null && tempStockChanges.containsKey(variant.getId())) {
                    int reservedQuantity = tempStockChanges.get(variant.getId()).getOrDefault("reservedQuantity", 0);
                    actualStock = Math.max(0, actualStock - reservedQuantity);
                }

                result.put("stockQuantity", variant.getSoLuongTonKho());
                result.put("availableStock", actualStock);
                result.put("price", variant.getGia());
                result.put("discountedPrice", giaSauGiam); // Giá sau giảm
                result.put("phanTramGiam", variant.getChienDichGiamGia() != null && "ONGOING".equals(variant.getChienDichGiamGia().getStatus())
                        ? variant.getChienDichGiamGia().getPhanTramGiam() : BigDecimal.ZERO);
                result.put("productDetailId", variant.getId());
                result.put("tenSanPham", variant.getSanPham().getTenSanPham());
                result.put("mauSac", variant.getMauSac().getTenMau());
                result.put("kichCo", variant.getKichCo().getTen());
                result.put("hinhAnh", chiTietSanPhamService.layAnhDauTien(variant));
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

    private BigDecimal tinhGiaSauGiam(ChiTietSanPham chiTiet) {
        BigDecimal gia = chiTiet.getGia();
        ChienDichGiamGia chienDich = chiTiet.getChienDichGiamGia();
        if (chienDich != null && "ONGOING".equals(chienDich.getStatus()) && (chienDich.getSoLuong() == null || chienDich.getSoLuong() > 0)) {
            BigDecimal phanTramGiam = chienDich.getPhanTramGiam();
            if (phanTramGiam != null) {
                BigDecimal giamGia = gia.multiply(phanTramGiam).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                return gia.subtract(giamGia);
            }
        }
        return gia;
    }

    @GetMapping("/products")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllProducts(@RequestParam(required = false, defaultValue = "") String keyword) {
        try {
            List<ChiTietSanPham> products;
            if (keyword.isEmpty()) {
                products = chiTietSanPhamService.findAllByTrangThai();
            } else {
                products = chiTietSanPhamService.findAllByTrangThaiAndKeyword(keyword);
            }

            List<Map<String, Object>> productList = products.stream().map(product -> {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("idChiTietSanPham", product.getId());
                productMap.put("tenSanPham", product.getSanPham().getTenSanPham());
                productMap.put("mauSanPham", product.getMauSac());
                productMap.put("kichSanPham", product.getKichCo());
                productMap.put("price", product.getGia());
                productMap.put("availableStock", getAvailableStock(product.getId(), product.getSoLuongTonKho()));
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
            response.put("hinhAnh", chiTietSanPhamService.layAnhDauTien(chiTiet));
            response.put("availableStock", getAvailableStock(id, chiTiet.getSoLuongTonKho()));
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
                        item.setAvailableStock(getAvailableStock(item.getIdChiTietSanPham(), chiTiet.getSoLuongTonKho()));
                    }
                }
            }
            return ResponseEntity.ok(gioHang);
        } catch (Exception e) {
            System.err.println("Exception in cart/get: " + e.getMessage());
            return ResponseEntity.badRequest().body(new GioHangDTO());
        }
    }

    @PostMapping("/cart/add")
    @ResponseBody
    public ResponseEntity<?> themVaoGioHang(@RequestBody Map<String, Object> request) {
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.get("shippingFee") != null ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;
            String tabId = (String) request.get("tabId");

            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy sản phẩm."));
            }

            int availableStock = getAvailableStock(productDetailId, chiTiet.getSoLuongTonKho());
            if (availableStock < quantity) {
                return ResponseEntity.badRequest().body(Map.of("error", "Số lượng tồn kho không đủ: còn " + availableStock));
            }

            GioHangDTO gioHang = gioHangService.themVaoGioHang(productDetailId, quantity, shippingFee, tabId);
            updateTempStock(productDetailId, quantity, true); // Reserve stock
            return ResponseEntity.ok(gioHang);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "UUID không hợp lệ."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi thêm vào giỏ hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/cart/update")
    @ResponseBody
    public ResponseEntity<?> capNhatGioHang(@RequestBody Map<String, Object> request) {
        try {
            UUID productDetailId = UUID.fromString((String) request.get("productDetailId"));
            int quantity = (int) request.get("quantity");
            BigDecimal shippingFee = request.get("shippingFee") != null ? new BigDecimal(request.get("shippingFee").toString()) : BigDecimal.ZERO;
            String tabId = (String) request.get("tabId");

            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy sản phẩm."));
            }

            GioHangDTO currentCart = gioHangService.layGioHang(shippingFee, tabId);
            int currentQuantity = currentCart.getDanhSachSanPham().stream()
                    .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                    .findFirst()
                    .map(GioHangItemDTO::getSoLuong)
                    .orElse(0);

            int availableStock = getAvailableStock(productDetailId, chiTiet.getSoLuongTonKho());
            if (quantity < 0 || availableStock < (quantity - currentQuantity)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Số lượng tồn kho không đủ: còn " + availableStock));
            }

            GioHangDTO gioHang = gioHangService.capNhatGioHang(productDetailId, quantity, shippingFee, tabId);
            updateTempStock(productDetailId, quantity - currentQuantity, true); // Update reservation
            return ResponseEntity.ok(gioHang);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "UUID không hợp lệ."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi cập nhật giỏ hàng: " + e.getMessage()));
        }
    }

    @PostMapping("/cart/remove")
    @ResponseBody
    public ResponseEntity<?> xoaKhoiGioHang(@RequestBody Map<String, String> request) {
        try {
            UUID productDetailId = UUID.fromString(request.get("productDetailId"));
            BigDecimal shippingFee = request.get("shippingFee") != null ? new BigDecimal(request.get("shippingFee")) : BigDecimal.ZERO;
            String tabId = request.get("tabId");

            GioHangDTO currentCart = gioHangService.layGioHang(shippingFee, tabId);
            int removedQuantity = currentCart.getDanhSachSanPham().stream()
                    .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                    .findFirst()
                    .map(GioHangItemDTO::getSoLuong)
                    .orElse(0);

            GioHangDTO gioHang = gioHangService.xoaKhoiGioHang(productDetailId, shippingFee, tabId);
            updateTempStock(productDetailId, -removedQuantity, false); // Release reserved stock
            return ResponseEntity.ok(gioHang);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa sản phẩm: " + e.getMessage()));
        }
    }

    @PostMapping("/cart/clear")
    @ResponseBody
    public ResponseEntity<?> xoaTatCaGioHang(@RequestParam String tabId) {
        try {
            GioHangDTO gioHang = gioHangService.layGioHang(BigDecimal.ZERO, tabId);
            for (GioHangItemDTO item : gioHang.getDanhSachSanPham()) {
                updateTempStock(item.getIdChiTietSanPham(), -item.getSoLuong(), false); // Release all reserved stock
            }
            gioHang = gioHangService.xoaTatCaGioHang(tabId);
            donHangTamRepository.deleteByTabId(tabId);
            session.removeAttribute("tempStockChanges");
            return ResponseEntity.ok(gioHang);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa giỏ hàng: " + e.getMessage()));
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