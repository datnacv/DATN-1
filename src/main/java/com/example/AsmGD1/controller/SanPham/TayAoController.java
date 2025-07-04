package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.TayAo;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.TayAoService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public String listTayAo(@RequestParam(value = "search", required = false) String search, Model model) {
        List<TayAo> tayAoList;
        if (search != null && !search.trim().isEmpty()) {
            tayAoList = tayAoService.searchTayAo(search);
        } else {
            tayAoList = tayAoService.getAllTayAo();
        }
        // Reverse the list to show newest entries first (assumes database order is oldest first)
        Collections.reverse(tayAoList);
        model.addAttribute("tayAoList", tayAoList);
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        return "WebQuanLy/tay-ao";
    }

    @PostMapping("/tay-ao/save")
    public String saveTayAo(@ModelAttribute TayAo tayAo) {
        tayAoService.saveTayAo(tayAo);
        return "redirect:/acvstore/tay-ao";
    }

    @GetMapping("/tay-ao/delete/{id}")
    public String deleteTayAo(@PathVariable UUID id) {
        tayAoService.deleteTayAo(id);
        return "redirect:/acvstore/tay-ao";
    }
}