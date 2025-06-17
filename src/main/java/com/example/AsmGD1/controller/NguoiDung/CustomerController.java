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
@RequestMapping("/acvstore/customers")
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

    @GetMapping("/index")
    public String customerIndex(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "") String keyword,
                                Model model) {
        // Gửi danh sách khách hàng làm trang chính cho vai trò customer
        Page<NguoiDung> customers = nguoiDungService.findUsersByVaiTro("customer", keyword, page, 5);
        model.addAttribute("customers", customers.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customers.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("customer", new NguoiDung());
        return "WebQuanly/list-khach-hang"; // Sử dụng cùng template với danh sách khách hàng
    }

    @PostMapping("/add")
    public String addCustomer(@ModelAttribute("customer") NguoiDung customer, RedirectAttributes redirectAttributes) {
        try {
            customer.setVaiTro("customer");
            nguoiDungService.save(customer);
            redirectAttributes.addFlashAttribute("message", "Thêm khách hàng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Thêm khách hàng thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/acvstore/customers";
    }

    @PostMapping("/edit")
    public String editCustomer(@ModelAttribute("customer") NguoiDung customer, RedirectAttributes redirectAttributes) {
        try {
            customer.setVaiTro("customer");
            nguoiDungService.save(customer);
            redirectAttributes.addFlashAttribute("message", "Sửa khách hàng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Sửa khách hàng thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/acvstore/customers";
    }

    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            nguoiDungService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Xóa khách hàng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Xóa khách hàng thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/acvstore/customers";
    }
}