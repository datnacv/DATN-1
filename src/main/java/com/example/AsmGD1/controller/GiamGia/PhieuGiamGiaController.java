package com.example.AsmGD1.controller.GiamGia;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.entity.PhuongThucThanhToan;           // [PTTT]
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository; // [PTTT]
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaSpecification;
import com.example.AsmGD1.service.GiamGia.GuiMailService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/acvstore/phieu-giam-gia")
public class PhieuGiamGiaController {

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private PhieuGiamGiaCuaNguoiDungService phieuService;

    @Autowired
    private GuiMailService guiMailService;

    @Autowired
    private NguoiDungService nguoiDungService;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository; // [PTTT]

    // ===================== RBAC helpers =====================
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            return "ADMIN".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private boolean isCurrentUserEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            return "EMPLOYEE".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung user) {
            model.addAttribute("user", user);
            model.addAttribute("isAdmin", "ADMIN".equalsIgnoreCase(user.getVaiTro()));
            model.addAttribute("isEmployee", "EMPLOYEE".equalsIgnoreCase(user.getVaiTro()));
        } else {
            NguoiDung defaultUser = new NguoiDung();
            defaultUser.setTenDangNhap("Unknown");
            defaultUser.setVaiTro("GUEST");
            model.addAttribute("user", defaultUser);
            model.addAttribute("isAdmin", false);
            model.addAttribute("isEmployee", false);
        }
    }

    // ===================== LIST =====================
    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String fromDate,
                       @RequestParam(required = false) String toDate,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {

        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        LocalDateTime from = (fromDate != null && !fromDate.isEmpty())
                ? LocalDateTime.parse(fromDate, formatter) : null;

        LocalDateTime to = (toDate != null && !toDate.isEmpty())
                ? LocalDateTime.parse(toDate, formatter) : null;

        Page<PhieuGiamGia> pageResult = phieuGiamGiaRepository.findAll(
                PhieuGiamGiaSpecification.filter(search, type, status, from, to), pageable
        );

        List<PhieuGiamGia> vouchers = pageResult.getContent();
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        Map<UUID, Map<String, String>> formats = new HashMap<>();
        for (PhieuGiamGia v : vouchers) {
            Map<String, String> map = new HashMap<>();
            map.put("giaTriGiam", nf.format(v.getGiaTriGiam()) + ("PERCENT".equalsIgnoreCase(v.getLoai()) ? " %" : " ₫"));
            if (v.getGiaTriGiamToiDa() != null) map.put("giaTriGiamToiDa", nf.format(v.getGiaTriGiamToiDa()) + " ₫");
            if (v.getGiaTriGiamToiThieu() != null) map.put("giaTriGiamToiThieu", nf.format(v.getGiaTriGiamToiThieu()) + " ₫");
            formats.put(v.getId(), map);
        }

        model.addAttribute("vouchers", vouchers);
        model.addAttribute("formats", formats);
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);
        addUserInfoToModel(model);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("search", search);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("type", type);
        model.addAttribute("status", status);

        model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll()); // [PTTT] nếu cần filter UI

        return "WebQuanLy/voucher-list";
    }

    private String getTrangThai(PhieuGiamGia v) {
        LocalDateTime now = LocalDateTime.now();
        if (v.getNgayBatDau() == null || v.getNgayKetThuc() == null) return "Không xác định";
        if (now.isBefore(v.getNgayBatDau())) return "Sắp diễn ra";
        else if (!now.isAfter(v.getNgayKetThuc())) return "Đang diễn ra";
        else return "Đã kết thúc";
    }

    // ===================== CREATE =====================
    @GetMapping("/create")
    public String createForm(Model model, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        model.addAttribute("voucher", new PhieuGiamGia());
        model.addAttribute("customers", phieuService.layTatCaKhachHang());
        model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll()); // [PTTT]
        model.addAttribute("selectedPaymentMethodIds", Collections.emptyList());        // [PTTT]
        addUserInfoToModel(model);
        return "WebQuanLy/voucher-create";
    }

    @PostMapping("/create")
    public String create(@RequestParam String giaTriGiam,
                         @RequestParam(required = false) String giaTriGiamToiDa,
                         @RequestParam(required = false) String giaTriGiamToiThieu,
                         @ModelAttribute PhieuGiamGia voucher,
                         @RequestParam(required = false) boolean sendMail,
                         @RequestParam(required = false) List<UUID> selectedCustomerIds,
                         @RequestParam(required = false, name = "selectedPaymentMethodIds") // [PTTT]
                         List<UUID> selectedPaymentMethodIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        List<String> errors = new ArrayList<>();

        // ---- Validate cơ bản (giữ nguyên logic cũ) ----
        if (voucher.getMa() == null || voucher.getMa().trim().isEmpty()) {
            errors.add("Mã phiếu không được để trống.");
        } else {
            voucher.setMa(voucher.getMa().trim());
            if (phieuGiamGiaRepository.existsByMaIgnoreCase(voucher.getMa())) {
                errors.add("Mã phiếu đã tồn tại.");
            }
        }

        if (voucher.getTen() == null || voucher.getTen().trim().isEmpty()) {
            errors.add("Tên phiếu không được để trống.");
        }

        if (voucher.getLoai() == null || (!voucher.getLoai().equalsIgnoreCase("PERCENT")
                && !voucher.getLoai().equalsIgnoreCase("CASH"))) {
            errors.add("Kiểu giảm không hợp lệ.");
        }

        try {
            BigDecimal giaTri = new BigDecimal(giaTriGiam.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            if (giaTri.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Giá trị giảm phải lớn hơn 0.");
            } else {
                voucher.setGiaTriGiam(giaTri);
            }
            if ("PERCENT".equalsIgnoreCase(voucher.getLoai()) && giaTri.compareTo(new BigDecimal("100")) > 0) {
                errors.add("Giảm theo % không được vượt quá 100.");
            }
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm không hợp lệ.");
        }

        if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
            try {
                if (giaTriGiamToiDa != null && !giaTriGiamToiDa.isEmpty()) {
                    BigDecimal gtd = new BigDecimal(giaTriGiamToiDa.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
                    if (gtd.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Giá trị giảm tối đa phải lớn hơn 0.");
                    } else {
                        voucher.setGiaTriGiamToiDa(gtd);
                    }
                } else {
                    errors.add("Phải nhập giá trị giảm tối đa khi giảm theo %.");
                }
            } catch (NumberFormatException e) {
                errors.add("Giá trị giảm tối đa không hợp lệ.");
            }
        } else {
            voucher.setGiaTriGiamToiDa(null);
        }

        try {
            if (giaTriGiamToiThieu != null && !giaTriGiamToiThieu.isEmpty()) {
                BigDecimal min = new BigDecimal(giaTriGiamToiThieu.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
                if (min.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Đơn tối thiểu phải >= 0.");
                } else {
                    voucher.setGiaTriGiamToiThieu(min);
                }
            }
        } catch (NumberFormatException e) {
            errors.add("Đơn tối thiểu không hợp lệ.");
        }

        if ("CASH".equalsIgnoreCase(voucher.getLoai())
                && voucher.getGiaTriGiam() != null
                && voucher.getGiaTriGiamToiThieu() != null
                && voucher.getGiaTriGiam().compareTo(voucher.getGiaTriGiamToiThieu()) > 0) {
            errors.add("Giá trị giảm không được lớn hơn đơn tối thiểu áp dụng.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() == null) errors.add("Ngày bắt đầu không được để trống.");
        else if (voucher.getNgayBatDau().isBefore(now)) errors.add("Ngày bắt đầu không được nằm trong quá khứ.");

        if (voucher.getNgayKetThuc() == null) errors.add("Ngày kết thúc không được để trống.");
        else if (voucher.getNgayKetThuc().isBefore(now)) errors.add("Ngày kết thúc không được nằm trong quá khứ.");

        if (voucher.getNgayBatDau() != null && voucher.getNgayKetThuc() != null
                && voucher.getNgayBatDau().isAfter(voucher.getNgayKetThuc())) {
            errors.add("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            if (selectedCustomerIds == null || selectedCustomerIds.isEmpty()) {
                errors.add("Vui lòng chọn ít nhất một khách hàng khi tạo phiếu cá nhân.");
            }
        } else if ("cong_khai".equalsIgnoreCase(voucher.getKieuPhieu())) {
            Integer gioiHan = voucher.getGioiHanSuDung();
            if (gioiHan == null || gioiHan <= 0) {
                errors.add("Vui lòng nhập số lượt sử dụng hợp lệ cho phiếu công khai.");
            } else {
                voucher.setSoLuong(gioiHan);
            }
        }

        // ---- Nếu lỗi: trả form + nạp lại danh sách PTTT ----
        if (!errors.isEmpty()) {
            model.addAttribute("errorMessage", String.join("<br>", errors));
            model.addAttribute("voucher", voucher);
            model.addAttribute("customers", phieuService.layTatCaKhachHang());
            model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll()); // [PTTT]
            model.addAttribute("selectedPaymentMethodIds", selectedPaymentMethodIds == null ? Collections.emptyList() : selectedPaymentMethodIds); // [PTTT]
            addUserInfoToModel(model);
            return "WebQuanLy/voucher-create";
        }

        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            voucher.setGioiHanSuDung(1);
        }

        voucher.setThoiGianTao(LocalDateTime.now());

        // ---- Gán phương thức thanh toán Many-to-Many ---- [PTTT]
        bindPaymentMethods(voucher, selectedPaymentMethodIds);

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

    // ===================== VIEW =====================
    @GetMapping("/view/{id}")
    public String viewDetails(@PathVariable UUID id,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "5") int size,
                              @RequestParam(required = false) String search,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        PhieuGiamGia voucher = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));

        model.addAttribute("viewMode", true);
        model.addAttribute("readOnly", true);

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

        // [PTTT] Hiển thị danh sách PTTT đã gán
        List<PhuongThucThanhToan> phuongThucList = phuongThucThanhToanRepository.findAll();
        List<UUID> selectedPaymentMethodIds = voucher.getPhuongThucThanhToans()
                .stream().map(PhuongThucThanhToan::getId).toList();

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
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);

        model.addAttribute("phuongThucList", phuongThucList);                // [PTTT]
        model.addAttribute("selectedPaymentMethodIds", selectedPaymentMethodIds); // [PTTT]

        addUserInfoToModel(model);
        return "WebQuanLy/voucher-detail";
    }

    // ===================== EDIT =====================
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size,
                               @RequestParam(required = false) String search,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        PhieuGiamGia voucher = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        String status = getTrangThai(voucher);
        if (!"Sắp diễn ra".equals(status)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể chỉnh sửa phiếu giảm giá ở trạng thái 'Sắp diễn ra'.");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        model.addAttribute("viewMode", false);
        model.addAttribute("readOnly", false);

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

        // [PTTT] cấp dữ liệu PTTT
        List<PhuongThucThanhToan> phuongThucList = phuongThucThanhToanRepository.findAll();
        List<UUID> selectedPaymentMethodIds = voucher.getPhuongThucThanhToans()
                .stream().map(PhuongThucThanhToan::getId).toList();

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
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);

        model.addAttribute("phuongThucList", phuongThucList);                 // [PTTT]
        model.addAttribute("selectedPaymentMethodIds", selectedPaymentMethodIds); // [PTTT]

        addUserInfoToModel(model);
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
                         @RequestParam(required = false, name = "selectedPaymentMethodIds") // [PTTT]
                         List<UUID> selectedPaymentMethodIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        voucher.setId(id);
        PhieuGiamGia existing = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));
        String status = getTrangThai(existing);
        if (!"Sắp diễn ra".equals(status)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể chỉnh sửa phiếu giảm giá ở trạng thái 'Sắp diễn ra'.");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        voucher.setThoiGianTao(existing.getThoiGianTao());
        List<String> errors = new ArrayList<>();

        // ---- Validate (giữ nguyên) ----
        if (voucher.getMa() == null || voucher.getMa().trim().isEmpty()) {
            errors.add("Mã phiếu không được để trống");
        } else {
            String trimmedMa = voucher.getMa().trim();
            if (phieuGiamGiaRepository.existsByMaIgnoreCase(trimmedMa) && !trimmedMa.equalsIgnoreCase(existing.getMa())) {
                errors.add("Mã phiếu đã tồn tại");
            }
            voucher.setMa(trimmedMa);
        }

        if (voucher.getTen() == null || voucher.getTen().trim().isEmpty()) {
            errors.add("Tên phiếu không được để trống");
        } else {
            voucher.setTen(voucher.getTen().trim());
        }

        try {
            BigDecimal value = new BigDecimal(giaTriGiam.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            if (value.compareTo(BigDecimal.ZERO) <= 0) errors.add("Giá trị giảm phải lớn hơn 0");
            voucher.setGiaTriGiam(value);
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm không hợp lệ");
        }

        try {
            if (giaTriGiamToiDa != null && !giaTriGiamToiDa.trim().isEmpty()) {
                BigDecimal max = new BigDecimal(giaTriGiamToiDa.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
                if ("PERCENT".equalsIgnoreCase(voucher.getLoai()) && max.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Giá trị giảm tối đa phải lớn hơn 0");
                }
                voucher.setGiaTriGiamToiDa(max);
            } else if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
                errors.add("Phải nhập giá trị giảm tối đa khi chọn giảm theo %");
            }
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm tối đa không hợp lệ");
        }

        try {
            if (giaTriGiamToiThieu != null && !giaTriGiamToiThieu.trim().isEmpty()) {
                BigDecimal min = new BigDecimal(giaTriGiamToiThieu.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
                if (min.compareTo(BigDecimal.ZERO) < 0) errors.add("Đơn tối thiểu không được âm");
                voucher.setGiaTriGiamToiThieu(min);
            }
        } catch (NumberFormatException e) {
            errors.add("Đơn tối thiểu không hợp lệ");
        }

        if ("CASH".equalsIgnoreCase(voucher.getLoai())
                && voucher.getGiaTriGiam() != null
                && voucher.getGiaTriGiamToiThieu() != null
                && voucher.getGiaTriGiam().compareTo(voucher.getGiaTriGiamToiThieu()) > 0) {
            errors.add("Giá trị giảm không được lớn hơn đơn tối thiểu áp dụng.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() == null) errors.add("Ngày bắt đầu không được để trống.");
        else if (voucher.getNgayBatDau().isBefore(now)) errors.add("Ngày bắt đầu không được nằm trong quá khứ.");

        if (voucher.getNgayKetThuc() == null) errors.add("Ngày kết thúc không được để trống.");
        else if (voucher.getNgayKetThuc().isBefore(now)) errors.add("Ngày kết thúc không được nằm trong quá khứ.");

        if (voucher.getNgayBatDau() != null && voucher.getNgayKetThuc() != null
                && voucher.getNgayBatDau().isAfter(voucher.getNgayKetThuc())) {
            errors.add("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())
                && (selectedCustomerIds == null || selectedCustomerIds.isEmpty())) {
            errors.add("Vui lòng chọn khách hàng áp dụng cho phiếu cá nhân");
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errorMessage", String.join("<br>", errors));
            model.addAttribute("voucher", voucher);
            model.addAttribute("customers", phieuService.layTatCaKhachHang());
            model.addAttribute("selectedCustomerIds", selectedCustomerIds);

            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            model.addAttribute("giaTriGiamStr", voucher.getGiaTriGiam() != null ? nf.format(voucher.getGiaTriGiam()) : "");
            model.addAttribute("giaTriGiamToiDaStr", voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) : "");
            model.addAttribute("giaTriGiamToiThieuStr", voucher.getGiaTriGiamToiThieu() != null ? nf.format(voucher.getGiaTriGiamToiThieu()) : "");

            // [PTTT] nạp lại dữ liệu
            model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());
            model.addAttribute("selectedPaymentMethodIds", selectedPaymentMethodIds == null ? Collections.emptyList() : selectedPaymentMethodIds);

            addUserInfoToModel(model);
            return "WebQuanLy/voucher-edit";
        }

        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            voucher.setGioiHanSuDung(1);
        }

        // ---- Cập nhật liên kết Many-to-Many ---- [PTTT]
        bindPaymentMethods(voucher, selectedPaymentMethodIds);

        PhieuGiamGia savedVoucher = phieuGiamGiaRepository.save(voucher);

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

    // ===================== DELETE =====================
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        PhieuGiamGia voucher = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));
        String status = getTrangThai(voucher);
        if ("Đang diễn ra".equals(status) || "Không xác định".equals(status)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể xóa phiếu giảm giá sắp diễn ra hoặc đã kết thúc.");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        try {
            phieuService.xoaTatCaGanKetTheoPhieu(id);
            phieuGiamGiaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa phiếu giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }

        return "redirect:/acvstore/phieu-giam-gia";
    }

    // ===================== Helpers =====================
    /** Gán/cập nhật Many-to-Many phương thức thanh toán cho voucher. */
    private void bindPaymentMethods(PhieuGiamGia voucher, List<UUID> ids) { // [PTTT]
        // Xóa link cũ (nếu có)
        if (voucher.getPhuongThucThanhToans() != null) {
            voucher.getPhuongThucThanhToans().clear();
        } else {
            voucher.setPhuongThucThanhToans(new HashSet<>());
        }

        if (ids == null || ids.isEmpty()) return;

        List<PhuongThucThanhToan> methods = phuongThucThanhToanRepository.findAllById(ids);
        // lọc null / trùng
        Set<PhuongThucThanhToan> unique = methods.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        voucher.getPhuongThucThanhToans().addAll(unique);
    }
}