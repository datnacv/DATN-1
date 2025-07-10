package com.example.AsmGD1.controller.WebKhachHang;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.WebKhachHang.KHChiTietSanPhamRepository;
import com.example.AsmGD1.repository.WebKhachHang.KHDonHangTamRepository;
import com.example.AsmGD1.repository.WebKhachHang.KHPhieuGiamGiaCuaNguoiDungRepository;
import com.example.AsmGD1.repository.WebKhachHang.KHPhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.WebKhachHang.KHNguoiDungRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.example.AsmGD1.service.WebKhachHang.KHCheckoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/api")
public class KHCheckoutController {

    @Autowired
    private KHCheckoutService checkoutService;

    @Autowired
    private KHChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private KHPhieuGiamGiaCuaNguoiDungRepository phieuGiamGiaCuaNguoiDungRepository;

    @Autowired
    private KHPhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Autowired
    private KHNguoiDungRepository nguoiDungRepository;

    @Autowired
    private KHDonHangTamRepository donHangTamRepository;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/getChiTietSanPham")
    public ResponseEntity<Map<String, Object>> getChiTietSanPham(
            @RequestParam UUID sanPhamId,
            @RequestParam UUID sizeId,
            @RequestParam UUID colorId) {
        ChiTietSanPham chiTietSanPham = chiTietSanPhamRepository.findBySanPhamIdAndSizeIdAndColorId(sanPhamId, sizeId, colorId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tìm thấy"));
        Map<String, Object> response = new HashMap<>();
        response.put("id", chiTietSanPham.getId());
        response.put("gia", chiTietSanPham.getGia());
        response.put("soLuongTonKho", chiTietSanPham.getSoLuongTonKho());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/update-address")
    public ResponseEntity<?> updateAddress(@RequestBody Map<String, String> addressData, Principal principal) {
        NguoiDung nguoiDung = nguoiDungService.findByTenDangNhap(principal.getName());
        if (nguoiDung != null) {
            nguoiDung.setHoTen(addressData.get("hoTen"));
            nguoiDung.setSoDienThoai(addressData.get("soDienThoai"));
            nguoiDung.setTinhThanhPho(addressData.get("tinhThanhPho"));
            nguoiDung.setQuanHuyen(addressData.get("quanHuyen"));
            nguoiDung.setPhuongXa(addressData.get("phuongXa"));
            nguoiDung.setChiTietDiaChi(addressData.get("chiTietDiaChi"));
            nguoiDungService.save(nguoiDung);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/cart/buy-now")
    public ResponseEntity<Map<String, String>> buyNow(@RequestBody Map<String, Object> request) {
        UUID chiTietSanPhamId = UUID.fromString(request.get("chiTietSanPhamId").toString());
        Integer soLuong = Integer.parseInt(request.get("soLuong").toString());
        DonHangTam donHangTam = checkoutService.createTempOrder(chiTietSanPhamId, soLuong);
        Map<String, String> response = new HashMap<>();
        response.put("tempOrderId", donHangTam.getId().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam(required = false) UUID tempOrderId, Model model) {
        NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new RuntimeException("Người dùng không tìm thấy"));

        List<PhieuGiamGiaCuaNguoiDung> vouchers = checkoutService.getAvailableVouchers();
        List<PhuongThucThanhToan> phuongThucThanhToans = phuongThucThanhToanRepository.findAll();

        List<ChiTietDonHang> orderItems;
        BigDecimal tongTienHang = BigDecimal.ZERO;

        if (tempOrderId != null) {
            DonHangTam donHangTam = donHangTamRepository.findById(tempOrderId)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng tạm không tìm thấy"));
            try {
                orderItems = objectMapper.readValue(
                        donHangTam.getDanhSachSanPham(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ChiTietDonHang.class)
                );
                tongTienHang = orderItems.stream()
                        .map(ChiTietDonHang::getThanhTien)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi đọc danh sách sản phẩm");
            }
        } else {
            // Handle checkout from cart (not implemented in this example)
            orderItems = List.of();
        }

        model.addAttribute("nguoiDung", nguoiDung);
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("phuongThucThanhToans", phuongThucThanhToans);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("tongTienHang", tongTienHang);

        return "WebKhachHang/thanh-toan";
    }

    @PostMapping("/checkout/apply-voucher")
    public ResponseEntity<Map<String, Object>> applyVoucher(@RequestBody Map<String, String> request) {
        String voucherId = request.get("voucherId");
        String voucherCode = request.get("voucherCode");
        BigDecimal tongTienHang = new BigDecimal(request.get("tongTienHang"));
        BigDecimal giamGia = checkoutService.applyVoucher(voucherId, voucherCode, tongTienHang);
        Map<String, Object> response = new HashMap<>();
        response.put("giaTriGiam", giamGia);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/confirm")
    public ResponseEntity<Void> confirmOrder(@RequestBody Map<String, Object> request) {
        String diaChiGiaoHang = request.get("diaChiGiaoHang").toString();
        String soDienThoaiKhachHang = request.get("soDienThoaiKhachHang").toString();
        String ghiChu = request.get("ghiChu").toString();
        String phuongThucVanChuyen = request.get("phuongThucVanChuyen").toString();
        BigDecimal phiVanChuyen = new BigDecimal(request.get("phiVanChuyen").toString());
        UUID phuongThucThanhToanId = UUID.fromString(request.get("phuongThucThanhToanId").toString());
        UUID phieuGiamGiaId = request.get("phieuGiamGiaId") != null ? UUID.fromString(request.get("phieuGiamGiaId").toString()) : null;
        UUID tempOrderId = request.get("tempOrderId") != null ? UUID.fromString(request.get("tempOrderId").toString()) : null;

        checkoutService.confirmOrder(diaChiGiaoHang, soDienThoaiKhachHang, ghiChu, phuongThucVanChuyen, phiVanChuyen, phuongThucThanhToanId, phieuGiamGiaId, tempOrderId);
        return ResponseEntity.ok().build();
    }
}