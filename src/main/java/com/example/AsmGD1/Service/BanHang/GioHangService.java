package com.example.AsmGD1.service.BanHang;

import com.example.AsmGD1.dto.BanHang.GioHangDTO;
import com.example.AsmGD1.dto.BanHang.GioHangItemDTO;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import com.example.AsmGD1.service.SanPham.ChiTietSanPhamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;   // Đã có, dùng để so sánh với các trường trong PhieuGiamGia
import java.time.LocalDateTime; // Đã có, dùng cho LocalDateTime.now()
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GioHangService {

    @Autowired
    private ChiTietSanPhamService chiTietSanPhamService;

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    private static final String CART_SESSION_KEY = "CART";
    private static final String APPLIED_DISCOUNT_SESSION_KEY = "APPLIED_DISCOUNT";
    private static final String APPLIED_VOUCHER_ID_SESSION_KEY = "APPLIED_VOUCHER_ID";

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0 VNĐ", symbols);
        return df.format(amount);
    }

    public List<GioHangItemDTO> getCurrentCart() {
        HttpSession session = getSession();
        List<GioHangItemDTO> cart = (List<GioHangItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    private void saveCart(List<GioHangItemDTO> cart) {
        HttpSession session = getSession();
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession();
    }

    public GioHangDTO themVaoGioHang(UUID productDetailId, int quantity, BigDecimal shippingFee) {
        System.out.println("--- themVaoGioHang: Bắt đầu ---");
        List<GioHangItemDTO> currentCart = getCurrentCart();

        GioHangItemDTO existingItem = currentCart.stream()
                .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                .findFirst()
                .orElse(null);

        ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
        if (chiTiet == null) {
            System.err.println("LỖI: Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
            throw new RuntimeException("Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
        }

        if (existingItem != null) {
            int newQuantity = existingItem.getSoLuong() + quantity;
            if (newQuantity > chiTiet.getSoLuongTonKho()) {
                System.err.println("LỖI: Số lượng tồn kho không đủ sau khi cộng dồn. Còn: " + chiTiet.getSoLuongTonKho() + ", Yêu cầu: " + newQuantity);
                throw new RuntimeException("Số lượng tồn kho không đủ sau khi cộng dồn: chỉ còn " + chiTiet.getSoLuongTonKho());
            }
            existingItem.setSoLuong(newQuantity);
            existingItem.setThanhTien(chiTiet.getGia().multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            if (quantity > chiTiet.getSoLuongTonKho()) {
                System.err.println("LỖI: Số lượng tồn kho không đủ. Còn: " + chiTiet.getSoLuongTonKho() + ", Yêu cầu: " + quantity);
                throw new RuntimeException("Số lượng tồn kho không đủ: chỉ còn " + chiTiet.getSoLuongTonKho());
            }
            GioHangItemDTO newItem = new GioHangItemDTO();
            newItem.setIdChiTietSanPham(productDetailId);
            newItem.setTenSanPham(chiTiet.getSanPham() != null ? chiTiet.getSanPham().getTenSanPham() : "Sản phẩm không rõ");
            newItem.setMauSac(chiTiet.getMauSac() != null ? chiTiet.getMauSac().getTenMau() : "Không rõ");
            newItem.setKichCo(chiTiet.getKichCo() != null ? chiTiet.getKichCo().getTen() : "Không rõ");
            newItem.setSoLuong(quantity);
            newItem.setGia(chiTiet.getGia());
            newItem.setThanhTien(chiTiet.getGia().multiply(BigDecimal.valueOf(quantity)));
            currentCart.add(newItem);
        }

        saveCart(currentCart);
        // Reset thông tin giảm giá khi giỏ hàng thay đổi
        getSession().removeAttribute(APPLIED_DISCOUNT_SESSION_KEY);
        getSession().removeAttribute(APPLIED_VOUCHER_ID_SESSION_KEY);
        System.out.println("--- themVaoGioHang: Kết thúc, giỏ hàng đã thay đổi, giảm giá bị reset ---");
        return layGioHang(shippingFee);
    }

    public void capNhatTongGioHang(GioHangDTO gioHangDTO, List<GioHangItemDTO> danhSachSanPham, BigDecimal shippingFee) {
        gioHangDTO.setDanhSachSanPham(new ArrayList<>(danhSachSanPham));

        BigDecimal tongTienHang = danhSachSanPham.stream()
                .map(GioHangItemDTO::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        gioHangDTO.setTongTienHang(formatCurrency(tongTienHang));

        BigDecimal giamGia = (BigDecimal) getSession().getAttribute(APPLIED_DISCOUNT_SESSION_KEY);
        UUID idPhieuGiamGia = (UUID) getSession().getAttribute(APPLIED_VOUCHER_ID_SESSION_KEY);

        if (giamGia == null) {
            giamGia = BigDecimal.ZERO;
        }

        gioHangDTO.setGiamGia(formatCurrency(giamGia));
        gioHangDTO.setIdPhieuGiamGia(idPhieuGiamGia);
        gioHangDTO.setDaApDungPhieuGiamGia(idPhieuGiamGia != null);

        BigDecimal tong = tongTienHang.subtract(giamGia).add(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        gioHangDTO.setTong(formatCurrency(tong));

        gioHangDTO.setDaXoaPhieuGiamGia(false);
        gioHangDTO.setGiamGiaDaXoa(formatCurrency(BigDecimal.ZERO));
        gioHangDTO.setPhiVanChuyen(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        gioHangDTO.setKhachHangDaChon(gioHangDTO.getKhachHangDaChon());
        gioHangDTO.setIdPhieuGiamGia(gioHangDTO.getIdPhieuGiamGia());
        gioHangDTO.setPhuongThucBanHang(gioHangDTO.getPhuongThucBanHang());
        gioHangDTO.setPhuongThucThanhToan(gioHangDTO.getPhuongThucThanhToan());
    }

    public GioHangDTO layGioHang(BigDecimal shippingFee) {
        System.out.println("--- layGioHang: Bắt đầu ---");
        List<GioHangItemDTO> currentCart = getCurrentCart();
        GioHangDTO gioHangDTO = new GioHangDTO();
        capNhatTongGioHang(gioHangDTO, currentCart, shippingFee);

        List<GioHangItemDTO> updatedCartItems = currentCart.stream().map(item -> {
            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(item.getIdChiTietSanPham());
            if (chiTiet != null) {
                item.setStockQuantity(chiTiet.getSoLuongTonKho());
            }
            return item;
        }).collect(Collectors.toList());
        gioHangDTO.setDanhSachSanPham(updatedCartItems);

        System.out.println("--- layGioHang: Kết thúc, giỏ hàng trả về: ---");
        System.out.println("  Tổng tiền hàng: " + gioHangDTO.getTongTienHang());
        System.out.println("  Giảm giá: " + gioHangDTO.getGiamGia());
        System.out.println("  Tổng cuối: " + gioHangDTO.getTong());
        System.out.println("  ID Voucher: " + gioHangDTO.getIdPhieuGiamGia());
        System.out.println("----------------------------------------------");
        return gioHangDTO;
    }

    public GioHangDTO capNhatGioHang(UUID productDetailId, int quantity, BigDecimal shippingFee) {
        System.out.println("--- capNhatGioHang: Bắt đầu ---");
        List<GioHangItemDTO> currentCart = getCurrentCart();

        GioHangItemDTO existingItem = currentCart.stream()
                .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                .findFirst()
                .orElse(null);

        ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
        if (chiTiet == null) {
            System.err.println("LỖI: Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
            throw new RuntimeException("Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
        }

        if (quantity > chiTiet.getSoLuongTonKho()) {
            throw new RuntimeException("Số lượng tồn kho không đủ: chỉ còn " + chiTiet.getSoLuongTonKho());
        }

        if (existingItem != null) {
            existingItem.setSoLuong(quantity);
            existingItem.setThanhTien(chiTiet.getGia().multiply(BigDecimal.valueOf(quantity)));
        } else {
            System.out.println("CẢNH BÁO: Sản phẩm " + productDetailId + " không có trong giỏ, đang thêm mới khi cập nhật.");
            GioHangItemDTO newItem = new GioHangItemDTO();
            newItem.setIdChiTietSanPham(productDetailId);
            newItem.setTenSanPham(chiTiet.getSanPham() != null ? chiTiet.getSanPham().getTenSanPham() : "Sản phẩm không rõ");
            newItem.setMauSac(chiTiet.getMauSac() != null ? chiTiet.getMauSac().getTenMau() : "Không rõ");
            newItem.setKichCo(chiTiet.getKichCo() != null ? chiTiet.getKichCo().getTen() : "Không rõ");
            newItem.setSoLuong(quantity);
            newItem.setGia(chiTiet.getGia());
            newItem.setThanhTien(chiTiet.getGia().multiply(BigDecimal.valueOf(quantity)));
            currentCart.add(newItem);
        }

        saveCart(currentCart);
        getSession().removeAttribute(APPLIED_DISCOUNT_SESSION_KEY);
        getSession().removeAttribute(APPLIED_VOUCHER_ID_SESSION_KEY);
        System.out.println("--- capNhatGioHang: Kết thúc, giỏ hàng đã thay đổi, giảm giá bị reset ---");
        return layGioHang(shippingFee);
    }

    public GioHangDTO xoaKhoiGioHang(UUID productDetailId, BigDecimal shippingFee) {
        System.out.println("--- xoaKhoiGioHang: Bắt đầu ---");
        List<GioHangItemDTO> currentCart = getCurrentCart();
        boolean removed = currentCart.removeIf(item -> item.getIdChiTietSanPham().equals(productDetailId));

        if (removed) {
            saveCart(currentCart);
            getSession().removeAttribute(APPLIED_DISCOUNT_SESSION_KEY);
            getSession().removeAttribute(APPLIED_VOUCHER_ID_SESSION_KEY);
            System.out.println("--- xoaKhoiGioHang: Kết thúc, sản phẩm đã xóa, giảm giá bị reset ---");
        } else {
            System.out.println("--- xoaKhoiGioHang: Không tìm thấy sản phẩm để xóa ---");
        }
        return layGioHang(shippingFee);
    }

    public GioHangDTO xoaTatCaGioHang() {
        System.out.println("--- xoaTatCaGioHang: Bắt đầu ---");
        List<GioHangItemDTO> currentCart = getCurrentCart();
        currentCart.clear();

        saveCart(currentCart);
        getSession().removeAttribute(APPLIED_DISCOUNT_SESSION_KEY);
        getSession().removeAttribute(APPLIED_VOUCHER_ID_SESSION_KEY);
        System.out.println("--- xoaTatCaGioHang: Kết thúc, giỏ hàng đã xóa, giảm giá bị reset ---");
        return layGioHang(BigDecimal.ZERO);
    }

    public GioHangDTO apDungPhieuGiamGia(UUID voucherId, BigDecimal shippingFee) {
        System.out.println("\n--- apDungPhieuGiamGia: Bắt đầu xử lý ---");
        System.out.println("ID Phiếu giảm giá nhận được: " + voucherId);
        System.out.println("Phí vận chuyển: " + shippingFee);

        List<GioHangItemDTO> currentCart = getCurrentCart();
        BigDecimal tongTienHang = currentCart.stream()
                .map(GioHangItemDTO::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("Tổng tiền hàng hiện tại trong giỏ: " + formatCurrency(tongTienHang));

        BigDecimal giamGiaThucTe = BigDecimal.ZERO;
        UUID idPhieuGiamGiaApDung = null;

        PhieuGiamGia phieu = phieuGiamGiaRepository.findById(voucherId).orElse(null);

        if (phieu == null) {
            System.out.println("LỖI: Không tìm thấy phiếu giảm giá với ID: " + voucherId + ". Không áp dụng giảm giá.");
            getSession().removeAttribute(APPLIED_DISCOUNT_SESSION_KEY);
            getSession().removeAttribute(APPLIED_VOUCHER_ID_SESSION_KEY);
        } else {
            System.out.println("--- Thông tin Phiếu giảm giá tìm thấy ---");
            System.out.println("  Mã phiếu: " + phieu.getMa());
            System.out.println("  Tên phiếu: " + phieu.getTen());
            System.out.println("  Loại giảm giá: " + phieu.getLoai());
            System.out.println("  Giá trị giảm: " + phieu.getGiaTriGiam());
            System.out.println("  Giá trị giảm tối đa: " + phieu.getGiaTriGiamToiDa());
            System.out.println("  Giá trị giảm tối thiểu: " + phieu.getGiaTriGiamToiThieu());
            System.out.println("  Ngày bắt đầu (DB): " + phieu.getNgayBatDau()); // Giờ đây chắc chắn là LocalDate
            System.out.println("  Ngày kết thúc (DB): " + phieu.getNgayKetThuc()); // Giờ đây chắc chắn là LocalDate
            System.out.println("  Số lượng còn lại: " + phieu.getSoLuong());
            System.out.println("----------------------------------------");

            boolean duDieuKienApDung = true;
            LocalDate today = LocalDate.now(); // Lấy ngày hiện tại (chỉ ngày, không giờ)

            // So sánh LocalDate với LocalDate
            if (phieu.getNgayKetThuc() != null && phieu.getNgayKetThuc().isBefore(today)) {
                System.out.println("  Voucher HẾT HẠN sử dụng. (Ngày kết thúc: " + phieu.getNgayKetThuc() + ")");
                duDieuKienApDung = false;
            }
            if (phieu.getNgayBatDau() != null && phieu.getNgayBatDau().isAfter(today)) {
                System.out.println("  Voucher CHƯA ĐẾN HẠN sử dụng. (Ngày bắt đầu: " + phieu.getNgayBatDau() + ")");
                duDieuKienApDung = false;
            }
            if (phieu.getSoLuong() != null && phieu.getSoLuong() <= 0) {
                System.out.println("  Voucher ĐÃ HẾT SỐ LƯỢNG.");
                duDieuKienApDung = false;
            }
            if (phieu.getGiaTriGiamToiThieu() != null && tongTienHang.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                System.out.println("  TỔNG TIỀN HÀNG (" + formatCurrency(tongTienHang) + ") KHÔNG ĐẠT MỨC TỐI THIỂU (" + formatCurrency(phieu.getGiaTriGiamToiThieu()) + ") để áp dụng voucher.");
                duDieuKienApDung = false;
            }

            if (duDieuKienApDung) {
                if ("PERCENT".equals(phieu.getLoai())) {
                    BigDecimal phanTramGiam = phieu.getGiaTriGiam();
                    BigDecimal soTienGiamDuKien = tongTienHang.multiply(phanTramGiam)
                            .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
                    System.out.println("  [LOẠI PERCENT] Giảm dự kiến: " + formatCurrency(soTienGiamDuKien));

                    if (phieu.getGiaTriGiamToiDa() != null && soTienGiamDuKien.compareTo(phieu.getGiaTriGiamToiDa()) > 0) {
                        giamGiaThucTe = phieu.getGiaTriGiamToiDa();
                        System.out.println("  [LOẠI PERCENT] Giảm vượt quá giới hạn tối đa. Áp dụng: " + formatCurrency(giamGiaThucTe));
                    } else {
                        giamGiaThucTe = soTienGiamDuKien;
                        System.out.println("  [LOẠI PERCENT] Áp dụng: " + formatCurrency(giamGiaThucTe));
                    }
                } else if ("CASH".equals(phieu.getLoai())) {
                    giamGiaThucTe = phieu.getGiaTriGiam();
                    System.out.println("  [LOẠI CASH] Giảm tiền mặt: " + formatCurrency(giamGiaThucTe));

                    if (giamGiaThucTe.compareTo(tongTienHang) > 0) {
                        giamGiaThucTe = tongTienHang;
                        System.out.println("  [LOẠI CASH] Giảm tiền mặt được điều chỉnh để không vượt quá tổng tiền hàng: " + formatCurrency(giamGiaThucTe));
                    }
                } else {
                    System.out.println("  LOẠI PHIẾU GIẢM GIÁ KHÔNG HỢP LỆ: " + phieu.getLoai() + ". Không áp dụng giảm giá.");
                }

                if (giamGiaThucTe.compareTo(BigDecimal.ZERO) > 0) {
                    idPhieuGiamGiaApDung = voucherId;
                    System.out.println("  Voucher được áp dụng thành công! Số tiền giảm: " + formatCurrency(giamGiaThucTe));
                } else {
                    System.out.println("  Số tiền giảm thực tế là 0. Voucher không được áp dụng.");
                }
            } else {
                System.out.println("  Voucher không đủ điều kiện áp dụng.");
            }
        }

        getSession().setAttribute(APPLIED_DISCOUNT_SESSION_KEY, giamGiaThucTe);
        getSession().setAttribute(APPLIED_VOUCHER_ID_SESSION_KEY, idPhieuGiamGiaApDung);

        GioHangDTO updatedGioHangDTO = layGioHang(shippingFee);

        System.out.println("--- apDungPhieuGiamGia: Kết thúc xử lý ---");
        System.out.println("  Giỏ hàng trả về sau khi áp dụng: " + updatedGioHangDTO);
        System.out.println("----------------------------------------------\n");
        return updatedGioHangDTO;
    }
}