package com.example.AsmGD1.service.BanHang;

import com.example.AsmGD1.dto.BanHang.GioHangDTO;
import com.example.AsmGD1.dto.BanHang.GioHangItemDTO;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.service.SanPham.ChiTietSanPhamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GioHangService {

    @Autowired
    private ChiTietSanPhamService chiTietSanPhamService;

    private static final String CART_SESSION_KEY = "CART";

    private String formatCurrency(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
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
        GioHangDTO gioHangDTO = new GioHangDTO();
        List<GioHangItemDTO> currentCart = getCurrentCart();

        // Tìm sản phẩm có cùng productDetailId
        GioHangItemDTO existingItem = currentCart.stream()
                .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                .findFirst()
                .orElse(null);

        ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
        if (chiTiet == null) {
            throw new RuntimeException("Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
        }

        if (chiTiet.getSoLuongTonKho() < quantity) {
            throw new RuntimeException("Số lượng tồn kho không đủ: chỉ còn " + chiTiet.getSoLuongTonKho());
        }

        if (existingItem != null) {
            // Cộng dồn số lượng
            int newQuantity = existingItem.getSoLuong() + quantity;
            if (chiTiet.getSoLuongTonKho() < newQuantity) {
                throw new RuntimeException("Số lượng tồn kho không đủ sau khi cộng dồn: chỉ còn " + chiTiet.getSoLuongTonKho());
            }
            existingItem.setSoLuong(newQuantity);
            existingItem.setThanhTien(chiTiet.getGia().multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            // Thêm mới sản phẩm
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

        capNhatTongGioHang(gioHangDTO, currentCart, shippingFee);
        saveCart(currentCart);
        return gioHangDTO;
    }

    public void capNhatTongGioHang(GioHangDTO gioHangDTO, List<GioHangItemDTO> danhSachSanPham, BigDecimal shippingFee) {
        gioHangDTO.setDanhSachSanPham(danhSachSanPham);

        BigDecimal tongTienHang = danhSachSanPham.stream()
                .map(GioHangItemDTO::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        gioHangDTO.setTongTienHang(formatCurrency(tongTienHang));

        BigDecimal giamGia = BigDecimal.ZERO;
        if (gioHangDTO.getIdPhieuGiamGia() != null) {
            // Giả định giảm 10,000 VNĐ (có thể tích hợp logic thực tế với voucher)
            giamGia = BigDecimal.valueOf(10000).min(tongTienHang); // Không giảm vượt tổng tiền
        }
        gioHangDTO.setGiamGia(formatCurrency(giamGia));

        BigDecimal tong = tongTienHang.subtract(giamGia)
                .add(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        gioHangDTO.setTong(formatCurrency(tong));

        gioHangDTO.setDaXoaPhieuGiamGia(false);
        gioHangDTO.setGiamGiaDaXoa(formatCurrency(BigDecimal.ZERO));
        gioHangDTO.setDaApDungPhieuGiamGia(gioHangDTO.getIdPhieuGiamGia() != null);
        gioHangDTO.setPhiVanChuyen(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        gioHangDTO.setKhachHangDaChon(gioHangDTO.getKhachHangDaChon());
        gioHangDTO.setIdPhieuGiamGia(gioHangDTO.getIdPhieuGiamGia());
        gioHangDTO.setPhuongThucBanHang(gioHangDTO.getPhuongThucBanHang());
        gioHangDTO.setPhuongThucThanhToan(gioHangDTO.getPhuongThucThanhToan());
    }

    public GioHangDTO layGioHang(BigDecimal shippingFee) {
        List<GioHangItemDTO> currentCart = getCurrentCart();
        GioHangDTO gioHangDTO = new GioHangDTO();
        capNhatTongGioHang(gioHangDTO, currentCart, shippingFee);

        // Thêm stockQuantity cho từng sản phẩm
        gioHangDTO.setDanhSachSanPham(currentCart.stream().map(item -> {
            ChiTietSanPham chiTiet = chiTietSanPhamService.findById(item.getIdChiTietSanPham());
            if (chiTiet != null) {
                item.setStockQuantity(chiTiet.getSoLuongTonKho()); // Giả sử có setter stockQuantity trong GioHangItemDTO
            }
            return item;
        }).collect(Collectors.toList()));

        return gioHangDTO;
    }

    public GioHangDTO capNhatGioHang(UUID productDetailId, int quantity, BigDecimal shippingFee) {
        List<GioHangItemDTO> currentCart = getCurrentCart();

        // Tìm sản phẩm trong giỏ hàng
        GioHangItemDTO existingItem = currentCart.stream()
                .filter(item -> item.getIdChiTietSanPham().equals(productDetailId))
                .findFirst()
                .orElse(null);

        ChiTietSanPham chiTiet = chiTietSanPhamService.findById(productDetailId);
        if (chiTiet == null) {
            throw new RuntimeException("Không tìm thấy biến thể sản phẩm với ID: " + productDetailId);
        }

        if (quantity > chiTiet.getSoLuongTonKho()) {
            throw new RuntimeException("Số lượng tồn kho không đủ: chỉ còn " + chiTiet.getSoLuongTonKho());
        }

        if (existingItem != null) {
            // Cập nhật số lượng
            existingItem.setSoLuong(quantity);
            existingItem.setThanhTien(chiTiet.getGia().multiply(BigDecimal.valueOf(quantity)));
        } else {
            // Thêm mới nếu không tìm thấy
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

        GioHangDTO gioHangDTO = new GioHangDTO();
        capNhatTongGioHang(gioHangDTO, currentCart, shippingFee);
        saveCart(currentCart);
        return gioHangDTO;
    }

    public GioHangDTO xoaKhoiGioHang(UUID productDetailId, BigDecimal shippingFee) {
        List<GioHangItemDTO> currentCart = getCurrentCart();
        currentCart.removeIf(item -> item.getIdChiTietSanPham().equals(productDetailId));

        GioHangDTO gioHangDTO = new GioHangDTO();
        capNhatTongGioHang(gioHangDTO, currentCart, shippingFee);
        saveCart(currentCart);
        return gioHangDTO;
    }

    public GioHangDTO xoaTatCaGioHang() {
        List<GioHangItemDTO> currentCart = getCurrentCart();
        currentCart.clear();

        GioHangDTO gioHangDTO = new GioHangDTO();
        capNhatTongGioHang(gioHangDTO, currentCart, BigDecimal.ZERO);
        saveCart(currentCart);
        return gioHangDTO;
    }

    public GioHangDTO apDungPhieuGiamGia(UUID voucherId, BigDecimal shippingFee) {
        GioHangDTO gioHangDTO = new GioHangDTO();
        gioHangDTO.setIdPhieuGiamGia(voucherId);
        gioHangDTO.setDaApDungPhieuGiamGia(true);

        // Logic giả định: Giảm 10,000 VNĐ hoặc 10% tổng tiền (lấy giá trị nhỏ hơn)
        List<GioHangItemDTO> currentCart = getCurrentCart();
        BigDecimal tongTienHang = currentCart.stream()
                .map(GioHangItemDTO::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal giamGia = BigDecimal.valueOf(10000).min(tongTienHang.multiply(BigDecimal.valueOf(0.1)));
        gioHangDTO.setGiamGia(formatCurrency(giamGia));

        capNhatTongGioHang(gioHangDTO, currentCart, shippingFee);
        return gioHangDTO;
    }
}