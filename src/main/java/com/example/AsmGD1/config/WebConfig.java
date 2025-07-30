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

        // ======== CẤU HÌNH ẢNH SẢN PHẨM (/images/**) ==========
        String productUploadPath;
        String productDirPath;

        if (os.contains("win")) {
            productUploadPath = "file:///C:/DATN/uploads/san_pham/";
            productDirPath = "C:/DATN/uploads/san_pham/";
        } else {
            String userHome = System.getProperty("user.home");
            productUploadPath = "file://" + userHome + "/DATN/uploads/san_pham/";
            productDirPath = userHome + "/DATN/uploads/san_pham/";
        }

        try {
            Path productPath = Paths.get(productDirPath);
            if (!Files.exists(productPath)) {
                Files.createDirectories(productPath);
                System.out.println("Đã tạo thư mục sản phẩm: " + productPath);
            } else {
                System.out.println("Thư mục sản phẩm đã tồn tại: " + productPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo hoặc truy cập thư mục sản phẩm: " + productDirPath, e);
        }

        registry.addResourceHandler("/images/**")
                .addResourceLocations(productUploadPath)
                .setCachePeriod(0);

        // ======== CẤU HÌNH ẢNH BẰNG CHỨNG (/uploads/bang-chung/**) ==========
        String evidenceUploadPath;
        String evidenceDirPath;

        if (os.contains("win")) {
            evidenceUploadPath = "file:///C:/DATN/uploads/bang-chung/";
            evidenceDirPath = "C:/DATN/uploads/bang-chung/";
        } else {
            String userHome = System.getProperty("user.home");
            evidenceUploadPath = "file://" + userHome + "/DATN/uploads/bang-chung/";
            evidenceDirPath = userHome + "/DATN/uploads/bang-chung/";
        }

        try {
            Path evidencePath = Paths.get(evidenceDirPath);
            if (!Files.exists(evidencePath)) {
                Files.createDirectories(evidencePath);
                System.out.println("Đã tạo thư mục bằng chứng: " + evidencePath);
            } else {
                System.out.println("Thư mục bằng chứng đã tồn tại: " + evidencePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo hoặc truy cập thư mục bằng chứng: " + evidenceDirPath, e);
        }

        registry.addResourceHandler("/uploads/bang-chung/**")
                .addResourceLocations(evidenceUploadPath)
                .setCachePeriod(0);
    }
}
