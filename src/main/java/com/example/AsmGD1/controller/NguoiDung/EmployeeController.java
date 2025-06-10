package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        return "WebQuanly/list-nhan-vien";
    }

    @PostMapping("/add")
    public String addEmployee(@ModelAttribute("employee") NguoiDung employee, RedirectAttributes redirectAttributes) {
        try {
            if (employee.getTrangThai() == null) {
                employee.setTrangThai(true); // Giá trị mặc định
            }
            employee.setVaiTro("employee");
            nguoiDungService.save(employee);
            redirectAttributes.addFlashAttribute("message", "Thêm nhân viên thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Thêm nhân viên thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/employees";
    }

    @PostMapping("/edit")
    public String editEmployee(@ModelAttribute("employee") NguoiDung employee, RedirectAttributes redirectAttributes) {
        NguoiDung existingEmployee = nguoiDungService.findById(employee.getId());
        if (existingEmployee == null) {
            redirectAttributes.addFlashAttribute("message", "Không tìm thấy nhân viên với ID: " + employee.getId());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/employees";
        }
        try {
            if (employee.getTrangThai() == null) {
                employee.setTrangThai(true); // Giá trị mặc định
            }
            employee.setVaiTro("employee");
            nguoiDungService.save(employee);
            redirectAttributes.addFlashAttribute("message", "Sửa nhân viên thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Sửa nhân viên thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/employees";
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        NguoiDung employee = nguoiDungService.findById(id);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("message", "Không tìm thấy nhân viên với ID: " + id);
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/employees";
        }
        try {
            nguoiDungService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Xóa nhân viên thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Xóa nhân viên thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/employees";
    }
}