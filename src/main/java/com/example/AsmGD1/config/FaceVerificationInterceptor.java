package com.example.AsmGD1.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class FaceVerificationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        String path = request.getRequestURI();
        System.out.println("Intercepted path: " + path); // Log để debug

        // Không chặn các trang login, verify, logout, và register-face
        if (path.startsWith("/acvstore/login") || path.startsWith("/acvstore/verify-face") ||
                path.startsWith("/logout") || path.startsWith("/acvstore/employees/register-face")) {
            System.out.println("Path allowed without verification: " + path);
            return true;
        }

        // Kiểm tra vai trò ADMIN và trạng thái xác minh khuôn mặt
        if (session != null && "ADMIN".equals(session.getAttribute("ROLE"))) {
            Boolean verified = (Boolean) session.getAttribute("faceVerified");
            System.out.println("Face verified status for ADMIN: " + verified);
            if (verified == null || !verified) {
                System.out.println("Redirecting to face verification for user: " + session.getAttribute("username"));
                response.sendRedirect("/acvstore/employees/verify-face");
                return false;
            }
        }

        return true;
    }
}