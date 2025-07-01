package com.example.AsmGD1.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.Map;

@Controller
public class FaceVerificationController {

    @PostMapping("/acvstore/employees/verify-face")
    @ResponseBody
    public ResponseEntity<?> verifyFace(@RequestBody Map<String, String> payload,
                                        Authentication auth,
                                        HttpSession session) {
        String username = auth.getName();
        String imageData = payload.get("image");

        // ✅ Lấy đường dẫn tuyệt đối đến thư mục chứa ảnh
        String folderPath = System.getProperty("user.dir") + File.separator + "faces" + File.separator;
        String registeredImagePath = folderPath + username + ".jpg";
        File registeredImage = new File(registeredImagePath);

        // ❌ Nếu chưa có ảnh đã đăng ký
        if (!registeredImage.exists()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Chưa đăng ký khuôn mặt."
            ));
        }

        try {
            // ✅ Giải mã ảnh base64 được gửi từ frontend
            String base64Image = imageData.split(",")[1];
            byte[] uploadedBytes = Base64.getDecoder().decode(base64Image);

            // ✅ Đọc ảnh đã đăng ký từ file
            byte[] registeredBytes = new byte[(int) registeredImage.length()];
            try (FileInputStream fis = new FileInputStream(registeredImage)) {
                fis.read(registeredBytes);
            }

            // ✅ So sánh ảnh bằng BufferedImage pixel-by-pixel
            BufferedImage img1, img2;
            try {
                img1 = ImageIO.read(new ByteArrayInputStream(registeredBytes));
                img2 = ImageIO.read(new ByteArrayInputStream(uploadedBytes));
                if (img1 == null || img2 == null) {
                    throw new IOException("Không đọc được ảnh (ImageIO trả về null).");
                }
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", "Lỗi khi đọc ảnh. Có thể ảnh không đúng định dạng JPEG hoặc bị hỏng."
                ));
            }


            boolean matched = false;
            if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
                long totalDiff = 0;
                for (int x = 0; x < img1.getWidth(); x++) {
                    for (int y = 0; y < img1.getHeight(); y++) {
                        int rgb1 = img1.getRGB(x, y);
                        int rgb2 = img2.getRGB(x, y);

                        int r1 = (rgb1 >> 16) & 0xff;
                        int g1 = (rgb1 >> 8) & 0xff;
                        int b1 = rgb1 & 0xff;

                        int r2 = (rgb2 >> 16) & 0xff;
                        int g2 = (rgb2 >> 8) & 0xff;
                        int b2 = rgb2 & 0xff;

                        totalDiff += Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
                    }
                }

                double avgDiff = totalDiff / (double)(img1.getWidth() * img1.getHeight() * 3);
                matched = avgDiff < 15; // ➜ Có thể điều chỉnh ngưỡng này nếu cần
            }


            if (matched) {
                session.setAttribute("faceVerified", true);
                return ResponseEntity.ok(Map.of("success", true, "redirect", "/acvstore/thong-ke"));
            } else {
                return ResponseEntity.ok(Map.of("success", false, "message", "Khuôn mặt không khớp."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Lỗi trong quá trình xác minh."
            ));
        }
    }

}
