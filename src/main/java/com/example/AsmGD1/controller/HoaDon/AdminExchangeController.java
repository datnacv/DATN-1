package com.example.AsmGD1.controller.HoaDon;

import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.WebKhachHang.LichSuDoiSanPhamRepository;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import com.example.AsmGD1.service.WebKhachHang.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/acvstore/exchange-requests")
public class AdminExchangeController {

    private static final Logger logger = LoggerFactory.getLogger(AdminExchangeController.class);

    @Autowired
    private LichSuDoiSanPhamRepository lichSuDoiSanPhamRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private EmailService emailService;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @GetMapping
    public String listExchangeRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String searchMaDonHang,
            @RequestParam(required = false) String searchHoTen,
            Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String tenDangNhap = authentication.getName();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            model.addAttribute("user", user);
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "thoiGianDoi");
        Pageable pageable = PageRequest.of(page, 10, sort);

        Page<LichSuDoiSanPham> exchangeRequests;
        if ((searchMaDonHang != null && !searchMaDonHang.isEmpty()) || (searchHoTen != null && !searchHoTen.isEmpty())) {
            exchangeRequests = lichSuDoiSanPhamRepository.findByMaDonHangOrHoTen(
                    searchMaDonHang != null ? searchMaDonHang.trim() : "",
                    searchHoTen != null ? searchHoTen.trim() : "",
                    pageable);
        } else {
            exchangeRequests = lichSuDoiSanPhamRepository.findAll(pageable);
        }

        model.addAttribute("exchangeRequests", exchangeRequests.getContent());
        model.addAttribute("currentPage", exchangeRequests.getNumber());
        model.addAttribute("totalPages", exchangeRequests.getTotalPages());
        model.addAttribute("searchMaDonHang", searchMaDonHang);
        model.addAttribute("searchHoTen", searchHoTen);
        return "WebQuanLy/exchange-requests";
    }

    @PostMapping("/approve/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> approveExchangeRequest(@PathVariable UUID id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Kiểm tra quyền admin
            if (authentication == null || !authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền xác nhận yêu cầu đổi hàng.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            LichSuDoiSanPham lichSu = lichSuDoiSanPhamRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Yêu cầu đổi hàng không tồn tại."));

            if (!"Chờ xử lý".equals(lichSu.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Yêu cầu đổi hàng không ở trạng thái chờ xử lý.");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra tồn kho cho sản phẩm thay thế
            ChiTietSanPham replacementProduct = lichSu.getChiTietSanPhamThayThe();
            if (replacementProduct.getSoLuongTonKho() < lichSu.getSoLuong()) {
                response.put("success", false);
                response.put("message", "Sản phẩm thay thế không đủ tồn kho. Tồn kho hiện tại: " + replacementProduct.getSoLuongTonKho() + ", Yêu cầu: " + lichSu.getSoLuong());
                return ResponseEntity.badRequest().body(response);
            }

            // Cập nhật trạng thái yêu cầu đổi hàng
            lichSu.setTrangThai("Đã xác nhận");
            lichSuDoiSanPhamRepository.save(lichSu);

            // Cập nhật chi tiết đơn hàng
            ChiTietDonHang chiTietDonHang = lichSu.getChiTietDonHang();
            chiTietDonHang.setTrangThaiDoiSanPham(true);
            chiTietDonHang.setLyDoDoiHang(lichSu.getLyDoDoiHang());

            // Cập nhật tồn kho
            ChiTietSanPham originalProduct = chiTietDonHang.getChiTietSanPham();
            originalProduct.setSoLuongTonKho(originalProduct.getSoLuongTonKho() + lichSu.getSoLuong()); // Thêm lại số lượng sản phẩm gốc
            replacementProduct.setSoLuongTonKho(replacementProduct.getSoLuongTonKho() - lichSu.getSoLuong()); // Trừ đúng số lượng sản phẩm thay thế
            chiTietSanPhamRepository.save(originalProduct);
            chiTietSanPhamRepository.save(replacementProduct);

            // Cập nhật trạng thái hóa đơn
            HoaDon hoaDon = lichSu.getHoaDon();
            hoaDon.setTrangThai("Đã đổi hàng");
            hoaDonRepository.save(hoaDon);

            // Thêm lịch sử hóa đơn
            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai("Đã đổi hàng");
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Admin đã xác nhận yêu cầu đổi sản phẩm: " + lichSu.getLyDoDoiHang());
            hoaDon.getLichSuHoaDons().add(lichSuHoaDon);
            hoaDonRepository.save(hoaDon);

            // Gửi thông báo hệ thống cho khách hàng
            NguoiDung user = hoaDon.getDonHang().getNguoiDung();
            thongBaoService.taoThongBaoHeThong(
                    user.getTenDangNhap(),
                    "Yêu cầu đổi sản phẩm đã được xác nhận",
                    "Yêu cầu đổi sản phẩm cho đơn hàng mã " + hoaDon.getDonHang().getMaDonHang() + " đã được xác nhận.",
                    hoaDon.getDonHang()
            );

            // Gửi email thông báo cho khách hàng
            String emailContent = "<h2>Xác nhận yêu cầu đổi sản phẩm</h2>" +
                    "<p>Xin chào " + user.getHoTen() + ",</p>" +
                    "<p>Yêu cầu đổi sản phẩm của bạn cho đơn hàng mã <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã được xác nhận.</p>" +
                    "<p><strong>Lý do:</strong> " + lichSu.getLyDoDoiHang() + "</p>" +
                    (lichSu.getChenhLechGia() != null && lichSu.getChenhLechGia().compareTo(BigDecimal.ZERO) > 0 ?
                            "<p><strong>Chênh lệch giá cần thanh toán:</strong> " + formatVND(lichSu.getChenhLechGia().doubleValue()) + "</p>" +
                                    "<p>Vui lòng liên hệ đội ngũ hỗ trợ để hoàn tất thanh toán chênh lệch.</p>" : "") +
                    "<p>Cảm ơn bạn đã sử dụng dịch vụ của ACV Store!</p>" +
                    "<p>Trân trọng,<br>Đội ngũ ACV Store</p>";
            emailService.sendEmail(user.getEmail(), "Xác nhận đổi sản phẩm - ACV Store", emailContent);

            response.put("success", true);
            response.put("message", "Yêu cầu đổi sản phẩm đã được xác nhận.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi xác nhận yêu cầu đổi sản phẩm: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xác nhận yêu cầu. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reject/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> rejectExchangeRequest(@PathVariable UUID id,
                                                                     @RequestParam(required = false) String rejectReason,
                                                                     Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Kiểm tra quyền admin
            if (authentication == null || !authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền hủy yêu cầu đổi hàng.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            LichSuDoiSanPham lichSu = lichSuDoiSanPhamRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Yêu cầu đổi hàng không tồn tại."));

            if (!"Chờ xử lý".equals(lichSu.getTrangThai())) {
                response.put("success", false);
                response.put("message", "Yêu cầu đổi hàng không ở trạng thái chờ xử lý.");
                return ResponseEntity.badRequest().body(response);
            }

            // Cập nhật trạng thái yêu cầu đổi hàng
            lichSu.setTrangThai("Đã hủy");
            lichSuDoiSanPhamRepository.save(lichSu);

            // Cập nhật trạng thái hóa đơn về trạng thái trước đó (Vận chuyển thành công)
            HoaDon hoaDon = lichSu.getHoaDon();
            hoaDon.setTrangThai("Vận chuyển thành công");
            hoaDonRepository.save(hoaDon);

            // Thêm lịch sử hóa đơn
            LichSuHoaDon lichSuHoaDon = new LichSuHoaDon();
            lichSuHoaDon.setHoaDon(hoaDon);
            lichSuHoaDon.setTrangThai("Vận chuyển thành công");
            lichSuHoaDon.setThoiGian(LocalDateTime.now());
            lichSuHoaDon.setGhiChu("Admin đã hủy yêu cầu đổi sản phẩm: " + (rejectReason != null ? rejectReason : "Không có lý do cụ thể"));
            hoaDon.getLichSuHoaDons().add(lichSuHoaDon);
            hoaDonRepository.save(hoaDon);

            // Gửi thông báo cho khách hàng
            NguoiDung user = hoaDon.getDonHang().getNguoiDung();
            String emailContent = "<h2>Hủy yêu cầu đổi sản phẩm</h2>" +
                    "<p>Xin chào " + user.getHoTen() + ",</p>" +
                    "<p>Yêu cầu đổi sản phẩm của bạn cho đơn hàng mã <strong>" + hoaDon.getDonHang().getMaDonHang() + "</strong> đã bị hủy.</p>" +
                    "<p><strong>Lý do:</strong> " + (rejectReason != null ? rejectReason : "Không có lý do cụ thể") + "</p>" +
                    "<p>Vui lòng liên hệ đội ngũ hỗ trợ nếu bạn cần thêm thông tin.</p>" +
                    "<p>Trân trọng,<br>Đội ngũ ACV Store</p>";
            emailService.sendEmail(user.getEmail(), "Hủy yêu cầu đổi sản phẩm - ACV Store", emailContent);

            response.put("success", true);
            response.put("message", "Yêu cầu đổi sản phẩm đã bị hủy.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi hủy yêu cầu đổi sản phẩm: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi hủy yêu cầu. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String formatVND(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(value) + " VNĐ";
    }
}