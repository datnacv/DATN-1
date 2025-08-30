package com.example.AsmGD1.scheduler;

import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.LichSuHoaDon;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.HoaDon.LichSuHoaDonRepository;

import com.example.AsmGD1.service.HoaDon.HoaDonService;
import com.example.AsmGD1.service.ThongBao.ThongBaoService;
import com.example.AsmGD1.service.WebKhachHang.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class OrderAutoConfirmationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderAutoConfirmationScheduler.class);
//    AUTO_CONFIRM_DAYS
    private static final long AUTO_CONFIRM_MINUTES = 5; // Xác nhận sau 5 phút // Số ngày tự động xác nhận (có thể cấu hình qua application.properties nếu cần)
    private static final String TRANSPORT_SUCCESS_STATUS = "Vận chuyển thành công";
    private static final String COMPLETED_STATUS = "Hoàn thành";
    private static final String DON_HANG_SUCCESS_STATUS = "THANH_CONG";

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private ThongBaoService thongBaoService;

    @Autowired
    private EmailService emailService;

    /**
     * Scheduler chạy hàng ngày lúc 00:00 (midnight) để kiểm tra và tự động xác nhận đơn hàng.
     * Có thể thay đổi cron expression, ví dụ: @Scheduled(fixedRate = 60000) để chạy mỗi phút cho test.
     */
//    @Scheduled(cron = "0 0 0 * * ?")  // Hàng ngày lúc 00:00
    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackOn = Exception.class)
    public void autoConfirmOrders() {
        logger.info("Bắt đầu task tự động xác nhận đơn hàng...");

        // Tìm tất cả hóa đơn ở trạng thái "Vận chuyển thành công"
        List<HoaDon> pendingOrders = hoaDonRepository.findByTrangThai(TRANSPORT_SUCCESS_STATUS);
        int confirmedCount = 0;

        for (HoaDon hoaDon : pendingOrders) {
            try {
                // Tìm LichSuHoaDon gần nhất với trạng thái "Vận chuyển thành công"
                Optional<LichSuHoaDon> latestHistoryOpt = lichSuHoaDonRepository
                        .findFirstByHoaDonIdAndTrangThaiOrderByThoiGianDesc(hoaDon.getId(), TRANSPORT_SUCCESS_STATUS);

                if (latestHistoryOpt.isPresent()) {
                    LichSuHoaDon latestHistory = latestHistoryOpt.get();
                    LocalDateTime transportSuccessTime = latestHistory.getThoiGian();
                    Duration duration = Duration.between(transportSuccessTime, LocalDateTime.now());

                    // Nếu đã trôi qua >= 7 ngày
                    if (duration.toDays() >= AUTO_CONFIRM_MINUTES) {
                        // Tự động xác nhận
                        hoaDon.setTrangThai(COMPLETED_STATUS);
                        hoaDon.setNgayThanhToan(LocalDateTime.now());
                        hoaDon.setGhiChu("Tự động xác nhận đã nhận hàng sau " + AUTO_CONFIRM_MINUTES + " ngày");

                        DonHang donHang = hoaDon.getDonHang();
                        donHang.setTrangThai(DON_HANG_SUCCESS_STATUS);
                        hoaDonService.save(hoaDon);  // Lưu hóa đơn (cũng lưu donHang nếu cascade)

                        // Thêm lịch sử
                        hoaDonService.addLichSuHoaDon(hoaDon, COMPLETED_STATUS, "Tự động xác nhận sau " + AUTO_CONFIRM_MINUTES + " ngày");

                        // Gửi thông báo hệ thống cho admin
                        thongBaoService.taoThongBaoHeThong(
                                "admin",
                                "Đơn hàng tự động hoàn thành",
                                "Đơn hàng mã " + donHang.getMaDonHang() + " đã được tự động xác nhận hoàn thành sau " + AUTO_CONFIRM_MINUTES + " ngày.",
                                donHang
                        );

                        // Gửi email cho khách hàng
                        NguoiDung nguoiDung = donHang.getNguoiDung();
                        if (nguoiDung != null && nguoiDung.getEmail() != null) {
                            String emailContent = "<h2>Thông báo hoàn thành đơn hàng tự động</h2>" +
                                    "<p>Xin chào " + nguoiDung.getHoTen() + ",</p>" +
                                    "<p>Đơn hàng của bạn với mã <strong>" + donHang.getMaDonHang() + "</strong> đã được tự động xác nhận hoàn thành sau " + AUTO_CONFIRM_MINUTES + " ngày kể từ khi vận chuyển thành công.</p>" +
                                    "<p>Nếu bạn có bất kỳ vấn đề gì, vui lòng liên hệ hỗ trợ.</p>" +
                                    "<p>Cảm ơn bạn đã mua sắm tại ACV Store!</p>" +
                                    "<p>Trân trọng,<br>Đội ngũ ACV Store</p>";
                            emailService.sendEmail(nguoiDung.getEmail(), "Hoàn thành đơn hàng tự động - ACV Store", emailContent);
                        }

                        confirmedCount++;
                        logger.info("Tự động xác nhận đơn hàng {} thành công.", hoaDon.getId());
                    }
                }
            } catch (Exception e) {
                logger.error("Lỗi khi tự động xác nhận đơn hàng {}: {}", hoaDon.getId(), e.getMessage(), e);
            }
        }

        logger.info("Task hoàn tất: {} đơn hàng được tự động xác nhận.", confirmedCount);
    }
}