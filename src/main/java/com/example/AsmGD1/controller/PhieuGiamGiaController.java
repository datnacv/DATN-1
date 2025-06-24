package com.example.AsmGD1.controller;

import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaSpecification;
import com.example.AsmGD1.service.GiamGia.GuiMailService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

@Controller
@RequestMapping("/acvstore/phieu-giam-gia")
public class PhieuGiamGiaController {

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungService phieuService;

    @Autowired
    private GuiMailService guiMailService;

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String fromDate,
                       @RequestParam(required = false) String toDate,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {

        Pageable pageable = PageRequest.of(page, size);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Chuyển đổi ngày từ String sang LocalDate
        LocalDate from = (fromDate != null && !fromDate.isEmpty()) ? LocalDate.parse(fromDate, formatter) : null;
        LocalDate to = (toDate != null && !toDate.isEmpty()) ? LocalDate.parse(toDate, formatter) : null;

        // Lấy danh sách có filter
        Page<PhieuGiamGia> pageResult = phieuGiamGiaRepository.findAll(
                PhieuGiamGiaSpecification.filter(search, type, status, from, to), pageable
        );

        List<PhieuGiamGia> vouchers = pageResult.getContent();

        // Format giá trị giảm
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        Map<UUID, Map<String, String>> formats = new HashMap<>();
        for (PhieuGiamGia v : vouchers) {
            Map<String, String> map = new HashMap<>();
            map.put("giaTriGiam", nf.format(v.getGiaTriGiam()) + ("PERCENT".equalsIgnoreCase(v.getLoai()) ? " %" : " ₫"));
            if (v.getGiaTriGiamToiDa() != null)
                map.put("giaTriGiamToiDa", nf.format(v.getGiaTriGiamToiDa()) + " ₫");
            if (v.getGiaTriGiamToiThieu() != null)
                map.put("giaTriGiamToiThieu", nf.format(v.getGiaTriGiamToiThieu()) + " ₫");
            formats.put(v.getId(), map);
        }

        // Truyền dữ liệu sang view
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("formats", formats);
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);

        // Phân trang
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("pageSize", size);

        // Truyền lại filter cho form
        model.addAttribute("search", search);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("type", type);
        model.addAttribute("status", status);

