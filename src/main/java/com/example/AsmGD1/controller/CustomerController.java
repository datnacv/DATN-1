package com.example.AsmGD1.controller;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
        return "WebQuanly/customer/list";
    }

    @PostMapping("/add")
    public String addCustomer(@ModelAttribute("customer") NguoiDung customer) {
        customer.setVaiTro("customer");
        nguoiDungService.save(customer);
        return "redirect:/customers";
    }

    @PostMapping("/edit")
    public String editCustomer(
            @RequestParam("id") UUID id,
            @RequestParam("hoTen") String hoTen,
            @RequestParam("email") String email,
            @RequestParam("soDienThoai") String soDienThoai,
            @RequestParam("tenDangNhap") String tenDangNhap,
            @RequestParam("matKhau") String matKhau,
            @RequestParam(value = "ngaySinh", required = false) LocalDate ngaySinh,
            @RequestParam("gioiTinh") Boolean gioiTinh,
            @RequestParam(value = "diaChi", required = false) String diaChi) {
        NguoiDung customer = nguoiDungService.findById(id);
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy khách hàng với ID: " + id);
        }
        customer.setHoTen(hoTen);
        customer.setEmail(email);
        customer.setSoDienThoai(soDienThoai);
        customer.setTenDangNhap(tenDangNhap);
        customer.setMatKhau(matKhau);
        customer.setNgaySinh(ngaySinh);
        customer.setGioiTinh(gioiTinh);
        customer.setDiaChi(diaChi);
        customer.setVaiTro("customer");
        nguoiDungService.save(customer);
        return "redirect:/customers";
    }

    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable UUID id) {
        NguoiDung customer = nguoiDungService.findById(id);
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy khách hàng với ID: " + id);
        }
        nguoiDungService.deleteById(id);
        return "redirect:/customers";
    }
}