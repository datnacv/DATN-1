package com.example.AsmGD1.controller.FaceID;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class FaceRegistrationController {

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @PostMapping("/api/register-descriptor")
    public ResponseEntity<?> registerFace(@RequestBody Map<String, Object> payload, Authentication auth) {
        String username = auth.getName();
        List<Double> descriptorList = (List<Double>) payload.get("descriptor");

        if (username == null || descriptorList == null || descriptorList.size() != 128) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        Optional<NguoiDung> optionalUser = nguoiDungRepository.findByTenDangNhap(username);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy người dùng hoặc bị khóa"));
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (Double d : descriptorList) {
                dos.writeFloat(d.floatValue());
            }

            NguoiDung user = optionalUser.get();
            user.setFaceDescriptor(baos.toByteArray());
            user.setFaceRegistered(true);
            nguoiDungRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Đăng ký khuôn mặt thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi khi lưu descriptor"));
        }
    }
}
