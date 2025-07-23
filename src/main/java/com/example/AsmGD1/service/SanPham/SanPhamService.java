package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.dto.ChatBot.ChiTietSanPhamDTO;
import com.example.AsmGD1.dto.ChatBot.HoaDonSanPhamDTO;
import com.example.AsmGD1.dto.ChatBot.SanPhamWithChiTietDTO;
import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.ChiTietDonHang;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.HoaDon;
import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.repository.BanHang.ChiTietDonHangRepository;
import com.example.AsmGD1.repository.HoaDon.HoaDonRepository;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.SanPhamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SanPhamService {

    private static final Logger logger = LoggerFactory.getLogger(SanPhamService.class);

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    private final String UPLOAD_DIR;

    public SanPhamService() {
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

    public List<SanPham> findAll() {
        return sanPhamRepository.findAll();
    }

    public List<SanPham> findAllByTrangThai() {
        return sanPhamRepository.findAllByTrangThai();
    }

    public Page<SanPham> findAllPaginated(Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        Page<SanPham> sanPhamPage = sanPhamRepository.findAll(sortedByThoiGianTaoDesc);
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        return sanPhamPage;
    }

    public SanPham findById(UUID id) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));
        setTongSoLuong(sanPham);
        return sanPham;
    }

    public void save(SanPham sanPham) {
        sanPhamRepository.save(sanPham);
    }

    public void saveSanPhamWithImage(SanPham sanPham, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty() && imageFile.getOriginalFilename() != null && !imageFile.getOriginalFilename().isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, imageFile.getBytes());
                if (!Files.exists(filePath)) {
                    logger.error("Tệp không được lưu đúng cách: {}", filePath);
                    throw new RuntimeException("Không thể lưu tệp ảnh: " + fileName);
                }
                logger.info("Đã lưu ảnh: {}", fileName);
                sanPham.setUrlHinhAnh("/images/" + fileName);
            } catch (IOException e) {
                logger.error("Không thể lưu tệp ảnh: {}", imageFile.getOriginalFilename(), e);
                throw new RuntimeException("Không thể lưu tệp ảnh: " + imageFile.getOriginalFilename(), e);
            }
        }
        sanPhamRepository.save(sanPham);
    }

    public void deleteById(UUID id) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));
        if (sanPham.getUrlHinhAnh() != null && !sanPham.getUrlHinhAnh().isEmpty()) {
            try {
                String fileName = sanPham.getUrlHinhAnh().replace("/images/", "");
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("Đã xóa tệp ảnh: {}", filePath);
                }
            } catch (IOException e) {
                logger.error("Không thể xóa ảnh từ thư mục local: {}", sanPham.getUrlHinhAnh(), e);
            }
        }
        sanPhamRepository.deleteById(id);
    }

    public List<SanPham> findByTenSanPhamContaining(String name) {
        List<SanPham> sanPhams = sanPhamRepository.findByTenSanPhamContainingIgnoreCase(name);
        sanPhams.forEach(this::setTongSoLuong);
        return sanPhams;
    }

    public Page<SanPham> findByTenSanPhamContaining(String searchName, Boolean trangThai, Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        Page<SanPham> sanPhamPage;
        if (trangThai != null) {
            sanPhamPage = sanPhamRepository.findByTenSanPhamContainingIgnoreCaseAndTrangThai(searchName, trangThai, sortedByThoiGianTaoDesc);
        } else {
            sanPhamPage = sanPhamRepository.findByTenSanPhamContainingIgnoreCase(searchName, sortedByThoiGianTaoDesc);
        }
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        return sanPhamPage;
    }

    public Page<SanPham> findByTrangThai(Boolean trangThai, Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        Page<SanPham> sanPhamPage = sanPhamRepository.findByTrangThai(trangThai, sortedByThoiGianTaoDesc);
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        return sanPhamPage;
    }

    public Page<SanPham> getPagedProducts(Pageable pageable) {
        Page<SanPham> sanPhamPage = sanPhamRepository.findAll(pageable);
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        return sanPhamPage;
    }

    public List<SanPham> getAll() {
        List<SanPham> sanPhams = sanPhamRepository.findAll();
        sanPhams.forEach(this::setTongSoLuong);
        return sanPhams;
    }

    public Page<SanPham> searchByTenOrMa(String keyword, Pageable pageable) {
        Page<SanPham> sanPhamPage = sanPhamRepository.findByTenSanPhamContainingIgnoreCaseOrMaSanPhamContainingIgnoreCase(keyword, keyword, pageable);
        sanPhamPage.getContent().forEach(this::setTongSoLuong);
        return sanPhamPage;
    }

    private void setTongSoLuong(SanPham sanPham) {
        long tongSoLuong = chiTietSanPhamRepository.findBySanPhamId(sanPham.getId())
                .stream()
                .mapToLong(ChiTietSanPham::getSoLuongTonKho)
                .sum();
        sanPham.setTongSoLuong(tongSoLuong);
    }

    public List<SanPhamDto> getAllSanPhamDtos() {
        List<SanPham> danhSach = sanPhamRepository.findAllByTrangThai();
        return danhSach.stream().map(sp -> {
            SanPhamDto dto = new SanPhamDto();
            dto.setId(sp.getId());
            dto.setMaSanPham(sp.getMaSanPham());
            dto.setTenSanPham(sp.getTenSanPham());
            dto.setMoTa(sp.getMoTa());
            dto.setUrlHinhAnh(sp.getUrlHinhAnh());
            dto.setTrangThai(sp.getTrangThai());
            dto.setThoiGianTao(sp.getThoiGianTao());
            dto.setTongSoLuong(sp.getTongSoLuong());

            if (sp.getDanhMuc() != null) {
                dto.setDanhMucId(sp.getDanhMuc().getId());
                dto.setTenDanhMuc(sp.getDanhMuc().getTenDanhMuc());
            }

            dto.setPrice(sp.getMinPrice().toString());
            dto.setOldPrice(sp.getMaxPrice().toString());
            dto.setSold(String.valueOf(sp.getTotalStockQuantity()));
            return dto;
        }).toList();
    }

    public List<SanPhamWithChiTietDTO> getSanPhamWithChiTietDTOs() {
        List<SanPham> danhSachSanPham = sanPhamRepository.findAll();

        return danhSachSanPham.stream().map(sp -> {
            SanPhamWithChiTietDTO dto = new SanPhamWithChiTietDTO();
            dto.setId(sp.getId());
            dto.setMaSanPham(sp.getMaSanPham());
            dto.setTenSanPham(sp.getTenSanPham());
            dto.setTenDanhMuc(sp.getDanhMuc().getTenDanhMuc());
            dto.setUrlHinhAnh(sp.getUrlHinhAnh());
            dto.setTongSoLuong(sp.getTongSoLuong());

            // 1. Set chi tiết sản phẩm như cũ
            dto.setChiTietSanPhams(sp.getChiTietSanPhams().stream().map(ct -> {
                ChiTietSanPhamDTO ctDto = new ChiTietSanPhamDTO();
                ctDto.setId(ct.getId());
                ctDto.setKichCo(ct.getKichCo().getTen());
                ctDto.setMauSac(ct.getMauSac().getTenMau());
                ctDto.setChatLieu(ct.getChatLieu().getTenChatLieu());
                ctDto.setXuatXu(ct.getXuatXu().getTenXuatXu());
                ctDto.setGia(ct.getGia());
                ctDto.setSoLuongTonKho(ct.getSoLuongTonKho());
                return ctDto;
            }).collect(Collectors.toList()));

            // 2. Lấy hóa đơn liên quan đến sản phẩm
            List<ChiTietDonHang> donHangs = chiTietDonHangRepository.findBySanPhamId(sp.getId());

            List<HoaDonSanPhamDTO> hoaDonDtos = donHangs.stream().map(ct -> {
                HoaDon hoaDon = hoaDonRepository.findByDonHang_Id(ct.getDonHang().getId());
                if (hoaDon == null) return null; // tránh null pointer nếu đơn hàng chưa có hóa đơn

                HoaDonSanPhamDTO hdDto = new HoaDonSanPhamDTO();
                hdDto.setIdHoaDon(hoaDon.getId());
                hdDto.setIdDonHang(ct.getDonHang().getId());
                hdDto.setIdChiTietSanPham(ct.getChiTietSanPham().getId());
                hdDto.setTenSanPham(ct.getTenSanPham());
                hdDto.setSoLuong(ct.getSoLuong());
                hdDto.setGia(ct.getGia());
                hdDto.setThanhTien(ct.getThanhTien());
                hdDto.setGhiChu(ct.getGhiChu());
                hdDto.setNgayTaoHoaDon(hoaDon.getNgayTao());
                return hdDto;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            dto.setHoaDonSanPhams(hoaDonDtos);

            return dto;
        }).collect(Collectors.toList());
    }
}