package com.example.AsmGD1.controller.FaceID;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class FaceVerificationController {

    @PostMapping("/acvstore/verify-success") //
    @ResponseBody
    public ResponseEntity<?> setFaceVerified(HttpSession session, Authentication auth) {
        String username = auth.getName();
        session.setAttribute("faceVerified", true);
        System.out.println("✅ Xác minh khuôn mặt thành công cho user: " + username);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }


}
