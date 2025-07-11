package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.CoAo;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.CoAoService;
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
public class CoAoController {
    @Autowired
    private CoAoService coAoService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/co-ao")
    public String listCoAo(@RequestParam(value = "search", required = false) String search,
                           @RequestParam(value = "error", required = false) String errorMessage,
                           Model model) {
        List<CoAo> coAoList;
        if (search != null && !search.trim().isEmpty()) {
            coAoList = coAoService.searchCoAo(search);
        } else {
            coAoList = coAoService.getAllCoAo();
        }
        Collections.reverse(coAoList);
        model.addAttribute("coAoList", coAoList);
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
        return "WebQuanLy/co-ao";
    }

    @PostMapping("/co-ao/save")
    public String saveCoAo(@ModelAttribute CoAo coAo, Model model) {
        try {
            coAoService.saveCoAo(coAo);
            return "redirect:/acvstore/co-ao";
        } catch (IllegalArgumentException e) {
            List<CoAo> coAoList = coAoService.getAllCoAo();
            Collections.reverse(coAoList);
            model.addAttribute("coAoList", coAoList);
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/co-ao";
        }
    }

    @GetMapping("/co-ao/delete/{id}")
    public String deleteCoAo(@PathVariable UUID id, Model model) {
        try {
            coAoService.deleteCoAo(id);
            return "redirect:/acvstore/co-ao";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listCoAo(null, e.getMessage(), model);
        }
    }
}