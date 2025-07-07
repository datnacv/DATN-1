package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.MauSac;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.MauSacService;
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
public class MauSacController {
    @Autowired
    private MauSacService mauSacService;
    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/mau-sac")
    public String listMauSac(@RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "error", required = false) String errorMessage,
                             Model model) {
        List<MauSac> mauSacList;
        if (search != null && !search.trim().isEmpty()) {
            mauSacList = mauSacService.searchMauSac(search);
        } else {
            mauSacList = mauSacService.getAllMauSac();
        }
        Collections.reverse(mauSacList);
        model.addAttribute("mauSacList", mauSacList);
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
        return "WebQuanLy/mau-sac";
    }

    @PostMapping("/mau-sac/save")
    public String saveMauSac(@ModelAttribute MauSac mauSac, Model model) {
        try {
            mauSacService.saveMauSac(mauSac);
            return "redirect:/acvstore/mau-sac";
        } catch (IllegalArgumentException e) {
            List<MauSac> mauSacList = mauSacService.getAllMauSac();
            Collections.reverse(mauSacList);
            model.addAttribute("mauSacList", mauSacList);
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/mau-sac";
        }
    }

    @GetMapping("/mau-sac/delete/{id}")
    public String deleteMauSac(@PathVariable UUID id, Model model) {
        try {
            mauSacService.deleteMauSac(id);
            return "redirect:/acvstore/mau-sac";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listMauSac(null, e.getMessage(), model);
        }
    }
}