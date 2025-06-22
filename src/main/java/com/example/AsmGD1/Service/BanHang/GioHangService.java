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

    public String formatCurrency(BigDecimal amount) {
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
            newItem.setHinhAnh(chiTiet.getSanPham().getUrlHinhAnh() != null ? chiTiet.getSanPham().getUrlHinhAnh() : "https://via.placeholder.com/50");
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

        // Tính tổng tiền hàng
        BigDecimal tongTienHang = danhSachSanPham.stream()
                .map(GioHangItemDTO::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        gioHangDTO.setTongTienHangValue(tongTienHang); // Lưu giá trị số
        gioHangDTO.setTongTienHang(formatCurrency(tongTienHang)); // Định dạng

        // Lấy giá trị giảm giá từ session
        BigDecimal giamGia = (BigDecimal) getSession().getAttribute(APPLIED_DISCOUNT_SESSION_KEY);
        UUID idPhieuGiamGia = (UUID) getSession().getAttribute(APPLIED_VOUCHER_ID_SESSION_KEY);

        if (giamGia == null) {
            giamGia = BigDecimal.ZERO;
        }

        gioHangDTO.setGiamGiaValue(giamGia); // Lưu giá trị số
        gioHangDTO.setGiamGia(formatCurrency(giamGia)); // Định dạng
        gioHangDTO.setIdPhieuGiamGia(idPhieuGiamGia);
        gioHangDTO.setDaApDungPhieuGiamGia(idPhieuGiamGia != null);

        // Tính tổng tiền sau giảm giá
        BigDecimal tong = tongTienHang.subtract(giamGia).add(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        gioHangDTO.setTongValue(tong); // Lưu giá trị số
        gioHangDTO.setTong(formatCurrency(tong)); // Định dạng
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

        System.out.println("Debug - layGioHang: TongTienHangValue: " + gioHangDTO.getTongTienHangValue());
        System.out.println("Debug - layGioHang: GiamGiaValue: " + gioHangDTO.getGiamGiaValue());
        System.out.println("Debug - layGioHang: TongValue: " + gioHangDTO.getTongValue());
        System.out.println("Debug - layGioHang: TongTienHang: " + gioHangDTO.getTongTienHang());
        System.out.println("Debug - layGioHang: GiamGia: " + gioHangDTO.getGiamGia());
        System.out.println("Debug - layGioHang: Tong: " + gioHangDTO.getTong());
        System.out.println("Debug - layGioHang: IdPhieuGiamGia: " + gioHangDTO.getIdPhieuGiamGia());

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
            newItem.setHinhAnh(chiTiet.getSanPham().getUrlHinhAnh() != null ? chiTiet.getSanPham().getUrlHinhAnh() : "https://via.placeholder.com/50");
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

    public GioHangDTO apDungPhieuGiamGia(UUID voucherId, BigDecimal shippingFee, GioHangDTO gioHang) {
        System.out.println("--- apDungPhieuGiamGia: Bắt đầu, voucherId: " + voucherId + ", shippingFee: " + shippingFee + " ---");
        BigDecimal tongTienHang = gioHang.getTongTienHangValue(); // Sử dụng giá trị từ GioHangDTO
        System.out.println("Debug - Tổng tiền hàng từ giỏ: " + tongTienHang);

        BigDecimal giamGia = BigDecimal.ZERO;
        UUID appliedVoucherId = null;

        if (voucherId != null) {
            PhieuGiamGia phieu = phieuGiamGiaRepository.findById(voucherId)
                    .orElseThrow(() -> new RuntimeException("Phiếu giảm giá không tồn tại."));
            LocalDate today = LocalDate.now();
            System.out.println("Debug - Voucher info: " + phieu.getMa() + ", NgayBatDau: " + phieu.getNgayBatDau() +
                    ", NgayKetThuc: " + phieu.getNgayKetThuc() + ", SoLuong: " + phieu.getSoLuong() +
                    ", GiaTriGiamToiThieu: " + phieu.getGiaTriGiamToiThieu());

            if (phieu.getNgayBatDau() != null && phieu.getNgayBatDau().isAfter(today) ||
                    phieu.getNgayKetThuc() != null && phieu.getNgayKetThuc().isBefore(today) ||
                    phieu.getSoLuong() != null && phieu.getSoLuong() <= 0 ||
                    phieu.getGiaTriGiamToiThieu() != null && tongTienHang.compareTo(phieu.getGiaTriGiamToiThieu()) < 0) {
                System.out.println("Debug - Voucher không hợp lệ: " +
                        "NgayBatDau: " + (phieu.getNgayBatDau() != null && phieu.getNgayBatDau().isAfter(today)) +
                        ", NgayKetThuc: " + (phieu.getNgayKetThuc() != null && phieu.getNgayKetThuc().isBefore(today)) +
                        ", SoLuong: " + (phieu.getSoLuong() != null && phieu.getSoLuong() <= 0) +
                        ", GiaTriGiamToiThieu: " + (tongTienHang.compareTo(phieu.getGiaTriGiamToiThieu()) < 0));
            } else if ("PERCENT".equals(phieu.getLoai())) {
                giamGia = tongTienHang.multiply(phieu.getGiaTriGiam())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (phieu.getGiaTriGiamToiDa() != null && giamGia.compareTo(phieu.getGiaTriGiamToiDa()) > 0) {
                    giamGia = phieu.getGiaTriGiamToiDa();
                }
                appliedVoucherId = voucherId;
                System.out.println("Debug - Áp dụng giảm giá %: " + giamGia);
            } else if ("CASH".equals(phieu.getLoai())) {
                giamGia = phieu.getGiaTriGiam();
                if (giamGia.compareTo(tongTienHang) > 0) {
                    giamGia = tongTienHang;
                }
                appliedVoucherId = voucherId;
                System.out.println("Debug - Áp dụng giảm giá tiền mặt: " + giamGia);
            }
        } else {
            System.out.println("Debug - Không có voucherId, không áp dụng giảm giá.");
        }

        getSession().setAttribute(APPLIED_DISCOUNT_SESSION_KEY, giamGia);
        getSession().setAttribute(APPLIED_VOUCHER_ID_SESSION_KEY, appliedVoucherId);
        System.out.println("Debug - Giảm giá lưu session: " + giamGia + ", Voucher ID: " + appliedVoucherId);
        capNhatTongGioHang(gioHang, gioHang.getDanhSachSanPham(), shippingFee); // Cập nhật lại gioHang với giamGia
        System.out.println("--- apDungPhieuGiamGia: Kết thúc, Tổng tiền sau giảm: " + gioHang.getTong() + " ---");
        return gioHang;
    }}