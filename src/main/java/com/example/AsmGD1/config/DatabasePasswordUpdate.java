//package com.example.AsmGD1.config;
//
//import com.example.AsmGD1.entity.NguoiDung;
//import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//public class DatabasePasswordUpdate {
//
//    @Autowired
//    private NguoiDungRepository nguoiDungRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    @PostConstruct
//    public void updatePasswords() {
//        List<NguoiDung> nguoiDungs = nguoiDungRepository.findAll();
//        for (NguoiDung nguoiDung : nguoiDungs) {
//            // Kiểm tra nếu mật khẩu không phải dạng BCrypt
//            if (!nguoiDung.getMatKhau().startsWith("$2a$") && !nguoiDung.getMatKhau().startsWith("$2b$")) {
//                nguoiDung.setMatKhau(passwordEncoder.encode(nguoiDung.getMatKhau()));
//                nguoiDungRepository.save(nguoiDung);
//                System.out.println("Mật khẩu cho " + nguoiDung.getTenDangNhap() + " đã được mã hóa lại.");
//            }
//        }
//    }
//}