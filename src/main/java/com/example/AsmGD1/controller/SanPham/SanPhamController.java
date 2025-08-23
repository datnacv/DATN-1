package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.DanhMucService;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/san-pham")
public class SanPhamController {

    private static final Logger logger = LoggerFactory.getLogger(SanPhamController.class);

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private DanhMucService danhMucService;

    @Autowired
    private NguoiDungService nguoiDungService;

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
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("isAdmin", false);
        }
    }

    @GetMapping
    public String viewSanPhamPage(
            Model model,
            @RequestParam(value = "searchName", required = false) String searchName,
            @RequestParam(value = "trangThai", required = false) Boolean trangThai,
            @RequestParam(value = "page", defaultValue = "0") int page) {

        addUserInfoToModel(model);

        Pageable pageable = PageRequest.of(page, 5);
        Page<SanPham> sanPhamPage;

        if (searchName != null && !searchName.trim().isEmpty()) {
            searchName = searchName.trim();
            sanPhamPage = sanPhamService.searchByTenOrMa(searchName, pageable);
            if (trangThai != null) {
                sanPhamPage = sanPhamService.findByTenSanPhamContaining(searchName, trangThai, pageable);
            }
        } else if (trangThai != null) {
            sanPhamPage = sanPhamService.findByTrangThai(trangThai, pageable);
        } else {
            sanPhamPage = sanPhamService.findAllPaginated(pageable);
        }

        List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();

        model.addAttribute("sanPhamList", sanPhamPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", sanPhamPage.getTotalPages());
        model.addAttribute("searchName", searchName);
        model.addAttribute("selectedTrangThai", trangThai);
        model.addAttribute("sanPham", new SanPham());
        model.addAttribute("danhMucList", danhMucList);

        return "WebQuanLy/san-pham-list-form";
    }

    @GetMapping("/edit/{id}")
    public String editSanPham(@PathVariable("id") UUID id, Model model) {
        if (!isCurrentUserAdmin()) {
            return "redirect:/acvstore/san-pham?error=Bạn không có quyền truy cập chức năng này";
        }

        addUserInfoToModel(model);

        SanPham sanPham = sanPhamService.findById(id);
        if (sanPham == null) {
            model.addAttribute("error", "Sản phẩm không tồn tại");
            return "WebQuanLy/error";
        }

        List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
        model.addAttribute("sanPham", sanPham);
        model.addAttribute("danhMucList", danhMucList);

        return "WebQuanLy/edit-san-pham-form";
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSanPham(
            @ModelAttribute SanPhamDto dto,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!isCurrentUserAdmin()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện chức năng này!");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Validation
            String maSanPham = dto.getMaSanPham();
            String tenSanPham = dto.getTenSanPham();
            UUID danhMucId = dto.getDanhMucId();
            Boolean trangThai = dto.getTrangThai();

            if (maSanPham == null || maSanPham.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Mã sản phẩm không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }
            if (!maSanPham.matches("^[a-zA-Z0-9_-]+$")) {
                response.put("success", false);
                response.put("message", "Mã sản phẩm chỉ được chứa chữ cái, số, dấu gạch dưới hoặc gạch ngang!");
                return ResponseEntity.badRequest().body(response);
            }
            // Kiểm tra trùng lặp mã sản phẩm (thay vì findByMaSanPham)
            List<SanPham> existingProducts = sanPhamService.findAll();
            for (SanPham sp : existingProducts) {
                if (sp.getMaSanPham().equals(maSanPham) && !sp.getId().equals(dto.getId())) {
                    response.put("success", false);
                    response.put("message", "Mã sản phẩm đã tồn tại!");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            if (tenSanPham == null || tenSanPham.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên sản phẩm không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }
            tenSanPham = tenSanPham.replaceAll("\\s+", " ").trim();
            if (!tenSanPham.matches("^[a-zA-Z0-9_-]+(\\s[a-zA-Z0-9_-]+)*$")) {
                response.put("success", false);
                response.put("message", "Tên sản phẩm chỉ được chứa chữ cái, số, dấu gạch dưới, gạch ngang và khoảng trắng giữa các từ!");
                return ResponseEntity.badRequest().body(response);
            }
            // Kiểm tra trùng lặp tên sản phẩm
            for (SanPham sp : existingProducts) {
                if (sp.getTenSanPham().equals(tenSanPham) && !sp.getId().equals(dto.getId())) {
                    response.put("success", false);
                    response.put("message", "Tên sản phẩm đã tồn tại!");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            if (danhMucId == null) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn danh mục!");
                return ResponseEntity.badRequest().body(response);
            }
            DanhMuc danhMuc = danhMucService.getDanhMucById(danhMucId);
            if (danhMuc == null) {
                response.put("success", false);
                response.put("message", "Danh mục không tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            if (trangThai != null && trangThai && !sanPhamService.hasActiveChiTietSanPham(dto.getId())) {
                response.put("success", false);
                response.put("message", "Không thể bật trạng thái sản phẩm vì không có chi tiết sản phẩm nào đang hoạt động!");
                return ResponseEntity.badRequest().body(response);
            }

            // Update product
            SanPham sanPham = sanPhamService.findById(dto.getId());
            if (sanPham == null) {
                response.put("success", false);
                response.put("message", "Sản phẩm không tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            sanPham.setMaSanPham(maSanPham);
            sanPham.setTenSanPham(tenSanPham);
            sanPham.setMoTa(dto.getMoTa());
            sanPham.setTrangThai(trangThai != null ? trangThai : false);
            sanPham.setDanhMuc(danhMuc);

            // Handle image upload
            if (imageFile != null && !imageFile.isEmpty()) {
                String UPLOAD_DIR = System.getProperty("os.name").toLowerCase().contains("win")
                        ? "C:/DATN/uploads/san_pham/"
                        : System.getProperty("user.home") + "/DATN/uploads/san_pham/";
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, imageFile.getBytes());
                sanPham.setUrlHinhAnh("/images/" + fileName);
            }

            sanPhamService.save(sanPham);

            // Prepare response DTO
            SanPhamDto responseDto = new SanPhamDto();
            responseDto.setId(sanPham.getId());
            responseDto.setMaSanPham(sanPham.getMaSanPham());
            responseDto.setTenSanPham(sanPham.getTenSanPham());
            responseDto.setMoTa(sanPham.getMoTa());
            responseDto.setUrlHinhAnh(sanPham.getUrlHinhAnh());
            responseDto.setTrangThai(sanPham.getTrangThai());
            responseDto.setThoiGianTao(sanPham.getThoiGianTao());
            responseDto.setTongSoLuong(sanPham.getTongSoLuong());
            responseDto.setDanhMucId(danhMuc.getId());
            responseDto.setTenDanhMuc(danhMuc.getTenDanhMuc());

            response.put("success", true);
            response.put("message", "Cập nhật sản phẩm thành công!");
            response.put("sanPham", responseDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật sản phẩm: ", e);
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/get/{id}")
    @ResponseBody
    public ResponseEntity<SanPhamDto> getSanPham(@PathVariable("id") UUID id) {
        SanPham sp = sanPhamService.findById(id);
        if (sp == null) return ResponseEntity.notFound().build();

        SanPhamDto dto = new SanPhamDto();
        dto.setId(sp.getId());
        dto.setMaSanPham(sp.getMaSanPham());
        dto.setTenSanPham(sp.getTenSanPham());
        dto.setMoTa(sp.getMoTa());
        dto.setUrlHinhAnh(sp.getUrlHinhAnh());
        dto.setTrangThai(sp.getTrangThai());
        dto.setThoiGianTao(sp.getThoiGianTao());
        dto.setTongSoLuong(sp.getTongSoLuong());

        if (sp.getDanhMuc() != null) {
            dto.setDanhMucId(sp.getDanhMuc().getId());
            dto.setTenDanhMuc(sp.getDanhMuc().getTenDanhMuc());
        }

        return ResponseEntity.ok(dto);
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

            SanPham sanPham = sanPhamService.findById(id);
            if (sanPham == null) {
                response.put("success", false);
                response.put("message", "Sản phẩm không tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            if (trangThai != null && trangThai && !sanPhamService.hasActiveChiTietSanPham(id)) {
                response.put("success", false);
                response.put("message", "Không thể bật trạng thái sản phẩm vì không có chi tiết sản phẩm nào đang hoạt động!");
                return ResponseEntity.badRequest().body(response);
            }

            sanPham.setTrangThai(trangThai != null ? trangThai : false);
            sanPhamService.save(sanPham);

            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật trạng thái: ", e);
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}