package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.TayAo;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.TayAoService;
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
public class TayAoController {
    @Autowired
    private TayAoService tayAoService;
    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/tay-ao")
    public String listTayAo(@RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "error", required = false) String errorMessage,
                            Model model) {
        List<TayAo> tayAoList;
        if (search != null && !search.trim().isEmpty()) {
            tayAoList = tayAoService.searchTayAo(search);
        } else {
            tayAoList = tayAoService.getAllTayAo();
        }
        Collections.reverse(tayAoList);
        model.addAttribute("tayAoList", tayAoList);
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
        return "WebQuanLy/tay-ao";
    }

    @PostMapping("/tay-ao/save")
    public String saveTayAo(@ModelAttribute TayAo tayAo, Model model) {
        try {
            tayAoService.saveTayAo(tayAo);
            return "redirect:/acvstore/tay-ao";
        } catch (IllegalArgumentException e) {
            List<TayAo> tayAoList = tayAoService.getAllTayAo();
            Collections.reverse(tayAoList);
            model.addAttribute("tayAoList", tayAoList);
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/tay-ao";
        }
    }

    @GetMapping("/tay-ao/delete/{id}")
    public String deleteTayAo(@PathVariable UUID id, Model model) {
        try {
            tayAoService.deleteTayAo(id);
            return "redirect:/acvstore/tay-ao";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listTayAo(null, e.getMessage(), model);
        }
    }
}