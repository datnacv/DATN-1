package com.example.AsmGD1.service.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final NguoiDungService nguoiDungService;
    private final HttpSession session;

    public CustomOAuth2UserService(NguoiDungService nguoiDungService, HttpSession session) {
        this.nguoiDungService = nguoiDungService;
        this.session = session;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        NguoiDung nguoiDung = processOAuth2User(oAuth2User, userRequest.getClientRegistration().getRegistrationId());

        // Wrap NguoiDung in DefaultOAuth2User
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", nguoiDung.getEmail());
        attributes.put("name", nguoiDung.getHoTen());
        attributes.put("id", nguoiDung.getId().toString());

        return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().toUpperCase())),
                attributes,
                "name"
        );
    }

    private NguoiDung processOAuth2User(OAuth2User oAuth2User, String provider) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            throw new IllegalArgumentException("Email không thể null khi đăng nhập OAuth2");
        }

        NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email);
        if (nguoiDung == null) {
            // Create new user for OAuth2
            nguoiDung = new NguoiDung();
            nguoiDung.setId(UUID.randomUUID());
            nguoiDung.setEmail(email);
            nguoiDung.setHoTen(name != null ? name : "Unknown");
            nguoiDung.setVaiTro("CUSTOMER");
            nguoiDung.setThoiGianTao(LocalDateTime.now());
            nguoiDung.setTrangThai(true);
            nguoiDungService.save(nguoiDung);
        }

        // Chỉ set pendingUser nếu người dùng mới và chưa có tenDangNhap hoặc matKhau
        if (nguoiDung.getTenDangNhap() == null || nguoiDung.getMatKhau() == null) {
            session.setAttribute("pendingUser", nguoiDung);
        } else {
            session.removeAttribute("pendingUser");
        }

        return nguoiDung;
    }
}