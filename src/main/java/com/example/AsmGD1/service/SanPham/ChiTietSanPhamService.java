package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamBatchDto;
import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamUpdateDto;
import com.example.AsmGD1.dto.ChiTietSanPham.ChiTietSanPhamVariationDto;
import com.example.AsmGD1.entity.*;
import com.example.AsmGD1.repository.SanPham.*;
import com.example.AsmGD1.util.QRCodeUtil;
import com.google.zxing.WriterException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
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

    private final String UPLOAD_DIR;

    public ChiTietSanPhamService() {
        String os = System.getProperty("os.name").toLowerCase();
        UPLOAD_DIR = os.contains("win") ? "C:/DATN/uploads/san_pham/" : System.getProperty("user.home") + "/DATN/uploads/san_pham/";
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created directory: {}", UPLOAD_DIR);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + UPLOAD_DIR, e);
        }
    }

    @PostConstruct
    public void syncAllProductDetailsQRCode() {
        List<ChiTietSanPham> allDetails = chiTietSanPhamRepo.findAll();

        Set<String> validFileNames = allDetails.stream()
                .map(detail -> "qr_" + detail.getId() + ".png")
                .collect(Collectors.toSet());

        File qrDirectory = new File(QRCodeUtil.getBaseDir());

        if (qrDirectory.exists() && qrDirectory.isDirectory()) {
            File[] existingFiles = qrDirectory.listFiles((dir, name) -> name.endsWith(".png"));
            if (existingFiles != null) {
                for (File file : existingFiles) {
                    if (!validFileNames.contains(file.getName())) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            logger.info("🗑️ Đã xóa file QR không còn hợp lệ: {}", file.getName());
                        }
                    }
                }
            }
        }

        for (ChiTietSanPham detail : allDetails) {
            String qrFileName = "qr_" + detail.getId() + ".png";
            Path qrFilePath = Paths.get(QRCodeUtil.getBaseDir(), qrFileName);

            if (!Files.exists(qrFilePath)) {
                try {
                    QRCodeUtil.generateQRCodeImage(detail.getId().toString(), 250, 250, qrFileName);
                    logger.info("✅ QR code created for product detail ID: {}", detail.getId());
                } catch (Exception e) {
                    logger.error("❌ Lỗi khi tạo QR cho ID: {}", detail.getId(), e);
                }
            }
        }
    }

    @Transactional
    public void deleteChiTietSanPham(UUID id) {
        ChiTietSanPham chiTiet = chiTietSanPhamRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + id));

        String qrFileName = "qr_" + chiTiet.getId() + ".png";
        Path qrPath = Paths.get(QRCodeUtil.getBaseDir(), qrFileName);
        try {
            if (Files.exists(qrPath)) {
                Files.delete(qrPath);
                logger.info("🗑️ Đã xoá QR Code file: {}", qrPath);
            }
        } catch (IOException e) {
            logger.error("❌ Lỗi khi xóa QR Code file: {}", qrPath, e);
        }

        chiTietSanPhamRepo.delete(chiTiet);
    }

    public List<ChiTietSanPham> findAllByTrangThaiAndKeyword(String keyword) {
        return chiTietSanPhamRepo.findAllByTrangThaiAndKeyword(keyword);
    }

    public List<ChiTietSanPham> findByFilters(UUID productId, UUID colorId, UUID sizeId, UUID originId, UUID materialId,
                                              UUID styleId, UUID sleeveId, UUID collarId, UUID brandId, String gender, Boolean status) {
        StringBuilder query = new StringBuilder("SELECT ct FROM ChiTietSanPham ct " +
                "JOIN FETCH ct.sanPham sp " +
                "JOIN FETCH ct.kichCo kc " +
                "JOIN FETCH ct.mauSac ms " +
                "WHERE sp.id = :productId");
        Map<String, Object> params = new HashMap<>();
        params.put("productId", productId);

        if (colorId != null) {
            query.append(" AND ct.mauSac.id = :colorId");
            params.put("colorId", colorId);
        }
        if (sizeId != null) {
            query.append(" AND ct.kichCo.id = :sizeId");
            params.put("sizeId", sizeId);
        }
        if (originId != null) {
            query.append(" AND ct.xuatXu.id = :originId");
            params.put("originId", originId);
        }
        if (materialId != null) {
            query.append(" AND ct.chatLieu.id = :materialId");
            params.put("materialId", materialId);
        }
        if (styleId != null) {
            query.append(" AND ct.kieuDang.id = :styleId");
            params.put("styleId", styleId);
        }
        if (sleeveId != null) {
            query.append(" AND ct.tayAo.id = :sleeveId");
            params.put("sleeveId", sleeveId);
        }
        if (collarId != null) {
            query.append(" AND ct.coAo.id = :collarId");
            params.put("collarId", collarId);
        }
        if (brandId != null) {
            query.append(" AND ct.thuongHieu.id = :brandId");
            params.put("brandId", brandId);
        }
        if (gender != null && !gender.isEmpty()) {
            query.append(" AND ct.gioiTinh = :gender");
            params.put("gender", gender);
        }
        if (status != null) {
            query.append(" AND ct.trangThai = :status");
            params.put("status", status);
        }

        return chiTietSanPhamRepo.findByDynamicQuery(query.toString(), params);
    }

    @Transactional
    public void saveChiTietSanPhamVariationsDto(ChiTietSanPhamBatchDto batchDto) {
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
                continue;
            }

            MauSac mauSac = mauSacRepo.findById(variationDto.getColorId())
                    .orElseThrow(() -> new RuntimeException("Màu sắc không tồn tại ID: " + variationDto.getColorId()));
            KichCo kichCo = kichCoRepo.findById(variationDto.getSizeId())
                    .orElseThrow(() -> new RuntimeException("Kích cỡ không tồn tại ID: " + variationDto.getSizeId()));

            ChiTietSanPham existing = chiTietSanPhamRepo.findBySanPhamIdAndMauSacIdAndKichCoId(
                    sanPham.getId(), mauSac.getId(), kichCo.getId());
            if (existing != null) {
                logger.warn("Biến thể đã tồn tại: productId={}, colorId={}, sizeId={}",
                        sanPham.getId(), mauSac.getId(), kichCo.getId());
                continue;
            }

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
            pd.setTrangThai(variationDto.getStockQuantity() > 0);

            ChiTietSanPham savedDetail = chiTietSanPhamRepo.save(pd);

            try {
                QRCodeUtil.generateQRCodeForProduct(savedDetail.getId());
            } catch (IOException | WriterException e) {
                logger.error("Không thể tạo QR Code cho biến thể sản phẩm ID: {}", savedDetail.getId(), e);
            }

            List<MultipartFile> variationImages = batchDto.getColorImages() != null
                    ? batchDto.getColorImages().getOrDefault(variationDto.getColorId(), new ArrayList<>())
                    : new ArrayList<>();
            if (!variationImages.isEmpty()) {
                saveImagesToLocal(savedDetail, variationImages.stream().limit(3).collect(Collectors.toList()));
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
        pd.setTrangThai(dto.getStockQuantity() > 0 ? true : dto.getStatus() != null ? dto.getStatus() : true);

        ChiTietSanPham savedDetail = chiTietSanPhamRepo.save(pd);

        if (imageFiles != null && !imageFiles.isEmpty()) {
            saveImagesToLocal(savedDetail, imageFiles);
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
        existingDetail.setTrangThai(updateDto.getStatus() != null ? updateDto.getStatus() : updateDto.getStockQuantity() > 0);
        logger.info("Updating status to: {}", existingDetail.getTrangThai());

        chiTietSanPhamRepo.save(existingDetail);

        if (imageFiles != null && imageFiles.length > 0) {
            saveImagesToLocal(existingDetail, Arrays.asList(imageFiles));
        }
    }

    @Transactional
    public void deleteImage(UUID imageId) {
        HinhAnhSanPham image = hinhAnhSanPhamRepo.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Hình ảnh không tồn tại"));
        try {
            String fileName = image.getUrlHinhAnh().replace("/images/", "");
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Đã xóa tệp ảnh: {}", filePath);
            }
            hinhAnhSanPhamRepo.delete(image);
        } catch (IOException e) {
            throw new RuntimeException("Không thể xóa ảnh từ thư mục local: " + e.getMessage());
        }
    }

    private void saveImagesToLocal(ChiTietSanPham chiTietSanPham, List<MultipartFile> imageFiles) {
        // Xóa ảnh cũ trước khi lưu ảnh mới
        List<HinhAnhSanPham> existingImages = hinhAnhSanPhamRepo.findByChiTietSanPhamIdOrderByThuTu(chiTietSanPham.getId());
        for (HinhAnhSanPham oldImage : existingImages) {
            try {
                String fileName = oldImage.getUrlHinhAnh().replace("/images/", "");
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("Đã xóa ảnh cũ: {}", filePath);
                }
                hinhAnhSanPhamRepo.delete(oldImage);
            } catch (IOException e) {
                logger.error("Không thể xóa ảnh cũ: {}", oldImage.getUrlHinhAnh(), e);
            }
        }

        // Lưu ảnh mới với thứ tự chính xác
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (int i = 0; i < imageFiles.size() && i < 3; i++) {
                MultipartFile file = imageFiles.get(i);
                if (file != null && !file.isEmpty() && file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
                    try {
                        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                        Path filePath = Paths.get(UPLOAD_DIR, fileName);
                        Files.createDirectories(filePath.getParent());
                        Files.write(filePath, file.getBytes());
                        if (!Files.exists(filePath)) {
                            logger.error("Tệp không được lưu đúng cách: {}", filePath);
                            throw new RuntimeException("Không thể lưu tệp ảnh: " + fileName);
                        }
                        logger.info("Đã lưu ảnh {} với thứ tự: {}", fileName, i + 1);

                        HinhAnhSanPham img = new HinhAnhSanPham();
                        img.setChiTietSanPham(chiTietSanPham);
                        img.setUrlHinhAnh("/images/" + fileName);
                        img.setThuTu(i + 1); // Gán thứ tự từ 1 đến 3
                        hinhAnhSanPhamRepo.save(img);
                        logger.info("Đã lưu ảnh vào cơ sở dữ liệu: {} với thứ tự: {}", fileName, i + 1);
                    } catch (IOException e) {
                        logger.error("Không thể lưu tệp ảnh: {}", file.getOriginalFilename(), e);
                        throw new RuntimeException("Không thể lưu tệp ảnh: " + file.getOriginalFilename(), e);
                    }
                }
            }
        }
    }

    public List<HinhAnhSanPham> findHinhAnhSanPhamByChiTietSanPhamIdOrdered(UUID chiTietSanPhamId) {
        return hinhAnhSanPhamRepo.findByChiTietSanPhamIdOrderByThuTu(chiTietSanPhamId);
    }

    public List<ChiTietSanPham> findAll() {
        return chiTietSanPhamRepo.findAll();
    }

    public List<ChiTietSanPham> findByProductId(UUID productId) {
        return chiTietSanPhamRepo.findBySanPhamId(productId);
    }

    public ChiTietSanPham findById(UUID id) {
        ChiTietSanPham chiTiet = chiTietSanPhamRepo.findById(id).orElse(null);
        return chiTiet;
    }

    public ChiTietSanPham findBySanPhamIdAndMauSacIdAndKichCoId(UUID productId, UUID mauSacId, UUID kichCoId) {
        return chiTietSanPhamRepo.findBySanPhamIdAndMauSacIdAndKichCoId(productId, mauSacId, kichCoId);
    }

    public List<MauSac> findColorsByProductId(UUID productId) {
        return chiTietSanPhamRepo.findBySanPhamId(productId)
                .stream()
                .map(ChiTietSanPham::getMauSac)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<KichCo> findSizesByProductId(UUID productId) {
        return chiTietSanPhamRepo.findBySanPhamId(productId)
                .stream()
                .map(ChiTietSanPham::getKichCo)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public ChiTietSanPham save(ChiTietSanPham chiTietSanPham) {
        return chiTietSanPhamRepo.save(chiTietSanPham);
    }

    public List<ChiTietSanPham> findAllByTrangThai() {
        return chiTietSanPhamRepo.findAllByTrangThai();
    }

    @Transactional
    public void saveChiTietSanPhamWithImages(ChiTietSanPham pd, MultipartFile[] imageFiles) {
        ChiTietSanPham savedDetail = chiTietSanPhamRepo.save(pd);

        try {
            QRCodeUtil.generateQRCodeForProduct(savedDetail.getId());
        } catch (IOException | WriterException e) {
            logger.error("Không thể tạo QR Code cho sản phẩm ID: {}", savedDetail.getId(), e);
        }

        if (imageFiles != null && imageFiles.length > 0) {
            saveImagesToLocal(savedDetail, Arrays.asList(imageFiles));
        }
    }

    @Transactional
    public ChiTietSanPham updateStockAndStatus(UUID productDetailId, int quantityChange) {
        ChiTietSanPham productDetail = chiTietSanPhamRepo.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Chi tiết sản phẩm không tồn tại ID: " + productDetailId));

        int newStock = productDetail.getSoLuongTonKho() + quantityChange;
        if (newStock < 0) {
            throw new RuntimeException("Số lượng tồn kho không đủ cho sản phẩm: " + productDetail.getSanPham().getTenSanPham());
        }

        productDetail.setSoLuongTonKho(newStock);
        productDetail.setTrangThai(newStock > 0);

        return chiTietSanPhamRepo.save(productDetail);
    }

    public void updateStock(UUID productDetailId, int quantity) {
        ChiTietSanPham chiTiet = chiTietSanPhamRepo.findById(productDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        int newStock = chiTiet.getSoLuongTonKho() - quantity;
        if (newStock < 0) {
            throw new IllegalStateException("Số lượng tồn kho không đủ cho sản phẩm: " + chiTiet.getSanPham().getTenSanPham());
        }
        chiTiet.setSoLuongTonKho(newStock);
        chiTietSanPhamRepo.save(chiTiet);
    }

    public String layAnhDauTien(ChiTietSanPham chiTiet) {
        if (chiTiet.getHinhAnhSanPhams() != null && !chiTiet.getHinhAnhSanPhams().isEmpty()) {
            return chiTiet.getHinhAnhSanPhams().get(0).getUrlHinhAnh();
        }
        return "https://via.placeholder.com/50";
    }

}