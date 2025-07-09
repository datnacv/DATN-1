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
import java.time.LocalDate;
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

    @GetMapping
    public String danhSach(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "discountLevel", required = false) String discountLevel,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("ngayBatDau").descending());
        Page<ChienDichGiamGia> pageResult = chienDichService.locChienDich(keyword, startDate, endDate, status, discountLevel, pageable);

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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
        } else {
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        }

        return "WebQuanLy/discount-campaign-list";
    }

    @GetMapping("/create")
    public String hienFormTaoMoi(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        model.addAttribute("chienDich", new ChienDichGiamGia());
        Pageable pageable = PageRequest.of(page, size);
        Page<SanPham> productPage = sanPhamService.getPagedProducts(pageable);
        model.addAttribute("productPage", productPage);
        model.addAttribute("currentProductPage", productPage.getNumber());
        model.addAttribute("totalProductPages", productPage.getTotalPages());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
        }

        return "WebQuanLy/discount-campaign-form";
    }

    @PostMapping("/create")
    public String taoMoi(@ModelAttribute("chienDich") ChienDichGiamGia chienDich,
                         @RequestParam("selectedProductDetailIds") List<UUID> danhSachChiTietId,
                         Model model, RedirectAttributes redirectAttributes) {

        String error = validateChienDich(chienDich, danhSachChiTietId, true, null);
        if (error != null) {
            model.addAttribute("errorMessage", error);
            Pageable pageable = PageRequest.of(0, 10);
            Page<SanPham> productPage = sanPhamService.getPagedProducts(pageable);
            model.addAttribute("productPage", productPage);
            model.addAttribute("currentProductPage", productPage.getNumber());
            model.addAttribute("totalProductPages", productPage.getTotalPages());
            return "WebQuanLy/discount-campaign-form";
        }

        chienDich.setId(null);
        chienDich.setThoiGianTao(LocalDateTime.now());
        try {
            chienDichService.taoMoiChienDichKemChiTiet(chienDich, danhSachChiTietId);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo chiến dịch thành công!");
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            Pageable pageable = PageRequest.of(0, 10);
            Page<SanPham> productPage = sanPhamService.getPagedProducts(pageable);
            model.addAttribute("productPage", productPage);
            model.addAttribute("currentProductPage", productPage.getNumber());
            model.addAttribute("totalProductPages", productPage.getTotalPages());
            return "WebQuanLy/discount-campaign-form";
        }
        return "redirect:/acvstore/chien-dich-giam-gia";
    }

    @GetMapping("/edit/{id}")
    public String hienFormChinhSua(@PathVariable("id") UUID id,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(name = "selectedProductIds", required = false) String selectedProductIdsStr,
                                   Model model) {
        ChienDichGiamGia chienDich = chienDichService.timTheoId(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));

        String status = getStatus(chienDich);
        model.addAttribute("isReadOnly", "ONGOING".equals(status)); // Set read-only mode for ONGOING campaigns
        model.addAttribute("chienDich", chienDich);

        List<ChiTietSanPham> chiTietDaChon = chienDichService.layChiTietDaChonTheoChienDich(id);
        Set<UUID> selectedProductIds = chiTietDaChon.stream()
                .filter(ct -> ct.getSanPham() != null)
                .map(ct -> ct.getSanPham().getId())
                .collect(Collectors.toSet());

        if (selectedProductIdsStr != null && !selectedProductIdsStr.isEmpty()) {
            selectedProductIds.addAll(Arrays.stream(selectedProductIdsStr.split(",")).map(UUID::fromString).collect(Collectors.toSet()));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<SanPham> productPage = sanPhamService.getPagedProducts(pageable);

        model.addAttribute("selectedDetails", chiTietDaChon);
        model.addAttribute("selectedProductIds", selectedProductIds);
        model.addAttribute("productPage", productPage);
        model.addAttribute("currentProductPage", productPage.getNumber());
        model.addAttribute("totalProductPages", productPage.getTotalPages());

        return "WebQuanLy/discount-campaign-edit";
    }

    @PostMapping("/update")
    public String capNhatChienDich(@ModelAttribute("chienDich") ChienDichGiamGia chienDich,
                                   @RequestParam("selectedProductDetailIds") List<UUID> danhSachChiTietId,
                                   Model model, RedirectAttributes redirectAttributes) {

        ChienDichGiamGia chienDichCu = chienDichService.timTheoId(chienDich.getId()).orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));

        if (getStatus(chienDichCu).equals("ONGOING")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật chiến dịch đang diễn ra.");
            return "redirect:/acvstore/chien-dich-giam-gia";
        }

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
                    .map(id -> chienDichService.layChiTietTheoId(id).getSanPham().getId())
                    .collect(Collectors.toSet()));
            model.addAttribute("isReadOnly", false);
            return "WebQuanLy/discount-campaign-edit";
        }

        try {
            chienDichService.capNhatChienDichKemChiTiet(chienDich, danhSachChiTietId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật chiến dịch thành công!");
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            Pageable pageable = PageRequest.of(0, 10);
            Page<SanPham> productPage = sanPhamService.getPagedProducts(pageable);
            model.addAttribute("productPage", productPage);
            model.addAttribute("currentProductPage", productPage.getNumber());
            model.addAttribute("totalProductPages", productPage.getTotalPages());
            model.addAttribute("selectedDetails", chienDichService.layChiTietDaChonTheoChienDich(chienDich.getId()));
            model.addAttribute("selectedProductIds", danhSachChiTietId.stream()
                    .map(id -> chienDichService.layChiTietTheoId(id).getSanPham().getId())
                    .collect(Collectors.toSet()));
            model.addAttribute("isReadOnly", false);
            return "WebQuanLy/discount-campaign-edit";
        }
        return "redirect:/acvstore/chien-dich-giam-gia";
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

        if (isCreate && chienDich.getNgayBatDau().isBefore(LocalDate.now())) {
            return "Ngày bắt đầu phải là hiện tại hoặc tương lai.";
        }

        if (chiTietIds == null || chiTietIds.isEmpty()) {
            return "Vui lòng chọn ít nhất một chi tiết sản phẩm.";
        }

        return null;
    }

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
    public List<ChiTietSanPham> getMultipleProductDetails(@RequestParam("productIds") List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return productIds.stream()
                .flatMap(id -> chienDichService.layChiTietTheoSanPham(id).stream())
                .toList();
    }

    @GetMapping("/delete/{id}")
    public String xoaChienDich(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
        try {
            chienDichService.xoaChienDich(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa chiến dịch thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/acvstore/chien-dich-giam-gia";
    }

    @GetMapping("/search-products")
    @ResponseBody
    public Page<SanPham> timKiemSanPham(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("tenSanPham").ascending());
        if (keyword.isBlank()) {
            return sanPhamService.getPagedProducts(pageable);
        }
        return sanPhamService.searchByTenOrMa(keyword, pageable);
    }

    private String getStatus(ChienDichGiamGia chienDich) {
        LocalDate today = LocalDate.now();
        if (chienDich.getNgayBatDau().isAfter(today)) {
            return "UPCOMING";
        } else if (chienDich.getNgayKetThuc().isBefore(today)) {
            return "ENDED";
        } else {
            return "ONGOING";
        }
    }
}
