package com.ndh.ShopTechnology.services.user.impl;

import com.ndh.ShopTechnology.dto.request.user.AdminModUserInfoRequest;
import com.ndh.ShopTechnology.dto.request.user.CreateUserRequest;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportResponse;
import com.ndh.ShopTechnology.dto.response.catalog.CatalogImportRowResult;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.entities.role.RoleEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.RoleRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.importexport.ImportSupport;
import com.ndh.ShopTechnology.services.user.StaffImportService;
import com.ndh.ShopTechnology.services.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import nhân viên nội bộ. Tái dùng {@link UserService#createStaffUser}/{@link UserService#updateStaffUser}
 * nên kế thừa toàn bộ kiểm tra quyền & nghiệp vụ; mỗi dòng chạy transaction riêng (qua proxy service).
 *
 * <p>Chính sách: không xóa nhân viên vắng mặt; KHÔNG ghi đè mật khẩu khi cập nhật.
 */
@Service
public class StaffImportServiceImpl implements StaffImportService {

    private final ImportSupport support;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public StaffImportServiceImpl(ImportSupport support,
                                  UserService userService,
                                  UserRepository userRepository,
                                  RoleRepository roleRepository) {
        this.support = support;
        this.userService = userService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    private static final String C_USERNAME = "username", C_EMAIL = "email", C_PHONE = "phone",
            C_FULLNAME = "fullName", C_ROLE_ID = "roleId", C_ROLE_CODE = "roleCode", C_STATUS = "status",
            C_INFO1 = "info01", C_INFO2 = "info02", C_INFO3 = "info03", C_INFO4 = "info04";

    private static final Map<String, String> ALIASES = buildAliases();

    private static Map<String, String> buildAliases() {
        Map<String, String> m = new HashMap<>();
        for (String a : new String[]{"username", "taikhoan", "tendangnhap", "user"}) m.put(a, C_USERNAME);
        for (String a : new String[]{"email", "thudientu"}) m.put(a, C_EMAIL);
        for (String a : new String[]{"phone", "telephone", "phonenumber", "sodienthoai", "sdt", "dienthoai"}) m.put(a, C_PHONE);
        for (String a : new String[]{"fullname", "hoten", "tennhanvien", "ten", "name"}) m.put(a, C_FULLNAME);
        for (String a : new String[]{"roleid", "idvaitro", "idquyen"}) m.put(a, C_ROLE_ID);
        for (String a : new String[]{"rolecode", "vaitro", "quyen", "machucvu", "mavaitro"}) m.put(a, C_ROLE_CODE);
        for (String a : new String[]{"status", "trangthai"}) m.put(a, C_STATUS);
        for (String a : new String[]{"info01", "masap", "sapcode", "manhanvien"}) m.put(a, C_INFO1);
        for (String a : new String[]{"info02"}) m.put(a, C_INFO2);
        for (String a : new String[]{"info03"}) m.put(a, C_INFO3);
        for (String a : new String[]{"info04"}) m.put(a, C_INFO4);
        return m;
    }

    private static final String[] TEMPLATE_HEADERS = {
            "username", "email", "phone", "full_name", "role_id", "role_code", "status",
            "info01", "info02", "info03", "info04"
    };

    @Override
    public CatalogImportResponse importStaff(MultipartFile file) {
        List<String[]> rows = support.readRows(file);
        Map<Integer, String> col = support.mapHeader(rows.get(0), ALIASES);
        if (!col.containsValue(C_USERNAME) && !col.containsValue(C_EMAIL) && !col.containsValue(C_PHONE)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không tìm thấy cột định danh 'username' / 'email' / 'phone' ở dòng tiêu đề");
        }

        CatalogImportResponse resp = CatalogImportResponse.builder()
                .totalRows(0).createdCount(0).updatedCount(0).skippedCount(0).failureCount(0)
                .results(new ArrayList<>()).build();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (support.isRowEmpty(row)) continue;
            int excelRow = i + 1;
            String username = support.nullIfBlank(support.get(row, col, C_USERNAME));
            String email = support.nullIfBlank(support.get(row, col, C_EMAIL));
            String phone = support.nullIfBlank(support.get(row, col, C_PHONE));
            String key = username != null ? username : (email != null ? email : phone);
            try {
                UserEntity existing = resolveExisting(username, email, phone);
                Long roleId = resolveRoleId(
                        support.parseLong(support.get(row, col, C_ROLE_ID)),
                        support.nullIfBlank(support.get(row, col, C_ROLE_CODE)));
                String fullName = support.nullIfBlank(support.get(row, col, C_FULLNAME));
                Integer status = support.parseInt(support.get(row, col, C_STATUS));
                String info01 = support.nullIfBlank(support.get(row, col, C_INFO1));
                String info02 = support.nullIfBlank(support.get(row, col, C_INFO2));
                String info03 = support.nullIfBlank(support.get(row, col, C_INFO3));
                String info04 = support.nullIfBlank(support.get(row, col, C_INFO4));

                if (existing == null) {
                    if (username == null) {
                        throw new CustomApiException(HttpStatus.BAD_REQUEST,
                                "Tạo mới cần 'username'");
                    }
                    if (phone == null) {
                        throw new CustomApiException(HttpStatus.BAD_REQUEST,
                                "Tạo mới cần 'phone' (số điện thoại)");
                    }
                    CreateUserRequest req = CreateUserRequest.builder()
                            .username(username)
                            .email(email)
                            .phoneNumber(phone)
                            .fullName(fullName)
                            .roleId(roleId)
                            .info01(info01).info02(info02).info03(info03).info04(info04)
                            .build();
                    UserResponse created = userService.createStaffUser(req);
                    record(resp, excelRow, key, "CREATED", created.getId(),
                            "Đã tạo nhân viên (mật khẩu tạm sinh tự động)");
                } else {
                    // KHÔNG set password -> giữ nguyên mật khẩu hiện tại.
                    AdminModUserInfoRequest req = AdminModUserInfoRequest.builder()
                            .id(existing.getId())
                            .email(email)
                            .phoneNumber(phone)
                            .status(status)
                            .fullName(fullName)
                            .roleId(roleId)
                            .info01(info01).info02(info02).info03(info03).info04(info04)
                            .build();
                    UserResponse updated = userService.updateStaffUser(req);
                    record(resp, excelRow, key, "UPDATED", updated.getId(),
                            "Đã cập nhật nhân viên (không đổi mật khẩu)");
                }
            } catch (Exception e) {
                resp.setTotalRows(resp.getTotalRows() + 1);
                resp.setFailureCount(resp.getFailureCount() + 1);
                resp.getResults().add(CatalogImportRowResult.builder()
                        .rowNumber(excelRow).key(key).action("FAILED").success(false)
                        .message(ImportSupport.rootMessage(e))
                        .build());
            }
        }
        return resp;
    }

    private UserEntity resolveExisting(String username, String email, String phone) {
        UserEntity u = null;
        if (username != null) u = userRepository.findOneByUsername(username).orElse(null);
        if (u == null && email != null) u = userRepository.findOneByEmail(email).orElse(null);
        if (u == null && phone != null) u = userRepository.findOneByPhoneNumber(phone).orElse(null);
        return u;
    }

    /** Ưu tiên roleId; nếu trống thì tra roleCode. Trả null nếu không chỉ định (service sẽ áp role mặc định). */
    private Long resolveRoleId(Long roleId, String roleCode) {
        if (roleId != null) return roleId;
        if (roleCode != null) {
            RoleEntity role = roleRepository.findByCode(roleCode.trim().toUpperCase())
                    .orElseThrow(() -> new CustomApiException(HttpStatus.BAD_REQUEST,
                            "Không tìm thấy vai trò (role_code): " + roleCode));
            return role.getId();
        }
        return null;
    }

    private void record(CatalogImportResponse resp, int rowNum, String key, String action, Long id, String msg) {
        resp.setTotalRows(resp.getTotalRows() + 1);
        if ("CREATED".equals(action)) resp.setCreatedCount(resp.getCreatedCount() + 1);
        else resp.setUpdatedCount(resp.getUpdatedCount() + 1);
        resp.getResults().add(CatalogImportRowResult.builder()
                .rowNumber(rowNum).key(key).action(action).success(true).id(id).message(msg)
                .build());
    }

    @Override
    public byte[] buildTemplateXlsx() {
        String[][] examples = {
                {"nv.an", "an.nguyen@congty.vn", "0901234567", "Nguyễn Văn An", "", "STAFF", "1", "SAP001", "", "", ""},
                {"nv.binh", "binh.tran@congty.vn", "0907654321", "Trần Thị Bình", "3", "", "1", "SAP002", "", "", ""},
        };
        String[] guide = {
                "HƯỚNG DẪN IMPORT NHÂN VIÊN NỘI BỘ (STAFF)",
                "",
                "1. Mỗi DÒNG = một nhân viên.",
                "2. Khóa định danh để xác định đã tồn tại hay chưa: username → email → phone.",
                "   - Khớp 1 trong 3 => CẬP NHẬT nhân viên đó. Không khớp => TẠO MỚI.",
                "3. Tạo mới BẮT BUỘC có: username và phone. Mật khẩu để TRỐNG -> hệ thống tự sinh mật khẩu tạm.",
                "4. Vai trò: điền role_id HOẶC role_code (ví dụ STAFF). Để trống dùng vai trò mặc định.",
                "5. status: 1 = đang làm, 0 = ngưng (chỉ áp dụng khi CẬP NHẬT).",
                "6. info01 = mã SAP/mã nhân viên (dùng cho reset mật khẩu). info02..04: thông tin phụ.",
                "7. AN TOÀN: import KHÔNG xóa nhân viên vắng mặt và KHÔNG ghi đè mật khẩu khi cập nhật.",
        };
        return ImportSupport.buildTemplate("Nhan vien", TEMPLATE_HEADERS, examples, guide);
    }
}
