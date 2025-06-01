package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.ChatLieu;
import com.example.AsmGD1.repository.ChatLieuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChatLieuService {
    @Autowired
    private ChatLieuRepository chatLieuRepository;

    public List<ChatLieu> getAllChatLieu() {
        return chatLieuRepository.findAll();
    }

    public List<ChatLieu> searchChatLieu(String tenChatLieu) {
        return chatLieuRepository.findByTenChatLieuContainingIgnoreCase(tenChatLieu);
    }

    public ChatLieu getChatLieuById(UUID id) {
        return chatLieuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ChatLieu not found with id: " + id));
    }

    public ChatLieu saveChatLieu(ChatLieu chatLieu) {
        return chatLieuRepository.save(chatLieu);
    }

    public void deleteChatLieu(UUID id) {
        chatLieuRepository.deleteById(id);
    }
}