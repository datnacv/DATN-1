package com.example.AsmGD1.service.ThongBao;

import com.example.AsmGD1.entity.ChiTietThongBaoNhom;
import com.example.AsmGD1.entity.DonHang;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.ThongBaoNhom;
import com.example.AsmGD1.repository.NguoiDung.NguoiDungRepository;
import com.example.AsmGD1.repository.ThongBao.ChiTietThongBaoNhomRepository;
import com.example.AsmGD1.repository.ThongBao.ThongBaoNhomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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

    public List<ChiTietThongBaoNhom> layThongBaoTheoNguoiDung(UUID idNguoiDung) {
        List<ChiTietThongBaoNhom> list = chiTietThongBaoNhomRepository.findByNguoiDungIdOrderByThongBaoNhom_ThoiGianTaoDesc(idNguoiDung);
        return list != null ? list : List.of(); // tránh null
    }


    public long demSoThongBaoChuaXem(UUID idNguoiDung) {
        try {
            return chiTietThongBaoNhomRepository.countByNguoiDungIdAndDaXemFalse(idNguoiDung);
        } catch (Exception e) {
            System.err.println("Lỗi khi đếm thông báo chưa xem: " + e.getMessage());
            return 0;
        }
    }


    public void danhDauDaXem(UUID idThongBao, UUID idNguoiDung) {
        chiTietThongBaoNhomRepository.findByThongBaoNhom_IdAndNguoiDung_Id(idThongBao, idNguoiDung).ifPresent(thongBao -> {
            thongBao.setDaXem(true);
            chiTietThongBaoNhomRepository.save(thongBao);
        });
    }
}
