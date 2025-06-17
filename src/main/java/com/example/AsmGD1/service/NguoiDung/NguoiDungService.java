package com.example.AsmGD1.service.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NguoiDungService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    public Page<NguoiDung> findUsersByVaiTro(String vaiTro, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return nguoiDungRepository.findByVaiTroAndKeyword(vaiTro, keyword != null ? keyword : "", pageable);
    }

    public NguoiDung findById(UUID id) {
        return nguoiDungRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
    }

    public NguoiDung save(NguoiDung nguoiDung) {
        if (nguoiDung.getId() == null) {
            nguoiDung.setThoiGianTao(LocalDateTime.now());
            nguoiDung.setTrangThai(true);
            // Xóa mã hóa, lưu mật khẩu trực tiếp
        } else {
            NguoiDung existing = nguoiDungRepository.findById(nguoiDung.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + nguoiDung.getId()));
            // Không cần kiểm tra hoặc mã hóa, giữ nguyên mật khẩu
        }
        return nguoiDungRepository.save(nguoiDung);
    }

    public void deleteById(UUID id) {
        nguoiDungRepository.deleteById(id);
    }

    public NguoiDung findByTenDangNhap(String tenDangNhap) {
        return nguoiDungRepository.findByTenDangNhap(tenDangNhap)
                .orElse(null);
    }

    public NguoiDung getUserByEmail(String email) {
        return nguoiDungRepository.findByEmail(email).orElse(null);
    }
}