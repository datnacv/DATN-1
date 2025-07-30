package com.example.AsmGD1.service.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.*;
import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.LichSuTimKiem;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangSanPhamRepository;
import com.example.AsmGD1.repository.WebKhachHang.LichSuTimKiemRepository;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KhachhangSanPhamService {

    @Autowired
    private KhachHangSanPhamRepository khachHangSanPhamRepository;

    @Autowired
    private LichSuTimKiemRepository lichSuTimKiemRepository;

    @Autowired
    private SanPhamService sanPhamService;

    private static final Logger logger = LoggerFactory.getLogger(KhachhangSanPhamService.class);

    // Lấy danh sách sản phẩm mới
    public List<SanPhamDto> getNewProducts() {
        List<SanPham> sanPhams = khachHangSanPhamRepository.findNewProducts();
        return sanPhams.stream()
                .filter(sanPham -> khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPham.getId()).stream()
                        .anyMatch(chiTiet -> chiTiet.getTrangThai()))
                .map(this::convertToSanPhamDto)
                .limit(10) // Giới hạn 10 sản phẩm mới
                .collect(Collectors.toList());
    }

    // Lấy danh sách sản phẩm bán chạy
    public List<SanPhamDto> getBestSellingProducts() {
        List<Object[]> results = khachHangSanPhamRepository.findBestSellingProducts();
        return results.stream()
                .filter(result -> {
                    SanPham sanPham = (SanPham) result[0];
                    return khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPham.getId()).stream()
                            .anyMatch(chiTiet -> chiTiet.getTrangThai());
                })
                .map(result -> {
                    SanPham sanPham = (SanPham) result[0];
                    Long totalSold = (Long) result[1];
                    SanPhamDto dto = convertToSanPhamDto(sanPham);
                    dto.setSold(totalSold.toString()); // Cập nhật số lượng đã bán thực tế
                    return dto;
                })
                .limit(10) // Giới hạn 10 sản phẩm bán chạy
                .collect(Collectors.toList());
    }

    public ChiTietSanPhamDto getProductDetail(UUID sanPhamId) {
        List<ChiTietSanPham> details = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPhamId);
        if (details.isEmpty()) {
            return null;
        }
        return convertToChiTietSanPhamDto(details.get(0));
    }

    private ChiTietSanPhamDto convertToChiTietSanPhamDto(ChiTietSanPham chiTiet) {
        ChiTietSanPhamDto dto = new ChiTietSanPhamDto();
        dto.setId(chiTiet.getId());
        dto.setSanPhamId(chiTiet.getSanPham().getId());
        dto.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
        dto.setMaSanPham(chiTiet.getSanPham().getMaSanPham());
        dto.setMoTa(chiTiet.getSanPham().getMoTa());
        dto.setUrlHinhAnh(chiTiet.getSanPham().getUrlHinhAnh());
        dto.setGia(chiTiet.getGia());
        dto.setSoLuongTonKho(chiTiet.getSoLuongTonKho());
        dto.setGioiTinh(chiTiet.getGioiTinh());
        dto.setTrangThai(chiTiet.getTrangThai());
        dto.setDanhMucId(chiTiet.getSanPham().getDanhMuc().getId());
        dto.setTenDanhMuc(chiTiet.getSanPham().getDanhMuc().getTenDanhMuc());

        // Lấy danh sách màu sắc
        List<MauSacDto> mauSacList = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(chiTiet.getSanPham().getId())
                .stream()
                .map(d -> {
                    MauSacDto msDto = new MauSacDto();
                    msDto.setId(d.getMauSac().getId());
                    msDto.setTenMau(d.getMauSac().getTenMau());
                    return msDto;
                })
                .distinct()
                .collect(Collectors.toList());
        dto.setMauSacList(mauSacList);

        // Lấy danh sách kích cỡ
        List<KichCoDto> kichCoList = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(chiTiet.getSanPham().getId())
                .stream()
                .map(d -> {
                    KichCoDto kcDto = new KichCoDto();
                    kcDto.setId(d.getKichCo().getId());
                    kcDto.setTen(d.getKichCo().getTen());
                    return kcDto;
                })
                .distinct()
                .collect(Collectors.toList());
        dto.setKichCoList(kichCoList);

        // Tổ hợp kích cỡ - màu sắc hợp lệ
        List<ChiTietSanPham> allDetails = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(chiTiet.getSanPham().getId());

        List<ChiTietSanPhamDto.SizeColorCombination> combinations = allDetails.stream()
                .map(item -> {
                    ChiTietSanPhamDto.SizeColorCombination combo = new ChiTietSanPhamDto.SizeColorCombination();
                    combo.setSizeId(item.getKichCo().getId());
                    combo.setColorId(item.getMauSac().getId());
                    return combo;
                })
                .distinct()
                .collect(Collectors.toList());

        dto.setValidCombinations(combinations);


        // Lấy thông tin chất liệu, xuất xứ, thương hiệu, kiểu dáng, tay áo, cổ áo
        ChatLieuDto chatLieuDto = new ChatLieuDto();
        chatLieuDto.setId(chiTiet.getChatLieu().getId());
        chatLieuDto.setTenChatLieu(chiTiet.getChatLieu().getTenChatLieu());
        dto.setChatLieu(chatLieuDto);

        XuatXuDto xuatXuDto = new XuatXuDto();
        xuatXuDto.setId(chiTiet.getXuatXu().getId());
        xuatXuDto.setTenXuatXu(chiTiet.getXuatXu().getTenXuatXu());
        dto.setXuatXu(xuatXuDto);

        ThuongHieuDto thuongHieuDto = new ThuongHieuDto();
        thuongHieuDto.setId(chiTiet.getThuongHieu().getId());
        thuongHieuDto.setTenThuongHieu(chiTiet.getThuongHieu().getTenThuongHieu());
        dto.setThuongHieu(thuongHieuDto);

        KieuDangDto kieuDangDto = new KieuDangDto();
        kieuDangDto.setId(chiTiet.getKieuDang().getId());
        kieuDangDto.setTenKieuDang(chiTiet.getKieuDang().getTenKieuDang());
        dto.setKieuDang(kieuDangDto);

        TayAoDto tayAoDto = new TayAoDto();
        tayAoDto.setId(chiTiet.getTayAo().getId());
        tayAoDto.setTenTayAo(chiTiet.getTayAo().getTenTayAo());
        dto.setTayAo(tayAoDto);

        CoAoDto coAoDto = new CoAoDto();
        coAoDto.setId(chiTiet.getCoAo().getId());
        coAoDto.setTenCoAo(chiTiet.getCoAo().getTenCoAo());
        dto.setCoAo(coAoDto);

        // Lấy danh sách ảnh sản phẩm
        List<String> hinhAnhList = khachHangSanPhamRepository.findProductImagesByChiTietSanPhamId(chiTiet.getId());
        dto.setHinhAnhList(hinhAnhList);

        return dto;
    }

    private SanPhamDto convertToSanPhamDto(SanPham sanPham) {
        SanPhamDto dto = new SanPhamDto();
        dto.setId(sanPham.getId());
        dto.setTenSanPham(sanPham.getTenSanPham());
        dto.setMaSanPham(sanPham.getMaSanPham());
        dto.setMoTa(sanPham.getMoTa());
        dto.setUrlHinhAnh(sanPham.getUrlHinhAnh());
        dto.setTrangThai(sanPham.getTrangThai());
        dto.setDanhMucId(sanPham.getDanhMuc().getId());
        dto.setTenDanhMuc(sanPham.getDanhMuc().getTenDanhMuc());
        dto.setThoiGianTao(sanPham.getThoiGianTao());

        // Tính tổng soLuongTonKho từ ChiTietSanPham
        List<ChiTietSanPham> chiTietSanPhams = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPham.getId());
        Long tongSoLuong = chiTietSanPhams.stream()
                .filter(chiTiet -> chiTiet.getTrangThai()) // Chỉ tính các chi tiết active
                .mapToLong(ChiTietSanPham::getSoLuongTonKho)
                .sum();
        dto.setTongSoLuong(tongSoLuong != null ? tongSoLuong : 0L);

        // Ánh xạ dữ liệu flash sale
        BigDecimal minPrice = khachHangSanPhamRepository.findMinPriceBySanPhamId(sanPham.getId());
        dto.setPrice(minPrice != null ? minPrice.toString() : "0");
        dto.setOldPrice(minPrice != null ? minPrice.add(new BigDecimal("10")).toString() : "0");
        dto.setDiscount("10%");
        dto.setSold("50");
        dto.setProgress(50);

        return dto;
    }

    public List<SanPhamDto> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getNewProducts(); // Nếu không có từ khóa, trả về sản phẩm mới
        }
        List<SanPham> sanPhams = khachHangSanPhamRepository.searchProductsByKeyword(keyword.trim());
        return sanPhams.stream()
                .map(this::convertToSanPhamDto)
                .collect(Collectors.toList());
    }

    // Lưu lịch sử tìm kiếm
    @Transactional
    public void saveSearchHistory(NguoiDung nguoiDung, String keyword) {
        if (nguoiDung == null || keyword == null || keyword.trim().isEmpty()) {
            logger.warn("Cannot save search history: user or keyword is null/empty. User: {}, Keyword: {}", nguoiDung, keyword);
            return;
        }
        logger.info("Attempting to save search history for user ID: {}, keyword: {}", nguoiDung.getId(), keyword);
        boolean exists = lichSuTimKiemRepository.findByNguoiDungIdOrderByThoiGianTimKiemDesc(nguoiDung.getId())
                .stream()
                .anyMatch(lichSu -> lichSu.getTuKhoa().equalsIgnoreCase(keyword.trim()));
        if (!exists) {
            LichSuTimKiem lichSu = LichSuTimKiem.builder()
                    .tuKhoa(keyword.trim())
                    .nguoiDung(nguoiDung)
                    .thoiGianTimKiem(LocalDateTime.now()) // Gán thời gian rõ ràng
                    .build();
            lichSuTimKiemRepository.save(lichSu);
            logger.info("Saved search history for keyword: {}", keyword);
        } else {
            logger.info("Keyword '{}' already exists in search history for user ID: {}", keyword, nguoiDung.getId());
        }
    }

    // Lấy lịch sử tìm kiếm
    public List<String> getSearchHistory(UUID nguoiDungId) {
        return lichSuTimKiemRepository.findByNguoiDungIdOrderByThoiGianTimKiemDesc(nguoiDungId)
                .stream()
                .map(LichSuTimKiem::getTuKhoa)
                .limit(5)
                .toList();
    }

    // Tìm kiếm sản phẩm với gợi ý theo danh mục khi không có kết quả
    public List<SanPhamDto> searchProductsWithHistory(String keyword, NguoiDung nguoiDung) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getNewProducts();
        }

        List<SanPham> sanPhams = khachHangSanPhamRepository.searchProductsByKeyword(keyword.trim());
        if (sanPhams.isEmpty()) {
            // Gợi ý sản phẩm từ danh mục phổ biến
            return khachHangSanPhamRepository.findPopularCategoryProducts()
                    .stream()
                    .map(this::convertToSanPhamDto)
                    .limit(5)
                    .collect(Collectors.toList());
        }

        return sanPhams.stream()
                .map(this::convertToSanPhamDto)
                .collect(Collectors.toList());
    }

    public List<SanPhamDto> getSanPhamLienQuan(UUID sanPhamId, int limit) {
        List<SanPham> sanPhams = sanPhamService.getSanPhamLienQuan(sanPhamId, limit);
        return sanPhams.stream()
                .map(this::convertToSanPhamDto)
                .collect(Collectors.toList());
    }

}