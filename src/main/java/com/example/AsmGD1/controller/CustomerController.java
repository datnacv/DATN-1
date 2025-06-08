package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping
    public String listCustomers(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "") String keyword,
                                Model model) {
        Page<NguoiDung> customers = nguoiDungService.findUsersByVaiTro("customer", keyword, page, 5);
        model.addAttribute("customers", customers.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customers.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("customer", new NguoiDung());
        return "WebQuanly/list-khach-hang";
    }

    @PostMapping("/add")
    public String addCustomer(@ModelAttribute("customer") NguoiDung customer, RedirectAttributes redirectAttributes) {
        customer.setVaiTro("customer");
        nguoiDungService.save(customer);
        redirectAttributes.addFlashAttribute("message", "Thêm khách hàng thành công!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/customers";
    }

    @PostMapping("/edit")
    public String editCustomer(@ModelAttribute("customer") NguoiDung customer, RedirectAttributes redirectAttributes) {
        NguoiDung existingCustomer = nguoiDungService.findById(customer.getId());
        if (existingCustomer == null) {
            throw new RuntimeException("Không tìm thấy khách hàng với ID: " + customer.getId());
        }
        customer.setVaiTro("customer");
        nguoiDungService.save(customer);
        redirectAttributes.addFlashAttribute("message", "Sửa khách hàng thành công!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/customers";
    }

    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        NguoiDung customer = nguoiDungService.findById(id);
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy khách hàng với ID: " + id);
        }
        nguoiDungService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Xóa khách hàng thành công!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/customers";
    }
}