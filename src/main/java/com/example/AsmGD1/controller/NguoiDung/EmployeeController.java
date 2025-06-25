package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/employees")
public class EmployeeController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping
    public String listEmployees(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "") String keyword,
                                Model model) {
        Page<NguoiDung> employees = nguoiDungService.findUsersByVaiTro("employee", keyword, page, 5);
        model.addAttribute("employees", employees.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", employees.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("employee", new NguoiDung());
        return "WebQuanLy/list-nhan-vien";
    }

    @PostMapping("/add")
    public String addEmployee(@ModelAttribute("employee") NguoiDung employee, RedirectAttributes redirectAttributes) {
        try {
            if (employee.getTrangThai() == null) {
                employee.setTrangThai(true);
            }
            employee.setVaiTro("employee");
            nguoiDungService.save(employee);
            redirectAttributes.addFlashAttribute("message", "Thêm nhân viên thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Thêm nhân viên thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/employees";
    }

    @PostMapping("/edit")
    public String editEmployee(@ModelAttribute("employee") NguoiDung employee, RedirectAttributes redirectAttributes) {
        try {
            if (employee.getTrangThai() == null) {
                employee.setTrangThai(true);
            }
            employee.setVaiTro("employee");
            nguoiDungService.save(employee);
            redirectAttributes.addFlashAttribute("message", "Sửa nhân viên thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Sửa nhân viên thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/employees";
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            nguoiDungService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Xóa nhân viên thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Xóa nhân viên thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/employees";
    }

    @GetMapping("/admin-dashboard")
    public String showAdminDashboard(Model model) {
        model.addAttribute("message", "Chào mừng Admin đến với dashboard!");
        model.addAttribute("messageType", "success");
        return "WebQuanly/admin-dashboard";
    }

    @GetMapping("/employee-dashboard")
    public String showEmployeeDashboard(Model model) {
        model.addAttribute("message", "Chào mừng Nhân viên đến với dashboard!");
        model.addAttribute("messageType", "success");
        return "WebQuanly/employee-dashboard";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "WebQuanly/employee-login"; // Spring Security sẽ xử lý form login
    }
    @PostMapping("/verify-face")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyFace(@RequestBody Map<String, String> payload) {
        String imageBase64 = payload.get("image");

        // Loại bỏ tiền tố base64
        String base64Data = imageBase64.split(",")[1];

        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // ✅ Lưu ảnh vào thư mục src/main/resources/static/images/faces/
            String folderPath = "src/main/resources/static/images/faces/";
            Files.createDirectories(Paths.get(folderPath)); // Tạo folder nếu chưa có

            // ✅ Tạo tên file ảnh với thời gian hiện tại
            String fileName = "face_" + System.currentTimeMillis() + ".jpg";
            Path imagePath = Paths.get(folderPath + fileName);
            Files.write(imagePath, imageBytes);

            // TODO: Gọi AI/so sánh ảnh nếu cần ở đây
            boolean isFaceVerified = true; // Giả lập xác thực thành công

            if (isFaceVerified) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Xác minh thành công",
                        "redirect", "/acvstore/thong-ke"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Xác minh thất bại"
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lưu ảnh"
            ));
        }
    }


    @GetMapping("/verify-face")
    public String showVerifyFacePage() {
        return "WebQuanLy/verify-face";
    }


}
