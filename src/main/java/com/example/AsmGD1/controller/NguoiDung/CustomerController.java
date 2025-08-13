package com.example.AsmGD1.controller.NguoiDung;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.DiaChiKhachHangService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/customers")
public class CustomerController {

    @Autowired
    private NguoiDungService nguoiDungService;
    @Autowired
    private DiaChiKhachHangService diaChiKhachHangService;

    // Helper method
    // ể kiểm tra quyền admin
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung currentUser = (NguoiDung) auth.getPrincipal();
            return "ADMIN".equalsIgnoreCase(currentUser.getVaiTro());
        }
        return false;
    }

    // Helper method để kiểm tra quyền employee
    private boolean isCurrentUserEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            return "EMPLOYEE".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    // Helper method để kiểm tra quyền admin hoặc employee
    private boolean isCurrentUserAdminOrEmployee() {
        return isCurrentUserAdmin() || isCurrentUserEmployee();
    }

    // Helper method để thêm thông tin user vào model
    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
            model.addAttribute("currentUser", user);
            model.addAttribute("isAdmin", "ADMIN".equalsIgnoreCase(user.getVaiTro()));
            model.addAttribute("isEmployee", "EMPLOYEE".equalsIgnoreCase(user.getVaiTro()));
        } else {
            List<NguoiDung> admins = nguoiDungService.findUsersByVaiTro("ADMIN", "", 0, 1).getContent();
            NguoiDung defaultUser = admins.isEmpty() ? new NguoiDung() : admins.get(0);
            model.addAttribute("user", defaultUser);
            model.addAttribute("currentUser", defaultUser);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isEmployee", false);
        }
    }

    @GetMapping
    public String listCustomers(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "") String keyword,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        // Kiểm tra quyền truy cập - cả admin và employee đều có thể xem
        if (!isCurrentUserAdminOrEmployee()) {
            redirectAttributes.addFlashAttribute("message", "Bạn không có quyền truy cập chức năng này!");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/acvstore/thong-ke";
        }

        // Thêm thông tin user và quyền vào model
        addUserInfoToModel(model);

        Page<NguoiDung> customers = nguoiDungService.findUsersByVaiTro("customer", keyword, page, 5);
        model.addAttribute("customers", customers.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customers.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("customer", new NguoiDung());

        return "WebQuanLy/list-khach-hang";
    }

    @PostMapping("/add")
    public String addCustomer(@ModelAttribute("customer") NguoiDung customer,
                              RedirectAttributes redirectAttributes,
                              Authentication authentication) {
        if (!isCurrentUserAdminOrEmployee()) {
            redirectAttributes.addFlashAttribute("message", "Bạn không có quyền thêm khách hàng!");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/acvstore/customers";
        }

        try {
            // ⬇️ Đổi dòng dưới đây
            // nguoiDungService.save(customer);
            diaChiKhachHangService.saveCustomerWithDefaultAddress(customer);

            redirectAttributes.addFlashAttribute("message", "Thêm khách hàng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Thêm khách hàng thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/customers";
    }


    @PostMapping("/edit")
    public String editCustomer(@ModelAttribute("customer") NguoiDung customer,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        if (!isCurrentUserAdminOrEmployee()) {
            redirectAttributes.addFlashAttribute("message", "Bạn không có quyền sửa khách hàng!");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/acvstore/customers";
        }

        try {
            // ⬇️ Đổi dòng dưới đây
            // nguoiDungService.save(customer);
            diaChiKhachHangService.updateCustomerAndAppendAddress(customer);

            redirectAttributes.addFlashAttribute("message", "Sửa khách hàng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Sửa thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/customers";
    }




    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable UUID id,
                                 RedirectAttributes redirectAttributes,
                                 Authentication authentication) {
        // Kiểm tra quyền admin hoặc employee
        if (!isCurrentUserAdminOrEmployee()) {
            redirectAttributes.addFlashAttribute("message", "Bạn không có quyền xóa khách hàng!");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/acvstore/customers";
        }

        try {
            nguoiDungService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Xóa khách hàng thành công!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Xóa thất bại: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/acvstore/customers";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        addUserInfoToModel(model);
        model.addAttribute("message", "Chào mừng đến với dashboard!");
        model.addAttribute("messageType", "success");
        return "WebQuanLy/customer-dashboard";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "WebQuanLy/customer-login";
    }

    @GetMapping("/by-phone/{phone}")
    @ResponseBody
    public ResponseEntity<?> getCustomerByPhone(@PathVariable String phone) {
        NguoiDung customer = nguoiDungService.findBySoDienThoai(phone);
        if (customer == null || !"CUSTOMER".equalsIgnoreCase(customer.getVaiTro())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(new SimpleCustomerDTO(
                customer.getId(),
                customer.getHoTen(),
                customer.getEmail()
        ));
    }

    record SimpleCustomerDTO(UUID id, String hoTen, String email) {}

}
