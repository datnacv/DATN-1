package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.entity.ChatLieu;
import com.example.AsmGD1.entity.KichCo;
import com.example.AsmGD1.repository.SanPham.ChatLieuRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChatLieuService {
    @Autowired
    private ChatLieuRepository chatLieuRepository;

    @Autowired
    private ChiTietSanPhamRepository sanPhamChiTietRepository;

    // Lấy danh sách chất liệu với phân trang
    public Page<ChatLieu> getAllChatLieu(Pageable pageable) {
        return chatLieuRepository.findAll(pageable);
    }

    // Lấy tất cả chất liệu (không phân trang)
    public List<ChatLieu> getAllChatLieu() {
        return chatLieuRepository.findAll();
    }

    // Tìm kiếm chất liệu với phân trang
    public Page<ChatLieu> searchChatLieu(String tenChatLieu, Pageable pageable) {
        String keyword = (tenChatLieu == null) ? "" : tenChatLieu.trim();
        return chatLieuRepository.findByTenChatLieuContainingIgnoreCase(keyword, pageable);
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