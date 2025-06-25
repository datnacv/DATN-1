package com.example.AsmGD1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Controller
class FaceRegistrationController {
    @GetMapping("/acvstore/employees/register-face")
    @PreAuthorize("hasRole('ADMIN')")
    public String showRegisterFacePage() {
        return "WebQuanLy/register-face"; // Chỉ định đúng thư mục và tên file
    }

    @PostMapping("/acvstore/employees/register-face")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerFace(@RequestBody Map<String, String> payload, Authentication auth) {
        String username = auth.getName();
        String imageData = payload.get("image");

        try {
            // Tên file theo username
            String folderPath = "faces/";
            String filePath = folderPath + username + ".jpg";

            // Nếu file đã tồn tại thì không cho đăng ký lại
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Khuôn mặt đã được đăng ký trước đó."));
            }

            // Tạo thư mục nếu chưa có
            new File(folderPath).mkdirs();

            // Cắt bỏ prefix base64 và lưu ảnh
            String base64Image = imageData.split(",")[1];
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            try (OutputStream stream = new FileOutputStream(filePath)) {
                stream.write(imageBytes);
            }

            return ResponseEntity.ok(Map.of("message", "Đăng ký khuôn mặt thành công!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Đăng ký thất bại"));
        }
    }



}
