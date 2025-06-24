package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.service.SanPham.DanhMucService;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import com.example.AsmGD1.util.CloudinaryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
    private CloudinaryUtil cloudinaryUtil;

    @GetMapping
    public String viewSanPhamPage(
            Model model,
            @RequestParam(value = "searchName", required = false) String searchName,
            @RequestParam(value = "trangThai", required = false) Boolean trangThai,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10);
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
        return "/WebQuanLy/san-pham-list-form";
    }

    @GetMapping("/edit/{id}")
    public String editSanPham(@PathVariable("id") UUID id, Model model) {
        SanPham sanPham = sanPhamService.findById(id);
        List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
        model.addAttribute("sanPham", sanPham);
        model.addAttribute("sanPhamList", sanPhamService.findAll());
        model.addAttribute("danhMucList", danhMucList);
        return "/WebQuanLy/san-pham-list-form";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveSanPham(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("Received payload: " + payload.toString());

            UUID id = UUID.fromString((String) payload.get("id"));
            String maSanPham = (String) payload.get("maSanPham");
            String tenSanPham = (String) payload.get("tenSanPham");
            String moTa = (String) payload.get("moTa");
            String urlHinhAnh = (String) payload.get("urlHinhAnh");
            Boolean trangThai = (Boolean) payload.get("trangThai");
            UUID danhMucId = payload.get("danhMucId") != null ? UUID.fromString((String) payload.get("danhMucId")) : null;

            SanPham existingSanPham = sanPhamService.findById(id);
            if (existingSanPham == null) {
                response.put("success", false);
                response.put("message", "Sản phẩm không tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            existingSanPham.setMaSanPham(maSanPham);
            existingSanPham.setTenSanPham(tenSanPham);
            existingSanPham.setMoTa(moTa);
            existingSanPham.setUrlHinhAnh(urlHinhAnh);
            existingSanPham.setTrangThai(trangThai != null ? trangThai : false);

            if (danhMucId != null) {
                DanhMuc danhMuc = danhMucService.getDanhMucById(danhMucId);
                if (danhMuc != null) {
                    existingSanPham.setDanhMuc(danhMuc);
                } else {
                    response.put("success", false);
                    response.put("message", "Danh mục không tồn tại!");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            sanPhamService.save(existingSanPham);

            // Tạo DTO để trả về (tránh lỗi recursion)
            SanPhamDto dto = new SanPhamDto();
            dto.setId(existingSanPham.getId());
            dto.setMaSanPham(existingSanPham.getMaSanPham());
            dto.setTenSanPham(existingSanPham.getTenSanPham());
            dto.setMoTa(existingSanPham.getMoTa());
            dto.setUrlHinhAnh(existingSanPham.getUrlHinhAnh());
            dto.setTrangThai(existingSanPham.getTrangThai());
            dto.setThoiGianTao(existingSanPham.getThoiGianTao());

            if (existingSanPham.getDanhMuc() != null) {
                dto.setDanhMucId(existingSanPham.getDanhMuc().getId());
                dto.setTenDanhMuc(existingSanPham.getDanhMuc().getTenDanhMuc());
            }

            response.put("success", true);
            response.put("message", "Lưu sản phẩm thành công!");
            response.put("sanPham", dto);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            response.put("success", false);
            response.put("message", "Lỗi khi lưu sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/upload-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("imageFile") MultipartFile imageFile) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = cloudinaryUtil.uploadImage(imageFile);
                response.put("success", true);
                response.put("url", imageUrl);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không có file ảnh được chọn!");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi tải ảnh lên: " + e.getMessage());
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
        if (sp.getDanhMuc() != null) {
            dto.setDanhMucId(sp.getDanhMuc().getId());
            dto.setTenDanhMuc(sp.getDanhMuc().getTenDanhMuc());
        }

        return ResponseEntity.ok(dto);
    }
}