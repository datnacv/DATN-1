package com.example.AsmGD1.controller.GiamGia;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.entity.PhieuGiamGia;
import com.example.AsmGD1.entity.PhuongThucThanhToan;
import com.example.AsmGD1.repository.BanHang.PhuongThucThanhToanRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaRepository;
import com.example.AsmGD1.repository.GiamGia.PhieuGiamGiaSpecification;
import com.example.AsmGD1.service.GiamGia.GuiMailService;
import com.example.AsmGD1.service.GiamGia.PhieuGiamGiaCuaNguoiDungService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
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
                       @RequestParam(defaultValue = "5") int size,
                       Model model) {

        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            return "redirect:/login";
        }

        // ===== Sort: mới nhất trước (DESC theo thoiGianTao), tie-breaker theo id =====
        Sort sort = Sort.by(Sort.Direction.DESC, "thoiGianTao")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(page, size, sort);

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
            if ("SHIPPING".equalsIgnoreCase(v.getPhamViApDung())) {
                if ("FREESHIP_FULL".equalsIgnoreCase(v.getLoai())) {
                    map.put("giaTriGiam", "Freeship toàn phần");
                } else if ("FREESHIP_CAP".equalsIgnoreCase(v.getLoai())) {
                    String cap = v.getGiaTriGiamToiDa() != null ? nf.format(v.getGiaTriGiamToiDa()) + " ₫" : "0 ₫";
                    map.put("giaTriGiam", "Giảm phí ship tối đa " + cap);
                } else {
                    map.put("giaTriGiam", "-");
                }
            } else {
                String suffix = "PERCENT".equalsIgnoreCase(v.getLoai()) ? " %" : " ₫";
                map.put("giaTriGiam", (v.getGiaTriGiam() != null ? nf.format(v.getGiaTriGiam()) : "0") + suffix);
                if (v.getGiaTriGiamToiDa() != null && "PERCENT".equalsIgnoreCase(v.getLoai())) {
                    map.put("giaTriGiamToiDa", nf.format(v.getGiaTriGiamToiDa()) + " ₫");
                }
            }
            if (v.getGiaTriGiamToiThieu() != null) {
                map.put("giaTriGiamToiThieu", nf.format(v.getGiaTriGiamToiThieu()) + " ₫");
            }
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

        // Check số lượng trước
        if (v.getSoLuong() != null && v.getSoLuong() <= 0) {
            return "Đã kết thúc";
        }

        if (v.getNgayBatDau() != null && v.getNgayKetThuc() != null) {
            if (now.isBefore(v.getNgayBatDau())) {
                return "Sắp diễn ra";
            } else if (!now.isAfter(v.getNgayKetThuc())) {
                return "Đang diễn ra";
            } else {
                return "Đã kết thúc";
            }
        }
        return "Không xác định";
    }


    @GetMapping("/create")
    public String createForm(Model model, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setPhamViApDung("ORDER"); // mặc định
        model.addAttribute("voucher", voucher);
        model.addAttribute("customers", phieuService.layTatCaKhachHang());
        model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());
        model.addAttribute("selectedPtttIds", new ArrayList<UUID>());
        addUserInfoToModel(model);
        return "WebQuanLy/voucher-create";
    }

    @PostMapping("/create")
    @Transactional
    public String create(@RequestParam(required = false) String giaTriGiam,
                         @RequestParam(required = false) String giaTriGiamToiDa,
                         @RequestParam(required = false) String giaTriGiamToiThieu,
                         @ModelAttribute PhieuGiamGia voucher,
                         @RequestParam(required = false) boolean sendMail,
                         @RequestParam(required = false) List<UUID> selectedCustomerIds,
                         @RequestParam(required = false) List<UUID> selectedPtttIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {

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
        } else {
            voucher.setTen(voucher.getTen().trim());
        }

        // Validate phạm vi áp dụng
        if (voucher.getPhamViApDung() == null ||
                !(voucher.getPhamViApDung().equalsIgnoreCase("ORDER") ||
                        voucher.getPhamViApDung().equalsIgnoreCase("SHIPPING"))) {
            errors.add("Phạm vi áp dụng không hợp lệ (chỉ ORDER hoặc SHIPPING).");
        }

        // Parse helpers
        BigDecimal parsedGiaTriGiam = BigDecimal.ZERO;
        BigDecimal parsedGiaTriGiamToiDa = null;
        BigDecimal parsedGiaTriGiamToiThieu = null;

        try {
            if (giaTriGiam != null && !giaTriGiam.isBlank()) {
                parsedGiaTriGiam = new BigDecimal(giaTriGiam.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            }
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm không hợp lệ.");
        }

        try {
            if (giaTriGiamToiDa != null && !giaTriGiamToiDa.isBlank()) {
                parsedGiaTriGiamToiDa = new BigDecimal(giaTriGiamToiDa.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            }
        } catch (NumberFormatException e) {
            errors.add("Giá trị giảm tối đa không hợp lệ.");
        }

        try {
            if (giaTriGiamToiThieu != null && !giaTriGiamToiThieu.isBlank()) {
                parsedGiaTriGiamToiThieu = new BigDecimal(giaTriGiamToiThieu.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
                if (parsedGiaTriGiamToiThieu.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Đơn tối thiểu phải >= 0.");
                }
            }
        } catch (NumberFormatException e) {
            errors.add("Đơn tối thiểu không hợp lệ.");
        }

        // Nhánh theo phạm vi
        if ("ORDER".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if (voucher.getLoai() == null ||
                    !(voucher.getLoai().equalsIgnoreCase("PERCENT") || voucher.getLoai().equalsIgnoreCase("CASH"))) {
                errors.add("Kiểu giảm (ORDER) không hợp lệ (PERCENT hoặc CASH).");
            } else {
                if (parsedGiaTriGiam == null || parsedGiaTriGiam.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Giá trị giảm phải lớn hơn 0.");
                } else {
                    voucher.setGiaTriGiam(parsedGiaTriGiam);
                }
                if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
                    if (parsedGiaTriGiam.compareTo(new BigDecimal("100")) > 0) {
                        errors.add("Giảm theo % không được vượt quá 100.");
                    }
                    if (parsedGiaTriGiamToiDa == null || parsedGiaTriGiamToiDa.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Phải nhập giá trị giảm tối đa khi giảm theo %.");
                    } else {
                        voucher.setGiaTriGiamToiDa(parsedGiaTriGiamToiDa);
                    }
                } else {
                    // CASH
                    voucher.setGiaTriGiamToiDa(null);
                    if (parsedGiaTriGiamToiThieu != null &&
                            parsedGiaTriGiam.compareTo(parsedGiaTriGiamToiThieu) > 0) {
                        errors.add("Giá trị giảm không được lớn hơn đơn tối thiểu áp dụng.");
                    }
                }
            }
        } else if ("SHIPPING".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if (voucher.getLoai() == null ||
                    !(voucher.getLoai().equalsIgnoreCase("FREESHIP_FULL") ||
                            voucher.getLoai().equalsIgnoreCase("FREESHIP_CAP"))) {
                errors.add("Loại freeship không hợp lệ (FREESHIP_FULL hoặc FREESHIP_CAP).");
            } else {
                voucher.setGiaTriGiam(BigDecimal.ZERO); // freeship không dùng giaTriGiam
                if ("FREESHIP_CAP".equalsIgnoreCase(voucher.getLoai())) {
                    if (parsedGiaTriGiamToiDa == null || parsedGiaTriGiamToiDa.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Phải nhập 'Giảm phí ship tối đa' (> 0) cho FREESHIP_CAP.");
                    } else {
                        voucher.setGiaTriGiamToiDa(parsedGiaTriGiamToiDa);
                        if (parsedGiaTriGiamToiThieu != null && parsedGiaTriGiamToiThieu.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal maxAllowedDiscount = parsedGiaTriGiamToiThieu.multiply(new BigDecimal("1.5"));
                            if (parsedGiaTriGiamToiDa.compareTo(maxAllowedDiscount) > 0) {
                                errors.add("Phí giảm tối đa không được vượt quá 150% giá trị đơn hàng tối thiểu.");
                            }
                        }
                    }
                } else {
                    // FREESHIP_FULL
                    voucher.setGiaTriGiamToiDa(null);
                }
            }
        }

        // Gán đơn tối thiểu (dùng chung)
        if (parsedGiaTriGiamToiThieu != null) {
            voucher.setGiaTriGiamToiThieu(parsedGiaTriGiamToiThieu);
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

        if (voucher.getNgayBatDau() != null
                && voucher.getNgayKetThuc() != null
                && voucher.getNgayBatDau().isAfter(voucher.getNgayKetThuc())) {
            errors.add("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        // Phiếu cá nhân/công khai
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            if (selectedCustomerIds == null || selectedCustomerIds.isEmpty()) {
                errors.add("Vui lòng chọn ít nhất một khách hàng khi tạo phiếu cá nhân.");
            } else {
                voucher.setGioiHanSuDung(1);
            }
        } else if ("cong_khai".equalsIgnoreCase(voucher.getKieuPhieu())) {
            Integer gioiHan = voucher.getGioiHanSuDung();
            if (gioiHan == null || gioiHan <= 0) {
                errors.add("Vui lòng nhập số lượt sử dụng hợp lệ cho phiếu công khai.");
            } else {
                voucher.setSoLuong(gioiHan);
            }
        }

        // Phương thức thanh toán áp dụng (BẮT BUỘC)
        if (selectedPtttIds == null || selectedPtttIds.isEmpty()) {
            errors.add("Vui lòng chọn ít nhất một phương thức thanh toán áp dụng.");
            voucher.setPhuongThucThanhToans(new HashSet<>()); // để render lại form không lỗi null
        } else {
            List<PhuongThucThanhToan> foundPttts = phuongThucThanhToanRepository.findAllById(selectedPtttIds);
            if (foundPttts.size() != selectedPtttIds.size() || foundPttts.isEmpty()) {
                errors.add("Một hoặc nhiều phương thức thanh toán không hợp lệ.");
                voucher.setPhuongThucThanhToans(new HashSet<>());
            } else {
                voucher.setPhuongThucThanhToans(new HashSet<>(foundPttts));
            }
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

        // ===== Set thời gian tạo nếu chưa có (đảm bảo sort “mới nhất trước”) =====
        if (voucher.getThoiGianTao() == null) {
            voucher.setThoiGianTao(LocalDateTime.now());
        }

        try {
            PhieuGiamGia savedVoucher = phieuGiamGiaRepository.save(voucher);

            if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu()) && selectedCustomerIds != null) {
                List<NguoiDung> selectedUsers = phieuService.layNguoiDungTheoIds(selectedCustomerIds);
                for (NguoiDung user : selectedUsers) {
                    phieuService.ganPhieuChoNguoiDung(user, savedVoucher);
                }

                if (sendMail) {
                    for (NguoiDung user : selectedUsers) {
                        guiMailService.guiPhieuGiamGia(user, savedVoucher);
                    }
                    redirectAttributes.addFlashAttribute("mailMessage", "Đã gửi email đến khách hàng được chọn.");
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu giảm giá thành công!");
            return "redirect:/acvstore/phieu-giam-gia";
        } catch (Exception e) {
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
        String giaTriGiamStr;
        String giaTriGiamToiDaStr = voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) : "";
        String giaTriGiamToiThieuStr = voucher.getGiaTriGiamToiThieu() != null ? nf.format(voucher.getGiaTriGiamToiThieu()) : "";
        String gioiHanSuDungStr = voucher.getGioiHanSuDung() != null ? String.valueOf(voucher.getGioiHanSuDung()) : "";

        if ("SHIPPING".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if ("FREESHIP_FULL".equalsIgnoreCase(voucher.getLoai())) {
                giaTriGiamStr = "Freeship toàn phần";
            } else if ("FREESHIP_CAP".equalsIgnoreCase(voucher.getLoai())) {
                giaTriGiamStr = "Giảm phí ship tối đa " + (voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) + " ₫" : "0 ₫");
            } else {
                giaTriGiamStr = "-";
            }
        } else {
            giaTriGiamStr = nf.format(voucher.getGiaTriGiam());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<NguoiDung> customerPage = (search != null && !search.isBlank())
                ? phieuService.timKiemKhachHangPhanTrang(search, pageable)
                : phieuService.layTatCaKhachHangPhanTrang(pageable);

        List<UUID> selectedCustomerIds = new ArrayList<>();
        List<UUID> selectedPtttIds = voucher.getPhuongThucThanhToans().stream().map(PhuongThucThanhToan::getId).toList();
        List<PhuongThucThanhToan> phuongThucList = selectedPtttIds.isEmpty()
                ? new ArrayList<>()
                : phuongThucThanhToanRepository.findAllById(selectedPtttIds);

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
        model.addAttribute("selectedPtttIds", selectedPtttIds);
        model.addAttribute("phuongThucList", phuongThucList);
        model.addAttribute("currentCustomerPage", page);
        model.addAttribute("totalCustomerPages", customerPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);
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
        String giaTriGiamStr;
        String giaTriGiamToiDaStr = voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) : "";
        String giaTriGiamToiThieuStr = voucher.getGiaTriGiamToiThieu() != null ? nf.format(voucher.getGiaTriGiamToiThieu()) : "";
        String gioiHanSuDungStr = voucher.getGioiHanSuDung() != null ? String.valueOf(voucher.getGioiHanSuDung()) : "";

        if ("SHIPPING".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if ("FREESHIP_FULL".equalsIgnoreCase(voucher.getLoai())) {
                giaTriGiamStr = "Freeship toàn phần";
            } else if ("FREESHIP_CAP".equalsIgnoreCase(voucher.getLoai())) {
                giaTriGiamStr = "Giảm phí ship tối đa " + (voucher.getGiaTriGiamToiDa() != null ? nf.format(voucher.getGiaTriGiamToiDa()) + " ₫" : "0 ₫");
            } else {
                giaTriGiamStr = "-";
            }
        } else {
            giaTriGiamStr = nf.format(voucher.getGiaTriGiam());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<NguoiDung> customerPage = (search != null && !search.isBlank())
                ? phieuService.timKiemKhachHangPhanTrang(search, pageable)
                : phieuService.layTatCaKhachHangPhanTrang(pageable);

        List<UUID> selectedCustomerIds = new ArrayList<>();
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            List<NguoiDung> daDuocGan = phieuService.layNguoiDungTheoPhieu(voucher.getId());
            selectedCustomerIds = daDuocGan.stream().map(NguoiDung::getId).toList();
        }
        List<UUID> selectedPtttIds = voucher.getPhuongThucThanhToans().stream().map(PhuongThucThanhToan::getId).toList();

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
                         @RequestParam(required = false) String giaTriGiam,
                         @RequestParam(required = false) String giaTriGiamToiDa,
                         @RequestParam(required = false) String giaTriGiamToiThieu,
                         @ModelAttribute PhieuGiamGia voucher,
                         @RequestParam(required = false) boolean sendMail,
                         @RequestParam(required = false) List<UUID> selectedCustomerIds,
                         @RequestParam(required = false) List<UUID> selectedPtttIds,
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

        // Validate Mã & Tên
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

        // Validate phạm vi
        if (voucher.getPhamViApDung() == null ||
                !(voucher.getPhamViApDung().equalsIgnoreCase("ORDER") ||
                        voucher.getPhamViApDung().equalsIgnoreCase("SHIPPING"))) {
            errors.add("Phạm vi áp dụng không hợp lệ (chỉ ORDER hoặc SHIPPING).");
        }

        // Parse số
        BigDecimal parsedGiaTriGiam = BigDecimal.ZERO;
        BigDecimal parsedGiaTriGiamToiDa = null;
        BigDecimal parsedGiaTriGiamToiThieu = null;
        try {
            if (giaTriGiam != null && !giaTriGiam.isBlank()) {
                parsedGiaTriGiam = new BigDecimal(giaTriGiam.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            }
        } catch (NumberFormatException e) { errors.add("Giá trị giảm không hợp lệ"); }
        try {
            if (giaTriGiamToiDa != null && !giaTriGiamToiDa.isBlank()) {
                parsedGiaTriGiamToiDa = new BigDecimal(giaTriGiamToiDa.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
            }
        } catch (NumberFormatException e) { errors.add("Giá trị giảm tối đa không hợp lệ"); }
        try {
            if (giaTriGiamToiThieu != null && !giaTriGiamToiThieu.isBlank()) {
                parsedGiaTriGiamToiThieu = new BigDecimal(giaTriGiamToiThieu.replaceAll("[^\\d.]", "").replaceFirst("\\.(\\d+?)\\.", "$1"));
                if (parsedGiaTriGiamToiThieu.compareTo(BigDecimal.ZERO) < 0) errors.add("Đơn tối thiểu không được âm");
            }
        } catch (NumberFormatException e) { errors.add("Đơn tối thiểu không hợp lệ"); }

        // Theo phạm vi / loại
        if ("ORDER".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if (voucher.getLoai() == null ||
                    !(voucher.getLoai().equalsIgnoreCase("PERCENT") || voucher.getLoai().equalsIgnoreCase("CASH"))) {
                errors.add("Kiểu giảm (ORDER) không hợp lệ (PERCENT hoặc CASH).");
            } else {
                if (parsedGiaTriGiam == null || parsedGiaTriGiam.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Giá trị giảm phải lớn hơn 0");
                } else {
                    voucher.setGiaTriGiam(parsedGiaTriGiam);
                }
                if ("PERCENT".equalsIgnoreCase(voucher.getLoai())) {
                    if (parsedGiaTriGiam.compareTo(new BigDecimal("100")) > 0) errors.add("Giảm theo % không được vượt quá 100");
                    if (parsedGiaTriGiamToiDa == null || parsedGiaTriGiamToiDa.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Phải nhập giá trị giảm tối đa khi chọn giảm theo %");
                    } else {
                        voucher.setGiaTriGiamToiDa(parsedGiaTriGiamToiDa);
                    }
                } else { // CASH
                    voucher.setGiaTriGiamToiDa(null);
                    if (parsedGiaTriGiamToiThieu != null && parsedGiaTriGiam.compareTo(parsedGiaTriGiamToiThieu) > 0) {
                        errors.add("Giá trị giảm không được lớn hơn đơn tối thiểu áp dụng.");
                    }
                }
            }
        } else if ("SHIPPING".equalsIgnoreCase(voucher.getPhamViApDung())) {
            if (voucher.getLoai() == null ||
                    !(voucher.getLoai().equalsIgnoreCase("FREESHIP_FULL") || voucher.getLoai().equalsIgnoreCase("FREESHIP_CAP"))) {
                errors.add("Loại freeship không hợp lệ (FREESHIP_FULL hoặc FREESHIP_CAP).");
            } else {
                voucher.setGiaTriGiam(BigDecimal.ZERO);
                if ("FREESHIP_CAP".equalsIgnoreCase(voucher.getLoai())) {
                    if (parsedGiaTriGiamToiDa == null || parsedGiaTriGiamToiDa.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Phải nhập 'Giảm phí ship tối đa' (> 0) cho FREESHIP_CAP");
                    } else {
                        voucher.setGiaTriGiamToiDa(parsedGiaTriGiamToiDa);
                        if (parsedGiaTriGiamToiThieu != null && parsedGiaTriGiamToiThieu.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal maxAllowed = parsedGiaTriGiamToiThieu.multiply(new BigDecimal("1.5"));
                            if (parsedGiaTriGiamToiDa.compareTo(maxAllowed) > 0) {
                                errors.add("Phí giảm tối đa không được vượt quá 150% giá trị đơn hàng tối thiểu.");
                            }
                        }
                    }
                } else {
                    voucher.setGiaTriGiamToiDa(null);
                }
            }
        }

        if (parsedGiaTriGiamToiThieu != null) {
            voucher.setGiaTriGiamToiThieu(parsedGiaTriGiamToiThieu);
        }

        // Thời gian
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() == null) {
            errors.add("Ngày bắt đầu không được để trống.");
        } else if (voucher.getNgayBatDau().isBefore(now)) {
            errors.add("Ngày bắt đầu không được trước thời điểm hiện tại.");
        }
        if (voucher.getNgayKetThuc() == null) {
            errors.add("Ngày kết thúc không được để trống.");
        } else if (voucher.getNgayKetThuc().isBefore(now)) {
            errors.add("Ngày kết thúc không được nằm trong quá khứ.");
        }
        if (voucher.getNgayBatDau() != null && voucher.getNgayKetThuc() != null
                && voucher.getNgayBatDau().isAfter(voucher.getNgayKetThuc())) {
            errors.add("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }

        // Phiếu cá nhân
        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu()) &&
                (selectedCustomerIds == null || selectedCustomerIds.isEmpty())) {
            errors.add("Vui lòng chọn khách hàng áp dụng cho phiếu cá nhân");
        }

        // PTTT (BẮT BUỘC)
        if (selectedPtttIds == null || selectedPtttIds.isEmpty()) {
            errors.add("Vui lòng chọn ít nhất một phương thức thanh toán áp dụng.");
            voucher.setPhuongThucThanhToans(new HashSet<>());
        } else {
            List<PhuongThucThanhToan> found = phuongThucThanhToanRepository.findAllById(selectedPtttIds);
            if (found.size() != selectedPtttIds.size() || found.isEmpty()) {
                errors.add("Một hoặc nhiều phương thức thanh toán không hợp lệ.");
                voucher.setPhuongThucThanhToans(new HashSet<>());
            } else {
                voucher.setPhuongThucThanhToans(new HashSet<>(found));
            }
        }

        if (!errors.isEmpty()) {
            model.addAttribute("voucher", existing);
            model.addAttribute("errorMessage", String.join("<br>", errors));
            model.addAttribute("customers", phieuService.layTatCaKhachHang());
            model.addAttribute("phuongThucList", phuongThucThanhToanRepository.findAll());
            model.addAttribute("selectedPtttIds", selectedPtttIds != null ? selectedPtttIds : new ArrayList<>());
            model.addAttribute("selectedCustomerIds", selectedCustomerIds != null ? selectedCustomerIds : new ArrayList<>());

            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
            model.addAttribute("giaTriGiamStr", existing.getGiaTriGiam() != null ? nf.format(existing.getGiaTriGiam()) : "");
            model.addAttribute("giaTriGiamToiDaStr", existing.getGiaTriGiamToiDa() != null ? nf.format(existing.getGiaTriGiamToiDa()) : "");
            model.addAttribute("giaTriGiamToiThieuStr", existing.getGiaTriGiamToiThieu() != null ? nf.format(existing.getGiaTriGiamToiThieu()) : "");
            model.addAttribute("gioiHanSuDungStr", existing.getGioiHanSuDung() != null ? String.valueOf(existing.getGioiHanSuDung()) : "");
            model.addAttribute("getStatus", (Function<PhieuGiamGia, String>) this::getTrangThai);

            addUserInfoToModel(model);
            return "WebQuanLy/voucher-edit";
        }

        if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu())) {
            voucher.setGioiHanSuDung(1);
        }

        phieuGiamGiaRepository.deletePhuongThucThanhToanByPhieuGiamGiaId(id);

        try {
            PhieuGiamGia savedVoucher = phieuGiamGiaRepository.save(voucher);

            if ("ca_nhan".equalsIgnoreCase(voucher.getKieuPhieu()) && selectedCustomerIds != null) {
                List<NguoiDung> users = phieuService.layNguoiDungTheoIds(selectedCustomerIds);
                for (NguoiDung user : users) {
                    phieuService.ganPhieuChoNguoiDung(user, savedVoucher);
                }
                if (sendMail) {
                    try {
                        for (NguoiDung user : users) guiMailService.guiPhieuGiamGia(user, savedVoucher);
                        redirectAttributes.addFlashAttribute("mailMessage", "Đã gửi mail cập nhật cho khách hàng");
                    } catch (Exception e) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi gửi mail: " + e.getMessage());
                    }
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phiếu giảm giá thành công!");
            return "redirect:/acvstore/phieu-giam-gia";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật phiếu giảm giá: " + e.getMessage());
            return "redirect:/acvstore/phieu-giam-gia";
        }
    }

    // ===== XÓA: chỉ cho phép khi "Sắp diễn ra" =====
    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteByPath(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        return doDelete(id, redirectAttributes);
    }

    @PostMapping({"/delete", "/delete/"})
    @Transactional
    public String deleteByParam(@RequestParam("id") UUID id, RedirectAttributes redirectAttributes) {
        return doDelete(id, redirectAttributes);
    }

    private String doDelete(UUID id, RedirectAttributes redirectAttributes) {
        if (!isCurrentUserAdmin()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập chức năng này!");
            return "redirect:/acvstore/phieu-giam-gia";
        }

        PhieuGiamGia voucher = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));

        String status = getTrangThai(voucher);
        if (!"Sắp diễn ra".equals(status)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Chỉ có thể xóa phiếu giảm giá ở trạng thái 'Sắp diễn ra'.");
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

    // ====== API: TÓM TẮT SỐ LƯỢNG & TRẠNG THÁI (cho AJAX) ======
    @GetMapping("/api/summary")
    @ResponseBody
    public List<Map<String, Object>> summaryByIds(@RequestParam("ids") String idsCsv) {
        if (!isCurrentUserAdmin() && !isCurrentUserEmployee()) {
            return Collections.emptyList();
        }
        if (idsCsv == null || idsCsv.isBlank()) return Collections.emptyList();

        List<UUID> ids = Arrays.stream(idsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());

        List<PhieuGiamGia> list = phieuGiamGiaRepository.findAllById(ids);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PhieuGiamGia v : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", v.getId().toString());
            m.put("soLuong", v.getSoLuong());
            m.put("status", getTrangThai(v));
            result.add(m);
        }
        return result;
    }
}
