package com.example.AsmGD1.controller.SanPham;

import com.example.AsmGD1.entity.ChatLieu;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.SanPham.ChatLieuService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public String listChatLieu(@RequestParam(value = "search", required = false) String search, Model model) {
        List<ChatLieu> chatLieuList;
        if (search != null && !search.trim().isEmpty()) {
            chatLieuList = chatLieuService.searchChatLieu(search);
        } else {
            chatLieuList = chatLieuService.getAllChatLieu();
        }
        // Reverse the list to show newest entries first (assumes database order is oldest first)
        Collections.reverse(chatLieuList);
        model.addAttribute("chatLieuList", chatLieuList);
        List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("admin", "", 0, 1).getContent();
        model.addAttribute("user", admins.isEmpty() ? new NguoiDung() : admins.get(0));
        return "WebQuanLy/chat-lieu";
    }

    @PostMapping("/chat-lieu/save")
    public String saveChatLieu(@ModelAttribute ChatLieu chatLieu) {
        chatLieuService.saveChatLieu(chatLieu);
        return "redirect:/acvstore/chat-lieu";
    }

    @GetMapping("/chat-lieu/delete/{id}")
    public String deleteChatLieu(@PathVariable UUID id) {
        chatLieuService.deleteChatLieu(id);
        return "redirect:/acvstore/chat-lieu";
    }
}