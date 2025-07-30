package com.example.AsmGD1.controller.ThongBao;

import com.example.AsmGD1.dto.ThongBao.ThongBaoDTO;
import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/thong-bao")
public class ThongBaoController {

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private NguoiDungService nguoiDungService;
    @GetMapping("/load")
    @ResponseBody
    public ResponseEntity<?> taiDanhSachThongBao(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof NguoiDung)) {
            return ResponseEntity.status(401).body("Người dùng chưa đăng nhập");
        }

        NguoiDung user = (NguoiDung) authentication.getPrincipal();
        List<ChiTietThongBaoNhom> danhSach = thongBaoService.layThongBaoTheoNguoiDung(user.getId());

        List<ThongBaoDTO> dtoList = danhSach.stream()
                .map(ThongBaoDTO::new)
                .collect(Collectors.toList());

        long soChuaDoc = danhSach.stream().filter(tb -> !tb.isDaXem()).count();

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", dtoList);
        response.put("unreadCount", soChuaDoc);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/xem")
    @ResponseBody
    public ResponseEntity<?> hienThiThongBao(Principal principal) {
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
        Map<String, Object> response = new HashMap<>();

        if (optionalNguoiDung.isPresent()) {
            NguoiDung nguoiDung = optionalNguoiDung.get();
            response.put("notifications", thongBaoService.layThongBaoTheoNguoiDung(nguoiDung.getId()));
            response.put("unreadCount", thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId()));
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            NguoiDung user = (auth != null && auth.getPrincipal() instanceof NguoiDung) ? (NguoiDung) auth.getPrincipal() : null;
            response.put("user", user != null ? user : new NguoiDung());
            return ResponseEntity.ok(response);
        } else {
            response.put("notifications", null);
            response.put("unreadCount", 0);
            response.put("user", null);
            return ResponseEntity.ok(response);
        }
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

    @PostMapping("/danh-dau-tat-ca")
    @ResponseBody
    public ResponseEntity<?> danhDauTatCaThongBaoDaXem(Principal principal) {
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());

        if (optionalNguoiDung.isPresent()) {
            NguoiDung nguoiDung = optionalNguoiDung.get();
            thongBaoService.danhDauTatCaDaXem(nguoiDung.getId());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Không tìm thấy người dùng.");
        }
    }
}