package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.XuatXu;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.XuatXuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore")
public class XuatXuController {
    @Autowired
    private XuatXuService xuatXuService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/xuat-xu")
    public String listXuatXu(@RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "error", required = false) String errorMessage,
                             Model model) {
        List<XuatXu> xuatXuList;
        if (search != null && !search.trim().isEmpty()) {
            xuatXuList = xuatXuService.searchXuatXu(search);
        } else {
            xuatXuList = xuatXuService.getAllXuatXu();
        }
        Collections.reverse(xuatXuList);
        model.addAttribute("xuatXuList", xuatXuList);
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "WebQuanLy/xuat-xu";
    }

    @PostMapping("/xuat-xu/save")
    public String saveXuatXu(@ModelAttribute XuatXu xuatXu, Model model) {
        try {
            xuatXuService.saveXuatXu(xuatXu);
            return "redirect:/acvstore/xuat-xu";
        } catch (IllegalArgumentException e) {
            List<XuatXu> xuatXuList = xuatXuService.getAllXuatXu();
            Collections.reverse(xuatXuList);
            model.addAttribute("xuatXuList", xuatXuList);
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/xuat-xu";
        }
    }

    @GetMapping("/xuat-xu/delete/{id}")
    public String deleteXuatXu(@PathVariable UUID id, Model model) {
        try {
            xuatXuService.deleteXuatXu(id);
            return "redirect:/acvstore/xuat-xu";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listXuatXu(null, e.getMessage(), model); // Gọi lại listXuatXu để render trang với lỗi
        }
    }
}