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

        public void danhDauTatCaDaXem(UUID idNguoiDung) {
            List<ChiTietThongBaoNhom> danhSach = chiTietThongBaoNhomRepository
                    .findByNguoiDungIdOrderByThongBaoNhom_ThoiGianTaoDesc(idNguoiDung);
            for (ChiTietThongBaoNhom tb : danhSach) {
                if (!tb.isDaXem()) {
                    tb.setDaXem(true);
                }
            }
            chiTietThongBaoNhomRepository.saveAll(danhSach);
        }

        public List<ChiTietThongBaoNhom> lay5ThongBaoMoiNhat(UUID idNguoiDung) {
            try {
                Pageable top5 = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "thongBaoNhom.thoiGianTao"));
                List<ChiTietThongBaoNhom> result = chiTietThongBaoNhomRepository.findByNguoiDungId(idNguoiDung, top5).getContent();
                System.out.println("Số thông báo trả về (lay5ThongBaoMoiNhat): " + result.size()); // Debug
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