        return "WebQuanLy/voucher-list";
    }

    private String getTrangThai(PhieuGiamGia v) {
        LocalDateTime now = LocalDateTime.now();
        if (v.getNgayBatDau() != null && v.getNgayKetThuc() != null) {
            if (now.isBefore(v.getNgayBatDau().atStartOfDay())) return "Sắp diễn ra";
            else if (!now.isAfter(v.getNgayKetThuc().atTime(23, 59))) return "Đang diễn ra";
            else return "Đã kết thúc";
        }
        return "Không xác định";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("voucher", new PhieuGiamGia());
        model.addAttribute("customers", phieuService.layTatCaKhachHang());
        return "WebQuanLy/voucher-create";
    }

    @PostMapping("/create")
    public String create(@RequestParam String giaTriGiam,
                         @RequestParam(required = false) String giaTriGiamToiDa,
                         @RequestParam(required = false) String giaTriGiamToiThieu,
                         @ModelAttribute PhieuGiamGia voucher,
                         @RequestParam(required = false) boolean sendMail,
                         @RequestParam(required = false) List<UUID> selectedCustomerIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        List<String> errors = new ArrayList<>();

        // Validate mã phiếu
        if (voucher.getMa() == null || voucher.getMa().trim().isEmpty()) {
            errors.add("Mã phiếu không được để trống.");
        } else {
            voucher.setMa(voucher.getMa().trim());
            boolean exists = phieuGiamGiaRepository.findAll().stream()
                    .anyMatch(v -> v.getMa() != null && v.getMa().equalsIgnoreCase(voucher.getMa()));
            if (exists) {
                errors.add("Mã phiếu đã tồn tại.");
            }
        }

        // Validate tên phiếu
        if (voucher.getTen() == null || voucher.getTen().trim().isEmpty()) {
            errors.add("Tên phiếu không được để trống.");
        }

        // Validate kiểu giảm
        if (voucher.getLoai() == null || (!voucher.getLoai().equalsIgnoreCase("PERCENT") && !voucher.getLoai().equalsIgnoreCase("CASH"))) {
            errors.add("Kiểu giảm không hợp lệ.");
        }

        // Validate giá trị giảm
        try {
            BigDecimal giaTri = new BigDecimal(giaTriGiam.replace(",", "").replace(".", ""));
            if (giaTri.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Giá trị giảm phải lớn hơn 0.");
            } else {
                voucher.setGiaTriGiam(giaTri);
            }

            if ("PERCENT".equalsIgnoreCase(voucher.getLoai()) && giaTri.compareTo(new BigDecimal("100")) > 0) {
                errors.add("Giảm theo % không được vượt quá 100.");
            }
        } catch (Exception e) {
            errors.add("Giá trị giảm không hợp lệ.");
        }

        // Validate giá trị giảm tối đa
        if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
            try {
                if (giaTriGiamToiDa != null && !giaTriGiamToiDa.isEmpty()) {
                    BigDecimal gtd = new BigDecimal(giaTriGiamToiDa.replace(",", "").replace(".", ""));
                    if (gtd.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Giá trị giảm tối đa phải lớn hơn 0.");
                    } else {
                        voucher.setGiaTriGiamToiDa(gtd);
                    }
                } else {
                    errors.add("Phải nhập giá trị giảm tối đa khi giảm theo %.");
                }
            } catch (Exception e) {
                errors.add("Giá trị giảm tối đa không hợp lệ.");
            }
        } else {
            voucher.setGiaTriGiamToiDa(null);
        }

        // Validate đơn tối thiểu
        try {
            if (giaTriGiamToiThieu != null && !giaTriGiamToiThieu.isEmpty()) {
                BigDecimal min = new BigDecimal(giaTriGiamToiThieu.replace(",", "").replace(".", ""));
                if (min.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Đơn tối thiểu phải >= 0.");
                } else {
                    voucher.setGiaTriGiamToiThieu(min);
                }
            }
        } catch (Exception e) {
            errors.add("Đơn tối thiểu không hợp lệ.");
        }

        // Validate ngày
        if (voucher.getNgayBatDau() == null) {
            errors.add("Ngày bắt đầu không được để trống.");
        }
        if (voucher.getNgayKetThuc() == null) {
            errors.add("Ngày kết thúc không được để trống.");
        }
        if (voucher.getNgayBatDau() != null && voucher.getNgayKetThuc() != null &&
                voucher.getNgayBatDau().isAfter(voucher.getNgayKetThuc())) {
            errors.add("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        // Phiếu cá nhân: phải có khách hàng
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            if (selectedCustomerIds == null || selectedCustomerIds.isEmpty()) {
                errors.add("Vui lòng chọn ít nhất một khách hàng khi tạo phiếu cá nhân.");
            }
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errorMessage", String.join("<br>", errors));
            model.addAttribute("voucher", voucher);
            model.addAttribute("customers", phieuService.layTatCaKhachHang());
            return "WebQuanLy/voucher-create";
        }

        // Gán lượt sử dụng = 1 nếu là phiếu cá nhân
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            voucher.setGioiHanSuDung(1);
        }

        voucher.setThoiGianTao(LocalDateTime.now());
        PhieuGiamGia savedVoucher = phieuGiamGiaRepository.save(voucher);

        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu()) && selectedCustomerIds != null) {
            List<NguoiDung> selectedUsers = phieuService.layNguoiDungTheoIds(selectedCustomerIds);
            for (NguoiDung user : selectedUsers) {
                phieuService.ganPhieuChoNguoiDung(user, savedVoucher);
            }

            if (sendMail) {
                for (NguoiDung user : selectedUsers) {
                    guiMailService.guiPhieuGiamGia(user.getEmail(), voucher.getTen(), voucher.getGiaTriGiam());
                }
                redirectAttributes.addFlashAttribute("mailMessage", "Đã gửi email đến khách hàng được chọn.");
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu giảm giá thành công!");
        return "redirect:/acvstore/phieu-giam-gia";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size,
                               @RequestParam(required = false) String search,
                               Model model) {
        PhieuGiamGia voucher = phieuGiamGiaRepository.findById(id).orElseThrow();

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String giaTriGiamStr = nf.format(voucher.getGiaTriGiam());
        String giaTriGiamToiDaStr = voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) : "";
        String giaTriGiamToiThieuStr = voucher.getGiaTriGiamToiThieu() != null ? nf.format(voucher.getGiaTriGiamToiThieu()) : "";
        String gioiHanSuDungStr = voucher.getGioiHanSuDung() != null ? String.valueOf(voucher.getGioiHanSuDung()) : "";

        Pageable pageable = PageRequest.of(page, size);
        Page<NguoiDung> customerPage = (search != null && !search.isBlank())
                ? phieuService.timKiemKhachHangPhanTrang(search, pageable)
                : phieuService.layTatCaKhachHangPhanTrang(pageable);

        List<UUID> selectedCustomerIds = new ArrayList<>();
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            List<NguoiDung> daDuocGan = phieuService.layNguoiDungTheoPhieu(voucher.getId());
            selectedCustomerIds = daDuocGan.stream().map(NguoiDung::getId).toList();
        }

        model.addAttribute("voucher", voucher);
        model.addAttribute("giaTriGiamStr", giaTriGiamStr);
        model.addAttribute("giaTriGiamToiDaStr", giaTriGiamToiDaStr);
        model.addAttribute("giaTriGiamToiThieuStr", giaTriGiamToiThieuStr);
        model.addAttribute("gioiHanSuDungStr", gioiHanSuDungStr);

        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("selectedCustomerIds", selectedCustomerIds);
        model.addAttribute("currentCustomerPage", page);
        model.addAttribute("totalCustomerPages", customerPage.getTotalPages());
        model.addAttribute("search", search);

        return "WebQuanLy/voucher-edit";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable UUID id,
                         @RequestParam String giaTriGiam,
                         @RequestParam(required = false) String giaTriGiamToiDa,
                         @RequestParam(required = false) String giaTriGiamToiThieu,
                         @ModelAttribute PhieuGiamGia voucher,
                         @RequestParam(required = false) boolean sendMail,
                         @RequestParam(required = false) List<UUID> selectedCustomerIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        voucher.setId(id);

        // ✅ FIX lỗi: lấy lại thời gian tạo cũ để tránh null
        PhieuGiamGia existing = phieuGiamGiaRepository.findById(id).orElseThrow();
        voucher.setThoiGianTao(existing.getThoiGianTao());

        List<String> errors = new ArrayList<>();

        // Validate mã phiếu
        if (voucher.getMa() == null || voucher.getMa().trim().isEmpty()) {
            errors.add("Mã phiếu không được để trống");
        } else {
            String trimmedMa = voucher.getMa().trim();
            Optional<PhieuGiamGia> sameCode = phieuGiamGiaRepository.findAll().stream()
                    .filter(v -> v.getMa() != null && v.getMa().equalsIgnoreCase(trimmedMa) && !v.getId().equals(id))
                    .findFirst();
            if (sameCode.isPresent()) {
                errors.add("Mã phiếu đã tồn tại");
            }
            voucher.setMa(trimmedMa);
        }

        // Validate tên phiếu
        if (voucher.getTen() == null || voucher.getTen().trim().isEmpty()) {
            errors.add("Tên phiếu không được để trống");
        } else {
            voucher.setTen(voucher.getTen().trim());
        }

        // Giá trị giảm
        try {
            BigDecimal value = new BigDecimal(giaTriGiam.replaceAll("[^\\d,]", "").replace(",", "."));
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Giá trị giảm phải lớn hơn 0");
            }
            voucher.setGiaTriGiam(value);
        } catch (Exception e) {
            errors.add("Giá trị giảm không hợp lệ");
        }

        // Giá trị giảm tối đa
        try {
            if (giaTriGiamToiDa != null && !giaTriGiamToiDa.trim().isEmpty()) {
                BigDecimal max = new BigDecimal(giaTriGiamToiDa.replaceAll("[^\\d,]", "").replace(",", "."));
                if ("PERCENT".equalsIgnoreCase(voucher.getLoai()) && max.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Giá trị giảm tối đa phải lớn hơn 0");
                }
                voucher.setGiaTriGiamToiDa(max);
            } else if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
                errors.add("Phải nhập giá trị giảm tối đa khi chọn giảm theo %");
            }
        } catch (Exception e) {
            errors.add("Giá trị giảm tối đa không hợp lệ");
        }

        // Đơn tối thiểu
        try {
            if (giaTriGiamToiThieu != null && !giaTriGiamToiThieu.trim().isEmpty()) {
                BigDecimal min = new BigDecimal(giaTriGiamToiThieu.replaceAll("[^\\d,]", "").replace(",", "."));
                if (min.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Đơn tối thiểu không được âm");
                }
                voucher.setGiaTriGiamToiThieu(min);
            }
        } catch (Exception e) {
            errors.add("Đơn tối thiểu không hợp lệ");
        }

        // Validate loại giảm
        if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
            if (voucher.getGiaTriGiam().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Giảm theo % phải lớn hơn 0");
            } else if (voucher.getGiaTriGiam().compareTo(new BigDecimal("100")) > 0) {
                errors.add("Giảm theo % không được vượt quá 100");
            }
        }

        // Ngày
        if (voucher.getNgayBatDau() == null) {
            errors.add("Ngày bắt đầu không được để trống");
        }
        if (voucher.getNgayKetThuc() == null) {
            errors.add("Ngày kết thúc không được để trống");
        }
        if (voucher.getNgayBatDau() != null && voucher.getNgayKetThuc() != null &&
                voucher.getNgayBatDau().isAfter(voucher.getNgayKetThuc())) {
            errors.add("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        // Phiếu cá nhân phải chọn khách hàng
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu()) &&
                (selectedCustomerIds == null || selectedCustomerIds.isEmpty())) {
            errors.add("Vui lòng chọn khách hàng áp dụng cho phiếu cá nhân");
        }

        // Nếu có lỗi thì quay lại form
        if (!errors.isEmpty()) {
            model.addAttribute("errorMessage", String.join("<br>", errors));
            model.addAttribute("voucher", voucher);
            model.addAttribute("customers", phieuService.layTatCaKhachHang());
            model.addAttribute("selectedCustomerIds", selectedCustomerIds);

            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            model.addAttribute("giaTriGiamStr", voucher.getGiaTriGiam() != null ? nf.format(voucher.getGiaTriGiam()) : "");
            model.addAttribute("giaTriGiamToiDaStr", voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) : "");
            model.addAttribute("giaTriGiamToiThieuStr", voucher.getGiaTriGiamToiThieu() != null ? nf.format(voucher.getGiaTriGiamToiThieu()) : "");
            model.addAttribute("gioiHanSuDungStr", voucher.getGioiHanSuDung() != null ? String.valueOf(voucher.getGioiHanSuDung()) : "");

            return "WebQuanLy/voucher-edit";
        }

        // ✅ Nếu là phiếu cá nhân: set lượt sử dụng = 1
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            voucher.setGioiHanSuDung(1);
        }

        // Lưu
        PhieuGiamGia savedVoucher = phieuGiamGiaRepository.save(voucher);

        // Gán và gửi mail
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu()) && selectedCustomerIds != null) {
            List<NguoiDung> selectedUsers = phieuService.layNguoiDungTheoIds(selectedCustomerIds);
            for (NguoiDung user : selectedUsers) {
                phieuService.ganPhieuChoNguoiDung(user, savedVoucher);
            }

            if (sendMail) {
                try {
                    for (NguoiDung user : selectedUsers) {
                        guiMailService.guiPhieuGiamGia(user.getEmail(), voucher.getTen(), voucher.getGiaTriGiam());
                    }
                    redirectAttributes.addFlashAttribute("mailMessage", "Đã gửi mail cập nhật cho khách hàng");
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi gửi mail: " + e.getMessage());
                }
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phiếu giảm giá thành công!");
        return "redirect:/acvstore/phieu-giam-gia";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            // Xóa trước các bản ghi liên kết
            phieuService.xoaTatCaGanKetTheoPhieu(id);

            // Sau đó xóa chính phiếu
            phieuGiamGiaRepository.deleteById(id);

            redirectAttributes.addFlashAttribute("successMessage", "Xóa phiếu giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }

        return "redirect:/acvstore/phieu-giam-gia";
    }
}