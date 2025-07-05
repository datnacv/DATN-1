package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.KichCo;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.KichCoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        List<KichCo> kichCoList;
        if (search != null && !search.trim().isEmpty()) {
            kichCoList = kichCoService.searchKichCo(search);
        } else {
            kichCoList = kichCoService.getAllKichCo();
        }
        Collections.reverse(kichCoList);
        model.addAttribute("kichCoList", kichCoList);
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "WebQuanLy/kich-co";
    }

    @PostMapping("/kich-co/save")
    public String saveKichCo(@ModelAttribute KichCo kichCo, Model model) {
        try {
            kichCoService.saveKichCo(kichCo);
            return "redirect:/acvstore/kich-co";
        } catch (IllegalArgumentException e) {
            List<KichCo> kichCoList = kichCoService.getAllKichCo();
            Collections.reverse(kichCoList);
            model.addAttribute("kichCoList", kichCoList);
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/kich-co";
        }
    }

    @GetMapping("/kich-co/delete/{id}")
    public String deleteKichCo(@PathVariable UUID id, Model model) {
        try {
            kichCoService.deleteKichCo(id);
            return "redirect:/acvstore/kich-co";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listKichCo(null, e.getMessage(), model);
        }
    }
}