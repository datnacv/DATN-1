package com.example.AsmGD1.service;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final NguoiDungRepository nguoiDungRepository;
    private final HttpSession session;

    public CustomOAuth2UserService(NguoiDungRepository nguoiDungRepository, HttpSession session) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.session = session;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        NguoiDung nguoiDung = processOAuth2User(oAuth2User, userRequest.getClientRegistration().getRegistrationId());

        boolean isNewUser = nguoiDung.getMatKhau() == null || nguoiDung.getSoDienThoai() == null || nguoiDung.getDiaChi() == null;

        if (isNewUser) {
            session.setAttribute("pendingUser", nguoiDung); // Chỉ lưu session nếu là user mới
        } else {
            session.removeAttribute("pendingUser"); // Xóa nếu user đã có
        }

        return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().toUpperCase())),
                oAuth2User.getAttributes(),
                "name"
        );
    }

    private NguoiDung processOAuth2User(OAuth2User oAuth2User, String provider) {
        String email = oAuth2User.getAttribute("email");
        String id = oAuth2User.getAttribute("id");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            throw new IllegalArgumentException("Email không thể null khi đăng nhập OAuth2");
        }

        // Kiểm tra nếu email đã tồn tại trong database
        Optional<NguoiDung> existingNguoiDungByEmail = nguoiDungRepository.findByEmail(email);
        if (existingNguoiDungByEmail.isPresent()) {
            return existingNguoiDungByEmail.get(); // Trả về user cũ nếu đã có email này
        }

        // Kiểm tra nếu tenDangNhap (dùng email làm tenDangNhap) đã tồn tại
        Optional<NguoiDung> existingNguoiDungByTenDangNhap = nguoiDungRepository.findByTenDangNhap(email);
        if (existingNguoiDungByTenDangNhap.isPresent()) {
            return existingNguoiDungByTenDangNhap.get();
        }

        // Nếu chưa tồn tại, tạo nguoiDung mới
        NguoiDung newNguoiDung = new NguoiDung();
        newNguoiDung.setTenDangNhap(email);
        newNguoiDung.setEmail(email);
        newNguoiDung.setHoTen(name);
        newNguoiDung.setVaiTro("CUSTOMER");
        newNguoiDung.setThoiGianTao(LocalDateTime.now());
        newNguoiDung.setTrangThai(true);

        return nguoiDungRepository.save(newNguoiDung);
    }
}