package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.KichCo;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.KichCoService;
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
public class KichCoController {

    @Autowired
    private KichCoService kichCoService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/kich-co")
    public String listKichCo(@RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "error", required = false) String errorMessage,
                             Model model) {
        List<KichCo> kichCoList = Collections.emptyList();

        try {
            kichCoList = search != null && !search.trim().isEmpty()
                    ? kichCoService.searchKichCo(search)
                    : kichCoService.getAllKichCo();

            if (kichCoList == null) kichCoList = Collections.emptyList();
            Collections.reverse(kichCoList);

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách kích cỡ: " + e.getMessage());
        }

        model.addAttribute("kichCoList", kichCoList);

        // Lấy thông tin user hiện tại và phân quyền
        UserInfo userInfo = getCurrentUserInfo();
        model.addAttribute("user", userInfo.getUser());
        model.addAttribute("isAdmin", userInfo.isAdmin());

        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }

        return "WebQuanLy/kich-co";
    }

    @PostMapping("/kich-co/save")
    public String saveKichCo(@ModelAttribute KichCo kichCo,
                             RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/kich-co";
        }

        try {
            kichCoService.saveKichCo(kichCo);
            String message = kichCo.getId() != null ? "Cập nhật kích cỡ thành công!" : "Thêm kích cỡ thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu kích cỡ thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/kich-co";
    }

    @GetMapping("/kich-co/delete/{id}")
    public String deleteKichCo(@PathVariable UUID id,
                               RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/kich-co";
        }

        try {
            kichCoService.deleteKichCo(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa kích cỡ thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa kích cỡ thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/kich-co";
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
