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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
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

    // Loại bỏ @Autowired private GioHangService gioHangService;

    @Transactional
    public KetQuaDonHangDTO taoDonHang(DonHangDTO donHangDTO) {
        NguoiDung khachHang = nguoiDungRepository.findById(donHangDTO.getKhachHangDaChon())
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại."));

        DonHang donHang = new DonHang();
        donHang.setNguoiDung(khachHang);
        donHang.setMaDonHang(taoMaDonHang());
        donHang.setTrangThaiThanhToan(donHangDTO.getPhuongThucThanhToan().equals("CASH") && donHangDTO.getSoTienKhachDua().compareTo(BigDecimal.ZERO) > 0);
        donHang.setPhiVanChuyen(donHangDTO.getPhiVanChuyen());
        donHang.setPhuongThucThanhToan(phuongThucThanhToanRepository.findById(UUID.fromString(donHangDTO.getPhuongThucThanhToan())).orElse(null));
        donHang.setPhuongThucBanHang(donHangDTO.getPhuongThucBanHang().equals("DELIVERY") ? "Giao hàng" : "Tại quầy");
        donHang.setSoTienKhachDua(donHangDTO.getSoTienKhachDua());
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setThoiGianThanhToan(donHang.getTrangThaiThanhToan() ? LocalDateTime.now() : null);

        BigDecimal tongTien = BigDecimal.ZERO;
        BigDecimal tienGiam = BigDecimal.ZERO;
        Map<UUID, Integer> soLuongTonKho = new HashMap<>();

        for (GioHangItemDTO item : donHangDTO.getDanhSachSanPham()) {
            ChiTietSanPham chiTiet = chiTietSanPhamRepository.findById(item.getIdChiTietSanPham())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại."));
            if (chiTiet.getSoLuongTonKho() < item.getSoLuong()) {
                throw new RuntimeException("Số lượng tồn kho không đủ cho sản phẩm: " + chiTiet.getSanPham().getTenSanPham());
            }

            ChiTietDonHang chiTietDonHang = new ChiTietDonHang();
            chiTietDonHang.setDonHang(donHang);
            chiTietDonHang.setChiTietSanPham(chiTiet);
            chiTietDonHang.setSoLuong(item.getSoLuong());
            chiTietDonHang.setGia(item.getGia());
            chiTietDonHang.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
            chiTietDonHang.setThanhTien(item.getThanhTien());
            chiTietDonHangRepository.save(chiTietDonHang);

            chiTiet.setSoLuongTonKho(chiTiet.getSoLuongTonKho() - item.getSoLuong());
            chiTietSanPhamRepository.save(chiTiet);
            soLuongTonKho.put(chiTiet.getId(), chiTiet.getSoLuongTonKho());

            tongTien = tongTien.add(item.getThanhTien());
        }

        if (donHangDTO.getIdPhieuGiamGia() != null) {
            PhieuGiamGiaCuaNguoiDung phieuGiam = phieuGiamGiaCuaNguoiDungRepository.findByPhieuGiamGia_Id(donHangDTO.getIdPhieuGiamGia()).stream()
                    .filter(v -> v.getNguoiDung().getId().equals(khachHang.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Phiếu giảm giá không hợp lệ."));
            tienGiam = phieuGiam.getPhieuGiamGia().getGiaTriGiam();
            if (phieuGiam.getPhieuGiamGia().getGiaTriGiamToiDa() != null && tienGiam.compareTo(phieuGiam.getPhieuGiamGia().getGiaTriGiamToiDa()) > 0) {
                tienGiam = phieuGiam.getPhieuGiamGia().getGiaTriGiamToiDa();
            }
            phieuGiam.setSoLuotConLai(phieuGiam.getSoLuotConLai() - 1);
            phieuGiamGiaCuaNguoiDungRepository.save(phieuGiam);
        }

        donHang.setTienGiam(tienGiam);
        donHang.setTongTien(tongTien.add(donHangDTO.getPhiVanChuyen()).subtract(tienGiam));
        donHangRepository.save(donHang);

//        gioHangService.xoaGioHang(); // Gọi qua BanHangController nếu cần

        KetQuaDonHangDTO ketQua = new KetQuaDonHangDTO();
        ketQua.setMaDonHang(donHang.getMaDonHang());
        ketQua.setSoLuongTonKho(soLuongTonKho);
        return ketQua;
    }

    @Transactional
    public DonHangDTO giuDonHang(DonHangDTO donHangDTO) {
        try {
            DonHangTam donHangTam = new DonHangTam();
            donHangTam.setKhachHang(donHangDTO.getKhachHangDaChon());
            donHangTam.setTong(new BigDecimal(donHangDTO.getTong().replace(" VNĐ", "").replace(".", "")));
            donHangTam.setThoiGianTao(LocalDateTime.now());
            donHangTam.setDanhSachSanPham(objectMapper.writeValueAsString(donHangDTO.getDanhSachSanPham()));
            donHangTam.setPhuongThucThanhToan(donHangDTO.getPhuongThucThanhToan());
            donHangTam.setPhuongThucBanHang(donHangDTO.getPhuongThucBanHang());
            donHangTam.setPhiVanChuyen(donHangDTO.getPhiVanChuyen());
            donHangTam.setPhieuGiamGia(donHangDTO.getIdPhieuGiamGia());
            donHangTamRepository.save(donHangTam);
            // Gọi xoaGioHang qua BanHangController nếu cần
            return donHangDTO;
        } catch (Exception e) {
            throw new RuntimeException("Không thể giữ đơn hàng: " + e.getMessage());
        }
    }

    public List<DonHangTamDTO> layDanhSachDonHangTam() {
        return donHangTamRepository.findAll().stream()
                .map(donHang -> {
                    DonHangTamDTO dto = new DonHangTamDTO();
                    dto.setId(donHang.getId());
                    dto.setTenKhachHang(nguoiDungRepository.findById(donHang.getKhachHang())
                            .map(NguoiDung::getHoTen)
                            .orElse("Không rõ"));
                    dto.setTong(donHang.getTong());
                    dto.setThoiGianTao(donHang.getThoiGianTao().toString());
                    try {
                        List<Map<String, Object>> jsonList = objectMapper.readValue(donHang.getDanhSachSanPham(), List.class);
                        List<GioHangItemDTO> danhSachSanPham = jsonList.stream().map(item -> {
                            GioHangItemDTO dtoItem = new GioHangItemDTO();
                            dtoItem.setIdChiTietSanPham(UUID.fromString((String) item.get("idChiTietSanPham")));
                            dtoItem.setTenSanPham((String) item.get("tenSanPham"));
                            dtoItem.setMauSac((String) item.get("mauSac"));
                            dtoItem.setKichCo((String) item.get("kichCo"));
                            dtoItem.setSoLuong(((Number) item.get("soLuong")).intValue());
                            dtoItem.setGia(new BigDecimal((String) item.get("gia")));
                            dtoItem.setThanhTien(new BigDecimal((String) item.get("thanhTien")));
                            return dtoItem;
                        }).collect(Collectors.toList());
                        dto.setDanhSachSanPham(danhSachSanPham);
                    } catch (Exception e) {
                        dto.setDanhSachSanPham(new ArrayList<>());
                    }
                    return dto;
                })
                .toList();
    }

    public DonHangTamDTO layChiTietDonHangTam(UUID id) {
        DonHangTam donHangTam = donHangTamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đơn hàng tạm không tồn tại."));
        DonHangTamDTO dto = new DonHangTamDTO();
        dto.setId(donHangTam.getId());
        dto.setTenKhachHang(nguoiDungRepository.findById(donHangTam.getKhachHang())
                .map(NguoiDung::getHoTen)
                .orElse("Không rõ"));
        dto.setTong(donHangTam.getTong());
        dto.setThoiGianTao(donHangTam.getThoiGianTao().toString());
        try {
            List<Map<String, Object>> jsonList = objectMapper.readValue(donHangTam.getDanhSachSanPham(), List.class);
            List<GioHangItemDTO> danhSachSanPham = jsonList.stream().map(item -> {
                GioHangItemDTO dtoItem = new GioHangItemDTO();
                dtoItem.setIdChiTietSanPham(UUID.fromString((String) item.get("idChiTietSanPham")));
                dtoItem.setTenSanPham((String) item.get("tenSanPham"));
                dtoItem.setMauSac((String) item.get("mauSac"));
                dtoItem.setKichCo((String) item.get("kichCo"));
                dtoItem.setSoLuong(((Number) item.get("soLuong")).intValue());
                dtoItem.setGia(new BigDecimal((String) item.get("gia")));
                dtoItem.setThanhTien(new BigDecimal((String) item.get("thanhTien")));
                return dtoItem;
            }).collect(Collectors.toList());
            dto.setDanhSachSanPham(danhSachSanPham);
        } catch (Exception e) {
            dto.setDanhSachSanPham(new ArrayList<>());
        }
        return dto;
    }

    @Transactional
    public GioHangDTO khoiPhucDonHang(UUID idDonHang) {
        DonHangTam donHangTam = donHangTamRepository.findById(idDonHang)
                .orElseThrow(() -> new RuntimeException("Đơn hàng tạm không tồn tại."));
        GioHangDTO gioHang = new GioHangDTO();
        gioHang.setKhachHangDaChon(donHangTam.getKhachHang());
        gioHang.setPhiVanChuyen(donHangTam.getPhiVanChuyen());
        gioHang.setIdPhieuGiamGia(donHangTam.getPhieuGiamGia());
        gioHang.setPhuongThucThanhToan(donHangTam.getPhuongThucThanhToan());
        gioHang.setPhuongThucBanHang(donHangTam.getPhuongThucBanHang());
        try {
            String jsonData = donHangTam.getDanhSachSanPham();
            if (jsonData != null && !jsonData.isEmpty()) {
                List<Map<String, Object>> jsonList = objectMapper.readValue(jsonData, List.class);
                List<GioHangItemDTO> danhSachSanPham = jsonList.stream().map(item -> {
                    GioHangItemDTO dtoItem = new GioHangItemDTO();
                    dtoItem.setIdChiTietSanPham(UUID.fromString((String) item.get("idChiTietSanPham")));
                    dtoItem.setTenSanPham((String) item.get("tenSanPham"));
                    dtoItem.setMauSac((String) item.get("mauSac"));
                    dtoItem.setKichCo((String) item.get("kichCo"));
                    dtoItem.setSoLuong(((Number) item.get("soLuong")).intValue());
                    dtoItem.setGia(new BigDecimal((String) item.get("gia")));
                    dtoItem.setThanhTien(new BigDecimal((String) item.get("thanhTien")));
                    return dtoItem;
                }).collect(Collectors.toList());
                gioHang.setDanhSachSanPham(danhSachSanPham);
            } else {
                gioHang.setDanhSachSanPham(new ArrayList<>());
            }
        } catch (Exception e) {
            gioHang.setDanhSachSanPham(new ArrayList<>());
        }
        // Cập nhật tổng giỏ hàng
        BigDecimal tongTienHang = gioHang.getDanhSachSanPham().stream()
                .map(GioHangItemDTO::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        gioHang.setTongTienHang(tongTienHang.toString() + " VNĐ"); // Cần định dạng đúng
        BigDecimal giamGia = (gioHang.getIdPhieuGiamGia() != null) ? BigDecimal.valueOf(10000) : BigDecimal.ZERO;
        gioHang.setGiamGia(giamGia.toString() + " VNĐ");
        BigDecimal tong = tongTienHang.subtract(giamGia).add(gioHang.getPhiVanChuyen() != null ? gioHang.getPhiVanChuyen() : BigDecimal.ZERO);
        gioHang.setTong(tong.toString() + " VNĐ");

        layPhien().setAttribute("gioHang", gioHang);
        donHangTamRepository.delete(donHangTam);
        return gioHang;
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