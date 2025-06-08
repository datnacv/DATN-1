package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.KichCo;
import com.example.AsmGD1.service.KichCoService;
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

    @GetMapping("/kich-co")
    public String listKichCo(@RequestParam(value = "search", required = false) String search, Model model) {
        List<KichCo> kichCoList;
        if (search != null && !search.trim().isEmpty()) {
            kichCoList = kichCoService.searchKichCo(search);
        } else {
            kichCoList = kichCoService.getAllKichCo();
        }
        // Reverse the list to show newest entries first (assumes database order is oldest first)
        Collections.reverse(kichCoList);
        model.addAttribute("kichCoList", kichCoList);
        return "WebQuanLy/kich-co";
    }

    @PostMapping("/kich-co/save")
    public String saveKichCo(@ModelAttribute KichCo kichCo) {
        kichCoService.saveKichCo(kichCo);
        return "redirect:/acvstore/kich-co";
    }

    @GetMapping("/kich-co/delete/{id}")
    public String deleteKichCo(@PathVariable UUID id) {
        kichCoService.deleteKichCo(id);
        return "redirect:/acvstore/kich-co";
    }
}