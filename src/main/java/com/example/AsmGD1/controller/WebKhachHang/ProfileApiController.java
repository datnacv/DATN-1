package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileApiController {

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public ResponseEntity<?> getProfile() {
        return ResponseEntity.ok(profileService.getCurrentUser());
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody NguoiDung user) {
        return ResponseEntity.ok(profileService.updateUser(user));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteProfile() {
        profileService.deleteUser();
        return ResponseEntity.ok(Map.of("message", "Tài khoản đã được xóa"));
    }

    @GetMapping("/addresses")
    public ResponseEntity<?> getAddresses() {
        List<DiaChiNguoiDung> addresses = profileService.getUserAddresses();
        return ResponseEntity.ok(addresses);
    }

    @PostMapping("/address")
    public ResponseEntity<?> addAddress(@RequestBody DiaChiNguoiDung address) {
        return ResponseEntity.ok(profileService.addAddress(address));
    }

    @PutMapping("/address/{addressId}")
    public ResponseEntity<?> updateAddress(@PathVariable UUID addressId, @RequestBody DiaChiNguoiDung address) {
        return ResponseEntity.ok(profileService.updateAddress(addressId, address));
    }

    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable UUID addressId) {
        profileService.deleteAddress(addressId);
        return ResponseEntity.ok(Map.of("message", "Xóa địa chỉ thành công"));
    }

    @PostMapping("/address/{addressId}/set-default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable UUID addressId) {
        profileService.setDefaultAddress(addressId);
        return ResponseEntity.ok(Map.of("message", "Đặt địa chỉ mặc định thành công"));
    }
}
