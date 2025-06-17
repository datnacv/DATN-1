//package com.example.AsmGD1.config;
//
//import com.example.AsmGD1.entity.NguoiDung;
//import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//@Service
//public class CustomUserDetailsService implements UserDetailsService {
//
//    @Autowired
//    private NguoiDungRepository nguoiDungRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
//        NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(tenDangNhap)
//                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + tenDangNhap));
//
//        return User.builder()
//                .username(nguoiDung.getTenDangNhap())
//                .password(nguoiDung.getMatKhau()) // Sử dụng plaintext, phù hợp với NoOpPasswordEncoder
//                .authorities("ROLE_" + nguoiDung.getVaiTro()) // Thêm prefix ROLE_
//                .disabled(!nguoiDung.getTrangThai())
//                .build();
//    }
//}