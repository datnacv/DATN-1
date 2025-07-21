package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.ThuongHieu;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.ThuongHieuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore")
public class ThuongHieuController {

    @Autowired
    private ThuongHieuService thuongHieuService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/thuong-hieu")
    public String listThuongHieu(@RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "error", required = false) String errorMessage,
                                 Model model) {
        List<ThuongHieu> thuongHieuList = Collections.emptyList();

        try {
            thuongHieuList = search != null && !search.trim().isEmpty()
                    ? thuongHieuService.searchThuongHieu(search)
                    : thuongHieuService.getAllThuongHieu();

            if (thuongHieuList == null) thuongHieuList = Collections.emptyList();
            Collections.reverse(thuongHieuList);

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách thương hiệu: " + e.getMessage());
        }

        model.addAttribute("thuongHieuList", thuongHieuList);

        // Lấy thông tin user hiện tại và phân quyền
        UserInfo userInfo = getCurrentUserInfo();
        model.addAttribute("user", userInfo.getUser());
        model.addAttribute("isAdmin", userInfo.isAdmin());

        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }

        return "WebQuanLy/thuong-hieu";
    }

    @PostMapping("/thuong-hieu/save")
    public String saveThuongHieu(@ModelAttribute ThuongHieu thuongHieu,
                                 RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/thuong-hieu";
        }

        try {
            thuongHieuService.saveThuongHieu(thuongHieu);
            String message = thuongHieu.getId() != null ? "Cập nhật thương hiệu thành công!" : "Thêm thương hiệu thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu thương hiệu thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/thuong-hieu";
    }

    @GetMapping("/thuong-hieu/delete/{id}")
    public String deleteThuongHieu(@PathVariable UUID id,
                                   RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/thuong-hieu";
        }

        try {
            thuongHieuService.deleteThuongHieu(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa thương hiệu thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa thương hiệu thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/thuong-hieu";
    }

    // Helper methods
    private boolean isCurrentUserAdmin() {
        return getCurrentUserInfo().isAdmin();
    }

    private UserInfo getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung currentUser = (NguoiDung) auth.getPrincipal();
            boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getVaiTro());
            return new UserInfo(currentUser, isAdmin);
        }

        // Fallback - tạo user mặc định
        NguoiDung defaultUser = new NguoiDung();
        defaultUser.setTenDangNhap("guest");
        defaultUser.setVaiTro("employee");
        return new UserInfo(defaultUser, false);
    }

    // Inner class để đóng gói thông tin user
    private static class UserInfo {
        private final NguoiDung user;
        private final boolean isAdmin;

        public UserInfo(NguoiDung user, boolean isAdmin) {
            this.user = user;
            this.isAdmin = isAdmin;
        }

        public NguoiDung getUser() { return user; }
        public boolean isAdmin() { return isAdmin; }
    }
}
