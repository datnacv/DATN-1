package com.example.AsmGD1.controller.ThongBao;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/thong-bao")
public class ThongBaoController {

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @GetMapping("/xem")
    public String hienThiThongBao(Model model, Principal principal) {
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());

        if (optionalNguoiDung.isPresent()) {
            NguoiDung nguoiDung = optionalNguoiDung.get();
            model.addAttribute("notifications", thongBaoService.layThongBaoTheoNguoiDung(nguoiDung.getId()));
            model.addAttribute("unreadCount", thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId()));
            model.addAttribute("user", nguoiDung);
        } else {
            model.addAttribute("notifications", null);
            model.addAttribute("unreadCount", 0);
            model.addAttribute("user", null);
        }

        return "fragments/accountDropdown :: accountDropdown";
    }

    @PostMapping("/danh-dau-da-xem")
    @ResponseBody
    public ResponseEntity<?> danhDauThongBaoDaXem(@RequestParam UUID idThongBao, Principal principal) {
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());

        if (optionalNguoiDung.isPresent()) {
            NguoiDung nguoiDung = optionalNguoiDung.get();
            thongBaoService.danhDauDaXem(idThongBao, nguoiDung.getId());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Không tìm thấy người dùng.");
        }
    }
}
