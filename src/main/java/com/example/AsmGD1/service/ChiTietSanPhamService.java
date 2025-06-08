package com.example.AsmGD1.service;

import com.example.AsmGD1.dto.ChiTietSanPhamBatchDto;
import com.example.AsmGD1.dto.ChiTietSanPhamUpdateDto;
import com.example.AsmGD1.dto.ChiTietSanPhamVariationDto;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.*;
import com.example.AsmGD1.util.CloudinaryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChiTietSanPhamService {
    private static final Logger logger = LoggerFactory.getLogger(ChiTietSanPhamService.class);

    @Autowired private ChiTietSanPhamRepository chiTietSanPhamRepo;
    @Autowired private HinhAnhSanPhamRepository hinhAnhSanPhamRepo;
    @Autowired private SanPhamRepository sanPhamRepo;
    @Autowired private MauSacRepository mauSacRepo;
    @Autowired private KichCoRepository kichCoRepo;
    @Autowired private ChatLieuRepository chatLieuRepo;
    @Autowired private XuatXuRepository xuatXuRepo;
    @Autowired private TayAoRepository tayAoRepo;
    @Autowired private CoAoRepository coAoRepo;
    @Autowired private KieuDangRepository kieuDangRepo;
    @Autowired private ThuongHieuRepository thuongHieuRepo;
    @Autowired private CloudinaryUtil cloudinaryUtil;

    @Transactional
    public void saveChiTietSanPhamVariationsDto(ChiTietSanPhamBatchDto batchDto, List<MultipartFile> imageFiles) {
        SanPham sanPham = sanPhamRepo.findById(batchDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại ID: " + batchDto.getProductId()));
        XuatXu xuatXu = xuatXuRepo.findById(batchDto.getOriginId())
                .orElseThrow(() -> new RuntimeException("Xuất xứ không tồn tại ID: " + batchDto.getOriginId()));
        ChatLieu chatLieu = chatLieuRepo.findById(batchDto.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Chất liệu không tồn tại ID: " + batchDto.getMaterialId()));
        KieuDang kieuDang = kieuDangRepo.findById(batchDto.getStyleId())
                .orElseThrow(() -> new RuntimeException("Kiểu dáng không tồn tại ID: " + batchDto.getStyleId()));
        TayAo tayAo = tayAoRepo.findById(batchDto.getSleeveId())
                .orElseThrow(() -> new RuntimeException("Tay áo không tồn tại ID: " + batchDto.getSleeveId()));
        CoAo coAo = coAoRepo.findById(batchDto.getCollarId())
                .orElseThrow(() -> new RuntimeException("Cổ áo không tồn tại ID: " + batchDto.getCollarId()));
        ThuongHieu thuongHieu = thuongHieuRepo.findById(batchDto.getBrandId())
                .orElseThrow(() -> new RuntimeException("Thương hiệu không tồn tại ID: " + batchDto.getBrandId()));

        for (ChiTietSanPhamVariationDto variationDto : batchDto.getVariations()) {
            if (variationDto.getColorId() == null || variationDto.getSizeId() == null ||
                    variationDto.getPrice() == null || variationDto.getStockQuantity() == null) {
                continue; // Bỏ qua các biến thể không hợp lệ
            }

            MauSac mauSac = mauSacRepo.findById(variationDto.getColorId())
                    .orElseThrow(() -> new RuntimeException("Màu sắc không tồn tại ID: " + variationDto.getColorId()));
            KichCo kichCo = kichCoRepo.findById(variationDto.getSizeId())
                    .orElseThrow(() -> new RuntimeException("Kích cỡ không tồn tại ID: " + variationDto.getSizeId()));

            ChiTietSanPham pd = new ChiTietSanPham();
            pd.setSanPham(sanPham);
            pd.setXuatXu(xuatXu);
            pd.setChatLieu(chatLieu);
            pd.setKieuDang(kieuDang);
            pd.setTayAo(tayAo);
            pd.setCoAo(coAo);
            pd.setThuongHieu(thuongHieu);
            pd.setMauSac(mauSac);
            pd.setKichCo(kichCo);
            pd.setGia(variationDto.getPrice());
            pd.setSoLuongTonKho(variationDto.getStockQuantity());
            pd.setGioiTinh(batchDto.getGender());
            pd.setThoiGianTao(LocalDateTime.now());
            pd.setTrangThai(true);

            ChiTietSanPham savedDetail = chiTietSanPhamRepo.save(pd);

            if (imageFiles != null && !imageFiles.isEmpty()) {
                saveImagesToCloudinary(savedDetail, imageFiles.stream().limit(3).collect(Collectors.toList()));
            }
        }
    }

    @Transactional
    public void saveSingleChiTietSanPham(ChiTietSanPhamUpdateDto dto, List<MultipartFile> imageFiles) {
        SanPham sanPham = sanPhamRepo.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại ID: " + dto.getProductId()));
        MauSac mauSac = mauSacRepo.findById(dto.getColorId())
                .orElseThrow(() -> new RuntimeException("Màu sắc không tồn tại ID: " + dto.getColorId()));
        KichCo kichCo = kichCoRepo.findById(dto.getSizeId())
                .orElseThrow(() -> new RuntimeException("Kích cỡ không tồn tại ID: " + dto.getSizeId()));
        XuatXu xuatXu = xuatXuRepo.findById(dto.getOriginId())
                .orElseThrow(() -> new RuntimeException("Xuất xứ không tồn tại ID: " + dto.getOriginId()));
        ChatLieu chatLieu = chatLieuRepo.findById(dto.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Chất liệu không tồn tại ID: " + dto.getMaterialId()));
        KieuDang kieuDang = kieuDangRepo.findById(dto.getStyleId())
                .orElseThrow(() -> new RuntimeException("Kiểu dáng không tồn tại ID: " + dto.getStyleId()));
        TayAo tayAo = tayAoRepo.findById(dto.getSleeveId())
                .orElseThrow(() -> new RuntimeException("Tay áo không tồn tại ID: " + dto.getSleeveId()));
        CoAo coAo = coAoRepo.findById(dto.getCollarId())
                .orElseThrow(() -> new RuntimeException("Cổ áo không tồn tại ID: " + dto.getCollarId()));
        ThuongHieu thuongHieu = thuongHieuRepo.findById(dto.getBrandId())
                .orElseThrow(() -> new RuntimeException("Thương hiệu không tồn tại ID: " + dto.getBrandId()));

        ChiTietSanPham pd = new ChiTietSanPham();
        pd.setSanPham(sanPham);
        pd.setMauSac(mauSac);
        pd.setKichCo(kichCo);
        pd.setXuatXu(xuatXu);
        pd.setChatLieu(chatLieu);
        pd.setKieuDang(kieuDang);
        pd.setTayAo(tayAo);
        pd.setCoAo(coAo);
        pd.setThuongHieu(thuongHieu);
        pd.setGia(dto.getPrice());
        pd.setSoLuongTonKho(dto.getStockQuantity());
        pd.setGioiTinh(dto.getGender());
        pd.setThoiGianTao(LocalDateTime.now());
        pd.setTrangThai(true);

        ChiTietSanPham savedDetail = chiTietSanPhamRepo.save(pd);

        if (imageFiles != null && !imageFiles.isEmpty()) {
            saveImagesToCloudinary(savedDetail, imageFiles);
        }
    }

    @Transactional
    public void updateChiTietSanPham(ChiTietSanPhamUpdateDto updateDto, MultipartFile[] imageFiles) {
        ChiTietSanPham existingDetail = chiTietSanPhamRepo.findById(updateDto.getId())
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại ID: " + updateDto.getId()));

        existingDetail.setSanPham(sanPhamRepo.findById(updateDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại")));
        existingDetail.setMauSac(mauSacRepo.findById(updateDto.getColorId())
                .orElseThrow(() -> new RuntimeException("Màu sắc không tồn tại")));
        existingDetail.setKichCo(kichCoRepo.findById(updateDto.getSizeId())
                .orElseThrow(() -> new RuntimeException("Kích cỡ không tồn tại")));
        existingDetail.setXuatXu(xuatXuRepo.findById(updateDto.getOriginId())
                .orElseThrow(() -> new RuntimeException("Xuất xứ không tồn tại")));
        existingDetail.setChatLieu(chatLieuRepo.findById(updateDto.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Chất liệu không tồn tại")));
        existingDetail.setKieuDang(kieuDangRepo.findById(updateDto.getStyleId())
                .orElseThrow(() -> new RuntimeException("Kiểu dáng không tồn tại")));
        existingDetail.setTayAo(tayAoRepo.findById(updateDto.getSleeveId())
                .orElseThrow(() -> new RuntimeException("Tay áo không tồn tại")));
        existingDetail.setCoAo(coAoRepo.findById(updateDto.getCollarId())
                .orElseThrow(() -> new RuntimeException("Cổ áo không tồn tại")));
        existingDetail.setThuongHieu(thuongHieuRepo.findById(updateDto.getBrandId())
                .orElseThrow(() -> new RuntimeException("Thương hiệu không tồn tại")));
        existingDetail.setGia(updateDto.getPrice());
        existingDetail.setSoLuongTonKho(updateDto.getStockQuantity());
        existingDetail.setGioiTinh(updateDto.getGender());
        existingDetail.setTrangThai(true);

        chiTietSanPhamRepo.save(existingDetail);

        if (imageFiles != null && imageFiles.length > 0) {
            saveImagesToCloudinary(existingDetail, List.of(imageFiles));
        }
    }

    @Transactional
    public void deleteById(UUID id) {
        ChiTietSanPham pd = chiTietSanPhamRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại ID: " + id));
        chiTietSanPhamRepo.delete(pd);
    }

    @Transactional
    public void deleteImage(UUID imageId) {
        HinhAnhSanPham image = hinhAnhSanPhamRepo.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Hình ảnh không tồn tại"));
        try {
            String url = image.getUrlHinhAnh();
            String publicId = url.substring(url.lastIndexOf("acvstore/products/") + "acvstore/products/".length(), url.lastIndexOf("."));
            cloudinaryUtil.deleteImage(publicId);
            hinhAnhSanPhamRepo.delete(image);
        } catch (IOException e) {
            throw new RuntimeException("Không thể xóa ảnh từ Cloudinary: " + e.getMessage());
        }
    }

    private void saveImagesToCloudinary(ChiTietSanPham productDetail, List<MultipartFile> imageFiles) {
        List<MultipartFile> limitedImages = imageFiles.stream().limit(3).collect(Collectors.toList());
        for (MultipartFile file : limitedImages) {
            if (file != null && !file.isEmpty()) {
                try {
                    String imageUrl = cloudinaryUtil.uploadImage(file);
                    HinhAnhSanPham img = new HinhAnhSanPham();
                    img.setChiTietSanPham(productDetail);
                    img.setUrlHinhAnh(imageUrl);
                    hinhAnhSanPhamRepo.save(img);
                } catch (IOException e) {
                    logger.error("Không thể lưu ảnh lên Cloudinary: ", e);
                }
            }
        }
    }

    public List<ChiTietSanPham> findAll() {
        return chiTietSanPhamRepo.findAll();
    }

    public List<ChiTietSanPham> findByProductId(UUID productId) {
        return chiTietSanPhamRepo.findBySanPhamId(productId);
    }

    public ChiTietSanPham findById(UUID id) {
        return chiTietSanPhamRepo.findById(id).orElse(null);
    }
}