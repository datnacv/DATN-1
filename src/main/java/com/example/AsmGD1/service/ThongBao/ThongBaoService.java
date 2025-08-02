package com.example.AsmGD1.service.ThongBao;

import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ThongBaoNhom;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ThongBao.ChiTietThongBaoNhomRepository;
import com.example.AsmGD1.repository.ThongBao.ThongBaoNhomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ThongBaoService {

    @Autowired
    private ThongBaoNhomRepository thongBaoNhomRepository;

    @Autowired
    private ChiTietThongBaoNhomRepository chiTietThongBaoNhomRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    public void taoThongBaoChoAdmin(DonHang donHang) {
        ThongBaoNhom thongBao = new ThongBaoNhom();
        thongBao.setId(UUID.randomUUID());
        thongBao.setDonHang(donHang);
        thongBao.setVaiTroNhan("admin");
        thongBao.setTieuDe("Khách hàng đặt đơn hàng");
        thongBao.setNoiDung("Mã đơn: " + donHang.getMaDonHang());
        thongBao.setThoiGianTao(LocalDateTime.now());
        thongBao.setTrangThai("Mới");
        thongBaoNhomRepository.save(thongBao);

        List<NguoiDung> danhSachAdmin = nguoiDungRepository.findByVaiTroAndTrangThai("admin", true);
        for (NguoiDung admin : danhSachAdmin) {
            ChiTietThongBaoNhom chiTiet = new ChiTietThongBaoNhom();
            chiTiet.setId(UUID.randomUUID());
            chiTiet.setThongBaoNhom(thongBao);
            chiTiet.setNguoiDung(admin);
            chiTiet.setDaXem(false);
            chiTietThongBaoNhomRepository.save(chiTiet);
        }
    }

    public List<ChiTietThongBaoNhom> layThongBaoTheoNguoiDung(UUID idNguoiDung, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("thongBaoNhom.thoiGianTao").descending());
            List<ChiTietThongBaoNhom> result = chiTietThongBaoNhomRepository
                    .findByNguoiDungId(idNguoiDung, pageable)
                    .getContent();
            System.out.println("Số thông báo trả về (layThongBaoTheoNguoiDung): " + result.size());
            return result;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông báo: " + e.getMessage());
            return List.of();
        }
    }

    public List<ChiTietThongBaoNhom> layThongBaoChuaXem(UUID idNguoiDung) {
        try {
            List<ChiTietThongBaoNhom> result = chiTietThongBaoNhomRepository
                    .findByNguoiDungIdAndDaXemFalseOrderByThongBaoNhom_ThoiGianTaoDesc(idNguoiDung);
            System.out.println("Số thông báo chưa đọc trả về (layThongBaoChuaXem): " + result.size());
            return result;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông báo chưa đọc: " + e.getMessage());
            return List.of();
        }
    }

    public long demSoThongBaoChuaXem(UUID idNguoiDung) {
        try {
            return chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemFalse(idNguoiDung);
        } catch (Exception e) {
            System.err.println("Lỗi khi đếm thông báo chưa xem: " + e.getMessage());
            return 0;
        }
    }

    public void danhDauDaXem(UUID idChiTietThongBao, UUID idNguoiDung) {
        System.out.println("Đánh dấu đã đọc - idChiTietThongBao: " + idChiTietThongBao + ", idNguoiDung: " + idNguoiDung);
        Optional<ChiTietThongBaoNhom> optionalThongBao = chiTietThongBaoNhomRepository.findById(idChiTietThongBao);
        if (optionalThongBao.isPresent()) {
            ChiTietThongBaoNhom thongBao = optionalThongBao.get();
            if (!thongBao.getNguoiDung().getId().equals(idNguoiDung)) {
                System.err.println("Thông báo không thuộc về người dùng: idNguoiDung=" + idNguoiDung);
                throw new IllegalArgumentException("Thông báo không thuộc về người dùng này.");
            }
            System.out.println("Tìm thấy thông báo: idChiTietThongBao=" + idChiTietThongBao + ", idNguoiDung=" + idNguoiDung);
            thongBao.setDaXem(true);
            chiTietThongBaoNhomRepository.save(thongBao);
            System.out.println("Đã cập nhật trạng thái daXem=true cho thông báo: " + idChiTietThongBao);
        } else {
            System.err.println("Không tìm thấy thông báo với idChiTietThongBao=" + idChiTietThongBao);
            throw new IllegalArgumentException("Không tìm thấy thông báo.");
        }
    }

    public void danhDauTatCaDaXem(UUID idNguoiDung) {
        List<ChiTietThongBaoNhom> danhSach = chiTietThongBaoNhomRepository
                .findByNguoiDungIdAndDaXemFalseOrderByThongBaoNhom_ThoiGianTaoDesc(idNguoiDung);
        for (ChiTietThongBaoNhom tb : danhSach) {
            tb.setDaXem(true);
        }
        chiTietThongBaoNhomRepository.saveAll(danhSach);
    }

    public List<ChiTietThongBaoNhom> lay5ThongBaoMoiNhat(UUID idNguoiDung) {
        try {
            Pageable top5 = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "thongBaoNhom.thoiGianTao"));
            List<ChiTietThongBaoNhom> result = chiTietThongBaoNhomRepository.findByNguoiDungId(idNguoiDung, top5).getContent();
            System.out.println("Số thông báo trả về (lay5ThongBaoMoiNhat): " + result.size());
            return result;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy 5 thông báo mới nhất: " + e.getMessage());
            return List.of();
        }
    }

    public long demTongSoThongBao(UUID idNguoiDung) {
        return chiTietThongBaoNhomRepository.countByNguoiDungId(idNguoiDung);
    }
}