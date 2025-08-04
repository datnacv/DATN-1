package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    /** Hàm dùng chung để load user & address */
    private void addUserAndAddresses(Model model) {
        model.addAttribute("user", profileService.getCurrentUser());
        model.addAttribute("addresses", profileService.getUserAddresses());
    }

    @GetMapping
    public String getProfile(Model model,
                             @RequestParam(required = false) String success,
                             @RequestParam(required = false) String error) {
        addUserAndAddresses(model);
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        return "WebKhachHang/profile";
    }

    @GetMapping("/edit")
    public String showEditForm(Model model) {
        model.addAttribute("user", profileService.getCurrentUser());
        return "WebKhachHang/profile-edit";
    }

    @PostMapping
    public String updateProfile(@ModelAttribute NguoiDung user) {
        try {
            profileService.updateUser(user);
            return "redirect:/profile?success=Cập nhật thông tin thành công";
        } catch (RuntimeException e) {
            return "redirect:/profile/edit?error=" + e.getMessage();
        }
    }

    @PostMapping("/delete")
    public String deleteProfile() {
        try {
            profileService.deleteUser();
            return "redirect:/logout";
        } catch (RuntimeException e) {
            return "redirect:/profile?error=" + e.getMessage();
        }
    }

    /** ==== Địa chỉ ==== */
    @GetMapping("/address/add")
    public String showAddAddressForm(Model model) {
        model.addAttribute("address", new DiaChiNguoiDung());
        return "WebKhachHang/address-add";
    }

    @PostMapping("/address")
    public String addAddress(@ModelAttribute DiaChiNguoiDung address) {
        try {
            profileService.addAddress(address);
            return "redirect:/profile?success=Thêm địa chỉ thành công";
        } catch (RuntimeException e) {
            return "redirect:/profile/address/add?error=" + e.getMessage();
        }
    }

    @GetMapping("/address/{addressId}/edit")
    public String showEditAddressForm(@PathVariable UUID addressId, Model model) {
        model.addAttribute("address", profileService.getAddressById(addressId));
        return "WebKhachHang/address-edit";
    }

    @PostMapping("/address/{addressId}")
    public String updateAddress(@PathVariable UUID addressId, @ModelAttribute DiaChiNguoiDung address) {
        try {
            profileService.updateAddress(addressId, address);
            return "redirect:/profile?success=Cập nhật địa chỉ thành công";
        } catch (RuntimeException e) {
            return "redirect:/profile/address/" + addressId + "/edit?error=" + e.getMessage();
        }
    }

    @PostMapping("/address/{addressId}/delete")
    public String deleteAddress(@PathVariable UUID addressId) {
        try {
            profileService.deleteAddress(addressId);
            return "redirect:/profile?success=Xóa địa chỉ thành công";
        } catch (RuntimeException e) {
            return "redirect:/profile?error=" + e.getMessage();
        }
    }

    @PostMapping("/address/{addressId}/set-default")
    public String setDefaultAddress(@PathVariable UUID addressId) {
        try {
            profileService.setDefaultAddress(addressId);
            return "redirect:/profile?success=Đặt địa chỉ mặc định thành công";
        } catch (RuntimeException e) {
            return "redirect:/profile?error=" + e.getMessage();
        }
    }

    /** ==== Quên mật khẩu ==== */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "WebKhachHang/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestOtp(@RequestParam String email, Model model) {
        try {
            profileService.sendOtp(email);
            model.addAttribute("success", "Mã OTP đã được gửi đến email của bạn");
            model.addAttribute("email", email);
            return "WebKhachHang/reset-password";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "WebKhachHang/forgot-password";
        }
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String otp,
                                @RequestParam String newPassword,
                                Model model) {
        try {
            String result = profileService.verifyOtp(email, otp);
            switch (result) {
                case "valid":
                    profileService.resetPassword(email, newPassword);
                    model.addAttribute("success", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập.");
                    return "login";
                case "expired":
                    model.addAttribute("error", "Mã OTP đã hết hạn");
                    break;
                case "invalid":
                    model.addAttribute("error", "Mã OTP không hợp lệ");
                    break;
                default:
                    model.addAttribute("error", "Không tìm thấy người dùng");
            }
            model.addAttribute("email", email);
            return "WebKhachHang/reset-password";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "WebKhachHang/reset-password";
        }
    }
}