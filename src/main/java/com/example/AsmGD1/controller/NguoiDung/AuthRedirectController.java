package com.example.AsmGD1.controller.NguoiDung;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthRedirectController {

    @GetMapping("/acvstore/employees/redirect")
    public String redirectAfterLogin(Authentication auth) {
        if (auth == null || auth.getAuthorities().isEmpty()) {
            return "redirect:/acvstore/employees/login?error=unauthorized";
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isEmployee = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        // ❌ Nếu login bằng customer → không cho truy cập
        if (!isAdmin && !isEmployee) {
            SecurityContextHolder.clearContext(); // clear phiên đăng nhập
            return "redirect:/acvstore/employees/login?error=accessDenied";
        }

        if (isAdmin) {
            return "redirect:/acvstore/employees/admin-dashboard";
        } else {
            return "redirect:/acvstore/employees/employee-dashboard";
        }
    }
}
