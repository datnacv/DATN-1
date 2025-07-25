package com.example.AsmGD1.controller.ViThanhToan;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ViThanhToan;
import com.example.AsmGD1.entity.YeuCauRutTien;
import com.example.AsmGD1.repository.ViThanhToan.ViThanhToanRepository;
import com.example.AsmGD1.repository.ViThanhToan.YeuCauRutTienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
public class YeuCauRutTienController {

    @Autowired
    private ViThanhToanRepository viRepo;

    @Autowired
    private YeuCauRutTienRepository rutTienRepo;

    @GetMapping("/rut-tien")
    public String showForm(Model model, @AuthenticationPrincipal NguoiDung user) {
        UUID idNguoiDung = user.getId();
        Optional<ViThanhToan> optionalVi = viRepo.findByIdNguoiDung(idNguoiDung);

        if (optionalVi.isPresent()) {
            ViThanhToan vi = optionalVi.get();
            BigDecimal tongDangCho = rutTienRepo.tongTienDangCho(vi.getId());
            BigDecimal soDuKhaDung = vi.getSoDu().subtract(tongDangCho);

            model.addAttribute("vi", vi);
            model.addAttribute("tongDangCho", tongDangCho);
            model.addAttribute("soDuKhaDung", soDuKhaDung);
        }

        return "WebKhachHang/rut-tien";
    }



    @PostMapping("/rut-tien")
    public String handleRutTien(@RequestParam("soTien") BigDecimal soTien,
                                @RequestParam("soTaiKhoan") String soTaiKhoan,
                                @RequestParam("nguoiThuHuong") String nguoiThuHuong,
                                @RequestParam("tenNganHang") String tenNganHang,
                                Model model,
                                @AuthenticationPrincipal NguoiDung user, RedirectAttributes redirectAttributes) {

        UUID idNguoiDung = user.getId();
        Optional<ViThanhToan> optionalVi = viRepo.findByIdNguoiDung(idNguoiDung);

        if (optionalVi.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy ví.");
            return "redirect:/rut-tien";
        }

        ViThanhToan vi = optionalVi.get();
        model.addAttribute("soDu", vi.getSoDu()); // ✅ Hiển thị số dư dù thành công hay lỗi

        BigDecimal tongDangCho = rutTienRepo.tongTienDangCho(vi.getId());
        BigDecimal soDuKhaDung = vi.getSoDu().subtract(tongDangCho);

        if (soDuKhaDung.compareTo(soTien) < 0) {
            redirectAttributes.addFlashAttribute("error", "Số dư khả dụng không đủ để rút.");
            return "redirect:/rut-tien";
        }


        // Tạo yêu cầu rút tiền
        YeuCauRutTien yeuCau = new YeuCauRutTien();
        yeuCau.setMaGiaoDich("RT" + UUID.randomUUID().toString().substring(0, 8));
        yeuCau.setViThanhToan(vi);
        yeuCau.setSoTien(soTien);
        yeuCau.setTrangThai("Đang chờ");
        yeuCau.setThoiGianYeuCau(LocalDateTime.now());
        yeuCau.setSoTaiKhoan(soTaiKhoan);
        yeuCau.setNguoiThuHuong(nguoiThuHuong);
        yeuCau.setTenNganHang(tenNganHang);
        model.addAttribute("vi", vi);
        model.addAttribute("tongDangCho", tongDangCho);
        model.addAttribute("soDuKhaDung", soDuKhaDung);
        rutTienRepo.save(yeuCau);

        redirectAttributes.addFlashAttribute("success", "Gửi yêu cầu rút tiền thành công. Đang chờ duyệt.");
        return "redirect:/rut-tien";


    }
}
