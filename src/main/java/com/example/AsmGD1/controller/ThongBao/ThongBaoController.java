package com.example.AsmGD1.controller.ThongBao;

import com.example.AsmGD1.dto.ThongBao.ThongBaoDTO;
import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ThongBao.ChiTietThongBaoNhomRepository;
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

    @Autowired
    private ChiTietThongBaoNhomRepository chiTietThongBaoNhomRepository;

    @GetMapping("/load")
    @ResponseBody
    public ResponseEntity<?> taiDanhSachThongBao(
            Authentication authentication,
            @RequestParam(value = "unread", defaultValue = "false") boolean unread,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Người dùng chưa đăng nhập");
        }

        String username = authentication.getName();
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(username);
        if (!optionalNguoiDung.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Không tìm thấy người dùng");
        }

        NguoiDung user = optionalNguoiDung.get();
        List<ChiTietThongBaoNhom> danhSach;
        if (unread) {
            danhSach = thongBaoService.lay5ThongBaoChuaXem(user.getId());
        } else {
            danhSach = thongBaoService.lay5ThongBaoMoiNhat(user.getId());
        }

        List<ThongBaoDTO> dtoList = danhSach.stream()
                .map(ThongBaoDTO::new)
                .limit(5)
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
            @RequestParam(defaultValue = "") String status,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        Map<String, Object> responseMap = new HashMap<>();
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());

        if (optionalNguoiDung.isPresent()) {
            NguoiDung nguoiDung = optionalNguoiDung.get();
            List<ChiTietThongBaoNhom> notifications = thongBaoService.layThongBaoTheoNguoiDungVaTrangThai(nguoiDung.getId(), page, size, status);
            long totalCount = thongBaoService.demTongSoThongBaoTheoTrangThai(nguoiDung.getId(), status);

            List<ThongBaoDTO> dtoList = notifications.stream()
                    .map(ThongBaoDTO::new)
                    .collect(Collectors.toList());

            responseMap.put("notifications", dtoList);
            responseMap.put("unreadCount", thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId()));
            responseMap.put("totalCount", totalCount);
            responseMap.put("user", nguoiDung);
            return ResponseEntity.ok(responseMap);
        } else {
            responseMap.put("notifications", null);
            responseMap.put("unreadCount", 0);
            responseMap.put("totalCount", 0);
            responseMap.put("user", null);
            return ResponseEntity.ok(responseMap);
        }
    }

    @PostMapping("/danh-dau-da-xem")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> danhDauThongBaoDaXem(@RequestParam UUID idChiTietThongBao, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("Nhận yêu cầu đánh dấu đã đọc - idChiTietThongBao: " + idChiTietThongBao + ", Principal: " + (principal != null ? principal.getName() : "null"));
            if (principal == null) {
                response.put("error", "Người dùng chưa đăng nhập.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (!optionalNguoiDung.isPresent()) {
                System.out.println("Không tìm thấy người dùng với tên đăng nhập: " + principal.getName());
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            NguoiDung nguoiDung = optionalNguoiDung.get();
            System.out.println("Người dùng tìm thấy: " + nguoiDung.getTenDangNhap() + ", id: " + nguoiDung.getId());
            thongBaoService.danhDauDaXem(idChiTietThongBao, nguoiDung.getId());
            long unreadCount = thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId());
            System.out.println("Số thông báo chưa đọc sau khi cập nhật: " + unreadCount);
            response.put("unreadCount", unreadCount);
            response.put("message", "Đã đánh dấu là đã đọc thành công");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.err.println("Lỗi khi đánh dấu đã đọc: " + e.getMessage());
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            System.err.println("Lỗi server khi đánh dấu đã đọc: " + e.getMessage());
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
            response.put("message", "Đã đánh dấu tất cả thông báo là đã đọc thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/debug/thong-bao")
    @ResponseBody
    public List<ChiTietThongBaoNhom> debugThongBao(Principal principal) {
        Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
        if (optionalNguoiDung.isPresent()) {
            return chiTietThongBaoNhomRepository.findByNguoiDungIdOrderByThongBaoNhom_ThoiGianTaoDesc(optionalNguoiDung.get().getId());
        }
        return List.of();
    }

    @PostMapping("/danh-dau-chua-xem")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> danhDauChuaXem(@RequestParam UUID idChiTietThongBao, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("error", "Người dùng chưa đăng nhập.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<NguoiDung> optionalNguoiDung = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (!optionalNguoiDung.isPresent()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            NguoiDung nguoiDung = optionalNguoiDung.get();
            thongBaoService.danhDauChuaXem(idChiTietThongBao, nguoiDung.getId());
            long unreadCount = thongBaoService.demSoThongBaoChuaXem(nguoiDung.getId());
            response.put("unreadCount", unreadCount);
            response.put("message", "Đã đánh dấu là chưa xem thành công");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/xoa")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaMotThongBao(@RequestParam UUID idChiTietThongBao, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("error", "Người dùng chưa đăng nhập.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (nguoiDungOpt.isEmpty()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            thongBaoService.xoaThongBao(idChiTietThongBao, nguoiDungOpt.get().getId());
            response.put("message", "Đã xóa thông báo.");
            response.put("unreadCount", thongBaoService.demSoThongBaoChuaXem(nguoiDungOpt.get().getId()));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/xoa-tat-ca")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaTatCaThongBao(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("error", "Người dùng chưa đăng nhập.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findByTenDangNhap(principal.getName());
            if (nguoiDungOpt.isEmpty()) {
                response.put("error", "Không tìm thấy người dùng.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            thongBaoService.xoaTatCaThongBao(nguoiDungOpt.get().getId());
            response.put("message", "Đã xóa toàn bộ thông báo.");
            response.put("unreadCount", 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}