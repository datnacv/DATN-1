package com.example.AsmGD1.controller.FaceID;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

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

        float[] newDescriptor = convertToFloatArray(descriptorList);

        // ✅ So sánh với descriptor của người dùng khác
        List<NguoiDung> allUsers = nguoiDungRepository.findAll();
        for (NguoiDung other : allUsers) {
            if (!other.getTenDangNhap().equals(username)
                    && other.getFaceDescriptor() != null
                    && other.getFaceDescriptor().length > 0) {

                try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(other.getFaceDescriptor()))) {
                    float[] existingDescriptor = new float[128];
                    for (int i = 0; i < 128; i++) {
                        existingDescriptor[i] = dis.readFloat();
                    }

                    double distance = computeEuclideanDistance(newDescriptor, existingDescriptor);
                    if (distance < 0.5) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "message", "❌ Khuôn mặt này đã được đăng ký bởi tài khoản khác: " + other.getTenDangNhap()
                        ));
                    }
                } catch (Exception e) {
                    return ResponseEntity.status(500).body(Map.of("message", "Lỗi khi kiểm tra khuôn mặt đã tồn tại"));
                }
            }
        }

        // ✅ Nếu không trùng thì lưu descriptor
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

            return ResponseEntity.ok(Map.of("message", "✅ Đăng ký khuôn mặt thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi khi lưu descriptor"));
        }
    }

    // 🔧 Hàm tính khoảng cách Euclidean giữa hai vector
    private double computeEuclideanDistance(float[] d1, float[] d2) {
        double sum = 0;
        for (int i = 0; i < d1.length; i++) {
            double diff = d1[i] - d2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    // 🔧 Convert list Double -> array float
    private float[] convertToFloatArray(List<Double> list) {
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).floatValue();
        }
        return result;
    }
}
