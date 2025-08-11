package com.example.AsmGD1.controller.GiamGia;

import com.example.AsmGD1.entity.PhuongThucThanhToan;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaSpecification;
import com.example.AsmGD1.service.GiamGia.GuiMailService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
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
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
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

    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;

    // RBAC Helper Methods
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            return "ADMIN".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private boolean isCurrentUserEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
            return "EMPLOYEE".equalsIgnoreCase(user.getVaiTro());
        }
        return false;
    }

    private void addUserInfoToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NguoiDung) {
            NguoiDung user = (NguoiDung) auth.getPrincipal();
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
                ? LocalDateTime.parse(fromDate, formatter)
                : null;

        LocalDateTime to = (toDate != null && !toDate.isEmpty())
                ? LocalDateTime.parse(toDate, formatter)
                : null;

        Page<PhieuGiamGia> pageResult = phieuGiamGiaRepository.findAll(
                PhieuGiamGiaSpecification.filter(search, type, status, from, to), pageable
        );

        List<PhieuGiamGia> vouchers = pageResult.getContent();
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
        model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());

        return "WebQuanLy/voucher-list";
    }

    private String getTrangThai(PhieuGiamGia v) {
        LocalDateTime now = LocalDateTime.now();
        if (v.getNgayBatDau() == null || v.getNgayKetThuc() == null) {
            return "Không xác định";
        }
        if (now.isBefore(v.getNgayBatDau())) {
            return "Sắp diễn ra";
        } else if (!now.isAfter(v.getNgayKetThuc())) {
            return "Đang diễn ra";
        } else {
            return "Đã kết thúc";
        }
    }

    @GetMapping("/create")
    public String createForm(Model model, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        model.addAttribute("voucher", new PhieuGiamGia());
        model.addAttribute("customers", phieuService.layTatCaKhachHang());
        model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());
        model.addAttribute("selectedPtttIds", new ArrayList<UUID>());
        addUserInfoToModel(model);
        return "WebQuanLy/voucher-create";
    }

    @PostMapping("/create")
    @Transactional
    public String create(@RequestParam String giaTriGiam,
                         @RequestParam(required = false) String giaTriGiamToiDa,
                         @RequestParam(required = false) String giaTriGiamToiThieu,
                         @ModelAttribute PhieuGiamGia voucher,
                         @RequestParam(required = false) boolean sendMail,
                         @RequestParam(required = false) List<UUID> selectedCustomerIds,
                         @RequestParam(required = false) List<UUID> selectedPtttIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        System.out.println("Selected PTTT IDs: " + selectedPtttIds); // Log để debug

        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        List<String> errors = new ArrayList<>();

        // Validate mã phiếu
        if (voucher.getMa() == null || voucher.getMa().trim().isEmpty()) {
            errors.add("Mã phiếu không được để trống.");
        } else {
            voucher.setMa(voucher.getMa().trim());
            if (phieuGiamGiaRepository.existsByMaIgnoreCase(voucher.getMa())) {
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

        // Validate giá trị giảm tối đa
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

        // Validate đơn tối thiểu
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

        // Kiểm tra nếu giảm tiền mặt thì giá trị giảm không được lớn hơn đơn tối thiểu
        if ("CASH".equalsIgnoreCase(voucher.getLoai())
                && voucher.getGiaTriGiam() != null
                && voucher.getGiaTriGiamToiThieu() != null
                && voucher.getGiaTriGiam().compareTo(voucher.getGiaTriGiamToiThieu()) > 0) {
            errors.add("Giá trị giảm không được lớn hơn đơn tối thiểu áp dụng.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (voucher.getNgayBatDau() == null) {
            errors.add("Ngày bắt đầu không được để trống.");
        } else if (voucher.getNgayBatDau().isBefore(now)) {
            errors.add("Ngày bắt đầu không được nằm trong quá khứ.");
        }

        if (voucher.getNgayKetThuc() == null) {
            errors.add("Ngày kết thúc không được để trống.");
        } else if (voucher.getNgayKetThuc().isBefore(now)) {
            errors.add("Ngày kết thúc không được nằm trong quá khứ.");
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
        } else if ("cong_khai".equalsIgnoreCase(voucher.getKieuPhieu())) {
            Integer gioiHan = voucher.getGioiHanSuDung();
            if (gioiHan == null || gioiHan <= 0) {
                errors.add("Vui lòng nhập số lượt sử dụng hợp lệ cho phiếu công khai.");
            } else {
                voucher.setSoLuong(gioiHan);
            }
        }

        // Xử lý phương thức thanh toán (SỬA: Bỏ vòng lặp)
        Set<PhuongThucThanhToan> phuongThucThanhToans = new HashSet<>();
        if (selectedPtttIds != null && !selectedPtttIds.isEmpty()) {
            List<PhuongThucThanhToan> foundPttts = phuongThucThanhToanRepository.findAllById(selectedPtttIds);
            System.out.println("Found PTTTs: " + foundPttts);
            if (foundPttts.size() != selectedPtttIds.size()) {
                errors.add("Một hoặc nhiều phương thức thanh toán không hợp lệ.");
            } else {
                phuongThucThanhToans = new HashSet<>(foundPttts);
                voucher.setPhuongThucThanhToans(phuongThucThanhToans); // Hibernate sẽ tự đồng bộ quan hệ
            }
        } else {
            voucher.setPhuongThucThanhToans(new HashSet<>());
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errorMessage", String.join("<br>", errors));
            model.addAttribute("voucher", voucher);
            model.addAttribute("customers", phieuService.layTatCaKhachHang());
            model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());
            model.addAttribute("selectedPtttIds", selectedPtttIds != null ? selectedPtttIds : new ArrayList<>());
            addUserInfoToModel(model);
            return "WebQuanLy/voucher-create";
        }

        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            voucher.setGioiHanSuDung(1);
        }

        try {
            PhieuGiamGia savedVoucher = phieuGiamGiaRepository.save(voucher);
            System.out.println("Saved Voucher ID: " + savedVoucher.getId());
            System.out.println("Saved Voucher PTTTs: " + savedVoucher.getPhuongThucThanhToans());

            // Kiểm tra bảng nối
            List<Object[]> joinTableRecords = phieuGiamGiaRepository.findJoinTableRecords(savedVoucher.getId());
            System.out.println("Join Table Records: " + joinTableRecords);

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
        } catch (Exception e) {
            System.err.println("Error saving voucher: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu phiếu giảm giá: " + e.getMessage());
            return "redirect:/acvstore/phieu-giam-gia";
        }
    }

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
        List<UUID> selectedPtttIds = new ArrayList<>();
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            List<NguoiDung> daDuocGan = phieuService.layNguoiDungTheoPhieu(voucher.getId());
            selectedCustomerIds = daDuocGan.stream().map(NguoiDung::getId).toList();
        }
        selectedPtttIds = voucher.getPhuongThucThanhToans().stream().map(PhuongThucThanhToan::getId).toList();

        model.addAttribute("voucher", voucher);
        model.addAttribute("giaTriGiamStr", giaTriGiamStr);
        model.addAttribute("giaTriGiamToiDaStr", giaTriGiamToiDaStr);
        model.addAttribute("giaTriGiamToiThieuStr", giaTriGiamToiThieuStr);
        model.addAttribute("gioiHanSuDungStr", gioiHanSuDungStr);
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("selectedCustomerIds", selectedCustomerIds);
        model.addAttribute("selectedPtttIds", selectedPtttIds);
        model.addAttribute("currentCustomerPage", page);
        model.addAttribute("totalCustomerPages", customerPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);
        model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());
        addUserInfoToModel(model);

        return "WebQuanLy/voucher-detail";
    }

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
        List<UUID> selectedPtttIds = new ArrayList<>();
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            List<NguoiDung> daDuocGan = phieuService.layNguoiDungTheoPhieu(voucher.getId());
            selectedCustomerIds = daDuocGan.stream().map(NguoiDung::getId).toList();
        }
        selectedPtttIds = voucher.getPhuongThucThanhToans().stream().map(PhuongThucThanhToan::getId).toList();

        model.addAttribute("voucher", voucher);
        model.addAttribute("giaTriGiamStr", giaTriGiamStr);
        model.addAttribute("giaTriGiamToiDaStr", giaTriGiamToiDaStr);
        model.addAttribute("giaTriGiamToiThieuStr", giaTriGiamToiThieuStr);
        model.addAttribute("gioiHanSuDungStr", gioiHanSuDungStr);
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("selectedCustomerIds", selectedCustomerIds);
        model.addAttribute("selectedPtttIds", selectedPtttIds);
        model.addAttribute("currentCustomerPage", page);
        model.addAttribute("totalCustomerPages", customerPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);
        model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());
        addUserInfoToModel(model);

        return "WebQuanLy/voucher-edit";
    }

    @PostMapping("/edit/{id}")
    @Transactional
    public String update(@PathVariable UUID id,
                         @RequestParam String giaTriGiam,
                         @RequestParam(required = false) String giaTriGiamToiDa,
                         @RequestParam(required = false) String giaTriGiamToiThieu,
                         @ModelAttribute PhieuGiamGia voucher,
                         @RequestParam(required = false) boolean sendMail,
                         @RequestParam(required = false) List<UUID> selectedCustomerIds,
                         @RequestParam(required = false) List<UUID> selectedPtttIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        System.out.println("Selected PTTT IDs: " + selectedPtttIds); // Log để debug

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

        // Validate mã phiếu
        if (voucher.getMa() == null || voucher.getMa().trim().isEmpty()) {
            errors.add("Mã phiếu không được để trống");
        } else {
            String trimmedMa = voucher.getMa().trim();
            if (phieuGiamGiaRepository.existsByMaIgnoreCase(trimmedMa) && !trimmedMa.equalsIgnoreCase(existing.getMa())) {
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
            BigDecimal value = new BigDecimal(giaTriGiam.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Giá trị giảm phải lớn hơn 0");
            }
            voucher.setGiaTriGiam(value);
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm không hợp lệ");
        }

        // Giá trị giảm tối đa
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

        // Đơn tối thiểu
        try {
            if (giaTriGiamToiThieu != null && !giaTriGiamToiThieu.trim().isEmpty()) {
                BigDecimal min = new BigDecimal(giaTriGiamToiThieu.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
                if (min.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Đơn tối thiểu không được âm");
                }
                voucher.setGiaTriGiamToiThieu(min);
            }
        } catch (NumberFormatException e) {
            errors.add("Đơn tối thiểu không hợp lệ");
        }

        // Kiểm tra nếu giảm tiền mặt thì giá trị giảm không được lớn hơn đơn tối thiểu
        if ("CASH".equalsIgnoreCase(voucher.getLoai())
                && voucher.getGiaTriGiam() != null
                && voucher.getGiaTriGiamToiThieu() != null
                && voucher.getGiaTriGiam().compareTo(voucher.getGiaTriGiamToiThieu()) > 0) {
            errors.add("Giá trị giảm không được lớn hơn đơn tối thiểu áp dụng.");
        }

        // Validate loại giảm
        if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
            if (voucher.getGiaTriGiam() != null && voucher.getGiaTriGiam().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Giảm theo % phải lớn hơn 0");
            } else if (voucher.getGiaTriGiam() != null && voucher.getGiaTriGiam().compareTo(new BigDecimal("100")) > 0) {
                errors.add("Giảm theo % không được vượt quá 100");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        if (voucher.getNgayBatDau() == null) {
            errors.add("Ngày bắt đầu không được để trống.");
        } else if (voucher.getNgayBatDau().isBefore(now)) {
            errors.add("Ngày bắt đầu không được nằm trong quá khứ.");
        }

        if (voucher.getNgayKetThuc() == null) {
            errors.add("Ngày kết thúc không được để trống.");
        } else if (voucher.getNgayKetThuc().isBefore(now)) {
            errors.add("Ngày kết thúc không được nằm trong quá khứ.");
        }

        if (voucher.getNgayBatDau() != null && voucher.getNgayKetThuc() != null &&
                voucher.getNgayBatDau().isAfter(voucher.getNgayKetThuc())) {
            errors.add("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        // Phiếu cá nhân phải chọn khách hàng
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu()) &&
                (selectedCustomerIds == null || selectedCustomerIds.isEmpty())) {
            errors.add("Vui lòng chọn khách hàng áp dụng cho phiếu cá nhân");
        }

        // Xử lý phương thức thanh toán (SỬA: Bỏ vòng lặp)
        Set<PhuongThucThanhToan> phuongThucThanhToans = new HashSet<>();
        if (selectedPtttIds != null && !selectedPtttIds.isEmpty()) {
            List<PhuongThucThanhToan> foundPttts = phuongThucThanhToanRepository.findAllById(selectedPtttIds);
            System.out.println("Found PTTTs: " + foundPttts);
            if (foundPttts.size() != selectedPtttIds.size()) {
                errors.add("Một hoặc nhiều phương thức thanh toán không hợp lệ.");
            } else {
                phuongThucThanhToans = new HashSet<>(foundPttts);
                voucher.setPhuongThucThanhToans(phuongThucThanhToans); // Hibernate sẽ tự đồng bộ quan hệ
            }
        } else {
            voucher.setPhuongThucThanhToans(new HashSet<>());
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errorMessage", String.join("<br>", errors));
            model.addAttribute("voucher", voucher);
            model.addAttribute("customers", phieuService.layTatCaKhachHang());
            model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());
            model.addAttribute("selectedPtttIds", selectedPtttIds != null ? selectedPtttIds : new ArrayList<>());

            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            model.addAttribute("giaTriGiamStr", voucher.getGiaTriGiam() != null ? nf.format(voucher.getGiaTriGiam()) : "");
            model.addAttribute("giaTriGiamToiDaStr", voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) : "");
            model.addAttribute("giaTriGiamToiThieuStr", voucher.getGiaTriGiamToiThieu() != null ? nf.format(voucher.getGiaTriGiamToiThieu()) : "");
            model.addAttribute("gioiHanSuDungStr", voucher.getGioiHanSuDung() != null ? String.valueOf(voucher.getGioiHanSuDung()) : "");

            addUserInfoToModel(model);
            return "WebQuanLy/voucher-edit";
        }

        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            voucher.setGioiHanSuDung(1);
        }

        // Xóa các phương thức thanh toán cũ trước khi cập nhật
        phieuGiamGiaRepository.deletePhuongThucThanhToanByPhieuGiamGiaId(id);

        try {
            PhieuGiamGia savedVoucher = phieuGiamGiaRepository.save(voucher);
            System.out.println("Saved Voucher ID: " + savedVoucher.getId());
            System.out.println("Saved Voucher PTTTs: " + savedVoucher.getPhuongThucThanhToans());

            // Kiểm tra bảng nối
            List<Object[]> joinTableRecords = phieuGiamGiaRepository.findJoinTableRecords(savedVoucher.getId());
            System.out.println("Join Table Records: " + joinTableRecords);

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
        } catch (Exception e) {
            System.err.println("Error updating voucher: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật phiếu giảm giá: " + e.getMessage());
            return "redirect:/acvstore/phieu-giam-gia";
        }
    }

    @PostMapping("/delete/{id}")
    @Transactional
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
            phieuGiamGiaRepository.deletePhuongThucThanhToanByPhieuGiamGiaId(id);
            phieuGiamGiaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa phiếu giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }

        return "redirect:/acvstore/phieu-giam-gia";
    }
}