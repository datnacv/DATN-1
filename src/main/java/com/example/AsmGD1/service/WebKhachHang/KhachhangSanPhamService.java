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
import java.math.RoundingMode;
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

    // Ưu tiên % KM theo biến thể; nếu biến thể không có thì dùng % theo sản phẩm; nếu vẫn không có thì 0%
    private BigDecimal getEffectiveDiscountPercent(UUID sanPhamId, ChiTietSanPham ct) {
        return chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId())
                .map(ChienDichGiamGia::getPhanTramGiam)
                .orElseGet(() -> chienDichGiamGiaService.getActiveCampaignForProduct(sanPhamId)
                        .map(ChienDichGiamGia::getPhanTramGiam)
                        .orElse(BigDecimal.ZERO));
    }

    // Min giá đã giảm trên toàn bộ biến thể active
    private BigDecimal getMinDiscountedPriceForProduct(UUID sanPhamId) {
        List<ChiTietSanPham> details = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPhamId);
        if (details.isEmpty()) return BigDecimal.ZERO;

        boolean hasDetailDiscount = details.stream()
                .anyMatch(ct -> chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId()).isPresent());

        BigDecimal productPercent = hasDetailDiscount
                ? BigDecimal.ZERO
                : chienDichGiamGiaService.getActiveCampaignForProduct(sanPhamId)
                .map(ChienDichGiamGia::getPhanTramGiam)
                .orElse(BigDecimal.ZERO);

        return details.stream()
                .filter(ChiTietSanPham::getTrangThai)
                .map(ct -> {
                    BigDecimal base = ct.getGia();
                    BigDecimal percent = chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId())
                            .map(ChienDichGiamGia::getPhanTramGiam)
                            .orElse(productPercent);
                    BigDecimal rate = percent.movePointLeft(2);
                    return base.subtract(base.multiply(rate)).setScale(0, RoundingMode.HALF_UP);
                })
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }


    public ChiTietSanPhamDto convertToChiTietSanPhamDto(ChiTietSanPham chiTiet) {
        if (chiTiet == null) return null;

        ChiTietSanPhamDto dto = new ChiTietSanPhamDto();

        // ===== Thông tin cơ bản =====
        dto.setId(chiTiet.getId());
        dto.setSanPhamId(chiTiet.getSanPham().getId());
        dto.setTenSanPham(chiTiet.getSanPham().getTenSanPham());
        dto.setMaSanPham(chiTiet.getSanPham().getMaSanPham());
        dto.setMoTa(chiTiet.getSanPham().getMoTa());
        dto.setUrlHinhAnh(chiTiet.getSanPham().getUrlHinhAnh());
        dto.setSoLuongTonKho(chiTiet.getSoLuongTonKho());
        dto.setGioiTinh(chiTiet.getGioiTinh());
        dto.setTrangThai(chiTiet.getTrangThai());
        if (chiTiet.getSanPham().getDanhMuc() != null) {
            dto.setDanhMucId(chiTiet.getSanPham().getDanhMuc().getId());
            dto.setTenDanhMuc(chiTiet.getSanPham().getDanhMuc().getTenDanhMuc());
        }

        // ===== Giá / Khuyến mãi =====
        BigDecimal originalPrice = chiTiet.getGia() != null ? chiTiet.getGia() : BigDecimal.ZERO;
        dto.setOldPrice(originalPrice); // giá gốc

        // Ưu tiên % KM theo biến thể; nếu không có thì theo sản phẩm; nếu vẫn không có thì 0%
        BigDecimal discountPercent = getEffectiveDiscountPercent(chiTiet.getSanPham().getId(), chiTiet);
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = discountPercent.movePointLeft(2); // 30 -> 0.30
            BigDecimal discounted = originalPrice.subtract(originalPrice.multiply(rate))
                    .setScale(0, RoundingMode.HALF_UP);
            dto.setGia(discounted);
        } else {
            dto.setGia(originalPrice);
        }

        // Tên chiến dịch (ưu tiên theo biến thể nếu có)
        String campaignName = chienDichGiamGiaService.getActiveCampaignForProductDetail(chiTiet.getId())
                .map(ChienDichGiamGia::getTen)
                .orElseGet(() -> chienDichGiamGiaService.getActiveCampaignForProduct(chiTiet.getSanPham().getId())
                        .map(ChienDichGiamGia::getTen)
                        .orElse(null));
        dto.setDiscountCampaignName(campaignName);

        // ===== Gom dữ liệu từ mọi biến thể ACTIVE của sản phẩm =====
        List<ChiTietSanPham> allDetails = khachHangSanPhamRepository
                .findActiveProductDetailsBySanPhamId(chiTiet.getSanPham().getId());

        // --- Danh sách màu (distinct theo id, giữ thứ tự xuất hiện)
        Map<UUID, MauSacDto> colorMap = new LinkedHashMap<>();
        for (ChiTietSanPham d : allDetails) {
            if (d.getMauSac() != null) {
                MauSacDto ms = new MauSacDto();
                ms.setId(d.getMauSac().getId());
                ms.setTenMau(d.getMauSac().getTenMau());
                colorMap.putIfAbsent(ms.getId(), ms);
            }
        }
        dto.setMauSacList(new ArrayList<>(colorMap.values()));

        // --- Danh sách kích cỡ (distinct theo id, giữ thứ tự)
        Map<UUID, KichCoDto> sizeMap = new LinkedHashMap<>();
        for (ChiTietSanPham d : allDetails) {
            if (d.getKichCo() != null) {
                KichCoDto kc = new KichCoDto();
                kc.setId(d.getKichCo().getId());
                kc.setTen(d.getKichCo().getTen());
                sizeMap.putIfAbsent(kc.getId(), kc);
            }
        }
        dto.setKichCoList(new ArrayList<>(sizeMap.values()));

        // --- Tổ hợp size-color hợp lệ (khử trùng lặp an toàn)
        List<ChiTietSanPhamDto.SizeColorCombination> combinations = new ArrayList<>();
        Set<String> comboKeys = new LinkedHashSet<>();
        for (ChiTietSanPham d : allDetails) {
            UUID sizeId = d.getKichCo() != null ? d.getKichCo().getId() : null;
            UUID colorId = d.getMauSac() != null ? d.getMauSac().getId() : null;
            String key = (sizeId != null ? sizeId.toString() : "null") + "|" + (colorId != null ? colorId.toString() : "null");
            if (comboKeys.add(key)) {
                ChiTietSanPhamDto.SizeColorCombination c = new ChiTietSanPhamDto.SizeColorCombination();
                c.setSizeId(sizeId);
                c.setColorId(colorId);
                combinations.add(c);
            }
        }
        dto.setValidCombinations(combinations);

        // ===== Thuộc tính mô tả (theo biến thể hiện tại) =====
        if (chiTiet.getChatLieu() != null) {
            ChatLieuDto chatLieuDto = new ChatLieuDto();
            chatLieuDto.setId(chiTiet.getChatLieu().getId());
            chatLieuDto.setTenChatLieu(chiTiet.getChatLieu().getTenChatLieu());
            dto.setChatLieu(chatLieuDto);
        }

        if (chiTiet.getXuatXu() != null) {
            XuatXuDto xuatXuDto = new XuatXuDto();
            xuatXuDto.setId(chiTiet.getXuatXu().getId());
            xuatXuDto.setTenXuatXu(chiTiet.getXuatXu().getTenXuatXu());
            dto.setXuatXu(xuatXuDto);
        }

        if (chiTiet.getThuongHieu() != null) {
            ThuongHieuDto thuongHieuDto = new ThuongHieuDto();
            thuongHieuDto.setId(chiTiet.getThuongHieu().getId());
            thuongHieuDto.setTenThuongHieu(chiTiet.getThuongHieu().getTenThuongHieu());
            dto.setThuongHieu(thuongHieuDto);
        }

        if (chiTiet.getKieuDang() != null) {
            KieuDangDto kieuDangDto = new KieuDangDto();
            kieuDangDto.setId(chiTiet.getKieuDang().getId());
            kieuDangDto.setTenKieuDang(chiTiet.getKieuDang().getTenKieuDang());
            dto.setKieuDang(kieuDangDto);
        }

        if (chiTiet.getTayAo() != null) {
            TayAoDto tayAoDto = new TayAoDto();
            tayAoDto.setId(chiTiet.getTayAo().getId());
            tayAoDto.setTenTayAo(chiTiet.getTayAo().getTenTayAo());
            dto.setTayAo(tayAoDto);
        }

        if (chiTiet.getCoAo() != null) {
            CoAoDto coAoDto = new CoAoDto();
            coAoDto.setId(chiTiet.getCoAo().getId());
            coAoDto.setTenCoAo(chiTiet.getCoAo().getTenCoAo());
            dto.setCoAo(coAoDto);
        }

        // ===== Ảnh: ảnh đại diện + ảnh của TẤT CẢ biến thể =====
        LinkedHashSet<String> images = new LinkedHashSet<>();

        // 1) Ảnh đại diện sản phẩm (đứng đầu)
        String mainImg = chiTiet.getSanPham().getUrlHinhAnh();
        if (mainImg != null && !mainImg.isBlank()) {
            images.add(mainImg.trim());
        }

        // 2) Ảnh của mọi biến thể
        for (ChiTietSanPham d : allDetails) {
            List<String> imgs = khachHangSanPhamRepository.findProductImagesByChiTietSanPhamId(d.getId());
            if (imgs != null) {
                for (String url : imgs) {
                    if (url != null) {
                        String u = url.trim();
                        if (!u.isEmpty()) images.add(u);
                    }
                }
            }
        }
        dto.setHinhAnhList(new ArrayList<>(images));

        return dto;
    }

    // =================== convertToSanPhamDto ===================
    private SanPhamDto convertToSanPhamDto(SanPham sanPham) {
        SanPhamDto dto = new SanPhamDto();

        // --- Thông tin cơ bản
        dto.setId(sanPham.getId());
        dto.setTenSanPham(sanPham.getTenSanPham());
        dto.setMaSanPham(sanPham.getMaSanPham());
        dto.setMoTa(sanPham.getMoTa());
        dto.setUrlHinhAnh(sanPham.getUrlHinhAnh());
        dto.setTrangThai(sanPham.getTrangThai());
        dto.setThoiGianTao(sanPham.getThoiGianTao());
        if (sanPham.getDanhMuc() != null) {
            dto.setDanhMucId(sanPham.getDanhMuc().getId());
            dto.setTenDanhMuc(sanPham.getDanhMuc().getTenDanhMuc());
        }

        // --- Biến thể đang bán
        List<ChiTietSanPham> activeDetails = khachHangSanPhamRepository
                .findActiveProductDetailsBySanPhamId(sanPham.getId())
                .stream().filter(ChiTietSanPham::getTrangThai)
                .collect(Collectors.toList());

        // Tổng tồn kho
        long tongSoLuong = activeDetails.stream()
                .mapToLong(ChiTietSanPham::getSoLuongTonKho).sum();
        dto.setTongSoLuong(tongSoLuong);

        // oldPrice = min giá gốc
        BigDecimal minOriginal = activeDetails.stream()
                .map(ChiTietSanPham::getGia)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        dto.setOldPrice(minOriginal.toPlainString());

        // --- QUY TẮC GIẢM GIÁ
        // Nếu có ít nhất 1 chiến dịch theo biến thể -> bỏ qua chiến dịch theo sản phẩm
        boolean hasDetailDiscount = activeDetails.stream()
                .anyMatch(ct -> chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId()).isPresent());

        BigDecimal productPercent = hasDetailDiscount
                ? BigDecimal.ZERO
                : chienDichGiamGiaService.getActiveCampaignForProduct(sanPham.getId())
                .map(ChienDichGiamGia::getPhanTramGiam)
                .orElse(BigDecimal.ZERO);

        // price = min giá đã giảm trên toàn bộ biến thể
        BigDecimal minDiscounted = activeDetails.stream()
                .map(ct -> {
                    BigDecimal base = ct.getGia();
                    if (base == null) return null;
                    BigDecimal percent = chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId())
                            .map(ChienDichGiamGia::getPhanTramGiam)
                            .orElse(productPercent);
                    BigDecimal rate = percent.movePointLeft(2); // 30 -> 0.30
                    return base.subtract(base.multiply(rate)).setScale(0, RoundingMode.HALF_UP);
                })
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        dto.setPrice(minDiscounted.toPlainString()); // FE hiển thị: "Từ <price>"

        // discount = % giảm lớn nhất đang áp dụng trên các biến thể (hoặc % theo SP nếu không có biến thể giảm)
        BigDecimal maxPercent = activeDetails.stream()
                .map(ct -> chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId())
                        .map(ChienDichGiamGia::getPhanTramGiam)
                        .orElse(productPercent))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        dto.setDiscount(maxPercent.stripTrailingZeros().toPlainString() + "%");

        // Tên chiến dịch (ưu tiên theo biến thể nếu có)
        String campaignName = hasDetailDiscount
                ? activeDetails.stream()
                .map(ct -> chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId()))
                .filter(Optional::isPresent)
                .map(o -> o.get().getTen())
                .findFirst()
                .orElse(null)
                : chienDichGiamGiaService.getActiveCampaignForProduct(sanPham.getId())
                .map(ChienDichGiamGia::getTen)
                .orElse(null);
        dto.setDiscountCampaignName(campaignName);

        // (tuỳ bạn dùng)
        dto.setSold("50");
        dto.setProgress(50);

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


    // =================== mapProjectionToDto ===================
    private SanPhamDto mapProjectionToDto(Object[] row) {
        SanPham p = (SanPham) row[0];
        BigDecimal minGia = (BigDecimal) row[1]; // giá gốc min từ query
        BigDecimal maxGia = (BigDecimal) row[2]; // giá gốc max từ query
        Long sold = row[3] != null ? ((Number) row[3]).longValue() : 0L;

        SanPhamDto dto = new SanPhamDto();

        // --- Thông tin cơ bản
        dto.setId(p.getId());
        dto.setTenSanPham(p.getTenSanPham());
        dto.setMaSanPham(p.getMaSanPham());
        dto.setMoTa(p.getMoTa());
        dto.setTrangThai(p.getTrangThai());
        dto.setThoiGianTao(p.getThoiGianTao());
        dto.setUrlHinhAnh(p.getUrlHinhAnh());
        if (p.getDanhMuc() != null) {
            dto.setDanhMucId(p.getDanhMuc().getId());
            dto.setTenDanhMuc(p.getDanhMuc().getTenDanhMuc());
        }

        // --- Biến thể active
        List<ChiTietSanPham> activeDetails = khachHangSanPhamRepository
                .findActiveProductDetailsBySanPhamId(p.getId())
                .stream().filter(ChiTietSanPham::getTrangThai)
                .collect(Collectors.toList());

        // Tồn kho
        dto.setTongSoLuong(activeDetails.stream()
                .mapToLong(ChiTietSanPham::getSoLuongTonKho).sum());

        // Old price: hiển thị đẹp dạng khoảng nếu có
        String oldPrice = (minGia != null && maxGia != null && minGia.compareTo(maxGia) < 0)
                ? minGia.toPlainString() + " - " + maxGia.toPlainString()
                : (minGia != null ? minGia.toPlainString() : "0");
        dto.setOldPrice(oldPrice);

        // --- QUY TẮC GIẢM GIÁ
        boolean hasDetailDiscount = activeDetails.stream()
                .anyMatch(ct -> chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId()).isPresent());

        BigDecimal productPercent = hasDetailDiscount
                ? BigDecimal.ZERO
                : chienDichGiamGiaService.getActiveCampaignForProduct(p.getId())
                .map(ChienDichGiamGia::getPhanTramGiam)
                .orElse(BigDecimal.ZERO);

        // price = min đã giảm
        BigDecimal minDiscounted;
        if (!activeDetails.isEmpty()) {
            minDiscounted = activeDetails.stream()
                    .map(ct -> {
                        BigDecimal base = ct.getGia();
                        if (base == null) return null;
                        BigDecimal percent = chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId())
                                .map(ChienDichGiamGia::getPhanTramGiam)
                                .orElse(productPercent);
                        BigDecimal rate = percent.movePointLeft(2);
                        return base.subtract(base.multiply(rate)).setScale(0, RoundingMode.HALF_UP);
                    })
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(minGia != null ? minGia : BigDecimal.ZERO);
        } else {
            // Không có biến thể active: giảm trực tiếp trên minGia theo % sản phẩm (nếu có)
            BigDecimal base = (minGia != null) ? minGia : BigDecimal.ZERO;
            minDiscounted = base.subtract(base.multiply(productPercent.movePointLeft(2)))
                    .setScale(0, RoundingMode.HALF_UP);
        }
        dto.setPrice(minDiscounted.toPlainString()); // FE hiển thị: "Từ <price>"

        // discount = % lớn nhất đang áp dụng
        BigDecimal maxPercent = activeDetails.stream()
                .map(ct -> chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId())
                        .map(ChienDichGiamGia::getPhanTramGiam)
                        .orElse(productPercent))
                .max(BigDecimal::compareTo)
                .orElse(productPercent); // nếu không có biến thể, dùng % theo SP
        dto.setDiscount(maxPercent.stripTrailingZeros().toPlainString() + "%");

        // Tên chiến dịch
        String campaignName = hasDetailDiscount
                ? activeDetails.stream()
                .map(ct -> chienDichGiamGiaService.getActiveCampaignForProductDetail(ct.getId()))
                .filter(Optional::isPresent)
                .map(o -> o.get().getTen())
                .findFirst()
                .orElse(null)
                : chienDichGiamGiaService.getActiveCampaignForProduct(p.getId())
                .map(ChienDichGiamGia::getTen)
                .orElse(null);
        dto.setDiscountCampaignName(campaignName);

        // Sold
        dto.setSold(String.valueOf(sold != null ? sold : 0L));

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