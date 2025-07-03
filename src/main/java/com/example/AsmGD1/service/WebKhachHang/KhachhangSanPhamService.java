package com.example.AsmGD1.service.WebKhachHang;

import com.example.AsmGD1.dto.KhachHang.*;
import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KhachhangSanPhamService {

    @Autowired
    private KhachHangSanPhamRepository khachHangSanPhamRepository;

    // Lấy danh sách sản phẩm mới
    public List<SanPhamDto> getNewProducts() {
        List<SanPham> sanPhams = khachHangSanPhamRepository.findNewProducts();
        return sanPhams.stream()
                .map(this::convertToSanPhamDto)
                .limit(10) // Giới hạn 10 sản phẩm mới
                .collect(Collectors.toList());
    }

    // Lấy danh sách sản phẩm bán chạy
    public List<SanPhamDto> getBestSellingProducts() {
        List<Object[]> results = khachHangSanPhamRepository.findBestSellingProducts();
        return results.stream()
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

        // Ánh xạ dữ liệu giá và khuyến mãi
        BigDecimal minPrice = khachHangSanPhamRepository.findMinPriceBySanPhamId(sanPham.getId());
        dto.setPrice(minPrice != null ? minPrice.toString() : "0");
        dto.setOldPrice(minPrice != null ? minPrice.add(new BigDecimal("10000")).toString() : "0"); // Giả lập giá cũ
        dto.setDiscount("10%"); // Giả lập giảm giá
        dto.setProgress(50); // Giả lập tiến độ

        return dto;
    }
}