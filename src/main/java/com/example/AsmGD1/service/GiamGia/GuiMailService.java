package com.example.AsmGD1.service.GiamGia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GuiMailService {
    @Autowired
    private JavaMailSender mailSender;

    public void guiPhieuGiamGia(String toEmail, String tenPhieu, BigDecimal giaTri) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Bạn nhận được một phiếu giảm giá từ ACV Store!");
        message.setText("Bạn vừa nhận được phiếu giảm giá: " + tenPhieu +
                "\nGiá trị: " + giaTri + " VND\n\nTruy cập ACVStore để sử dụng ngay!");
        mailSender.send(message);
    }
}

