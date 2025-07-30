package com.example.AsmGD1.controller.ThongBao;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ControllerAdvice
public class ThongBaoAdvice {

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @ModelAttribute
    public void thongBaoChoTatCaTrang(Model model, Principal principal) {
        try {
            if (principal != null) {
                Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findByTenDangNhap(principal.getName());

                if (nguoiDungOpt.isPresent()) {
                    NguoiDung nguoiDung = nguoiDungOpt.get();
                    System.out.println("Logged in user: " + nguoiDung.getTenDangNhap());

                    model.addAttribute("user", nguoiDung);
                    model.addAttribute("unreadCount", thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId()));
                    model.addAttribute("notifications", thongBaoService.layThongBaoTheoNguoiDung(nguoiDung.getId()));
                } else {
                    System.out.println("Không tìm thấy người dùng!");
                    model.addAttribute("user", null);
                    model.addAttribute("unreadCount", 0);
                    model.addAttribute("notifications", List.of());
                }
            } else {
                System.out.println("Principal null");
                model.addAttribute("user", null);
                model.addAttribute("unreadCount", 0);
                model.addAttribute("notifications", List.of());
            }
        } catch (Exception e) {
            e.printStackTrace(); // log lỗi ra console
            model.addAttribute("user", null);
            model.addAttribute("unreadCount", 0);
            model.addAttribute("notifications", List.of());
        }
    }


}
