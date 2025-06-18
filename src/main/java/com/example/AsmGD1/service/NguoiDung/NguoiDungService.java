package com.example.AsmGD1.service.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class NguoiDungService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

            if (nguoiDungRepository.findByTenDangNhap(nguoiDung.getTenDangNhap()).isPresent()) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại.");
            }

            // Mã hóa mật khẩu khi tạo mới
            nguoiDung.setMatKhau(passwordEncoder.encode(nguoiDung.getMatKhau()));
        } else {
            // Nếu đang cập nhật user, chỉ mã hóa nếu mật khẩu thay đổi
            Optional<NguoiDung> existing = nguoiDungRepository.findById(nguoiDung.getId());
            if (existing.isPresent() && !existing.get().getMatKhau().equals(nguoiDung.getMatKhau())) {
                nguoiDung.setMatKhau(passwordEncoder.encode(nguoiDung.getMatKhau()));
            }
        }

        return nguoiDungRepository.save(nguoiDung);
    }

    public void deleteById(UUID id) {
        nguoiDungRepository.deleteById(id);
    }

    // Đã bỏ login thủ công vì dùng Spring Security
}
