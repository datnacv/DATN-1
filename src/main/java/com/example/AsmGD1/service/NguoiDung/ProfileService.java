package com.example.AsmGD1.service.NguoiDung;

import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.ProfileRepository;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private DiaChiNguoiDungRepository diaChiNguoiDungRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0[1-9][0-9]{8}$");

    public NguoiDung getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return profileRepository.findByTenDangNhapAndTrangThai(username, true)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại hoặc đã bị xóa"));
    }

    public NguoiDung updateUser(NguoiDung userData) {
        UUID id = getCurrentUser().getId();

        // Validate
        if (userData.getHoTen() == null || userData.getHoTen().trim().isEmpty()) {
            throw new RuntimeException("Họ tên không được để trống");
        }
        if (userData.getEmail() == null || userData.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (userData.getSoDienThoai() == null || userData.getSoDienThoai().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        if (!EMAIL_PATTERN.matcher(userData.getEmail()).matches()) {
            throw new RuntimeException("Email không hợp lệ");
        }
        if (!PHONE_PATTERN.matcher(userData.getSoDienThoai()).matches()) {
            throw new RuntimeException("Số điện thoại không hợp lệ");
        }
        if (!profileRepository.isEmailAndPhoneUnique(id, userData.getEmail(), userData.getSoDienThoai())) {
            throw new RuntimeException("Email hoặc số điện thoại đã được sử dụng");
        }

        // Update
        NguoiDung existingUser = profileRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại hoặc đã bị xóa"));

        existingUser.setHoTen(userData.getHoTen());
        existingUser.setEmail(userData.getEmail());
        existingUser.setSoDienThoai(userData.getSoDienThoai());
        existingUser.setNgaySinh(userData.getNgaySinh());
        existingUser.setGioiTinh(userData.getGioiTinh());
        existingUser.setTinhThanhPho(userData.getTinhThanhPho());
        existingUser.setQuanHuyen(userData.getQuanHuyen());
        existingUser.setPhuongXa(userData.getPhuongXa());
        existingUser.setChiTietDiaChi(userData.getChiTietDiaChi());
        existingUser.setVaiTro(userData.getVaiTro());

        return profileRepository.save(existingUser);
    }

    public void deleteUser() {
        UUID id = getCurrentUser().getId();
        NguoiDung user = profileRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại hoặc đã bị xóa"));
        user.setTrangThai(false);
        profileRepository.save(user);
    }

    // -------------------- Địa chỉ --------------------

    @Transactional
    public DiaChiNguoiDung addAddress(DiaChiNguoiDung address) {
        NguoiDung user = getCurrentUser();
        validateAddress(address);

        address.setNguoiDung(user);
        address.setThoiGianTao(LocalDateTime.now());
        if (address.getMacDinh() == null) {
            address.setMacDinh(false);
        }

        if (Boolean.TRUE.equals(address.getMacDinh())) {
            diaChiNguoiDungRepository.removeDefaultFlag(user.getId());
        }

        return diaChiNguoiDungRepository.save(address);
    }

    public List<DiaChiNguoiDung> getUserAddresses() {
        UUID userId = getCurrentUser().getId();
        return diaChiNguoiDungRepository.findByNguoiDung_Id(userId);
    }

    public DiaChiNguoiDung getAddressById(UUID addressId) {
        UUID userId = getCurrentUser().getId();
        return diaChiNguoiDungRepository.findById(addressId)
                .filter(addr -> addr.getNguoiDung().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại hoặc không thuộc về bạn"));
    }

    @Transactional
    public DiaChiNguoiDung updateAddress(UUID addressId, DiaChiNguoiDung addressData) {
        DiaChiNguoiDung address = getAddressById(addressId);
        validateAddress(addressData);

        address.setTinhThanhPho(addressData.getTinhThanhPho());
        address.setQuanHuyen(addressData.getQuanHuyen());
        address.setPhuongXa(addressData.getPhuongXa());
        address.setChiTietDiaChi(addressData.getChiTietDiaChi());

        if (Boolean.TRUE.equals(addressData.getMacDinh())) {
            diaChiNguoiDungRepository.removeDefaultFlag(address.getNguoiDung().getId());
            address.setMacDinh(true);
        } else {
            address.setMacDinh(false);
        }

        return diaChiNguoiDungRepository.save(address);
    }

    @Transactional
    public void deleteAddress(UUID addressId) {
        DiaChiNguoiDung address = getAddressById(addressId);
        diaChiNguoiDungRepository.delete(address);
    }

    @Transactional
    public void setDefaultAddress(UUID addressId) {
        DiaChiNguoiDung address = getAddressById(addressId);
        diaChiNguoiDungRepository.removeDefaultFlag(address.getNguoiDung().getId());
        address.setMacDinh(true);
        diaChiNguoiDungRepository.save(address);
    }

    private void validateAddress(DiaChiNguoiDung address) {
        if (address.getTinhThanhPho() == null || address.getTinhThanhPho().trim().isEmpty() ||
                address.getQuanHuyen() == null || address.getQuanHuyen().trim().isEmpty() ||
                address.getPhuongXa() == null || address.getPhuongXa().trim().isEmpty() ||
                address.getChiTietDiaChi() == null || address.getChiTietDiaChi().trim().isEmpty()) {
            throw new RuntimeException("Tất cả các trường địa chỉ là bắt buộc");
        }
    }

    // -------------------- OTP / Mật khẩu --------------------

    public void sendOtp(String email) {
        NguoiDung user = profileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(3));
        profileRepository.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP đặt lại mật khẩu");
        message.setText("Mã OTP của bạn là: " + otp);
        mailSender.send(message);
    }

    public String verifyOtp(String email, String otp) {
        Optional<NguoiDung> userOptional = profileRepository.findByEmail(email);
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
        NguoiDung user = profileRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        if (newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        user.setMatKhau(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        profileRepository.save(user);
    }

    private String generateOtp() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }
}
