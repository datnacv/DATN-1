package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.DanhMucService;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/san-pham")
public class SanPhamController {

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
            // Fallback for testing
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

        if (searchName != null && !searchName.isEmpty()) {
            sanPhamPage = sanPhamService.findByTenSanPhamContaining(searchName, trangThai, pageable);
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
        List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();

        model.addAttribute("sanPham", sanPham);
        model.addAttribute("sanPhamList", sanPhamService.findAll());
        model.addAttribute("danhMucList", danhMucList);

        return "WebQuanLy/san-pham-list-form";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveSanPham(
            @RequestParam("id") UUID id,
            @RequestParam("maSanPham") String maSanPham,
            @RequestParam("tenSanPham") String tenSanPham,
            @RequestParam(value = "moTa", required = false) String moTa,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "trangThai", defaultValue = "false") Boolean trangThai,
            @RequestParam(value = "danhMucId", required = false) UUID danhMucId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!isCurrentUserAdmin()) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện chức năng này!");
                return ResponseEntity.badRequest().body(response);
            }

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

            sanPham.setMaSanPham(maSanPham);
            sanPham.setTenSanPham(tenSanPham);
            sanPham.setMoTa(moTa);
            sanPham.setTrangThai(trangThai != null ? trangThai : false);

            if (danhMucId != null) {
                DanhMuc danhMuc = danhMucService.getDanhMucById(danhMucId);
                if (danhMuc != null) {
                    sanPham.setDanhMuc(danhMuc);
                } else {
                    response.put("success", false);
                    response.put("message", "Danh mục không tồn tại!");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            sanPhamService.saveSanPhamWithImage(sanPham, imageFile);

            SanPhamDto dto = new SanPhamDto();
            dto.setId(sanPham.getId());
            dto.setMaSanPham(sanPham.getMaSanPham());
            dto.setTenSanPham(sanPham.getTenSanPham());
            dto.setMoTa(sanPham.getMoTa());
            dto.setUrlHinhAnh(sanPham.getUrlHinhAnh());
            dto.setTrangThai(sanPham.getTrangThai());
            dto.setThoiGianTao(sanPham.getThoiGianTao());
            dto.setTongSoLuong(sanPham.getTongSoLuong());

            if (sanPham.getDanhMuc() != null) {
                dto.setDanhMucId(sanPham.getDanhMuc().getId());
                dto.setTenDanhMuc(sanPham.getDanhMuc().getTenDanhMuc());
            }

            response.put("success", true);
            response.put("message", "Lưu sản phẩm thành công!");
            response.put("sanPham", dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi lưu sản phẩm: " + e.getMessage());
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

            // Kiểm tra nếu bật trạng thái mà không có chi tiết sản phẩm hoạt động
            if (trangThai != null && trangThai && !sanPhamService.hasActiveChiTietSanPham(id)) {
                response.put("success", false);
                response.put("message", "Không thể bật trạng thái sản phẩm vì không có chi tiết sản phẩm nào hoạt động!");
                return ResponseEntity.badRequest().body(response);
            }

            sanPham.setTrangThai(trangThai != null ? trangThai : false);
            sanPhamService.save(sanPham);

            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}