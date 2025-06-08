package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.XuatXu;
import com.example.AsmGD1.service.XuatXuService;
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

    @GetMapping("/xuat-xu")
    public String listXuatXu(@RequestParam(value = "search", required = false) String search, Model model) {
        List<XuatXu> xuatXuList;
        if (search != null && !search.trim().isEmpty()) {
            xuatXuList = xuatXuService.searchXuatXu(search);
        } else {
            xuatXuList = xuatXuService.getAllXuatXu();
        }
        // Reverse the list to show newest entries first (assumes database order is oldest first)
        Collections.reverse(xuatXuList);
        model.addAttribute("xuatXuList", xuatXuList);
        return "WebQuanLy/xuat-xu";
    }

    @PostMapping("/xuat-xu/save")
    public String saveXuatXu(@ModelAttribute XuatXu xuatXu) {
        xuatXuService.saveXuatXu(xuatXu);
        return "redirect:/acvstore/xuat-xu";
    }

    @GetMapping("/xuat-xu/delete/{id}")
    public String deleteXuatXu(@PathVariable UUID id) {
        xuatXuService.deleteXuatXu(id);
        return "redirect:/acvstore/xuat-xu";
    }
}