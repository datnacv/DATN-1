package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.KieuDang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.KieuDangService;
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
public class KieuDangController {
    @Autowired
    private KieuDangService kieuDangService;
    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/kieu-dang")
    public String listKieuDang(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "error", required = false) String errorMessage,
                               Model model) {
        List<KieuDang> kieuDangList;
        if (search != null && !search.trim().isEmpty()) {
            kieuDangList = kieuDangService.searchKieuDang(search);
        } else {
            kieuDangList = kieuDangService.getAllKieuDang();
        }
        Collections.reverse(kieuDangList);
        model.addAttribute("kieuDangList", kieuDangList);
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
        return "WebQuanLy/kieu-dang";
    }

    @PostMapping("/kieu-dang/save")
    public String saveKieuDang(@ModelAttribute KieuDang kieuDang, Model model) {
        try {
            kieuDangService.saveKieuDang(kieuDang);
            return "redirect:/acvstore/kieu-dang";
        } catch (IllegalArgumentException e) {
            List<KieuDang> kieuDangList = kieuDangService.getAllKieuDang();
            Collections.reverse(kieuDangList);
            model.addAttribute("kieuDangList", kieuDangList);
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/kieu-dang";
        }
    }

    @GetMapping("/kieu-dang/delete/{id}")
    public String deleteKieuDang(@PathVariable UUID id, Model model) {
        try {
            kieuDangService.deleteKieuDang(id);
            return "redirect:/acvstore/kieu-dang";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listKieuDang(null, e.getMessage(), model);
        }
    }
}