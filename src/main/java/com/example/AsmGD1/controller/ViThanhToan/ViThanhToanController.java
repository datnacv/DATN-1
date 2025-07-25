package com.example.AsmGD1.controller.ViThanhToan;

import com.example.AsmGD1.entity.LichSuGiaoDichVi;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ViThanhToan;
import com.example.AsmGD1.repository.ViThanhToan.LichSuGiaoDichViRepository;
import com.example.AsmGD1.service.ViThanhToan.ViThanhToanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/vi")
public class ViThanhToanController {

    @Autowired
    private ViThanhToanService viService;

    @Autowired
    private LichSuGiaoDichViRepository lichSuRepo;

    // Trang xem ví
    @GetMapping
    public String xemVi(Model model, Authentication authentication) {
        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        UUID idNguoiDung = currentUser.getId();

        ViThanhToan vi = viService.findByUser(idNguoiDung);
        model.addAttribute("user", currentUser);
        model.addAttribute("vi", vi);

        if (vi != null) {
            BigDecimal tongDangCho = viService.tongTienDangCho(vi.getId());
            BigDecimal soDuKhaDung = vi.getSoDu().subtract(tongDangCho);

            model.addAttribute("soDuKhaDung", soDuKhaDung);
            model.addAttribute("tongDangCho", tongDangCho);
        }

        return "WebKhachHang/xem_vi";
    }


    // POST tạo ví nếu chưa có
    @PostMapping("/tao-vi")
    public String taoVi(Authentication authentication, RedirectAttributes attrs) {
        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        UUID idNguoiDung = currentUser.getId();

        ViThanhToan vi = viService.findByUser(idNguoiDung);
        if (vi == null) {
            viService.taoViMoi(idNguoiDung);
            attrs.addFlashAttribute("msg", "Tạo ví mới thành công!");
        } else {
            attrs.addFlashAttribute("msg", "Bạn đã có ví.");
        }

        return "redirect:/vi";
    }

    // POST nạp tiền
    @PostMapping("/nap-tien")
    public String napTien(
            @RequestParam BigDecimal soTien,
            Authentication authentication,
            RedirectAttributes attrs) {

        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        UUID idNguoiDung = currentUser.getId();

        viService.napTien(idNguoiDung, soTien);
        attrs.addFlashAttribute("msg", "Nạp tiền thành công");
        return "redirect:/vi";
    }

    // GET xem lịch sử ví
    @GetMapping("/lich-su")
    public String lichSuVi(Model model, Authentication authentication) {
        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        UUID idNguoiDung = currentUser.getId();

        ViThanhToan vi = viService.findByUser(idNguoiDung);

        // Nếu chưa có ví thì chuyển hưởng tạo ví
        if (vi == null) {
            model.addAttribute("msg", "Bạn chưa có ví để xem lịch sử.");
            return "redirect:/vi";
        }

        List<LichSuGiaoDichVi> lichSu = lichSuRepo.findByIdViThanhToanOrderByCreatedAtDesc(vi.getId());
        model.addAttribute("lichSu", lichSu);
        model.addAttribute("user", currentUser);
        return "WebKhachHang/lich_su";
    }

}
