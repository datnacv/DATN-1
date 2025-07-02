package com.example.AsmGD1.config;

import com.example.AsmGD1.service.NguoiDung.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

    @Bean
    public SecurityFilterChain employeeSecurityFilterChain(HttpSecurity http,
                                                           CustomerAccessBlockFilter blockFilter) throws Exception {
        http
                .securityMatcher("/acvstore/**")
                .addFilterBefore(blockFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/acvstore/login").permitAll()
                        .requestMatchers("/acvstore/thong-ke").hasRole("ADMIN")
                        .requestMatchers("/api/get-descriptor").permitAll()
                        .requestMatchers("/acvstore/employee-dashboard").hasRole("EMPLOYEE")
                        .requestMatchers("/acvstore/login", "/acvstore/employees/verify-face", "/acvstore/register-face").permitAll()
                        .requestMatchers("/acvstore/admin-dashboard").hasRole("ADMIN")
                        .requestMatchers("/acvstore/employee-dashboard").hasRole("EMPLOYEE")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/acvstore/login")
                        .loginProcessingUrl("/acvstore/login")
                        .defaultSuccessUrl("/acvstore/redirect", true)
                        .failureUrl("/acvstore/login?error=invalidCredentials")
                        .loginPage("/acvstore/login")
                        .loginProcessingUrl("/acvstore/login")
                        .successHandler((request, response, authentication) -> {
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
                            if (isAdmin) {
                                response.sendRedirect("/acvstore/employees/verify-face");
                            } else {
                                response.sendRedirect("/acvstore/redirect");
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
                        .authenticationEntryPoint(employeeAuthEntryPoint())
                        .accessDeniedHandler(accessDeniedHandlerEmployees())
                )
                .sessionManagement(session -> session
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

    // SecurityFilterChain mặc định cho tất cả các đường dẫn khác
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/acvstore/login", "/acvstore/employees/verify-face", "/acvstore/customers/login").permitAll()
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
