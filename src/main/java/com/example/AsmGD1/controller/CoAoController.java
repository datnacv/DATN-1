package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.CoAo;
import com.example.AsmGD1.service.CoAoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore")
public class CoAoController {
    @Autowired
    private CoAoService coAoService;

    @GetMapping("/co-ao")
    public String listCoAo(@RequestParam(value = "search", required = false) String search, Model model) {
        List<CoAo> coAoList;
        if (search != null && !search.trim().isEmpty()) {
            coAoList = coAoService.searchCoAo(search);
        } else {
            coAoList = coAoService.getAllCoAo();
        }
        // Reverse the list to show newest entries first (assumes database order is oldest first)
        Collections.reverse(coAoList);
        model.addAttribute("coAoList", coAoList);
        return "WebQuanLy/co-ao";
    }

    @PostMapping("/co-ao/save")
    public String saveCoAo(@ModelAttribute CoAo coAo) {
        coAoService.saveCoAo(coAo);
        return "redirect:/acvstore/co-ao";
    }

    @GetMapping("/co-ao/delete/{id}")
    public String deleteCoAo(@PathVariable UUID id) {
        coAoService.deleteCoAo(id);
        return "redirect:/acvstore/co-ao";
    }
}