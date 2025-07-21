package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.TayAo;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.TayAoService;
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
public class TayAoController {

    @Autowired
    private TayAoService tayAoService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/tay-ao")
    public String listTayAo(@RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "error", required = false) String errorMessage,
                            Model model) {
        List<TayAo> tayAoList = Collections.emptyList();

        try {
            tayAoList = search != null && !search.trim().isEmpty()
                    ? tayAoService.searchTayAo(search)
                    : tayAoService.getAllTayAo();

            if (tayAoList == null) tayAoList = Collections.emptyList();
            Collections.reverse(tayAoList);

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách tay áo: " + e.getMessage());
        }

        model.addAttribute("tayAoList", tayAoList);

        // Lấy thông tin user hiện tại và phân quyền
        UserInfo userInfo = getCurrentUserInfo();
        model.addAttribute("user", userInfo.getUser());
        model.addAttribute("isAdmin", userInfo.isAdmin());

        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }

        return "WebQuanLy/tay-ao";
    }

    @PostMapping("/tay-ao/save")
    public String saveTayAo(@ModelAttribute TayAo tayAo,
                            RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/tay-ao";
        }

        try {
            tayAoService.saveTayAo(tayAo);
            String message = tayAo.getId() != null ? "Cập nhật tay áo thành công!" : "Thêm tay áo thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu tay áo thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/tay-ao";
    }

    @GetMapping("/tay-ao/delete/{id}")
    public String deleteTayAo(@PathVariable UUID id,
                              RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/tay-ao";
        }

        try {
            tayAoService.deleteTayAo(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tay áo thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa tay áo thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/tay-ao";
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
