//package com.example.AsmGD1.config;
//
//import com.example.AsmGD1.entity.NguoiDung;
//import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ModelAttribute;
//
//@ControllerAdvice
//public class GlobalUserAdvice {
//
//    @Autowired
//    private NguoiDungService nguoiDungService;
//
//    @ModelAttribute
//    public void addUserToModel(Model model) {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
//            String tenDangNhap = auth.getName(); // Lấy ten_dang_nhap từ Spring Security
//            NguoiDung nguoiDung = nguoiDungService.findByTenDangNhap(tenDangNhap); // Sử dụng findByTenDangNhap
//
//            System.out.println("✅ NGUOI DUNG LOADED: " + nguoiDung);
//            model.addAttribute("nguoiDung", nguoiDung); // Thay đổi attribute thành "nguoiDung"
//        }
//    }
//}