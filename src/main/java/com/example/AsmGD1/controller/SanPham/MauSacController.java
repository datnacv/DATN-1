package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.MauSac;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.MauSacService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public String listMauSac(@RequestParam(value = "search", required = false) String search, Model model) {
        List<MauSac> mauSacList;
        if (search != null && !search.trim().isEmpty()) {
            mauSacList = mauSacService.searchMauSac(search);
        } else {
            mauSacList = mauSacService.getAllMauSac();
        }
        // Reverse the list to show newest entries first (assumes database order is oldest first)
        Collections.reverse(mauSacList);
        model.addAttribute("mauSacList", mauSacList);
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        return "WebQuanLy/mau-sac";
    }

    @PostMapping("/mau-sac/save")
    public String saveMauSac(@ModelAttribute MauSac mauSac) {
        mauSacService.saveMauSac(mauSac);
        return "redirect:/acvstore/mau-sac";
    }

    @GetMapping("/mau-sac/delete/{id}")
    public String deleteMauSac(@PathVariable UUID id) {
        mauSacService.deleteMauSac(id);
        return "redirect:/acvstore/mau-sac";
    }
}