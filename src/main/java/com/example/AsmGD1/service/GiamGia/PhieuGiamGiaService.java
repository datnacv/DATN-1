package com.example.AsmGD1.service.GiamGia;

import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaService {

    private static final Logger logger = LoggerFactory.getLogger(PhieuGiamGiaService.class);

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;

    public List<PhieuGiamGia> layTatCa() {
        return phieuGiamGiaRepository.findAll();
    }

    public PhieuGiamGia luu(PhieuGiamGia phieu) {
        if (phieu.getThoiGianTao() == null) {
            phieu.setThoiGianTao(LocalDateTime.now());
        }

        if ("ca_nhan".equalsIgnoreCase(phieu.getKieuPhieu())) {
            phieu.setSoLuong(1);
            phieu.setGioiHanSuDung(1);
        } else {
            if (phieu.getSoLuong() == null) {
                phieu.setSoLuong(1);
            }
            if (phieu.getGioiHanSuDung() == null) {
                phieu.setGioiHanSuDung(phieu.getSoLuong());
            }
        }

        return phieuGiamGiaRepository.save(phieu);
    }

    public String tinhTrang(PhieuGiamGia v) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime batDau = v.getNgayBatDau();
        LocalDateTime ketThuc = v.getNgayKetThuc();

        if (batDau != null && ketThuc != null) {
            if (now.isBefore(batDau)) return "Sắp diễn ra";
            else if (!now.isAfter(ketThuc)) return "Đang diễn ra";
            else return "Đã kết thúc";
        }

        return "Không xác định";
    }

    @Transactional
    public boolean apDungPhieuGiamGia(UUID phieuId) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findById(phieuId).orElse(null);
        if (phieu == null) {
            logger.warn("Không tìm thấy phiếu giảm giá: phieuId={}", phieuId);
            return false;
        }
        if (!"Đang diễn ra".equals(tinhTrang(phieu))) {
            logger.warn("Phiếu giảm giá không trong thời gian hiệu lực: phieuId={}", phieuId);
            return false;
        }

        Integer soLuong = phieu.getSoLuong();
        if (soLuong != null && soLuong > 0) {
            phieu.setSoLuong(soLuong - 1);
            phieuGiamGiaRepository.save(phieu);
            phieuGiamGiaRepository.flush();
            logger.info("Áp dụng phiếu công khai thành công: phieuId={}, soLuong moi={}", phieuId, phieu.getSoLuong());
            return true;
        }

        logger.warn("Phiếu công khai không khả dụng: soLuong={}", soLuong);
        return false;
    }

    /** Giảm trên tổng đơn (ORDER): đã có sẵn */
    public BigDecimal tinhTienGiamGia(PhieuGiamGia phieu, BigDecimal tongTien) {
        if (phieu == null || tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("⚠️ Đầu vào không hợp lệ - phieu: {}, tongTien: {}", phieu, tongTien);
            return BigDecimal.ZERO;
        }

        BigDecimal giamGia = BigDecimal.ZERO;
        BigDecimal giaTriGiam = phieu.getGiaTriGiam();

        if (giaTriGiam == null || giaTriGiam.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("⚠️ Giá trị giảm không hợp lệ - giaTriGiam: {}", giaTriGiam);
            return BigDecimal.ZERO;
        }

        String loai = phieu.getLoai();
        if (loai == null || loai.isBlank()) {
            logger.warn("⚠️ Loại phiếu null hoặc rỗng - ma: {}", phieu.getMa());
            return BigDecimal.ZERO;
        }

        String loaiChuanHoa = loai.trim().toUpperCase().replace("_", "").replace(" ", "");
        logger.info("🔎 Tính tiền giảm giá | Mã: {}, Loại gốc: {}, Loại chuẩn hóa: {}, Giá trị giảm: {}, Tổng tiền: {}",
                phieu.getMa(), loai, loaiChuanHoa, giaTriGiam, tongTien);

        if ("PERCENT".equals(loaiChuanHoa) || "PHANTRAM".equals(loaiChuanHoa)) {
            giamGia = tongTien.multiply(giaTriGiam)
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.HALF_UP);

            if (phieu.getGiaTriGiamToiDa() != null &&
                    giamGia.compareTo(phieu.getGiaTriGiamToiDa()) > 0) {
                logger.info("🔁 Áp dụng giới hạn tối đa: {} thay vì {}", phieu.getGiaTriGiamToiDa(), giamGia);
                giamGia = phieu.getGiaTriGiamToiDa();
            }
        } else if ("FIXED".equals(loaiChuanHoa) || "TIENMAT".equals(loaiChuanHoa) || "CASH".equals(loaiChuanHoa)) {
            giamGia = giaTriGiam;
            logger.info("🔁 Áp dụng giảm giá tiền mặt: {}", giamGia);
        } else {
            logger.warn("⚠️ Loại phiếu không hỗ trợ: {}", loaiChuanHoa);
            return BigDecimal.ZERO;
        }

        if (giamGia.compareTo(tongTien) > 0) {
            logger.info("🔁 Giảm giá vượt tổng tiền, giới hạn lại bằng tổng tiền: {}", tongTien);
            giamGia = tongTien;
        }

        logger.info("✅ Số tiền được giảm: {}", giamGia);
        return giamGia;
    }

    /** Giảm phí vận chuyển (SHIPPING): thêm mới cho freeship */
    public BigDecimal tinhGiamPhiShip(PhieuGiamGia phieu, BigDecimal phiShip, BigDecimal tongTruocShip) {
        if (phieu == null || phiShip == null || phiShip.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        if (!"SHIPPING".equalsIgnoreCase(phieu.getPhamViApDung())) return BigDecimal.ZERO;

        // Đơn tối thiểu (nếu có) so với subtotal
        if (phieu.getGiaTriGiamToiThieu() != null &&
                tongTruocShip != null &&
                tongTruocShip.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
            logger.info("🚫 Không đạt đơn tối thiểu freeship | yêu cầu: {}, thực tế: {}",
                    phieu.getGiaTriGiamToiThieu(), tongTruocShip);
            return BigDecimal.ZERO;
        }

        String loai = phieu.getLoai() == null ? "" : phieu.getLoai().trim().toUpperCase();
        switch (loai) {
            case "FREESHIP_FULL":
                return phiShip; // miễn toàn bộ
            case "FREESHIP_CAP":
                if (phieu.getGiaTriGiamToiDa() == null || phieu.getGiaTriGiamToiDa().compareTo(BigDecimal.ZERO) <= 0) {
                    logger.warn("⚠️ FREESHIP_CAP thiếu/không hợp lệ giaTriGiamToiDa");
                    return BigDecimal.ZERO;
                }
                return phiShip.min(phieu.getGiaTriGiamToiDa()); // miễn tối đa X₫
            default:
                logger.warn("⚠️ Loại freeship không hỗ trợ: {}", loai);
                return BigDecimal.ZERO;
        }
    }
}
