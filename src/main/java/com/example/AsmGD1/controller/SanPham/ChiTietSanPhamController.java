package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamBatchDto;
import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamUpdateDto;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangSanPhamRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/chi-tiet-san-pham")
public class ChiTietSanPhamController {
    private static final Logger logger = LoggerFactory.getLogger(ChiTietSanPhamController.class);

    @Autowired private ChiTietSanPhamService chiTietSanPhamService;
    @Autowired private SanPhamService sanPhamService;
    @Autowired private MauSacService mauSacService;
    @Autowired private KichCoService kichCoService;
    @Autowired private ChatLieuService chatLieuService;
    @Autowired private XuatXuService xuatXuService;
    @Autowired private TayAoService tayAoService;
    @Autowired private CoAoService coAoService;
    @Autowired private KieuDangService kieuDangService;
    @Autowired private ThuongHieuService thuongHieuService;
    @Autowired private DanhMucService danhMucService;
    @Autowired private KhachHangSanPhamRepository khachHangSanPhamRepository; // Thêm repository
    @Autowired private NguoiDungService nguoiDungService;

    @GetMapping
    public String xemTatCa(Model model,
                           @RequestParam(value = "productId", required = false) UUID productId,
                           @RequestParam(value = "colorId", required = false) UUID colorId,
                           @RequestParam(value = "sizeId", required = false) UUID sizeId,
                           @RequestParam(value = "originId", required = false) UUID originId,
                           @RequestParam(value = "materialId", required = false) UUID materialId,
                           @RequestParam(value = "styleId", required = false) UUID styleId,
                           @RequestParam(value = "sleeveId", required = false) UUID sleeveId,
                           @RequestParam(value = "collarId", required = false) UUID collarId,
                           @RequestParam(value = "brandId", required = false) UUID brandId,
                           @RequestParam(value = "gender", required = false) String gender,
                           @RequestParam(value = "status", required = false) Boolean status) {
        try {
            model.addAttribute("sanPham", new SanPham());
            model.addAttribute("sanPhams", sanPhamService.findAll());
            model.addAttribute("mauSacs", chiTietSanPhamService.findColorsByProductId(productId));
            model.addAttribute("kichCos", chiTietSanPhamService.findSizesByProductId(productId));
            model.addAttribute("xuatXus", xuatXuService.getAllXuatXu());
            model.addAttribute("chatLieus", chatLieuService.getAllChatLieu());
            model.addAttribute("tayAos", tayAoService.getAllTayAo());
            model.addAttribute("coAos", coAoService.getAllCoAo());
            model.addAttribute("kieuDangs", kieuDangService.getAllKieuDang());
            model.addAttribute("thuongHieus", thuongHieuService.getAllThuongHieu());
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));

