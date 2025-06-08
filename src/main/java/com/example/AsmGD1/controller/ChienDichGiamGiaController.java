package com.example.AsmGD1.controller;

import com.example.AsmGD1.Service.ChienDichGiamGiaService;
import com.example.AsmGD1.Service.SanPhamService;
import com.example.AsmGD1.entity.ChiTietSanPhamChienDichGiamGia;
import com.example.AsmGD1.entity.ChienDichGiamGia;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.SanPham;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/chien-dich-giam-gia")
public class ChienDichGiamGiaController {

    @Autowired
    private ChienDichGiamGiaService chienDichService;

    @Autowired
    private SanPhamService sanPhamService;



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

        return "admin/discount-campaign-list";
    }

    @GetMapping("/create")
    public String hienFormTaoMoi(Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        model.addAttribute("chienDich", new ChienDichGiamGia());
        Pageable pageable = PageRequest.of(page, size);
        Page<SanPham> productPage = sanPhamService.getPagedProducts(pageable);
        model.addAttribute("productPage", productPage);
        model.addAttribute("currentProductPage", productPage.getNumber());
        model.addAttribute("totalProductPages", productPage.getTotalPages());
        return "admin/discount-campaign-form";
    }

    @PostMapping("/create")
    public String taoMoi(@ModelAttribute("chienDich") ChienDichGiamGia chienDich,
                         @RequestParam("selectedProductDetailIds") List<UUID> danhSachChiTietId) {
        chienDich.setId(null);
        chienDich.setThoiGianTao(LocalDateTime.now());
        chienDichService.taoMoiChienDichKemChiTiet(chienDich, danhSachChiTietId);
        return "redirect:/admin/chien-dich-giam-gia";
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

    @GetMapping("/edit/{id}")
    public String hienFormChinhSua(@PathVariable("id") UUID id,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(name = "selectedProductIds", required = false) String selectedProductIdsStr,
                                   Model model) {
        ChienDichGiamGia chienDich = chienDichService.timTheoId(id).orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));

        List<ChiTietSanPhamChienDichGiamGia> lienKets = chienDichService.layLienKetChiTietTheoChienDich(id).stream()
                .filter(lk -> lk.getChiTietSanPham() != null
                        && lk.getChiTietSanPham().getSanPham() != null
                        && lk.getChiTietSanPham().getMauSac() != null
                        && lk.getChiTietSanPham().getKichCo() != null)
                .toList();

        List<ChiTietSanPham> chiTietDaChon = lienKets.stream().map(ChiTietSanPhamChienDichGiamGia::getChiTietSanPham).toList();
        Set<UUID> selectedProductIds = chiTietDaChon.stream().map(ct -> ct.getSanPham().getId()).collect(Collectors.toSet());

        if (selectedProductIdsStr != null && !selectedProductIdsStr.isEmpty()) {
            selectedProductIds.addAll(Arrays.stream(selectedProductIdsStr.split(",")).map(UUID::fromString).collect(Collectors.toSet()));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<SanPham> productPage = sanPhamService.getPagedProducts(pageable);

        model.addAttribute("chienDich", chienDich);
        model.addAttribute("selectedDetails", lienKets);
        model.addAttribute("selectedProductIds", selectedProductIds);
        model.addAttribute("productPage", productPage);
        model.addAttribute("currentProductPage", productPage.getNumber());
        model.addAttribute("totalProductPages", productPage.getTotalPages());

        return "admin/discount-campaign-edit";
    }

    @PostMapping("/update")
    public String capNhatChienDich(@ModelAttribute("chienDich") ChienDichGiamGia chienDich,
                                   @RequestParam("selectedProductDetailIds") List<UUID> danhSachChiTietId) {
        chienDichService.capNhatChienDichKemChiTiet(chienDich, danhSachChiTietId);
        return "redirect:/admin/chien-dich-giam-gia";
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
    public String xoaChienDich(@PathVariable("id") UUID id) {
        chienDichService.xoaChienDich(id);
        return "redirect:/admin/chien-dich-giam-gia";
    }
    @GetMapping("/search-products")
    @ResponseBody
    public Page<SanPham> timKiemSanPham(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("tenSanPham").ascending());

        if (keyword.isBlank()) {
            return sanPhamService.getPagedProducts(pageable);
        }

        return sanPhamService.searchByTenOrMa(keyword, pageable);
    }

}
