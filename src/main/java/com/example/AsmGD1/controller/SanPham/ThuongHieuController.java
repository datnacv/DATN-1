package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ThuongHieu;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.ThuongHieuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore")
public class ThuongHieuController {
    @Autowired
    private ThuongHieuService thuongHieuService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/thuong-hieu")
    public String listThuongHieu(@RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "error", required = false) String errorMessage,
                                 Model model) {
        List<ThuongHieu> thuongHieuList;
        if (search != null && !search.trim().isEmpty()) {
            thuongHieuList = thuongHieuService.searchThuongHieu(search);
        } else {
            thuongHieuList = thuongHieuService.getAllThuongHieu();
        }
        Collections.reverse(thuongHieuList);
        model.addAttribute("thuongHieuList", thuongHieuList);
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "WebQuanLy/thuong-hieu";
    }

    @PostMapping("/thuong-hieu/save")
    public String saveThuongHieu(@ModelAttribute ThuongHieu thuongHieu, Model model) {
        try {
            thuongHieuService.saveThuongHieu(thuongHieu);
            return "redirect:/acvstore/thuong-hieu";
        } catch (IllegalArgumentException e) {
            List<ThuongHieu> thuongHieuList = thuongHieuService.getAllThuongHieu();
            Collections.reverse(thuongHieuList);
            model.addAttribute("thuongHieuList", thuongHieuList);
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/thuong-hieu";
        }
    }

    @GetMapping("/thuong-hieu/delete/{id}")
    public String deleteThuongHieu(@PathVariable UUID id, Model model) {
        try {
            thuongHieuService.deleteThuongHieu(id);
            return "redirect:/acvstore/thuong-hieu";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listThuongHieu(null, e.getMessage(), model);
        }
    }
}