package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.KieuDang;
import com.example.AsmGD1.service.SanPham.KieuDangService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/kieu-dang")
    public String listKieuDang(@RequestParam(value = "search", required = false) String search, Model model) {
        List<KieuDang> kieuDangList;
        if (search != null && !search.trim().isEmpty()) {
            kieuDangList = kieuDangService.searchKieuDang(search);
        } else {
            kieuDangList = kieuDangService.getAllKieuDang();
        }
        // Reverse the list to show newest entries first (assumes database order is oldest first)
        Collections.reverse(kieuDangList);
        model.addAttribute("kieuDangList", kieuDangList);
        return "WebQuanLy/kieu-dang";
    }

    @PostMapping("/kieu-dang/save")
    public String saveKieuDang(@ModelAttribute KieuDang kieuDang) {
        kieuDangService.saveKieuDang(kieuDang);
        return "redirect:/acvstore/kieu-dang";
    }

    @GetMapping("/kieu-dang/delete/{id}")
    public String deleteKieuDang(@PathVariable UUID id) {
        kieuDangService.deleteKieuDang(id);
        return "redirect:/acvstore/kieu-dang";
    }
}