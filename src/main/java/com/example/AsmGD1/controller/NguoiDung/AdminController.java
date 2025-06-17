package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/acvstore/admins")
public class AdminController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping
    public String listAdmins(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "") String keyword,
                             Model model) {
        Page<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", keyword, page, 5);
        model.addAttribute("admins", admins.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", admins.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("admin", new NguoiDung());
        return "WebQuanly/list-admin";
    }

    @PostMapping("/add")
    public String addAdmin(@ModelAttribute("admin") NguoiDung admin, RedirectAttributes redirectAttributes) {
        try {
            admin.setVaiTro("admin");
            nguoiDungService.save(admin);
            redirectAttributes.addFlashAttribute("message", "Thêm admin thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Thêm admin thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/acvstore/admins";
    }

    @PostMapping("/edit")
    public String editAdmin(@ModelAttribute("admin") NguoiDung admin, RedirectAttributes redirectAttributes) {
        try {
            admin.setVaiTro("admin");
            nguoiDungService.save(admin);
            redirectAttributes.addFlashAttribute("message", "Sửa admin thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Sửa admin thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/acvstore/admins";
    }

    @PostMapping("/delete/{id}")
    public String deleteAdmin(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            nguoiDungService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Xóa admin thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Xóa admin thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/acvstore/admins";
    }
}