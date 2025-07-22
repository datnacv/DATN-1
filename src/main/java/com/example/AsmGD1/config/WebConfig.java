package com.example.AsmGD1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String os = System.getProperty("os.name").toLowerCase();
        String uploadPath;
        String directoryPath;

        if (os.contains("win")) {
            uploadPath = "file:///C:/DATN/uploads/san_pham/"; // với 3 dấu / sau file:
            directoryPath = "C:/DATN/uploads/san_pham/";
        } else {
            String userHome = System.getProperty("user.home");
            uploadPath = "file://" + userHome + "/DATN/uploads/san_pham/";
            directoryPath = userHome + "/DATN/uploads/san_pham/";
        }

        // Tạo thư mục nếu chưa tồn tại
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Đã tạo thư mục: " + path);
            } else {
                System.out.println("Thư mục đã tồn tại: " + path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo hoặc truy cập thư mục tải lên: " + directoryPath, e);
        }

        // Ánh xạ /images/** đến thư mục uploads
        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(0);
    }
}