package com.example.AsmGD1.controller.GiaoHangNhanh;

import com.example.AsmGD1.dto.KhachHang.GiaoHangNhanh.DiaChiResponse;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutRestController {

    @Autowired
    private NguoiDungService nguoiDungService;

    @GetMapping("/addresses")
    public DiaChiResponse getUserAddress(Authentication authentication) {
        NguoiDung currentUser = (NguoiDung) authentication.getPrincipal();
        DiaChiResponse res = new DiaChiResponse();
        res.setTinhThanhPho(currentUser.getTinhThanhPho());
        res.setQuanHuyen(currentUser.getQuanHuyen());
        res.setPhuongXa(currentUser.getPhuongXa());
        res.setChiTietDiaChi(currentUser.getChiTietDiaChi());
        return res;
    }
}