            if (productId != null) {
                SanPham sanPhamDaChon = sanPhamService.findById(productId);
                if (sanPhamDaChon != null) {
                    model.addAttribute("selectedProductId", productId);
                    model.addAttribute("sanPhamDaChon", sanPhamDaChon);
                    List<ChiTietSanPham> chiTietList = chiTietSanPhamService.findByFilters(
                            productId, colorId, sizeId, originId, materialId, styleId, sleeveId, collarId, brandId, gender, status);
                    chiTietList.forEach(pd -> logger.info("Product ID: {}, Status: {}", pd.getId(), pd.getTrangThai()));
                    model.addAttribute("chiTietSanPhamList", chiTietList);
                    model.addAttribute("selectedColorId", colorId);
                    model.addAttribute("selectedSizeId", sizeId);
                    model.addAttribute("selectedOriginId", originId);
                    model.addAttribute("selectedMaterialId", materialId);
                    model.addAttribute("selectedStyleId", styleId);
                    model.addAttribute("selectedSleeveId", sleeveId);
                    model.addAttribute("selectedCollarId", collarId);
                    model.addAttribute("selectedBrandId", brandId);
                    model.addAttribute("selectedGender", gender);
                    model.addAttribute("selectedStatus", status);
                } else {
                    model.addAttribute("chiTietSanPhamList", chiTietSanPhamService.findAll());
                }
            } else {
                model.addAttribute("chiTietSanPhamList", chiTietSanPhamService.findAll());
            }
            return "/WebQuanLy/chi-tiet-san-pham-form";
        } catch (Exception e) {
            logger.error("Lỗi khi tải trang chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi tải trang: " + e.getMessage());
            return "/WebQuanLy/error";
        }
    }

    @GetMapping("/add")
    public String hienThiFormThem(Model model, @RequestParam(value = "productId", required = false) UUID productId) {
        try {
            model.addAttribute("sanPham", new SanPham());
            model.addAttribute("sanPhams", sanPhamService.findAll());
            model.addAttribute("mauSacs", mauSacService.getAllMauSac());
            model.addAttribute("kichCos", kichCoService.getAllKichCo());
            model.addAttribute("chatLieus", chatLieuService.getAllChatLieu());
            model.addAttribute("xuatXus", xuatXuService.getAllXuatXu());
            model.addAttribute("tayAos", tayAoService.getAllTayAo());
            model.addAttribute("coAos", coAoService.getAllCoAo());
            model.addAttribute("kieuDangs", kieuDangService.getAllKieuDang());
            model.addAttribute("thuongHieus", thuongHieuService.getAllThuongHieu());
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
            logger.info("Số lượng danh mục: {}", danhMucList.size());
            model.addAttribute("danhMucList", danhMucList);
            if (productId != null) {
                SanPham sanPhamDaChon = sanPhamService.findById(productId);
                model.addAttribute("selectedProductId", productId);
                model.addAttribute("sanPhamDaChon", sanPhamDaChon);
            }
            return "/WebQuanLy/add-chi-tiet-san-pham-form";
        } catch (Exception e) {
            logger.error("Lỗi khi tải trang thêm chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi tải trang: " + e.getMessage());
            return "/WebQuanLy/error";
        }
    }

    @PostMapping("/save")
    public String luuChiTietSanPhamDon(@ModelAttribute ChiTietSanPhamUpdateDto dto,
                                       @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                       Model model) {
        try {
            chiTietSanPhamService.saveSingleChiTietSanPham(dto, imageFiles);
            return "redirect:/acvstore/chi-tiet-san-pham?productId=" + dto.getProductId() + "&success=Thêm thành công";
        } catch (Exception e) {
            logger.error("Lỗi khi lưu chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi lưu chi tiết sản phẩm: " + e.getMessage());
            return "/WebQuanLy/add-chi-tiet-san-pham-form";
        }
    }

    @PostMapping("/save-batch")
    public String luuChiTietSanPhamBatch(@ModelAttribute ChiTietSanPhamBatchDto batchDto, Model model) {
        try {
            chiTietSanPhamService.saveChiTietSanPhamVariationsDto(batchDto);
            return "redirect:/acvstore/chi-tiet-san-pham?productId=" + batchDto.getProductId() + "&success=Thêm thành công";
        } catch (Exception e) {
            logger.error("Lỗi khi lưu batch chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi lưu chi tiết sản phẩm: " + e.getMessage());
            return "/WebQuanLy/add-chi-tiet-san-pham-form";
        }
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> layChiTietSanPham(@PathVariable UUID id) {
        try {
            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(id);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy chi tiết sản phẩm"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", chiTiet.getId());
            response.put("productId", chiTiet.getSanPham().getId());
            response.put("colorId", chiTiet.getMauSac().getId());
            response.put("sizeId", chiTiet.getKichCo().getId());
            response.put("originId", chiTiet.getXuatXu().getId());
            response.put("materialId", chiTiet.getChatLieu().getId());
            response.put("styleId", chiTiet.getKieuDang().getId());
            response.put("sleeveId", chiTiet.getTayAo().getId());
            response.put("collarId", chiTiet.getCoAo().getId());
            response.put("brandId", chiTiet.getThuongHieu().getId());
            response.put("price", chiTiet.getGia());
            response.put("stockQuantity", chiTiet.getSoLuongTonKho());
            response.put("gender", chiTiet.getGioiTinh());
            response.put("status", chiTiet.getTrangThai());
            response.put("images", chiTiet.getHinhAnhSanPhams().stream()
                    .map(img -> Map.of("id", img.getId(), "imageUrl", img.getUrlHinhAnh()))
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy chi tiết sản phẩm ID {}: ", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi tải chi tiết sản phẩm: " + e.getMessage()));
        }
    }

    @PostMapping("/update/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> capNhatChiTietSanPham(@PathVariable UUID id,
                                                                     @ModelAttribute ChiTietSanPhamUpdateDto updateDto,
                                                                     @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles) {
        try {
            updateDto.setId(id);
            logger.info("Received updateDto with status: {}", updateDto.getStatus());
            chiTietSanPhamService.updateChiTietSanPham(updateDto, imageFiles);
            return ResponseEntity.ok(Map.of("message", "Cập nhật chi tiết sản phẩm thành công"));
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật chi tiết sản phẩm ID {}: ", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi cập nhật chi tiết sản phẩm: " + e.getMessage()));
        }
    }

    @PostMapping("/delete-image/{imageId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaAnh(@PathVariable UUID imageId) {
        try {
            chiTietSanPhamService.deleteImage(imageId);
            return ResponseEntity.ok(Map.of("message", "Xóa ảnh thành công"));
        } catch (Exception e) {
            logger.error("Lỗi khi xóa ảnh ID {}: ", imageId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa ảnh: " + e.getMessage()));
        }
    }

    @PostMapping("/save-auto-product")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveQuickAddProduct(@ModelAttribute SanPham sanPham, @RequestParam(value = "danhMuc.id", required = false) UUID danhMucId) {
        Map<String, Object> response = new HashMap<>();
        try {
            SanPham newSanPham = new SanPham();
            newSanPham.setMaSanPham(sanPham.getMaSanPham());
            newSanPham.setTenSanPham(sanPham.getTenSanPham());
            newSanPham.setMoTa(sanPham.getMoTa());
            newSanPham.setUrlHinhAnh(sanPham.getUrlHinhAnh());
            newSanPham.setTrangThai(true);
            newSanPham.setThoiGianTao(LocalDateTime.now());

            if (danhMucId != null) {
                DanhMuc danhMuc = danhMucService.getDanhMucById(danhMucId);
                if (danhMuc != null) {
                    newSanPham.setDanhMuc(danhMuc);
                } else {
                    response.put("success", false);
                    response.put("message", "Danh mục không tồn tại!");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                response.put("success", false);
                response.put("message", "Vui lòng chọn danh mục!");
                return ResponseEntity.badRequest().body(response);
            }

            sanPhamService.save(newSanPham);
            response.put("success", true);
            response.put("id", newSanPham.getId());
            response.put("tenSanPham", newSanPham.getTenSanPham());
            response.put("message", "Thêm sản phẩm thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi thêm nhanh sản phẩm: ", e);
            response.put("success", false);
            response.put("message", "Lỗi khi thêm sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Thêm endpoint để lấy chiTietSanPhamId cho phía client
    @GetMapping("/api")
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
}