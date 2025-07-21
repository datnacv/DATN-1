package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.ChatLieu;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.ChatLieuService;
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
public class ChatLieuController {

    @Autowired
    private ChatLieuService chatLieuService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/chat-lieu")
    public String listChatLieu(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "error", required = false) String errorMessage,
                               Model model) {
        List<ChatLieu> chatLieuList = Collections.emptyList();

        try {
            chatLieuList = search != null && !search.trim().isEmpty()
                    ? chatLieuService.searchChatLieu(search)
                    : chatLieuService.getAllChatLieu();

            if (chatLieuList == null) chatLieuList = Collections.emptyList();
            Collections.reverse(chatLieuList);

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách chất liệu: " + e.getMessage());
        }

        model.addAttribute("chatLieuList", chatLieuList);

        // Lấy thông tin user hiện tại và phân quyền
        UserInfo userInfo = getCurrentUserInfo();
        model.addAttribute("user", userInfo.getUser());
        model.addAttribute("isAdmin", userInfo.isAdmin());

        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }

        return "WebQuanLy/chat-lieu";
    }

    @PostMapping("/chat-lieu/save")
    public String saveChatLieu(@ModelAttribute ChatLieu chatLieu,
                               RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/chat-lieu";
        }

        try {
            chatLieuService.saveChatLieu(chatLieu);
            String message = chatLieu.getId() != null ? "Cập nhật chất liệu thành công!" : "Thêm chất liệu thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu chất liệu thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/chat-lieu";
    }

    @GetMapping("/chat-lieu/delete/{id}")
    public String deleteChatLieu(@PathVariable UUID id,
                                 RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này!");
            return "redirect:/acvstore/chat-lieu";
        }

        try {
            chatLieuService.deleteChatLieu(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa chất liệu thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa chất liệu thất bại: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/acvstore/chat-lieu";
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
