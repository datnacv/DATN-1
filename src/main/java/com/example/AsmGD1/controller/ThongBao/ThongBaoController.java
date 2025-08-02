package com.example.AsmGD1.controller.ThongBao;

import com.example.AsmGD1.dto.ThongBao.ThongBaoDTO;
import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<?> taiDanhSachThongBao(Authentication authentication, HttpServletResponse response) {
        // Ngăn cache
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Người dùng chưa đăng nhập");
        }

        String username = authentication.getName();
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(username);
        if (!optionalNguoiDung.isPresent()) {
            return ResponseEntity.status(401).body("Không tìm thấy người dùng");
        }

        NguoiDung user = optionalNguoiDung.get();
        List<ChiTietThongBaoNhom> danhSach = thongBaoService.lay5ThongBaoMoiNhat(user.getId());

        List<ThongBaoDTO> dtoList = danhSach.stream()
                .map(ThongBaoDTO::new)
                .limit(5) // Đảm bảo chỉ lấy 5 thông báo
                .collect(Collectors.toList());

        long soChuaDoc = thongBaoService.demSoThongBaoChuaXem(user.getId());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("notifications", dtoList);
        responseMap.put("unreadCount", soChuaDoc);

        return ResponseEntity.ok(responseMap);
    }

    @GetMapping("/xem")
    @ResponseBody
    public ResponseEntity<?> hienThiThongBao(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            HttpServletResponse response) {

        // Ngăn cache
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        Map<String, Object> responseMap = new HashMap<>();
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());

        if (optionalNguoiDung.isPresent()) {
            NguoiDung nguoiDung = optionalNguoiDung.get();
            List<ChiTietThongBaoNhom> notifications = thongBaoService.layThongBaoTheoNguoiDung(nguoiDung.getId(), page, size);
            long totalCount = thongBaoService.demTongSoThongBao(nguoiDung.getId()); // thêm dòng này

            List<ThongBaoDTO> dtoList = notifications.stream()
                    .map(ThongBaoDTO::new)
                    .collect(Collectors.toList());

            responseMap.put("notifications", dtoList);
            responseMap.put("unreadCount", thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId()));
            responseMap.put("totalCount", totalCount); // thêm dòng này
            responseMap.put("user", nguoiDung);
            return ResponseEntity.ok(responseMap);
        } else {
            responseMap.put("notifications", null);
            responseMap.put("unreadCount", 0);
            responseMap.put("totalCount", 0); // thêm dòng này
            responseMap.put("user", null);
            return ResponseEntity.ok(responseMap);
        }
    }


    @PostMapping("/danh-dau-da-xem")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> danhDauThongBaoDaXem(@RequestParam UUID idThongBao, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (!optionalNguoiDung.isPresent()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            NguoiDung nguoiDung = optionalNguoiDung.get();
            thongBaoService.danhDauDaXem(idThongBao, nguoiDung.getId());
            long unreadCount = thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId());
            response.put("unreadCount", unreadCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/danh-dau-tat-ca")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> danhDauTatCaThongBaoDaXem(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (!optionalNguoiDung.isPresent()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            NguoiDung nguoiDung = optionalNguoiDung.get();
            thongBaoService.danhDauTatCaDaXem(nguoiDung.getId());
            long unreadCount = thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId());
            response.put("unreadCount", unreadCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}