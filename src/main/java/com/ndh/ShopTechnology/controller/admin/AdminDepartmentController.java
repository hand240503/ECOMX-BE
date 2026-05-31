package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.dto.request.department.AddDepartmentMemberRequest;
import com.ndh.ShopTechnology.dto.request.department.CreateDepartmentRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.department.DepartmentResponse;
import com.ndh.ShopTechnology.services.department.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/admin/departments")
@RequiredArgsConstructor
public class AdminDepartmentController {

    private final DepartmentService departmentService;

    // ── List & Get ─────────────────────────────────────────────────────────────

    /** Admin/Manager: xem tất cả phòng ban. */
    @GetMapping
    public ResponseEntity<APIResponse<List<DepartmentResponse>>> listAll() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", departmentService.listAll(), null, null));
    }

    /** Bất kỳ staff nào đã đăng nhập: xem phòng ban của chính mình. */
    @GetMapping("/my")
    public ResponseEntity<APIResponse<List<DepartmentResponse>>> myDepartments() {
        return ResponseEntity.ok(APIResponse.of(true, "OK", departmentService.getMyDepartments(), null, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<DepartmentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", departmentService.getById(id), null, null));
    }

    // ── CRUD (Admin/Manager only — validated in service) ──────────────────────

    @PostMapping
    public ResponseEntity<APIResponse<DepartmentResponse>> create(
            @Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.of(true, "Tạo phòng ban thành công", departmentService.create(request), null, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<DepartmentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Cập nhật phòng ban thành công",
                departmentService.update(id, request), null, null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Map<String, Long>>> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(APIResponse.of(true, "Đã xoá phòng ban", Map.of("id", id), null, null));
    }

    // ── Member management (Admin/Manager only) ────────────────────────────────

    /**
     * Thêm thành viên vào phòng ban.
     * Body: { "position": "LEADER" | "MEMBER" }
     * Mỗi phòng ban chỉ được có 1 LEADER.
     */
    @PostMapping("/{id}/members/{userId}")
    public ResponseEntity<APIResponse<DepartmentResponse>> addMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody(required = false) AddDepartmentMemberRequest request) {
        String position = (request != null && request.getPosition() != null)
                ? request.getPosition() : "MEMBER";
        return ResponseEntity.ok(APIResponse.of(true, "Đã thêm thành viên",
                departmentService.addMember(id, userId, position), null, null));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<APIResponse<Void>> removeMember(
            @PathVariable Long id, @PathVariable Long userId) {
        departmentService.removeMember(id, userId);
        return ResponseEntity.ok(APIResponse.of(true, "Đã xoá thành viên", null, null, null));
    }
}
