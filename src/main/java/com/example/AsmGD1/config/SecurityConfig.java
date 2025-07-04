package com.example.AsmGD1.config;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.CustomUserDetailsService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.PrintWriter;

@Configuration
public class SecurityConfig {

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandlerEmployees() {
        AccessDeniedHandlerImpl handler = new AccessDeniedHandlerImpl();
        handler.setErrorPage("/acvstore/login?error=accessDenied");
        return handler;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandlerCustomers() {
        AccessDeniedHandlerImpl handler = new AccessDeniedHandlerImpl();
        handler.setErrorPage("/customers/login?error=accessDenied");
        return handler;
    }

    @Bean
    public AuthenticationEntryPoint employeeAuthEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect("/acvstore/login");
        };
    }

    @Bean
    public AuthenticationEntryPoint customerAuthEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect("/customers/login");
        };
    }

    // AuthenticationEntryPoint mặc định cho các đường dẫn khác
    @Bean
    public AuthenticationEntryPoint defaultAuthEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect("/customers/login");
        };
    }
    // ✅ BỔ SUNG MỚI: JSON entry point cho các API (ví dụ /verify-success)
    @Bean
    public AuthenticationEntryPoint jsonAuthEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"success\": false, \"message\": \"Unauthorized\"}");
            writer.flush();
        };
    }


    @Bean
    public SecurityFilterChain employeeSecurityFilterChain(HttpSecurity http,
                                                           CustomerAccessBlockFilter blockFilter,
                                                           FaceVerificationFilter faceVerificationFilter,
                                                           NguoiDungService nguoiDungService) throws Exception {
        http
                .securityMatcher("/acvstore/**")
                .addFilterBefore(blockFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(faceVerificationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/acvstore/login", "/acvstore/register-face", "/acvstore/verify-face").permitAll()
                        .requestMatchers("/acvstore/verify-success").authenticated()
                        .requestMatchers("/acvstore/**").hasRole("ADMIN")
                        .requestMatchers("/acvstore/employee-dashboard").hasRole("EMPLOYEE")
                        .requestMatchers("/acvstore/admin-dashboard").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/acvstore/login")
                        .loginProcessingUrl("/acvstore/login")
                        .successHandler((request, response, authentication) -> {
                            String tenDangNhap = authentication.getName();
                            NguoiDung nguoiDung = nguoiDungService.findByTenDangNhap(tenDangNhap);
                            request.getSession().setAttribute("faceVerified", false);

                            if (nguoiDung != null) {
                                byte[] descriptor = nguoiDung.getFaceDescriptor();
                                if (descriptor == null || descriptor.length == 0) {
                                    response.sendRedirect("/acvstore/register-face");
                                } else {
                                    response.sendRedirect("/acvstore/verify-face");
                                }
                            } else {
                                response.sendRedirect("/acvstore/login?error=notfound");
                            }
                        })
                        .failureUrl("/acvstore/login?error=invalidCredentials")
                        .usernameParameter("tenDangNhap")
                        .passwordParameter("matKhau")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/acvstore/logout")
                        .logoutSuccessUrl("/acvstore/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                jsonAuthEntryPoint(),
                                new AntPathRequestMatcher("/acvstore/verify-success", "POST")
                        )
                        // Dành cho các route còn lại của /acvstore/**
                        .defaultAuthenticationEntryPointFor(
                                employeeAuthEntryPoint(),
                                new AntPathRequestMatcher("/acvstore/**")
                        )
                        .accessDeniedHandler(accessDeniedHandlerEmployees())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredUrl("/acvstore/login?expired")
                        )
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/acvstore/login?invalid")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public SecurityFilterChain customerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/customers/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/customers/login").permitAll()
                        .anyRequest().hasRole("CUSTOMER")
                )
                .formLogin(form -> form
                        .loginPage("/customers/login")
                        .loginProcessingUrl("/customers/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/customers/login?error=invalidCredentials")
                        .usernameParameter("tenDangNhap")
                        .passwordParameter("matKhau")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/customers/logout")
                        .logoutSuccessUrl("/customers/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customerAuthEntryPoint())
                        .accessDeniedHandler(accessDeniedHandlerCustomers())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredUrl("/customers/login?expired")
                        )
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/customers/login?invalid")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/acvstore/login", "/acvstore/verify-face", "/acvstore/login", "/customers/login", "/api/cart/check-auth", "/api/cart/get-user").permitAll()
                        .requestMatchers("/cart", "/api/cart/**").authenticated()
                        .requestMatchers("/acvstore/login", "/acvstore/verify-face").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/customers/login")
                        .loginProcessingUrl("/customers/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/customers/login?error=invalidCredentials")
                        .usernameParameter("tenDangNhap")
                        .passwordParameter("matKhau")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/customers/logout")
                        .logoutSuccessUrl("/customers/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(defaultAuthEntryPoint())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredUrl("/customers/login?expired")
                        )
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/customers/login?invalid")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService) {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // ⚠️ Dùng BCrypt trong môi trường thực tế
    }
}
