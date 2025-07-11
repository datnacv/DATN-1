package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.ChatLieu;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.ChatLieuService;
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
public class ChatLieuController {
    @Autowired
    private ChatLieuService chatLieuService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/chat-lieu")
    public String listChatLieu(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "error", required = false) String errorMessage,
                               Model model) {
        List<ChatLieu> chatLieuList;
        if (search != null && !search.trim().isEmpty()) {
            chatLieuList = chatLieuService.searchChatLieu(search);
        } else {
            chatLieuList = chatLieuService.getAllChatLieu();
        }
        Collections.reverse(chatLieuList);
        model.addAttribute("chatLieuList", chatLieuList);
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
        return "WebQuanLy/chat-lieu";
    }

    @PostMapping("/chat-lieu/save")
    public String saveChatLieu(@ModelAttribute ChatLieu chatLieu, Model model) {
        try {
            chatLieuService.saveChatLieu(chatLieu);
            return "redirect:/acvstore/chat-lieu";
        } catch (IllegalArgumentException e) {
            List<ChatLieu> chatLieuList = chatLieuService.getAllChatLieu();
            Collections.reverse(chatLieuList);
            model.addAttribute("chatLieuList", chatLieuList);
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
            model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
            model.addAttribute("errorMessage", e.getMessage());
            return "WebQuanLy/chat-lieu";
        }
    }

    @GetMapping("/chat-lieu/delete/{id}")
    public String deleteChatLieu(@PathVariable UUID id, Model model) {
        try {
            chatLieuService.deleteChatLieu(id);
            return "redirect:/acvstore/chat-lieu";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listChatLieu(null, e.getMessage(), model);
        }
    }
}