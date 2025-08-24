package com.example.AsmGD1.controller.GiamGia;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/chien-dich-giam-gia")
public class ChienDichGiamGiaController {

    @Autowired
    private ChienDichGiamGiaService chienDichService;

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private NguoiDungService nguoiDungService;

    // ===== RBAC helpers =====
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            return "admin".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private boolean isCurrentUserEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            return "employee".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            model.addAttribute("user", user);
            model.addAttribute("isAdmin", "admin".equalsIgnoreCase(user.getVaiTro()));
            model.addAttribute("isEmployee", "employee".equalsIgnoreCase(user.getVaiTro()));
        } else {
            NguoiDung defaultUser = new NguoiDung();
            defaultUser.setTenDangNhap("Unknown");
            defaultUser.setVaiTro("GUEST");
            model.addAttribute("user", defaultUser);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isEmployee", false);
        }
    }

    // ===== List =====
    @GetMapping
    public String danhSach(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "discountLevel", required = false) String discountLevel,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model
    ) {
        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            return "redirect:/login";
        }

        // Sắp xếp: mới tạo lên đầu, tie-break theo id
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.DESC, "thoiGianTao", "id")
        );

        Page<ChienDichGiamGia> pageResult =
                chienDichService.locChienDich(keyword, startDate, endDate, status, discountLevel, pageable);

        model.addAttribute("campaignList", pageResult.getContent());
        model.addAttribute("currentPage", pageResult.getNumber());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalItems", pageResult.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("status", status);
        model.addAttribute("discountLevel", discountLevel);
        model.addAttribute("pageSize", size);

        addUserInfoToModel(model);
        return "WebQuanLy/discount-campaign-list";
    }


    // ===== Create form =====
    @GetMapping("/create")
    public String hienFormTaoMoi(Model model,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        if (!model.containsAttribute("chienDich")) {
            model.addAttribute("chienDich", new ChienDichGiamGia());
        }

        Pageable pageable = PageRequest.of(page, size);
        // CHỈ LẤY SẢN PHẨM CÒN CTSP “RẢNH”
        Page<SanPham> productPage = sanPhamService.getPagedAvailableProducts(pageable);

        model.addAttribute("productPage", productPage);
        model.addAttribute("currentProductPage", productPage.getNumber());
        model.addAttribute("totalProductPages", productPage.getTotalPages());

        addUserInfoToModel(model);
        return "WebQuanLy/discount-campaign-form";
    }


    // ===== Create submit =====
    @PostMapping("/create")
    public String taoMoi(@ModelAttribute("chienDich") ChienDichGiamGia chienDich,
                         @RequestParam("selectedProductDetailIds") String selectedIdsRaw,
                         Model model, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        List<UUID> danhSachChiTietId = parseCsvUuids(selectedIdsRaw);

        String error = validateChienDich(chienDich, danhSachChiTietId, true, null);
        if (error != null) {
            model.addAttribute("errorMessage", error);
            // Giữ lại dữ liệu đã nhập
            model.addAttribute("chienDich", chienDich);

            Pageable pageable = PageRequest.of(0, 10);
            // CHỈ LẤY SẢN PHẨM CÒN CTSP “RẢNH”
            Page<SanPham> productPage = sanPhamService.getPagedAvailableProducts(pageable);

            model.addAttribute("productPage", productPage);
            model.addAttribute("currentProductPage", productPage.getNumber());
            model.addAttribute("totalProductPages", productPage.getTotalPages());
            addUserInfoToModel(model);
            return "WebQuanLy/discount-campaign-form";
        }

        chienDich.setId(null);
        chienDich.setThoiGianTao(LocalDateTime.now());
        try {
            chienDichService.taoMoiChienDichKemChiTiet(chienDich, danhSachChiTietId);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo chiến dịch thành công!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            // Giữ lại dữ liệu đã nhập
            model.addAttribute("chienDich", chienDich);

            Pageable pageable = PageRequest.of(0, 10);
            // CHỈ LẤY SẢN PHẨM CÒN CTSP “RẢNH”
            Page<SanPham> productPage = sanPhamService.getPagedAvailableProducts(pageable);

            model.addAttribute("productPage", productPage);
            model.addAttribute("currentProductPage", productPage.getNumber());
            model.addAttribute("totalProductPages", productPage.getTotalPages());
            addUserInfoToModel(model);
            return "WebQuanLy/discount-campaign-form";
        }
    }

    // ===== View =====
    @GetMapping("/view/{id}")
    public String xemChiTiet(@PathVariable("id") UUID id,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        try {
            ChienDichGiamGia chienDich = chienDichService.timTheoId(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));
            String status = getStatus(chienDich);

            // Lấy đúng CHI TIẾT đã chọn
            List<ChiTietSanPham> chiTietDaChon = chienDichService.layChiTietDaChonTheoChienDich(id);

            // SUY RA danh sách SẢN PHẨM từ chi tiết đã chọn (de-duplicate, sort theo tên)
            List<SanPham> selectedProducts = chiTietDaChon.stream()
                    .map(ChiTietSanPham::getSanPham)
                    .filter(Objects::nonNull)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(SanPham::getId, sp -> sp, (a, b) -> a, LinkedHashMap::new),
                            m -> m.values().stream()
                                    .sorted(Comparator.comparing(sp -> Optional.ofNullable(sp.getTenSanPham()).orElse("")))
                                    .toList()
                    ));

            model.addAttribute("isReadOnly", true);
            model.addAttribute("chienDich", chienDich);
            model.addAttribute("status", status);
            model.addAttribute("selectedProducts", selectedProducts);
            model.addAttribute("selectedDetails", chiTietDaChon);

            addUserInfoToModel(model);
            return "WebQuanLy/discount-campaign-view"; // <-- TÁCH VIEW RIÊNG
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải thông tin chiến dịch: " + e.getMessage());
            return "redirect:/acvstore/chien-dich-giam-gia";
        }
    }

    // ===== Edit form =====
    @GetMapping("/edit/{id}")
    public String hienFormChinhSua(@PathVariable("id") UUID id,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(name = "selectedProductIds", required = false) String selectedProductIdsStr,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        boolean isAdmin = isCurrentUserAdmin();
        boolean isEmployee = isCurrentUserEmployee();

        if (!isAdmin && !isEmployee) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        // Nhân viên luôn chuyển sang trang view
        if (isEmployee) {
            return "redirect:/acvstore/chien-dich-giam-gia/view/" + id;
        }

        try {
            ChienDichGiamGia chienDich = chienDichService.timTheoId(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));

            String status = getStatus(chienDich);
            boolean isReadOnly = !"UPCOMING".equals(status); // chỉ sửa khi UPCOMING

            if (isReadOnly) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Chỉ có thể chỉnh sửa chiến dịch sắp diễn ra.");
                return "redirect:/acvstore/chien-dich-giam-gia/view/" + id;
            }

            // Lấy danh sách CTSP đang gắn với campaign
            List<ChiTietSanPham> chiTietDaChon = chienDichService.layChiTietDaChonTheoChienDich(id);

            // SUY RA danh sách SẢN PHẨM đã được chọn từ các CTSP
            Set<UUID> selectedProductIds = chiTietDaChon.stream()
                    .map(ChiTietSanPham::getSanPham)
                    .filter(Objects::nonNull)
                    .map(SanPham::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // Merge thêm các id SP được truyền lên (nếu có)
            if (selectedProductIdsStr != null && !selectedProductIdsStr.isEmpty()) {
                selectedProductIds.addAll(Arrays.stream(selectedProductIdsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(UUID::fromString)
                        .collect(Collectors.toSet()));
            }

            Pageable pageable = PageRequest.of(page, size);
            // ⚠️ QUAN TRỌNG: chỉ load SP “rảnh” ∪ “đã chọn”
            Page<SanPham> productPage = sanPhamService.getPagedAvailableOrSelectedProducts(pageable, selectedProductIds);

            model.addAttribute("chienDich", chienDich);
            model.addAttribute("status", status);
            model.addAttribute("isReadOnly", false);

            model.addAttribute("selectedDetails", chiTietDaChon);
            model.addAttribute("selectedProductIds", selectedProductIds);
            model.addAttribute("productPage", productPage);
            model.addAttribute("currentProductPage", productPage.getNumber());
            model.addAttribute("totalProductPages", productPage.getTotalPages());

            addUserInfoToModel(model);
            return "WebQuanLy/discount-campaign-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải form chỉnh sửa: " + e.getMessage());
            return "redirect:/acvstore/chien-dich-giam-gia";
        }
    }



    // ===== Update submit =====
    @PostMapping("/update")
    public String capNhatChienDich(@ModelAttribute("chienDich") ChienDichGiamGia chienDich,
                                   @RequestParam("selectedProductDetailIds") String selectedIdsRaw,
                                   Model model, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        try {
            ChienDichGiamGia chienDichCu = chienDichService.timTheoId(chienDich.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));

            String currentStatus = getStatus(chienDichCu);
            if (!"UPCOMING".equals(currentStatus)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Chỉ có thể chỉnh sửa chiến dịch sắp diễn ra. Chiến dịch đang diễn ra hoặc đã kết thúc không thể chỉnh sửa.");
                return "redirect:/acvstore/chien-dich-giam-gia";
            }

            List<UUID> danhSachChiTietId = parseCsvUuids(selectedIdsRaw);
            String error = validateChienDich(chienDich, danhSachChiTietId, false, chienDichCu);
            if (error != null) {
                model.addAttribute("errorMessage", error);

                Pageable pageable = PageRequest.of(0, 10);
                Page<SanPham> productPage = sanPhamService.getPagedProducts(pageable);
                model.addAttribute("productPage", productPage);
                model.addAttribute("currentProductPage", productPage.getNumber());
                model.addAttribute("totalProductPages", productPage.getTotalPages());
                model.addAttribute("selectedDetails", chienDichService.layChiTietDaChonTheoChienDich(chienDich.getId()));
                model.addAttribute("selectedProductIds", danhSachChiTietId.stream()
                        .map(id -> {
                            try {
                                return chienDichService.layChiTietTheoId(id).getSanPham().getId();
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
                model.addAttribute("isReadOnly", false);
                model.addAttribute("viewMode", false);
                model.addAttribute("status", currentStatus);
                addUserInfoToModel(model);
                return "WebQuanLy/discount-campaign-edit";
            }

            // Giữ nguyên thời gian tạo ban đầu, tránh bị ghi đè khi update
            chienDich.setThoiGianTao(chienDichCu.getThoiGianTao());

            chienDichService.capNhatChienDichKemChiTiet(chienDich, danhSachChiTietId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật chiến dịch thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật: " + e.getMessage());
        }
        return "redirect:/acvstore/chien-dich-giam-gia";
    }


    // ===== APIs phụ trợ =====
    @GetMapping("/chi-tiet-san-pham")
    @ResponseBody
    public List<ChiTietSanPham> layChiTietSanPhamTheoSanPham(@RequestParam("idSanPham") UUID idSanPham) {
        return chienDichService.layChiTietTheoSanPham(idSanPham);
    }

    @GetMapping("/chi-tiet-da-chon")
    @ResponseBody
    public List<ChiTietSanPham> layChiTietDaChon(@RequestParam("idChienDich") UUID idChienDich) {
        return chienDichService.layChiTietDaChonTheoChienDich(idChienDich);
    }

    @GetMapping("/multiple-product-details")
    @ResponseBody
    public List<ChiTietSanPham> getMultipleProductDetails(
            @RequestParam("productIds") List<UUID> productIds,
            @RequestParam(name = "currentCampaignId", required = false) UUID currentCampaignId
    ) {
        if (productIds == null || productIds.isEmpty()) return List.of();

        // Khi đang SỬA: lấy CTSP "rảnh" HOẶC đang thuộc chính campaign này
        if (currentCampaignId != null) {
            return chienDichService.layChiTietConTrongTheoSanPham(productIds, currentCampaignId);
        }

        // Khi TẠO: chỉ trả CTSP rảnh (method cũ của bạn đã lọc theo nghiệp vụ tạo mới)
        return productIds.stream()
                .flatMap(id -> chienDichService.layChiTietTheoSanPham(id).stream())
                .toList();
    }


    @PostMapping("/delete/{id}")
    public String xoaChienDich(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

        try {
            chienDichService.xoaChienDich(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa chiến dịch thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/acvstore/chien-dich-giam-gia";
    }

    @GetMapping("/search-products")
    @ResponseBody
    public Page<SanPham> timKiemSanPham(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "selectedProductIds", required = false) List<UUID> selectedProductIds
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("tenSanPham").ascending());

        // Có danh sách SP đang được chọn -> dùng phiên bản UNION (rảnh ∪ đã chọn)
        if (selectedProductIds != null && !selectedProductIds.isEmpty()) {
            if (keyword.isBlank()) {
                return sanPhamService.getPagedAvailableOrSelectedProducts(pageable, selectedProductIds);
            }
            return sanPhamService.searchAvailableOrSelectedByTenOrMa(keyword, pageable, selectedProductIds);
        }

        // Không có SP đã chọn -> như tạo mới: chỉ SP rảnh
        if (keyword.isBlank()) {
            return sanPhamService.getPagedAvailableProducts(pageable);
        }
        return sanPhamService.searchAvailableByTenOrMa(keyword, pageable);
    }



    // ===== Helpers =====
    private String getStatus(ChienDichGiamGia chienDich) {
        LocalDateTime now = LocalDateTime.now();
        if (chienDich.getNgayBatDau().isAfter(now)) return "UPCOMING";
        else if (chienDich.getNgayKetThuc().isBefore(now)) return "ENDED";
        return "ONGOING";
    }

    private List<UUID> parseCsvUuids(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    private String validateChienDich(ChienDichGiamGia chienDich, List<UUID> chiTietIds, boolean isCreate, ChienDichGiamGia chienDichCu) {
        if (chienDich.getTen() == null || chienDich.getTen().trim().isEmpty() || chienDich.getTen().length() > 100) {
            return "Tên chiến dịch không được để trống và tối đa 100 ký tự.";
        }

        boolean tenChanged = chienDichCu == null || !chienDich.getTen().trim().equalsIgnoreCase(chienDichCu.getTen().trim());
        if (tenChanged && chienDichService.kiemTraTenTonTai(chienDich.getTen().trim())) {
            return "Tên chiến dịch đã tồn tại.";
        }

        if (chienDich.getMa() == null || chienDich.getMa().trim().isEmpty() || chienDich.getMa().length() > 50) {
            return "Mã chiến dịch không được để trống và tối đa 50 ký tự.";
        }

        if (!chienDich.getMa().matches("^[A-Za-z0-9_-]+$")) {
            return "Mã chiến dịch chỉ được chứa chữ cái, số, gạch dưới hoặc gạch ngang.";
        }

        boolean maChanged = chienDichCu == null || !chienDich.getMa().trim().equalsIgnoreCase(chienDichCu.getMa().trim());
        if (isCreate || maChanged) {
            if (chienDichService.kiemTraMaTonTai(chienDich.getMa().trim())) {
                return "Mã chiến dịch đã tồn tại.";
            }
        }

        if (chienDich.getPhanTramGiam() == null) {
            return "Phần trăm giảm không được để trống.";
        }

        try {
            if (chienDich.getPhanTramGiam().compareTo(BigDecimal.ZERO) <= 0) {
                return "Phần trăm giảm phải lớn hơn 0%.";
            }
            if (chienDich.getPhanTramGiam().compareTo(BigDecimal.valueOf(100)) > 0) {
                return "Phần trăm giảm không được vượt quá 100%.";
            }
        } catch (NumberFormatException e) {
            return "Phần trăm giảm phải là số hợp lệ, không được nhập chữ.";
        }

        if (chienDich.getNgayBatDau() == null || chienDich.getNgayKetThuc() == null) {
            return "Ngày bắt đầu và ngày kết thúc không được để trống.";
        }

        if (chienDich.getNgayKetThuc().isBefore(chienDich.getNgayBatDau())) {
            return "Ngày kết thúc không được trước ngày bắt đầu.";
        }

        if (isCreate && chienDich.getNgayBatDau().isBefore(LocalDateTime.now())) {
            return "Ngày bắt đầu phải là hiện tại hoặc tương lai.";
        }

        if (chiTietIds == null || chiTietIds.isEmpty()) {
            return "Vui lòng chọn ít nhất một chi tiết sản phẩm.";
        }

        return null;
    }
}
