package com.example.AsmGD1.service.ThongBao;

import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ThongBaoNhom;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ThongBao.ChiTietThongBaoNhomRepository;
import com.example.AsmGD1.repository.ThongBao.ThongBaoNhomRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        System.out.println("==> GỌI HÀM taoThongBaoChoAdmin()");
        System.out.println("==> Mã đơn hàng: " + donHang.getMaDonHang());

        List<NguoiDung> danhSachAdmin = nguoiDungRepository.findByVaiTroAndTrangThai("admin", true);
        System.out.println("==> Số admin: " + danhSachAdmin.size());

        if (danhSachAdmin.isEmpty()) {
            System.err.println("❌ Không tìm thấy admin nào để nhận thông báo.");
        }

        ThongBaoNhom thongBao = new ThongBaoNhom();
        thongBao.setId(UUID.randomUUID());
        thongBao.setDonHang(donHang);
        thongBao.setVaiTroNhan("admin");
        thongBao.setTieuDe("Khách hàng đặt đơn hàng");
        thongBao.setNoiDung("Mã đơn: " + donHang.getMaDonHang());
        thongBao.setThoiGianTao(LocalDateTime.now());
        thongBao.setTrangThai("Mới");
        thongBaoNhomRepository.save(thongBao);

        for (NguoiDung admin : danhSachAdmin) {
            ChiTietThongBaoNhom chiTiet = new ChiTietThongBaoNhom();
            chiTiet.setId(UUID.randomUUID());
            chiTiet.setThongBaoNhom(thongBao);
            chiTiet.setNguoiDung(admin);
            chiTiet.setDaXem(false);
            chiTietThongBaoNhomRepository.save(chiTiet);
            System.out.println("==> Gửi thông báo đến admin: " + admin.getHoTen());
        }
    }

    public void taoThongBaoHeThong(String vaiTroNhan, String tieuDe, String noiDung) {
        ThongBaoNhom thongBao = new ThongBaoNhom();
        thongBao.setId(UUID.randomUUID());
        thongBao.setVaiTroNhan(vaiTroNhan);
        thongBao.setTieuDe(tieuDe);
        thongBao.setNoiDung(noiDung);
        thongBao.setThoiGianTao(LocalDateTime.now());
        thongBao.setTrangThai("Mới");

        // ✅ Thêm dòng này để tránh lỗi nếu DB không cho phép null mặc định
        thongBao.setDonHang(null); // ← Quan trọng

        thongBaoNhomRepository.save(thongBao);

        List<NguoiDung> nguoiNhans = nguoiDungRepository.findByVaiTroAndTrangThai(vaiTroNhan, true);
        for (NguoiDung nd : nguoiNhans) {
            ChiTietThongBaoNhom ct = new ChiTietThongBaoNhom();
            ct.setId(UUID.randomUUID());
            ct.setThongBaoNhom(thongBao);
            ct.setNguoiDung(nd);
            ct.setDaXem(false);
            chiTietThongBaoNhomRepository.save(ct);
        }
    }



    public List<ChiTietThongBaoNhom> layThongBaoTheoNguoiDungVaTrangThai(UUID idNguoiDung, int page, int size, String status) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("thongBaoNhom.thoiGianTao").descending());
            if ("unread".equalsIgnoreCase(status)) {
                return chiTietThongBaoNhomRepository.findByNguoiDungIdAndDaXemFalse(idNguoiDung, pageable).getContent();
            } else if ("read".equalsIgnoreCase(status)) {
                return chiTietThongBaoNhomRepository.findByNguoiDungIdAndDaXemTrue(idNguoiDung, pageable).getContent();
            } else {
                return chiTietThongBaoNhomRepository.findByNguoiDungId(idNguoiDung, pageable).getContent();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông báo theo trạng thái: " + e.getMessage());
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

    public long demTongSoThongBao(UUID idNguoiDung) {
        try {
            return chiTietThongBaoNhomRepository.countByNguoiDungId(idNguoiDung);
        } catch (Exception e) {
            System.err.println("Lỗi khi đếm tổng số thông báo: " + e.getMessage());
            return 0;
        }
    }

    public long demTongSoThongBaoTheoTrangThai(UUID idNguoiDung, String status) {
        try {
            if ("unread".equalsIgnoreCase(status)) {
                return chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemFalse(idNguoiDung);
            } else if ("read".equalsIgnoreCase(status)) {
                return chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemTrue(idNguoiDung);
            } else {
                return chiTietThongBaoNhomRepository.countByNguoiDungId(idNguoiDung);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đếm thông báo theo trạng thái: " + e.getMessage());
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

    public List<ChiTietThongBaoNhom> lay5ThongBaoChuaXem(UUID idNguoiDung) {
        try {
            List<ChiTietThongBaoNhom> result = chiTietThongBaoNhomRepository
                    .findTop5ByNguoiDungIdAndDaXemFalseOrderByThongBaoNhom_ThoiGianTaoDesc(idNguoiDung);
            System.out.println("Số thông báo chưa đọc trả về (lay5ThongBaoChuaXem): " + result.size());
            return result;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy 5 thông báo chưa đọc: " + e.getMessage());
            return List.of();
        }
    }

    public void danhDauChuaXem(UUID idChiTietThongBao, UUID idNguoiDung) {
        Optional<ChiTietThongBaoNhom> optionalThongBao = chiTietThongBaoNhomRepository.findById(idChiTietThongBao);
        if (optionalThongBao.isPresent()) {
            ChiTietThongBaoNhom thongBao = optionalThongBao.get();
            if (!thongBao.getNguoiDung().getId().equals(idNguoiDung)) {
                throw new IllegalArgumentException("Thông báo không thuộc về người dùng này.");
            }
            thongBao.setDaXem(false);
            chiTietThongBaoNhomRepository.save(thongBao);
        } else {
            throw new IllegalArgumentException("Không tìm thấy thông báo.");
        }
    }

    public void xoaThongBao(UUID idThongBao, UUID idNguoiDung) {
        Optional<ChiTietThongBaoNhom> optional = chiTietThongBaoNhomRepository.findById(idThongBao);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông báo.");
        }

        ChiTietThongBaoNhom thongBao = optional.get();
        if (!thongBao.getNguoiDung().getId().equals(idNguoiDung)) {
            throw new IllegalArgumentException("Thông báo không thuộc về người dùng này.");
        }

        chiTietThongBaoNhomRepository.delete(thongBao);
    }

    @Transactional
    public void xoaTatCaThongBao(UUID idNguoiDung) {
        chiTietThongBaoNhomRepository.deleteByNguoiDungId(idNguoiDung);
    }

}