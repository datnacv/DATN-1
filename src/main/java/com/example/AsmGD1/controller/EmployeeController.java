package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/employees")
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
        return "WebQuanly/employee/list";
    }

    @PostMapping("/add")
    public String addEmployee(@ModelAttribute("employee") NguoiDung employee) {
        employee.setVaiTro("employee");
        nguoiDungService.save(employee);
        return "redirect:/employees";
    }

    @PostMapping("/edit")
    public String editEmployee(@ModelAttribute("employee") NguoiDung employee) {
        NguoiDung existingEmployee = nguoiDungService.findById(employee.getId());
        if (existingEmployee == null) {
            throw new RuntimeException("Không tìm thấy nhân viên với ID: " + employee.getId());
        }
        employee.setVaiTro("employee");
        nguoiDungService.save(employee);
        return "redirect:/employees";
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable UUID id) {
        NguoiDung employee = nguoiDungService.findById(id);
        if (employee == null) {
            throw new RuntimeException("Không tìm thấy nhân viên với ID: " + id);
        }
        nguoiDungService.deleteById(id);
        return "redirect:/employees";
    }
}