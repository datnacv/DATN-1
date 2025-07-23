package com.example.AsmGD1.service.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class NguoiDungService {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    public Page<NguoiDung> findUsersByVaiTroNotCustomer(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return nguoiDungRepository.findByVaiTroNotCustomer(keyword != null ? keyword : "", pageable);
    }

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

            // Kiểm tra tên đăng nhập trùng lặp
            if (nguoiDung.getTenDangNhap() != null && nguoiDungRepository.findByTenDangNhap(nguoiDung.getTenDangNhap()).isPresent()) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại.");
            }

            // Kiểm tra email trùng lặp
            if (nguoiDung.getEmail() != null && nguoiDungRepository.existsByEmail(nguoiDung.getEmail())) {
                throw new RuntimeException("Email đã tồn tại.");
            }

            // Kiểm tra số điện thoại trùng lặp
            if (nguoiDung.getSoDienThoai() != null && nguoiDungRepository.existsBySoDienThoai(nguoiDung.getSoDienThoai())) {
                throw new RuntimeException("Số điện thoại đã tồn tại.");
            }

            // Mã hóa mật khẩu nếu có
            if (nguoiDung.getMatKhau() != null && !nguoiDung.getMatKhau().isEmpty()) {
                nguoiDung.setMatKhau(passwordEncoder.encode(nguoiDung.getMatKhau()));
            }
        } else {
            // Khi cập nhật, chỉ mã hóa mật khẩu nếu nó thay đổi
            Optional<NguoiDung> existing = nguoiDungRepository.findById(nguoiDung.getId());
            if (existing.isPresent() && nguoiDung.getMatKhau() != null && !nguoiDung.getMatKhau().isEmpty()) {
                nguoiDung.setMatKhau(passwordEncoder.encode(nguoiDung.getMatKhau()));
            }
        }
        return nguoiDungRepository.save(nguoiDung);
    }

    public void deleteById(UUID id) {
        nguoiDungRepository.deleteById(id);
    }

    public boolean existsByPhone(String phone) {
        return nguoiDungRepository.existsBySoDienThoai(phone);
    }

    public NguoiDung findByTenDangNhap(String tenDangNhap) {
        return nguoiDungRepository.findByTenDangNhap(tenDangNhap)
                .orElse(null);
    }

    public NguoiDung getUserByEmail(String email) {
        return nguoiDungRepository.findByEmail(email).orElse(null);
    }

    public NguoiDung findByEmail(String email) {
        return nguoiDungRepository.findByEmail(email).orElse(null);
    }

    public void sendOtp(String email) {
        Optional<NguoiDung> userOptional = nguoiDungRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy người dùng với email: " + email);
        }

        NguoiDung user = userOptional.get();
        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
        nguoiDungRepository.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP đặt lại mật khẩu");
        message.setText("Mã OTP của bạn là: " + otp);
        mailSender.send(message);
    }

    private String generateOtp() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    public String verifyOtp(String email, String otp) {
        Optional<NguoiDung> userOptional = nguoiDungRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "not_found";
        }

        NguoiDung user = userOptional.get();
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return "expired";
        }
        return otp.equals(user.getResetOtp()) ? "valid" : "invalid";
    }

    public void resetPassword(String email, String newPassword) {
        Optional<NguoiDung> userOptional = nguoiDungRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy người dùng với email: " + email);
        }

        NguoiDung user = userOptional.get();
        user.setMatKhau(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        nguoiDungRepository.save(user);
    }
}
