package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.DanhMuc;
import com.example.AsmGD1.service.DanhMucService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/danh-muc")
    public String listDanhMuc(@RequestParam(value = "search", required = false) String search, Model model) {
        List<DanhMuc> danhMucList;
        if (search != null && !search.trim().isEmpty()) {
            danhMucList = danhMucService.searchDanhMuc(search);
        } else {
            danhMucList = danhMucService.getAllDanhMuc();
        }
        // Reverse the list to show newest entries first
        Collections.reverse(danhMucList);
        model.addAttribute("danhMucList", danhMucList);
        return "WebQuanLy/danh-muc";
    }

    @PostMapping("/danh-muc/save")
    public String saveDanhMuc(@ModelAttribute DanhMuc danhMuc) {
        danhMucService.saveDanhMuc(danhMuc);
        return "redirect:/acvstore/danh-muc";
    }

    @GetMapping("/danh-muc/delete/{id}")
    public String deleteDanhMuc(@PathVariable UUID id) {
        danhMucService.deleteDanhMuc(id);
        return "redirect:/acvstore/danh-muc";
    }
}