package com.example.AsmGD1.service.WebKhachHang;

import com.example.AsmGD1.dto.*;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.repository.WebKhachHang.KhachHangSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KhachhangSanPhamService {

    @Autowired
    private KhachHangSanPhamRepository khachHangSanPhamRepository;

    public List<SanPhamDto> getNewProducts() {
        return khachHangSanPhamRepository.findActiveProducts().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ChiTietSanPhamDto getProductDetail(UUID sanPhamId) {
        List<ChiTietSanPham> details = khachHangSanPhamRepository.findActiveProductDetailsBySanPhamId(sanPhamId);
        if (details.isEmpty()) {
            return null;
        }
        ChiTietSanPham firstDetail = details.get(0);
        ChiTietSanPhamDto dto = new ChiTietSanPhamDto();
        dto.setId(firstDetail.getId());
        dto.setSanPhamId(firstDetail.getSanPham().getId());
        dto.setTenSanPham(firstDetail.getSanPham().getTenSanPham());
        dto.setMaSanPham(firstDetail.getSanPham().getMaSanPham());
        dto.setMoTa(firstDetail.getSanPham().getMoTa());
        dto.setUrlHinhAnh(firstDetail.getSanPham().getUrlHinhAnh());
        dto.setGia(firstDetail.getGia());
        dto.setSoLuongTonKho(firstDetail.getSoLuongTonKho());
        dto.setGioiTinh(firstDetail.getGioiTinh());
        dto.setTrangThai(firstDetail.getTrangThai());
        dto.setDanhMucId(firstDetail.getSanPham().getDanhMuc().getId());
        dto.setTenDanhMuc(firstDetail.getSanPham().getDanhMuc().getTenDanhMuc());

        // Lấy danh sách màu sắc
        List<MauSacDto> mauSacList = details.stream()
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
        List<KichCoDto> kichCoList = details.stream()
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
        chatLieuDto.setId(firstDetail.getChatLieu().getId());
        chatLieuDto.setTenChatLieu(firstDetail.getChatLieu().getTenChatLieu());
        dto.setChatLieu(chatLieuDto);

        XuatXuDto xuatXuDto = new XuatXuDto();
        xuatXuDto.setId(firstDetail.getXuatXu().getId());
        xuatXuDto.setTenXuatXu(firstDetail.getXuatXu().getTenXuatXu());
        dto.setXuatXu(xuatXuDto);

        ThuongHieuDto thuongHieuDto = new ThuongHieuDto();
        thuongHieuDto.setId(firstDetail.getThuongHieu().getId());
        thuongHieuDto.setTenThuongHieu(firstDetail.getThuongHieu().getTenThuongHieu());
        dto.setThuongHieu(thuongHieuDto);

        KieuDangDto kieuDangDto = new KieuDangDto();
        kieuDangDto.setId(firstDetail.getKieuDang().getId());
        kieuDangDto.setTenKieuDang(firstDetail.getKieuDang().getTenKieuDang());
        dto.setKieuDang(kieuDangDto);

        TayAoDto tayAoDto = new TayAoDto();
        tayAoDto.setId(firstDetail.getTayAo().getId());
        tayAoDto.setTenTayAo(firstDetail.getTayAo().getTenTayAo());
        dto.setTayAo(tayAoDto);

        CoAoDto coAoDto = new CoAoDto();
        coAoDto.setId(firstDetail.getCoAo().getId());
        coAoDto.setTenCoAo(firstDetail.getCoAo().getTenCoAo());
        dto.setCoAo(coAoDto);

        // Lấy danh sách ảnh sản phẩm
        List<String> hinhAnhList = khachHangSanPhamRepository.findProductImagesByChiTietSanPhamId(firstDetail.getId());
        dto.setHinhAnhList(hinhAnhList);

        return dto;
    }

    private SanPhamDto convertToDto(SanPham sanPham) {
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
        dto.setPrice(sanPham.getMinPriceFormatted());
        return dto;
    }
}