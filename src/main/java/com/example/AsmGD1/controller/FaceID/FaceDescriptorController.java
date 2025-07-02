package com.example.AsmGD1.controller.FaceID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
public class FaceDescriptorController {

    @GetMapping("/api/get-descriptor")
    public ResponseEntity<?> getDescriptor(@RequestParam String username, Authentication auth) {
        System.out.println("🔐 Authenticated user: " + (auth != null ? auth.getName() : "null") + ", Requested: " + username);

        if (username == null || username.isEmpty() || username.equals("unknown")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username không hợp lệ"));
        }

        // Tùy bạn giữ hay bỏ check này
        if (auth == null || !username.equals(auth.getName())) {
            System.out.println("⚠️ Tên người dùng không khớp. Cho phép test.");
            // return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Không có quyền"));
        }

        String filePath = System.getProperty("user.dir") + "/descriptors/" + username + ".json";
        File descriptorFile = new File(filePath);
        if (!descriptorFile.exists()) {
            System.out.println("❌ File không tồn tại: " + filePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy descriptor cho " + username));
        }

        try (InputStream inputStream = new java.io.FileInputStream(descriptorFile)) {
            String jsonContent = new String(inputStream.readAllBytes());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> descriptorData = objectMapper.readValue(jsonContent, Map.class);

            // ✅ FIX: Chuyển đổi key từ "descriptor" thành "descriptors" và wrap trong array
            List<Double> descriptor = (List<Double>) descriptorData.get("descriptor");
            if (descriptor == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Descriptor data không hợp lệ"));
            }

            // JavaScript expect: { descriptors: [[...]] } - array of arrays
            Map<String, Object> response = Map.of("descriptors", List.of(descriptor));
            System.out.println("✅ Trả về descriptor cho " + username + ", size: " + descriptor.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi đọc descriptor: " + e.getMessage()));
        }
    }
}