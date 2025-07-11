package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.dto.ChatBot.ChiTietSanPhamDTO;
import com.example.AsmGD1.dto.ChatBot.SanPhamWithChiTietDTO;
import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.SanPham;
import com.example.AsmGD1.repository.SanPham.ChiTietSanPhamRepository;
import com.example.AsmGD1.repository.SanPham.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SanPhamService {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

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

    public void deleteById(UUID id) {
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

            return dto;
        }).collect(Collectors.toList());
    }
}