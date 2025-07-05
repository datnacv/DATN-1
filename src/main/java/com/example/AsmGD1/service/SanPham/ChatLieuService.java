package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.ChatLieu;
import com.example.AsmGD1.repository.SanPham.ChatLieuRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChatLieuService {
    @Autowired
    private ChatLieuRepository chatLieuRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

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

    public ChatLieu saveChatLieu(ChatLieu chatLieu) throws IllegalArgumentException {
        if (chatLieu.getTenChatLieu() == null || chatLieu.getTenChatLieu().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên chất liệu không được để trống");
        }
        if (chatLieuRepository.findByTenChatLieuContainingIgnoreCase(chatLieu.getTenChatLieu())
                .stream()
                .anyMatch(c -> !c.getId().equals(chatLieu.getId()) && c.getTenChatLieu().equalsIgnoreCase(chatLieu.getTenChatLieu()))) {
            throw new IllegalArgumentException("Tên chất liệu đã tồn tại");
        }
        return chatLieuRepository.save(chatLieu);
    }

    public void deleteChatLieu(UUID id) throws IllegalStateException {
        if (sanPhamChiTietRepository.existsByChatLieuId(id)) {
            throw new IllegalStateException("Không thể xóa chất liệu vì đang có sản phẩm chi tiết tham chiếu đến");
        }
        chatLieuRepository.deleteById(id);
    }
}