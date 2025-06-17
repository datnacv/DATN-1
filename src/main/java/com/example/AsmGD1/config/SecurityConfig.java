package com.example.AsmGD1.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // Không mã hóa mật khẩu
    }

    @Bean
    @Order(1)
    public SecurityFilterChain customerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/acvstore/WebKhachHang/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/acvstore/WebKhachHang/index", "/acvstore/login", "/acvstore/register", "/acvstore/oauth2/register").permitAll()
                        .requestMatchers("/acvstore/WebKhachHang/**").hasAuthority("customer")
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/acvstore/login")
                        .loginProcessingUrl("/acvstore/login")
                        .successHandler(customSuccessHandler())
                        .failureUrl("/acvstore/login?error=invalid_credentials")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/acvstore/logout")
                        .logoutSuccessUrl("/acvstore/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendRedirect("/acvstore/access-denied?loginPage=/acvstore/login");
                        })
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .expiredUrl("/acvstore/login?expired=true"));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain adminEmployeeSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/acvstore/admin/**", "/acvstore/WebNhanVien/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/acvstore/login").permitAll()
                        .requestMatchers("/acvstore/admin/**").hasAuthority("admin")
                        .requestMatchers("/acvstore/WebNhanVien/**").hasAuthority("employee")
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/acvstore/login")
                        .loginProcessingUrl("/acvstore/login")
                        .successHandler(customSuccessHandler())
                        .failureUrl("/acvstore/login?error=invalid_credentials")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/acvstore/logout")
                        .logoutSuccessUrl("/acvstore/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendRedirect("/acvstore/access-denied?loginPage=/acvstore/login");
                        })
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .expiredUrl("/acvstore/login?expired=true"));

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/acvstore/register", "/acvstore/oauth2/register", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/acvstore/login")
                        .loginProcessingUrl("/acvstore/login")
                        .successHandler(customSuccessHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/acvstore/logout")
                        .logoutSuccessUrl("/acvstore/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendRedirect("/acvstore/access-denied?loginPage=/acvstore/login");
                        })
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .expiredUrl("/acvstore/login?expired=true"));

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException {
                boolean hasRole = false;
                if (authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("customer"))) {
                    response.sendRedirect("/acvstore/WebKhachHang/index");
                    hasRole = true;
                }
                if (authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("admin"))) {
                    response.sendRedirect("/acvstore/admin/dashboard");
                    hasRole = true;
                }
                if (authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("employee"))) {
                    response.sendRedirect("/acvstore/WebNhanVien/dashboard");
                    hasRole = true;
                }
                if (!hasRole) {
                    SecurityContextHolder.clearContext();
                    response.sendRedirect("/acvstore/login?error=invalid_role");
                }
            }
        };
    }
}