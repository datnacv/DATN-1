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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
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
    @Autowired private KhachHangSanPhamRepository khachHangSanPhamRepository;
    @Autowired private NguoiDungService nguoiDungService;

    // Helper method to check if current user is admin
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            return "admin".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    // Helper method to add user info to model
    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
            model.addAttribute("isAdmin", "admin".equalsIgnoreCase(user.getVaiTro()));
        } else {
            // Fallback for testing
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("isAdmin", false);
        }
    }

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
            addUserInfoToModel(model);

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

            if (productId != null) {
                SanPham sanPhamDaChon = sanPhamService.findById(productId);
                if (sanPhamDaChon != null) {
                    model.addAttribute("selectedProductId", productId);
                    model.addAttribute("sanPhamDaChon", sanPhamDaChon);
                    List<ChiTietSanPham> chiTietList = chiTietSanPhamService.findByFilters(
                            productId, colorId, sizeId, originId, materialId, styleId, sleeveId, collarId, brandId, gender, status);
                    for (ChiTietSanPham pd : chiTietList) {
                        List<HinhAnhSanPham> images = chiTietSanPhamService.findHinhAnhSanPhamByChiTietSanPhamIdOrdered(pd.getId());
                        pd.setHinhAnhSanPhams(images);
                        logger.info("Tải {} ảnh cho chi tiết sản phẩm ID: {}", images.size(), pd.getId());
                    }
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
                    List<ChiTietSanPham> allDetails = chiTietSanPhamService.findAll();
                    for (ChiTietSanPham pd : allDetails) {
                        List<HinhAnhSanPham> images = chiTietSanPhamService.findHinhAnhSanPhamByChiTietSanPhamIdOrdered(pd.getId());
                        pd.setHinhAnhSanPhams(images);
                        logger.info("Tải {} ảnh cho chi tiết sản phẩm ID: {}", images.size(), pd.getId());
                    }
                    model.addAttribute("chiTietSanPhamList", allDetails);
                }
            } else {
                List<ChiTietSanPham> allDetails = chiTietSanPhamService.findAll();
                for (ChiTietSanPham pd : allDetails) {
                    List<HinhAnhSanPham> images = chiTietSanPhamService.findHinhAnhSanPhamByChiTietSanPhamIdOrdered(pd.getId());
                    pd.setHinhAnhSanPhams(images);
                    logger.info("Tải {} ảnh cho chi tiết sản phẩm ID: {}", images.size(), pd.getId());
                }
                model.addAttribute("chiTietSanPhamList", allDetails);
            }

            return "WebQuanLy/chi-tiet-san-pham-form";
        } catch (Exception e) {
            logger.error("Lỗi khi tải trang chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi tải trang: " + e.getMessage());
            return "/WebQuanLy/error";
        }
    }

    @GetMapping("/add")
    public String hienThiFormThem(Model model, @RequestParam(value = "productId", required = false) UUID productId) {
        try {
            // Check admin permission
            if (!isCurrentUserAdmin()) {
                return "redirect:/acvstore/chi-tiet-san-pham?error=Bạn không có quyền truy cập chức năng này";
            }

            // Add user info and role check
            addUserInfoToModel(model);

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

            List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
            logger.info("Số lượng danh mục: {}", danhMucList.size());
            model.addAttribute("danhMucList", danhMucList);

            if (productId != null) {
                SanPham sanPhamDaChon = sanPhamService.findById(productId);
                model.addAttribute("selectedProductId", productId);
                model.addAttribute("sanPhamDaChon", sanPhamDaChon);
            }

            return "WebQuanLy/add-chi-tiet-san-pham-form";
        } catch (Exception e) {
            logger.error("Lỗi khi tải trang thêm chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi tải trang: " + e.getMessage());
            return "/WebQuanLy/error";
        }
    }

    @PostMapping("/update-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStatus(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!isCurrentUserAdmin()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện chức năng này!");
                return ResponseEntity.badRequest().body(response);
            }

            UUID id = UUID.fromString((String) payload.get("id"));
            Boolean trangThai = (Boolean) payload.get("trangThai");

            ChiTietSanPham chiTietSanPham = chiTietSanPhamService.findById(id);
            if (chiTietSanPham == null) {
                response.put("success", false);
                response.put("message", "Chi tiết sản phẩm không tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            chiTietSanPham.setTrangThai(trangThai != null ? trangThai : false);
            chiTietSanPhamService.save(chiTietSanPham);

            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/save")
    public String luuChiTietSanPhamDon(@ModelAttribute ChiTietSanPhamUpdateDto dto,
                                       @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                       Model model) {
        try {
            // Check admin permission
            if (!isCurrentUserAdmin()) {
                return "redirect:/acvstore/chi-tiet-san-pham?error=Bạn không có quyền thực hiện chức năng này";
            }

            chiTietSanPhamService.saveSingleChiTietSanPham(dto, imageFiles);
            return "redirect:/acvstore/chi-tiet-san-pham?productId=" + dto.getProductId() + "&success=Thêm thành công";
        } catch (Exception e) {
            logger.error("Lỗi khi lưu chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi lưu chi tiết sản phẩm: " + e.getMessage());
            return "WebQuanLy/add-chi-tiet-san-pham-form";
        }
    }

    @PostMapping("/save-batch")
    public String luuChiTietSanPhamBatch(@ModelAttribute ChiTietSanPhamBatchDto batchDto, Model model) {
        try {
            // Check admin permission
            if (!isCurrentUserAdmin()) {
                return "redirect:/acvstore/chi-tiet-san-pham?error=Bạn không có quyền thực hiện chức năng này";
            }

            chiTietSanPhamService.saveChiTietSanPhamVariationsDto(batchDto);
            return "redirect:/acvstore/chi-tiet-san-pham?productId=" + batchDto.getProductId() + "&success=Thêm thành công";
        } catch (Exception e) {
            logger.error("Lỗi khi lưu batch chi tiết sản phẩm: ", e);
            model.addAttribute("error", "Lỗi khi lưu chi tiết sản phẩm: " + e.getMessage());
            return "WebQuanLy/add-chi-tiet-san-pham-form";
        }
    }

    @GetMapping("/edit/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> layChiTietSanPham(@PathVariable UUID id) {
        try {
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn không có quyền truy cập chức năng này"));
            }

            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(id);
            if (chiTiet == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy chi tiết sản phẩm"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", chiTiet.getId());
            response.put("productId", chiTiet.getSanPham() != null ? chiTiet.getSanPham().getId() : null);
            response.put("colorId", chiTiet.getMauSac() != null ? chiTiet.getMauSac().getId() : null);
            response.put("sizeId", chiTiet.getKichCo() != null ? chiTiet.getKichCo().getId() : null);
            response.put("originId", chiTiet.getXuatXu() != null ? chiTiet.getXuatXu().getId() : null);
            response.put("materialId", chiTiet.getChatLieu() != null ? chiTiet.getChatLieu().getId() : null);
            response.put("styleId", chiTiet.getKieuDang() != null ? chiTiet.getKieuDang().getId() : null);
            response.put("sleeveId", chiTiet.getTayAo() != null ? chiTiet.getTayAo().getId() : null);
            response.put("collarId", chiTiet.getCoAo() != null ? chiTiet.getCoAo().getId() : null);
            response.put("brandId", chiTiet.getThuongHieu() != null ? chiTiet.getThuongHieu().getId() : null);
            response.put("price", chiTiet.getGia());
            response.put("stockQuantity", chiTiet.getSoLuongTonKho());
            response.put("gender", chiTiet.getGioiTinh());
            response.put("status", chiTiet.getTrangThai());

            // Xử lý danh sách ảnh, kiểm tra null và sắp xếp
            List<Map<String, Object>> images = new ArrayList<>();
            if (chiTiet.getHinhAnhSanPhams() != null) {
                images = chiTiet.getHinhAnhSanPhams().stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(h -> h.getThuTu() != null ? h.getThuTu() : Integer.MAX_VALUE))
                        .map(img -> {
                            Map<String, Object> imageMap = new HashMap<>();
                            imageMap.put("id", img.getId());
                            imageMap.put("imageUrl", img.getUrlHinhAnh());
                            imageMap.put("thuTu", img.getThuTu() != null ? img.getThuTu() : 0);
                            return imageMap;
                        })
                        .collect(Collectors.toList());
            }
            response.put("images", images);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy chi tiết sản phẩm ID {}: ", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi tải chi tiết sản phẩm: " + e.getMessage()));
        }
    }

    @PostMapping("/update/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> capNhatChiTietSanPham(
            @PathVariable UUID id,
            @ModelAttribute ChiTietSanPhamUpdateDto updateDto,
            @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
            @RequestParam(value = "deletedImageIds", required = false) String deletedImageIdsStr) {
        try {
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn không có quyền thực hiện chức năng này"));
            }

            updateDto.setId(id);

            // 👇 Chuyển chuỗi về danh sách UUID
            List<UUID> deletedIds = new ArrayList<>();
            if (deletedImageIdsStr != null && !deletedImageIdsStr.isBlank()) {
                deletedIds = Arrays.stream(deletedImageIdsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
            }

            chiTietSanPhamService.updateChiTietSanPham(updateDto, imageFiles, deletedIds);
            return ResponseEntity.ok(Map.of(
                    "message", "Cập nhật chi tiết sản phẩm thành công",
                    "productId", updateDto.getProductId()
            ));
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật chi tiết sản phẩm ID {}: ", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi cập nhật chi tiết sản phẩm: " + e.getMessage()));
        }
    }


    @PostMapping("/delete-image/{imageId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaAnh(@PathVariable UUID imageId) {
        try {
            // Check admin permission
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn không có quyền thực hiện chức năng này"));
            }

            chiTietSanPhamService.deleteImage(imageId);
            return ResponseEntity.ok(Map.of("message", "Xóa ảnh thành công"));
        } catch (Exception e) {
            logger.error("Lỗi khi xóa ảnh ID {}: ", imageId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi xóa ảnh: " + e.getMessage()));
        }
    }

    @PostMapping("/save-auto-product")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveQuickAddProduct(
            @ModelAttribute SanPham sanPham,
            @RequestParam(value = "danhMuc.id", required = false) UUID danhMucId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Check admin permission
            if (!isCurrentUserAdmin()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện chức năng này!");
                return ResponseEntity.badRequest().body(response);
            }

            SanPham newSanPham = new SanPham();
            newSanPham.setMaSanPham(sanPham.getMaSanPham());
            newSanPham.setTenSanPham(sanPham.getTenSanPham());
            newSanPham.setMoTa(sanPham.getMoTa());
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

            // Lưu trữ ảnh cục bộ
            String UPLOAD_DIR = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "C:/DATN/uploads/san_pham/"
                    : System.getProperty("user.home") + "/DATN/uploads/san_pham/";
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, imageFile.getBytes());
                newSanPham.setUrlHinhAnh("/images/" + fileName);
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

    // API endpoint for client-side (no admin check needed for viewing)
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
