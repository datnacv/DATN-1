package com.example.AsmGD1.config;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.CustomOAuth2UserService;
import com.example.AsmGD1.service.NguoiDung.CustomUserDetailsService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.PrintWriter;

@Configuration
public class SecurityConfig implements ApplicationContextAware {

    private final CustomUserDetailsService customUserDetailsService;
    private final HttpSession session;
    private ApplicationContext applicationContext;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService, HttpSession session) {
        this.customUserDetailsService = customUserDetailsService;
        this.session = session;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

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
            System.out.println("Đang chuyển hướng đến /acvstore/login do: " + authException.getMessage());
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect("/acvstore/login");
        };
    }

    @Bean
    public AuthenticationEntryPoint customerAuthEntryPoint() {
        return (request, response, authException) -> {
            System.out.println("Đang chuyển hướng đến /customers/login do: " + authException.getMessage());
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect("/customers/login");
        };
    }

    @Bean
    public AuthenticationEntryPoint defaultAuthEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect("/customers/login");
        };
    }

    @Bean
    public AuthenticationEntryPoint jsonAuthEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"success\": false, \"message\": \"Không được phép\"}");
            writer.flush();
        };
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/search", "/api/product/**", "/api/cart/**", "/api/product/*/ratings", "/api/getChiTietSanPham", "/api/cart/check-auth", "/api/cart/get-user", "/api/san-pham/with-chi-tiet", "/api/san-pham/ban-chay", "/api/san-pham/moi-nhat", "/api/product/*/ratings", "/api/getChiTietSanPham").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jsonAuthEntryPoint())
                );
        return http.build();
    }

    @Bean
    public SecurityFilterChain employeeSecurityFilterChain(HttpSecurity http,
                                                           CustomerAccessBlockFilter blockFilter,
                                                           FaceVerificationFilter faceVerificationFilter) throws Exception {
        NguoiDungService nguoiDungService = applicationContext.getBean(NguoiDungService.class);
        http
                .securityMatcher("/acvstore/**")
                .addFilterBefore(blockFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(faceVerificationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/acvstore/login", "/acvstore/register-face", "/acvstore/verify-face").permitAll()
                        .requestMatchers("/acvstore/vi/**").hasRole("ADMIN")
                        .requestMatchers("/acvstore/verify-success").authenticated()

                        // Quản lý sản phẩm - Cả ADMIN và EMPLOYEE đều có thể xem sản phẩm
                        .requestMatchers("/acvstore/san-pham", "/acvstore/san-pham/get/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/acvstore/san-pham/save", "/acvstore/san-pham/update-status", "/acvstore/san-pham/upload-image").hasRole("ADMIN")

                        // Thuộc tính sản phẩm - CHỈ ADMIN
                        .requestMatchers("/acvstore/xuat-xu/**", "/acvstore/mau-sac/**", "/acvstore/kich-co/**",
                                "/acvstore/chat-lieu/**", "/acvstore/kieu-dang/**", "/acvstore/co-ao/**",
                                "/acvstore/tay-ao/**", "/acvstore/danh-muc/**", "/acvstore/thuong-hieu/**").hasRole("ADMIN")

                        // Quản lý nhân viên - CHỈ ADMIN
                        .requestMatchers("/acvstore/employees/**").hasRole("ADMIN")

                        // Truy cập bảng điều khiển
                        .requestMatchers("/acvstore/admin-dashboard").hasRole("ADMIN")
                        .requestMatchers("/acvstore/employee-dashboard").hasRole("EMPLOYEE")

                        // Thống kê - Cả ADMIN và EMPLOYEE
                        .requestMatchers("/acvstore/thong-ke").hasAnyRole("ADMIN", "EMPLOYEE")

                        // Bán hàng và hóa đơn - Cả ADMIN và EMPLOYEE
                        .requestMatchers("/acvstore/ban-hang/**", "/acvstore/hoa-don/**").hasAnyRole("ADMIN", "EMPLOYEE")

                        // Giảm giá - Cả ADMIN và EMPLOYEE
                        .requestMatchers("/acvstore/phieu-giam-gia/**", "/acvstore/chien-dich-giam-gia/**").hasAnyRole("ADMIN", "EMPLOYEE")

                        // Khách hàng - Cả ADMIN và EMPLOYEE
                        .requestMatchers("/acvstore/customers/**").hasAnyRole("ADMIN", "EMPLOYEE")

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
                                String vaiTro = nguoiDung.getVaiTro();
                                if ("EMPLOYEE".equalsIgnoreCase(vaiTro)) {
                                    response.sendRedirect("/acvstore/thong-ke");
                                } else if ("ADMIN".equalsIgnoreCase(vaiTro)) {
                                    byte[] descriptor = nguoiDung.getFaceDescriptor();
                                    if (descriptor == null || descriptor.length == 0) {
                                        response.sendRedirect("/acvstore/register-face");
                                    } else {
                                        response.sendRedirect("/acvstore/verify-face");
                                    }
                                } else {
                                    response.sendRedirect("/acvstore/login?error=unauthorizedRole");
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
                        .defaultAuthenticationEntryPointFor(
                                employeeAuthEntryPoint(),
                                new AntPathRequestMatcher("/acvstore/**")
                        )
                        .accessDeniedHandler(accessDeniedHandlerEmployees())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredUrl("/acvstore/login?expired")
                        )
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
                        .requestMatchers(
                                "/customers/login",
                                "/customers/register",
                                "/customers/register/**",
                                "/customers/oauth2/register",
                                "/customers/auth/forgot-password",
                                "/customers/auth/verify-otp",
                                "/customers/auth/reset-password",
                                "/customers/auth/resend-otp"
                        ).permitAll()
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
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/customers/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(new CustomOAuth2UserService(applicationContext.getBean(NguoiDungService.class), session)))
                        .successHandler((request, response, authentication) -> {
                            HttpSession session = request.getSession();
                            if (session.getAttribute("pendingUser") != null) {
                                response.sendRedirect("/customers/oauth2/register");
                            } else {
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                response.sendRedirect("/");
                            }
                        })
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
                        .invalidSessionUrl("/customers/login?invalid")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/chitietsanpham", "/new", "/all", "/bestsellers", "/category/**", "/search/**", "/acvstore/login", "/acvstore/verify-face", "/customers/login", "/customers/oauth2/register", "/api/cart/check-auth", "/api/cart/get-user", "/css/**", "/js/**", "/image/**", "/images/**", "/vi/**", "/uploads/**").permitAll()
                        .requestMatchers("/cart", "/api/cart/**").authenticated()
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
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/customers/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(new CustomOAuth2UserService(applicationContext.getBean(NguoiDungService.class), session)))
                        .successHandler((request, response, authentication) -> {
                            HttpSession session = request.getSession();
                            if (session.getAttribute("pendingUser") != null) {
                                response.sendRedirect("/customers/oauth2/register");
                            } else {
                                response.sendRedirect("/");
                            }
                        })
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
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredUrl("/customers/login?expired")
                        )
                        .invalidSessionUrl("/customers/login?invalid")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (request, response, authentication) -> {
            HttpSession session = request.getSession();
            if (session.getAttribute("pendingUser") != null) {
                response.sendRedirect("/customers/oauth2/register");
            } else {
                response.sendRedirect("/");
            }
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}