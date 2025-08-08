package com.example.AsmGD1.service.NguoiDung;

import com.example.AsmGD1.entity.DiaChiNguoiDung;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.repository.NguoiDung.DiaChiNguoiDungRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiaChiKhachHangService {

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private DiaChiNguoiDungRepository diaChiRepo;

    @Transactional
    public NguoiDung saveCustomerWithDefaultAddress(NguoiDung customer) {
        customer.setVaiTro("customer");
        customer.setTrangThai(true);
        NguoiDung saved = nguoiDungService.save(customer);

        if (hasAddress(saved)) {
            diaChiRepo.removeDefaultFlag(saved.getId());

            DiaChiNguoiDung dc = new DiaChiNguoiDung();
            dc.setNguoiDung(saved);
            dc.setChiTietDiaChi(saved.getChiTietDiaChi());
            dc.setPhuongXa(saved.getPhuongXa());
            dc.setQuanHuyen(saved.getQuanHuyen());
            dc.setTinhThanhPho(saved.getTinhThanhPho());
            dc.setMacDinh(true);
            dc.setNguoiNhan(saved.getHoTen());
            dc.setSoDienThoaiNguoiNhan(saved.getSoDienThoai());
            diaChiRepo.save(dc);
        }
        return saved;
    }

    @Transactional
    public NguoiDung updateCustomerAndAppendAddress(NguoiDung customer) {
        NguoiDung updated = nguoiDungService.save(customer);

        if (hasAddress(updated)) {
            diaChiRepo.removeDefaultFlag(updated.getId());

            DiaChiNguoiDung dc = new DiaChiNguoiDung();
            dc.setNguoiDung(updated);
            dc.setChiTietDiaChi(updated.getChiTietDiaChi());
            dc.setPhuongXa(updated.getPhuongXa());
            dc.setQuanHuyen(updated.getQuanHuyen());
            dc.setTinhThanhPho(updated.getTinhThanhPho());
            dc.setMacDinh(true);
            dc.setNguoiNhan(updated.getHoTen());
            dc.setSoDienThoaiNguoiNhan(updated.getSoDienThoai());
            diaChiRepo.save(dc);
        }
        return updated;
    }

    private boolean hasAddress(NguoiDung user) {
        return notBlank(user.getChiTietDiaChi()) ||
                notBlank(user.getPhuongXa()) ||
                notBlank(user.getQuanHuyen()) ||
                notBlank(user.getTinhThanhPho());
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
