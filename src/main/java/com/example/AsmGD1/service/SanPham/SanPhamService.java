package com.example.AsmGD1.service.SanPham;

import com.example.AsmGD1.dto.ChatBot.ChiTietSanPhamDTO;
import com.example.AsmGD1.dto.ChatBot.SanPhamWithChiTietDTO;
import com.example.AsmGD1.dto.SanPham.SanPhamDto;
import com.example.AsmGD1.entity.SanPham;
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

    public List<SanPham> findAll() {
        return sanPhamRepository.findAll();
    }

    public List<SanPham> findAllByTrangThai() {
        return sanPhamRepository.findAllByTrangThai();
    }

    public Page<SanPham> findAllPaginated(Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        return sanPhamRepository.findAll(sortedByThoiGianTaoDesc);
    }

    public SanPham findById(UUID id) {
        return sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));
    }

    public void save(SanPham sanPham) {
        sanPhamRepository.save(sanPham);
    }

    public void deleteById(UUID id) {
        sanPhamRepository.deleteById(id);
    }

    public List<SanPham> findByTenSanPhamContaining(String name) {
        return sanPhamRepository.findByTenSanPhamContainingIgnoreCase(name);
    }

    public Page<SanPham> findByTenSanPhamContaining(String searchName, Boolean trangThai, Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        if (trangThai != null) {
            return sanPhamRepository.findByTenSanPhamContainingIgnoreCaseAndTrangThai(searchName, trangThai, sortedByThoiGianTaoDesc);
        }
        return sanPhamRepository.findByTenSanPhamContainingIgnoreCase(searchName, sortedByThoiGianTaoDesc);
    }

    public Page<SanPham> findByTrangThai(Boolean trangThai, Pageable pageable) {
        Pageable sortedByThoiGianTaoDesc = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "thoiGianTao"));
        return sanPhamRepository.findByTrangThai(trangThai, sortedByThoiGianTaoDesc);
    }

    //nam
    public Page<SanPham> getPagedProducts(Pageable pageable) {
        return sanPhamRepository.findAll(pageable);
    }

    public List<SanPham> getAll() {
        return sanPhamRepository.findAll();
    }

//    public Optional<SanPham> findById(UUID id) {
//        return sanPhamRepository.findById(id);
//    }
    public Page<SanPham> searchByTenOrMa(String keyword, Pageable pageable) {
        return sanPhamRepository.findByTenSanPhamContainingIgnoreCaseOrMaSanPhamContainingIgnoreCase(keyword, keyword, pageable);
    }

    //chatbot
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

            if (sp.getDanhMuc() != null) {
                dto.setDanhMucId(sp.getDanhMuc().getId());
                dto.setTenDanhMuc(sp.getDanhMuc().getTenDanhMuc());
            }

            // Gán thêm giá, tồn kho, giá giảm nếu cần
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