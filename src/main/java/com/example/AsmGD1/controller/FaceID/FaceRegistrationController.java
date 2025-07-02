package com.example.AsmGD1.controller.FaceID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

@Controller
public class FaceRegistrationController {

    @GetMapping("/acvstore/employees/register-face")
    @PreAuthorize("hasRole('ADMIN')")
    public String showRegisterFacePage(Model model, Authentication auth) {
        System.out.println("Rendering register-face page for user: " + auth.getName());
        model.addAttribute("username", auth.getName());
        return "WebQuanLy/register-face"; // Giao diện Thymeleaf đăng ký khuôn mặt
    }

    @PostMapping("/api/register-descriptor")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> registerDescriptor(@RequestBody Map<String, Object> payload, Authentication auth) {
        String username = auth.getName();
        List<Double> descriptorList = (List<Double>) payload.get("descriptor");
        System.out.println("Registering descriptor for user: " + username);

        if (username == null || username.isEmpty() || username.equals("unknown")) {
            System.out.println("Invalid username: " + username);
            return ResponseEntity.badRequest().body(Map.of("message", "Username không hợp lệ"));
        }
        if (descriptorList == null || descriptorList.size() != 128) {
            System.out.println("Invalid descriptor: size = " + (descriptorList != null ? descriptorList.size() : "null"));
            return ResponseEntity.badRequest().body(Map.of("message", "Descriptor không hợp lệ"));
        }

        try {
            // Lưu file vào thư mục bên ngoài thay vì resources
            String folderPath = System.getProperty("user.dir") + "/descriptors/";
            new File(folderPath).mkdirs();
            String filePath = folderPath + username + ".json";
            System.out.println("Saving descriptor to: " + filePath);

            try (FileWriter writer = new FileWriter(filePath)) {
                ObjectMapper objectMapper = new ObjectMapper();
                writer.write(objectMapper.writeValueAsString(Map.of("descriptor", descriptorList)));
            }

            System.out.println("Descriptor saved successfully for: " + username);
            return ResponseEntity.ok(Map.of("message", "Đăng ký khuôn mặt thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error saving descriptor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi lưu descriptor"));
        }
    }

    @DeleteMapping("/acvstore/employees/delete-face")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> deleteFace(Authentication auth, HttpSession session) {
        String username = auth.getName();
        String filePath = System.getProperty("user.dir") + "/descriptors/" + username + ".json";
        System.out.println("Deleting descriptor for user: " + username + " at: " + filePath);

        File descriptorFile = new File(filePath);
        if (descriptorFile.exists()) {
            descriptorFile.delete();
            session.removeAttribute("faceVerified");
            System.out.println("Descriptor deleted successfully for: " + username);
            return ResponseEntity.ok(Map.of("message", "Đã xóa khuôn mặt đã đăng ký."));
        } else {
            System.out.println("Descriptor file not found: " + filePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Không tìm thấy khuôn mặt để xóa."));
        }
    }
}