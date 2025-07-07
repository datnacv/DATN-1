package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.DanhMucService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore")
public class DanhMucController {
    @Autowired
    private DanhMucService danhMucService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/danh-muc")
    public String listDanhMuc(@RequestParam(value = "search", required = false) String search,
                              @RequestParam(value = "error", required = false) String errorMessage,
                              Model model) {
        List<DanhMuc> danhMucList;
        if (search != null && !search.trim().isEmpty()) {
            danhMucList = danhMucService.searchDanhMuc(search);
        } else {
            danhMucList = danhMucService.getAllDanhMuc();
        }
        Collections.reverse(danhMucList);
        model.addAttribute("danhMucList", danhMucList);
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
        return "WebQuanLy/danh-muc";
    }

    @PostMapping("/danh-muc/save")
    public String saveDanhMuc(@ModelAttribute DanhMuc danhMuc, Model model) {
        try {
            danhMucService.saveDanhMuc(danhMuc);
            return "redirect:/acvstore/danh-muc";
        } catch (IllegalArgumentException e) {
            List<DanhMuc> danhMucList = danhMucService.getAllDanhMuc();
            Collections.reverse(danhMucList);
            model.addAttribute("danhMucList", danhMucList);
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/danh-muc";
        }
    }

    @GetMapping("/danh-muc/delete/{id}")
    public String deleteDanhMuc(@PathVariable UUID id, Model model) {
        try {
            danhMucService.deleteDanhMuc(id);
            return "redirect:/acvstore/danh-muc";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listDanhMuc(null, e.getMessage(), model);
        }
    }
}