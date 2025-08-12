package com.example.AsmGD1.service.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.*;
import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangSanPhamRepository;
import com.example.AsmGD1.repository.WebKhachHang.LichSuTimKiemRepository;
import com.example.AsmGD1.service.GiamGia.ChienDichGiamGiaService;
import com.example.AsmGD1.service.SanPham.SanPhamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KhachhangSanPhamService {

    @Autowired
    private KhachHangSanPhamRepository khachHangSanPhamRepository;

    @Autowired
    private LichSuTimKiemRepository lichSuTimKiemRepository;

    @Autowired
    private SanPhamService sanPhamService;

    @Autowired
    private ChienDichGiamGiaService chienDichGiamGiaService; // Inject dependency

    private static final Logger logger = LoggerFactory.getLogger(KhachhangSanPhamService.class);

    public Map<String, BigDecimal> getPriceRangeBySanPhamId(UUID sanPhamId) {
        List<ChiTietSanPham> list = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPhamId);
        if (list == null || list.isEmpty()) {
            return Map.of(
                    "minPrice", BigDecimal.ZERO,
                    "maxPrice", BigDecimal.ZERO,
                    "oldMinPrice", BigDecimal.ZERO,
                    "oldMaxPrice", BigDecimal.ZERO
            );
        }

        // Lấy giá hiện tại (gia) min và max
        BigDecimal minPrice = list.stream()
                .map(chiTiet -> {
                    BigDecimal originalPrice = chiTiet.getGia();
                    Optional<ChienDichGiamGia> chienDich = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTiet.getId());
                    if (chienDich.isPresent()) {
                        BigDecimal discountRate = chienDich.get().getPhanTramGiam().divide(BigDecimal.valueOf(100));
                        return originalPrice.subtract(originalPrice.multiply(discountRate));
                    }
                    return originalPrice;
                })
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = list.stream()
                .map(chiTiet -> {
                    BigDecimal originalPrice = chiTiet.getGia();
                    Optional<ChienDichGiamGia> chienDich = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTiet.getId());
                    if (chienDich.isPresent()) {
                        BigDecimal discountRate = chienDich.get().getPhanTramGiam().divide(BigDecimal.valueOf(100));
                        return originalPrice.subtract(originalPrice.multiply(discountRate));
                    }
                    return originalPrice;
                })
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Lấy giá gốc (oldPrice) min và max
        BigDecimal oldMinPrice = list.stream()
                .map(ChiTietSanPham::getGia)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal oldMaxPrice = list.stream()
                .map(ChiTietSanPham::getGia)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return Map.of(
                "minPrice", minPrice,
                "maxPrice", maxPrice,
                "oldMinPrice", oldMinPrice,
                "oldMaxPrice", oldMaxPrice
        );
    }

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

    public List<SanPhamDto> getAllActiveProductsDtos() {
        List<SanPham> list = khachHangSanPhamRepository.findActiveProducts();
        return list.stream().map(this::convertToSanPhamDto).collect(Collectors.toList());
    }

    public List<SanPhamDto> getAllActiveProductsDtosLimited(int limit) {
        List<SanPham> list = khachHangSanPhamRepository.findActiveProducts();
        return list.stream().map(this::convertToSanPhamDto).limit(limit).collect(Collectors.toList());
    }

    public List<DanhMuc> getActiveCategories(){
        return khachHangSanPhamRepository.findCategoriesHavingActiveProducts();
    }

    public List<SanPhamDto> getProductsByCategory(UUID categoryId){
        return khachHangSanPhamRepository.findActiveProductsByCategory(categoryId)
                .stream().map(this::convertToSanPhamDto).toList();
    }


    public ChiTietSanPhamDto getProductDetail(UUID sanPhamId) {
        List<ChiTietSanPham> details = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPhamId);
        if (details.isEmpty()) {
            return null;
        }
        return convertToChiTietSanPhamDto(details.get(0));
    }

    public ChiTietSanPhamDto convertToChiTietSanPhamDto(ChiTietSanPham chiTiet) {
        ChiTietSanPhamDto dto = new ChiTietSanPhamDto();
        dto.setId(chiTiet.getId());
        dto.setSanPhamId(chiTiet.getSanPham().getId());
        dto.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
        dto.setMaSanPham(chiTiet.getSanPham().getMaSanPham());
        dto.setMoTa(chiTiet.getSanPham().getMoTa());
        dto.setUrlHinhAnh(chiTiet.getSanPham().getUrlHinhAnh());
        dto.setSoLuongTonKho(chiTiet.getSoLuongTonKho());
        dto.setGioiTinh(chiTiet.getGioiTinh());
        dto.setTrangThai(chiTiet.getTrangThai());
        dto.setDanhMucId(chiTiet.getSanPham().getDanhMuc().getId());
        dto.setTenDanhMuc(chiTiet.getSanPham().getDanhMuc().getTenDanhMuc());

        // Thiết lập giá gốc
        BigDecimal originalPrice = chiTiet.getGia();
        dto.setOldPrice(originalPrice); // Giá gốc
        Optional<ChienDichGiamGia> chienDich = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTiet.getId());
        if (chienDich.isPresent()) {
            BigDecimal discountRate = chienDich.get().getPhanTramGiam().divide(BigDecimal.valueOf(100));
            BigDecimal discountedPrice = originalPrice.subtract(originalPrice.multiply(discountRate));
            dto.setGia(discountedPrice); // Giá sau giảm
            dto.setDiscountCampaignName(chienDich.get().getTen());
        } else {
            dto.setGia(originalPrice); // Không có chiến dịch, dùng giá gốc
            dto.setDiscountCampaignName(null);
        }

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

        // Tính tổng số lượng tồn kho từ ChiTietSanPham
        List<ChiTietSanPham> chiTietSanPhams = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPham.getId());
        Long tongSoLuong = chiTietSanPhams.stream()
                .filter(chiTiet -> chiTiet.getTrangThai())
                .mapToLong(ChiTietSanPham::getSoLuongTonKho)
                .sum();
        dto.setTongSoLuong(tongSoLuong != null ? tongSoLuong : 0L);

        // Lấy giá gốc (giá thấp nhất trong các biến thể)
        BigDecimal minPrice = khachHangSanPhamRepository.findMinPriceBySanPhamId(sanPham.getId());
        dto.setOldPrice(minPrice != null ? minPrice.toString() : "0");

        // Kiểm tra chiến dịch giảm giá
        BigDecimal discountedPrice = minPrice;
        String discountCampaignName = null;
        Optional<ChienDichGiamGia> chienDich = chienDichGiamGiaService.getActiveCampaignForProduct(sanPham.getId());
        if (chienDich.isPresent()) {
            BigDecimal discountRate = chienDich.get().getPhanTramGiam().divide(BigDecimal.valueOf(100));
            discountedPrice = minPrice.subtract(minPrice.multiply(discountRate));
            discountCampaignName = chienDich.get().getTen();
            dto.setDiscount(chienDich.get().getPhanTramGiam().toString() + "%");
        } else {
            // Mặc định không giảm giá nếu không có chiến dịch
            dto.setDiscount("0%");
        }

        // Thiết lập giá sau giảm
        dto.setPrice(discountedPrice != null ? discountedPrice.toString() : minPrice.toString());
        dto.setDiscountCampaignName(discountCampaignName);

        // Ánh xạ số lượng đã bán
        dto.setSold("50"); // Ví dụ: số lượng bán
        dto.setProgress(50); // Tiến độ giảm giá (tùy chỉnh)

        return dto;
    }


    public List<SanPhamDto> getSanPhamBanChayDtos() {
        List<Object[]> results = khachHangSanPhamRepository.findSanPhamBanChayWithGiaAndSold();
        return results.stream()
                .map(this::mapProjectionToDto)
                .limit(5) // ✅ Chỉ lấy 5 sản phẩm đầu tiên (bán chạy nhất)
                .collect(Collectors.toList());
    }


    public List<SanPhamDto> getSanPhamMoiNhatDtos() {
        List<Object[]> results = khachHangSanPhamRepository.findSanPhamMoiWithGiaAndSold();
        return results.stream()
                .map(this::mapProjectionToDto)
                .collect(Collectors.toList());
    }


    private SanPhamDto mapProjectionToDto(Object[] row) {
        SanPham p = (SanPham) row[0];
        BigDecimal minGia = (BigDecimal) row[1]; // Giá thấp nhất trong các biến thể
        BigDecimal maxGia = (BigDecimal) row[2]; // Giá cao nhất trong các biến thể
        Long sold = row[3] != null ? ((Number) row[3]).longValue() : 0L;

        SanPhamDto dto = new SanPhamDto();
        dto.setId(p.getId());
        dto.setTenSanPham(p.getTenSanPham());
        dto.setMaSanPham(p.getMaSanPham());
        dto.setMoTa(p.getMoTa());
        dto.setTrangThai(p.getTrangThai());
        dto.setThoiGianTao(p.getThoiGianTao());

        if (p.getDanhMuc() != null) {
            dto.setDanhMucId(p.getDanhMuc().getId());
            dto.setTenDanhMuc(p.getDanhMuc().getTenDanhMuc());
        }
        dto.setUrlHinhAnh(p.getUrlHinhAnh());

        // Tính tổng số lượng tồn kho từ ChiTietSanPham
        List<ChiTietSanPham> chiTietSanPhams = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(p.getId());
        Long tongSoLuong = chiTietSanPhams.stream()
                .filter(chiTiet -> chiTiet.getTrangThai())
                .mapToLong(ChiTietSanPham::getSoLuongTonKho)
                .sum();
        dto.setTongSoLuong(tongSoLuong != null ? tongSoLuong : 0L);

        // Thiết lập giá gốc
        String oldPrice = minGia != null ? minGia.toString() : "0";
        if (minGia != null && maxGia != null && minGia.compareTo(maxGia) < 0) {
            oldPrice = minGia + " - " + maxGia; // Giá gốc dạng khoảng
        }
        dto.setOldPrice(oldPrice);

        // Kiểm tra chiến dịch giảm giá
        BigDecimal discountedPrice = minGia;
        String discountCampaignName = null;
        Optional<ChienDichGiamGia> chienDich = chienDichGiamGiaService.getActiveCampaignForProduct(p.getId());
        if (chienDich.isPresent()) {
            BigDecimal discountRate = chienDich.get().getPhanTramGiam().divide(BigDecimal.valueOf(100));
            if (minGia != null && maxGia != null && minGia.compareTo(maxGia) < 0) {
                // Xử lý khoảng giá
                BigDecimal discountedMinPrice = minGia.subtract(minGia.multiply(discountRate));
                BigDecimal discountedMaxPrice = maxGia.subtract(maxGia.multiply(discountRate));
                dto.setPrice(discountedMinPrice + " - " + discountedMaxPrice); // Giá sau giảm dạng khoảng
            } else {
                // Xử lý giá đơn
                discountedPrice = minGia != null ? minGia.subtract(minGia.multiply(discountRate)) : null;
                dto.setPrice(discountedPrice != null ? discountedPrice.toString() : minGia.toString());
            }
            discountCampaignName = chienDich.get().getTen();
            dto.setDiscount(chienDich.get().getPhanTramGiam().toString() + "%");
        } else {
            // Không có chiến dịch, sử dụng giá gốc
            dto.setPrice(oldPrice);
            dto.setDiscount("0%");
        }
        dto.setDiscountCampaignName(discountCampaignName);
        dto.setSold(String.valueOf(sold));

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