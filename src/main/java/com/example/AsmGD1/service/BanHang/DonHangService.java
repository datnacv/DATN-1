package com.example.AsmGD1.service.BanHang;

import com.example.AsmGD1.dto.BanHang.*;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangRepository;
import com.example.AsmGD1.repository.BanHang.DonHangTamRepository;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaCuaNguoiDungRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DonHangService {
    @Autowired
    private DonHangRepository donHangRepository;
    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;
    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    @Autowired
    private PhieuGiamGiaCuaNguoiDungRepository phieuGiamGiaCuaNguoiDungRepository;
    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    @Autowired
    private DonHangTamRepository donHangTamRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Transactional
    public KetQuaDonHangDTO taoDonHang(DonHangDTO donHangDTO) {
        String soDienThoai = donHangDTO.getSoDienThoaiKhachHang();
        if (soDienThoai == null || soDienThoai.trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại khách hàng không được để trống.");
        }

        if ("Giao hàng".equals(donHangDTO.getPhuongThucBanHang()) &&
                (donHangDTO.getDiaChiGiaoHang() == null || donHangDTO.getDiaChiGiaoHang().trim().isEmpty()) &&
                !soDienThoai.equals("0999999999")) {
            throw new RuntimeException("Địa chỉ giao hàng không được để trống khi chọn phương thức giao hàng.");
        }

        NguoiDung khachHang = nguoiDungRepository.findBySoDienThoai(soDienThoai)
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại với số điện thoại: " + soDienThoai));

        DonHang donHang = new DonHang();
        donHang.setNguoiDung(khachHang);
        donHang.setMaDonHang(taoMaDonHang());
        donHang.setPhiVanChuyen(Optional.ofNullable(donHangDTO.getPhiVanChuyen()).orElse(BigDecimal.ZERO));
        donHang.setPhuongThucThanhToan(
                phuongThucThanhToanRepository.findById(donHangDTO.getPhuongThucThanhToan()).orElse(null));
        donHang.setPhuongThucBanHang(donHangDTO.getPhuongThucBanHang());
        donHang.setDiaChiGiaoHang(donHangDTO.getDiaChiGiaoHang());
        donHang.setThoiGianTao(LocalDateTime.now());

        BigDecimal tongTien = BigDecimal.ZERO;
        BigDecimal tienGiam = BigDecimal.ZERO;
        Map<UUID, Integer> soLuongTonKho = new HashMap<>();

        if (donHangDTO.getDanhSachSanPham() == null || donHangDTO.getDanhSachSanPham().isEmpty()) {
            throw new RuntimeException("Danh sách sản phẩm không được để trống.");
        }

        for (GioHangItemDTO item : donHangDTO.getDanhSachSanPham()) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(item.getIdChiTietSanPham())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + item.getIdChiTietSanPham()));

            if (chiTiet.getSoLuongTonKho() < item.getSoLuong()) {
                throw new RuntimeException("Số lượng tồn kho không đủ cho sản phẩm: " + chiTiet.getSanPham().getTenSanPham());
            }

            ChiTietDonHang chiTietDonHang = new ChiTietDonHang();
            chiTietDonHang.setChiTietSanPham(chiTiet);
            chiTietDonHang.setSoLuong(item.getSoLuong());
            chiTietDonHang.setGia(item.getGia());
            chiTietDonHang.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
            chiTietDonHang.setThanhTien(item.getThanhTien());
            chiTietDonHang.setGhiChu(item.getMauSac() + ", " + item.getKichCo());
            donHang.addChiTietDonHang(chiTietDonHang);

            chiTiet.setSoLuongTonKho(chiTiet.getSoLuongTonKho() - item.getSoLuong());
            chiTietSanPhamRepository.save(chiTiet);

            tongTien = tongTien.add(item.getThanhTien());
            soLuongTonKho.put(chiTiet.getId(), chiTiet.getSoLuongTonKho());
        }

        if (donHangDTO.getIdPhieuGiamGia() != null) {
            PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findById(donHangDTO.getIdPhieuGiamGia())
                    .orElseThrow(() -> new RuntimeException("Phiếu giảm giá không tồn tại."));

            if ("Phần trăm".equals(phieuGiamGia.getLoai())) {
                tienGiam = tongTien.multiply(phieuGiamGia.getGiaTriGiam())
                        .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
                if (phieuGiamGia.getGiaTriGiamToiDa() != null && tienGiam.compareTo(phieuGiamGia.getGiaTriGiamToiDa()) > 0) {
                    tienGiam = phieuGiamGia.getGiaTriGiamToiDa();
                }
            } else {
                tienGiam = phieuGiamGia.getGiaTriGiam().min(tongTien);
            }

            // Trừ lượt dùng của phiếu
            PhieuGiamGiaCuaNguoiDung phieuGiam = phieuGiamGiaCuaNguoiDungRepository
                    .findByPhieuGiamGia_IdAndNguoiDung_Id(phieuGiamGia.getId(), khachHang.getId()).orElse(null);
            if (phieuGiam != null && phieuGiam.getSoLuotConLai() > 0) {
                phieuGiam.setSoLuotConLai(phieuGiam.getSoLuotConLai() - 1);
                phieuGiamGiaCuaNguoiDungRepository.save(phieuGiam);
            } else if (phieuGiam != null) {
                throw new RuntimeException("Phiếu giảm giá đã hết lượt sử dụng.");
            }

            donHang.setTienGiam(tienGiam);
        } else {
            donHang.setTienGiam(BigDecimal.ZERO);
        }

        // Tính tổng tiền đơn hàng
        BigDecimal tongTienDonHang = tongTien.add(donHang.getPhiVanChuyen()).subtract(tienGiam);
        donHang.setTongTien(tongTienDonHang);

        // Xử lý tiền khách đưa và trạng thái thanh toán
        BigDecimal tienKhachDua = Optional.ofNullable(donHangDTO.getSoTienKhachDua()).orElse(BigDecimal.ZERO);
        donHang.setSoTienKhachDua(tienKhachDua);

        if (donHang.getPhuongThucThanhToan() != null &&
                "Tiền mặt".equals(donHang.getPhuongThucThanhToan().getTenPhuongThuc())) {
            if (tienKhachDua.compareTo(tongTienDonHang) < 0) {
                throw new RuntimeException("Số tiền khách đưa không đủ.");
            }
            donHang.setTrangThaiThanhToan(true);
            donHang.setThoiGianThanhToan(LocalDateTime.now());
        } else {
            donHang.setTrangThaiThanhToan(false);
        }

        // Lưu đơn hàng
        donHangRepository.save(donHang);

        KetQuaDonHangDTO ketQua = new KetQuaDonHangDTO();
        ketQua.setMaDonHang(donHang.getMaDonHang());
        ketQua.setSoLuongTonKho(soLuongTonKho);
        ketQua.setChangeAmount(tienKhachDua.subtract(tongTienDonHang));

        return ketQua;
    }


    @Transactional
    public DonHangDTO giuDonHang(DonHangDTO donHangDTO) {
        try {
            DonHangTam donHangTam = new DonHangTam();
            donHangTam.setId(UUID.randomUUID());
            donHangTam.setMaDonHangTam("KH" + System.currentTimeMillis());
            donHangTam.setTabId(donHangDTO.getTabId()); // Set tabId

            String soDienThoai = donHangDTO.getSoDienThoaiKhachHang();
            if (soDienThoai == null || soDienThoai.trim().isEmpty()) {
                throw new RuntimeException("Số điện thoại khách hàng không được để trống.");
            }
            NguoiDung khachHang = nguoiDungRepository.findBySoDienThoai(soDienThoai)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với số điện thoại: " + soDienThoai));
            donHangTam.setKhachHang(khachHang.getId());
            donHangTam.setSoDienThoaiKhachHang(donHangDTO.getSoDienThoaiKhachHang());
            BigDecimal tongTien = BigDecimal.ZERO;
            if (donHangDTO.getDanhSachSanPham() != null && !donHangDTO.getDanhSachSanPham().isEmpty()) {
                List<GioHangItemDTO> enhancedItems = new ArrayList<>();
                for (GioHangItemDTO item : donHangDTO.getDanhSachSanPham()) {
                    if (item.getTenSanPham() == null || item.getMauSac() == null || item.getKichCo() == null) {
                        ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(item.getIdChiTietSanPham())
                                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + item.getIdChiTietSanPham()));
                        item.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
                        item.setMauSac(chiTiet.getMauSac().getTenMau());
                        item.setKichCo(chiTiet.getKichCo().getTen());
                    }
                    enhancedItems.add(item);
                    tongTien = tongTien.add(item.getThanhTien());
                }
                donHangDTO.setDanhSachSanPham(enhancedItems);
            } else {
                throw new RuntimeException("Danh sách sản phẩm không được để trống.");
            }

            BigDecimal phiVanChuyen = donHangDTO.getPhiVanChuyen() != null ? donHangDTO.getPhiVanChuyen() : BigDecimal.ZERO;
            BigDecimal giamGia = BigDecimal.ZERO;
            if (donHangDTO.getIdPhieuGiamGia() != null) {
                PhieuGiamGia phieuGiamGia = phieuGiamGiaRepository.findById(donHangDTO.getIdPhieuGiamGia())
                        .orElseThrow(() -> new RuntimeException("Phiếu giảm giá không tồn tại."));
                giamGia = phieuGiamGia.getGiaTriGiam().min(tongTien);
            }
            donHangTam.setTong(tongTien.add(phiVanChuyen).subtract(giamGia));
            donHangTam.setPhiVanChuyen(phiVanChuyen);

            if (donHangDTO.getDanhSachSanPham() != null) {
                try {
                    String danhSachSanPhamJson = objectMapper.writeValueAsString(donHangDTO.getDanhSachSanPham());
                    donHangTam.setDanhSachSanPham(danhSachSanPhamJson);
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi khi chuyển đổi danh sách sản phẩm thành JSON: " + e.getMessage());
                }
            }

            donHangTam.setPhuongThucThanhToan(donHangDTO.getPhuongThucThanhToan() != null ? donHangDTO.getPhuongThucThanhToan().toString() : null);
            donHangTam.setPhuongThucBanHang(donHangDTO.getPhuongThucBanHang());
            donHangTam.setPhieuGiamGia(donHangDTO.getIdPhieuGiamGia());
            donHangTam.setThoiGianTao(LocalDateTime.now());

            donHangTamRepository.save(donHangTam);

            HttpSession session = layPhien();
            Map<UUID, Map<String, Object>> tempStockChanges = (Map<UUID, Map<String, Object>>) session.getAttribute("tempStockChanges");
            if (tempStockChanges != null) {
                session.removeAttribute("tempStockChanges");
            }

            return donHangDTO;
        } catch (Exception e) {
            throw new RuntimeException("Không thể giữ đơn hàng: " + e.getMessage());
        }
    }

    public List<DonHangTamDTO> layDanhSachDonHangTam() {
        List<DonHangTam> heldOrders = donHangTamRepository.findAll();
        return heldOrders.stream().map(order -> {
            DonHangTamDTO dto = new DonHangTamDTO();
            dto.setId(order.getId());
            if (order.getKhachHang() != null) {
                NguoiDung nguoiDung = nguoiDungService.findById(order.getKhachHang());
                dto.setTenKhachHang(nguoiDung != null ? nguoiDung.getHoTen() : "Không rõ");
            } else {
                dto.setTenKhachHang("Không rõ");
            }
            dto.setTong(order.getTong() != null ? order.getTong() : BigDecimal.ZERO);
            dto.setThoiGianTao(order.getThoiGianTao());
            dto.setPhuongThucThanhToan(order.getPhuongThucThanhToan() != null ? order.getPhuongThucThanhToan() : "Chưa chọn");
            dto.setPhuongThucBanHang(order.getPhuongThucBanHang() != null ? order.getPhuongThucBanHang() : "Chưa chọn");
            dto.setPhiVanChuyen(order.getPhiVanChuyen() != null ? order.getPhiVanChuyen() : BigDecimal.ZERO);
            dto.setIdPhieuGiamGia(order.getPhieuGiamGia());
            dto.setDanhSachItem(order.getDanhSachSanPham());
            dto.parseDanhSachSanPham(objectMapper);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void xoaDonHangTam(UUID idDonHang) {
        DonHangTam donHangTam = donHangTamRepository.findById(idDonHang)
                .orElseThrow(() -> new RuntimeException("Đơn hàng tạm không tồn tại."));
        donHangTamRepository.delete(donHangTam);
    }

    private String taoMaDonHang() {
        return "DH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public HttpSession layPhien() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession();
    }
}