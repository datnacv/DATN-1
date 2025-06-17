package com.example.AsmGD1.service.PhuongThucThanhToan;

import com.example.AsmGD1.entity.PhuongThucThanhToan;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PhuongThucThanhToanServiceImpl implements PhuongThucThanhToanService {
    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Override
    public List<PhuongThucThanhToan> findAll() {
        return phuongThucThanhToanRepository.findAll();
    }
}
