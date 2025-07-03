package com.example.AsmGD1.controller.FaceID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/acvstore")
public class FaceAuthPageController {

    @GetMapping("/verify-face")
    public String showFaceAuthPage() {
        return "WebQuanLy/verify-face"; // Đảm bảo file HTML tồn tại
    }
}

