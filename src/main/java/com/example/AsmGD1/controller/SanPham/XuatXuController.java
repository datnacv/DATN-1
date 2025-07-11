package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.XuatXu;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.XuatXuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/xuat-xu")
public class XuatXuController {
    @Autowired
    private XuatXuService xuatXuService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping
    public String listXuatXu(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "error", required = false) String errorMessage,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {
        Pageable pageable = PageRequest.of(page, 5); // Hiển thị 5 mục mỗi trang
        Page<XuatXu> xuatXuPage;

        if (search != null && !search.trim().isEmpty()) {
            xuatXuPage = xuatXuService.searchXuatXu(search, pageable);
        } else {
            xuatXuPage = xuatXuService.getAllXuatXu(pageable);
        }

        model.addAttribute("xuatXuList", xuatXuPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", xuatXuPage.getTotalPages());
        model.addAttribute("search", search);
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
        }
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "WebQuanLy/xuat-xu";
    }

    @PostMapping("/save")
    public String saveXuatXu(@ModelAttribute XuatXu xuatXu, Model model) {
        try {
            xuatXuService.saveXuatXu(xuatXu);
            return "redirect:/acvstore/xuat-xu";
        } catch (IllegalArgumentException e) {
            Page<XuatXu> xuatXuPage = xuatXuService.getAllXuatXu(PageRequest.of(0, 5));
            model.addAttribute("xuatXuList", xuatXuPage.getContent());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", xuatXuPage.getTotalPages());
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/xuat-xu";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteXuatXu(@PathVariable UUID id, Model model) {
        try {
            xuatXuService.deleteXuatXu(id);
            return "redirect:/acvstore/xuat-xu";
        } catch (IllegalStateException e) {
            Page<XuatXu> xuatXuPage = xuatXuService.getAllXuatXu(PageRequest.of(0, 5));
            model.addAttribute("xuatXuList", xuatXuPage.getContent());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", xuatXuPage.getTotalPages());
            model.addAttribute("errorMessage", e.getMessage());
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            return "WebQuanLy/xuat-xu";
        }
    }
}