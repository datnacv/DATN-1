

package com.example.AsmGD1.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // ✅ Đảm bảo Authentication được set vào context
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // ✅ Lưu SecurityContext vào session để giữ sau redirect
        HttpSession session = request.getSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);

        // ✅ Kiểm tra pendingUser
        if (session.getAttribute("pendingUser") != null) {
            response.sendRedirect("/customers/oauth2/register");
        } else {
            response.sendRedirect("/cart");
        }
    }
}
