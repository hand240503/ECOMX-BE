package com.ndh.ShopTechnology.services.department.impl;

import com.ndh.ShopTechnology.constants.RoleConstant;
import com.ndh.ShopTechnology.dto.request.department.CreateDepartmentRequest;
import com.ndh.ShopTechnology.dto.response.department.DepartmentMemberResponse;
import com.ndh.ShopTechnology.dto.response.department.DepartmentResponse;
import com.ndh.ShopTechnology.entities.department.DepartmentEntity;
import com.ndh.ShopTechnology.entities.department.UserDepartmentEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.DepartmentRepository;
import com.ndh.ShopTechnology.repository.UserDepartmentRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.department.DepartmentService;
import com.ndh.ShopTechnology.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private static final String POS_LEADER = "LEADER";
    private static final String POS_MEMBER = "MEMBER";

    private final DepartmentRepository departmentRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // ── Helpers để kiểm tra quyền ─────────────────────────────────────────────

    private UserEntity requireAdminOrManager() {
        UserEntity actor = userService.getCurrentUser();
        if (!actor.hasAnyRole(
                RoleConstant.ROLE_SUPER_ADMIN,
                RoleConstant.ROLE_ADMIN,
                RoleConstant.ROLE_MANAGER)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN,
                    "Chỉ Admin hoặc Manager mới có quyền thực hiện thao tác này.");
        }
        return actor;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> listAll() {
        return departmentRepository.findAll().stream()
                .map(d -> buildResponse(d, false))
                .collect(Collectors.toList());
    }

    // ── Get by id ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        DepartmentEntity dept = departmentRepository.findByIdWithMembers(id)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy phòng ban id=" + id));
        return buildResponse(dept, true);
    }

    // ── My departments (staff chỉ xem được phòng ban của mình) ───────────────

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getMyDepartments() {
        UserEntity currentUser = userService.getCurrentUser();
        List<DepartmentEntity> depts = departmentRepository.findActiveByUserId(currentUser.getId());
        return depts.stream()
                .map(d -> buildResponse(d, true))
                .collect(Collectors.toList());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        requireAdminOrManager();
        if (departmentRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new CustomApiException(HttpStatus.CONFLICT,
                    "Tên phòng ban '" + request.getName().trim() + "' đã tồn tại.");
        }
        DepartmentEntity dept = DepartmentEntity.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .color(request.getColor())
                .permissionCodesCsv(encodePermissions(request.getPermissionCodes()))
                .status(1)
                .build();
        dept = departmentRepository.save(dept);
        log.info("Department created: id={}, name={}", dept.getId(), dept.getName());
        return buildResponse(dept, false);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DepartmentResponse update(Long id, CreateDepartmentRequest request) {
        requireAdminOrManager();
        DepartmentEntity dept = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy phòng ban id=" + id));

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new CustomApiException(HttpStatus.CONFLICT,
                    "Tên phòng ban '" + request.getName().trim() + "' đã được dùng bởi phòng ban khác.");
        }
        dept.setName(request.getName().trim());
        dept.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        dept.setColor(request.getColor());
        dept.setPermissionCodesCsv(encodePermissions(request.getPermissionCodes()));
        dept = departmentRepository.save(dept);
        log.info("Department updated: id={}", id);
        return buildResponse(dept, false);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void delete(Long id) {
        requireAdminOrManager();
        DepartmentEntity dept = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy phòng ban id=" + id));
        departmentRepository.delete(dept);
        log.info("Department deleted: id={}", id);
    }

    // ── Add member ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DepartmentResponse addMember(Long departmentId, Long userId, String position) {
        requireAdminOrManager();
        DepartmentEntity dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy phòng ban id=" + departmentId));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy user id=" + userId));

        if (userDepartmentRepository.existsByUser_IdAndDepartment_Id(userId, departmentId)) {
            throw new CustomApiException(HttpStatus.CONFLICT,
                    "Người dùng đã là thành viên của phòng ban này.");
        }

        // Đảm bảo mỗi phòng ban chỉ có 1 LEADER
        String pos = (position != null && POS_LEADER.equalsIgnoreCase(position.trim()))
                ? POS_LEADER : POS_MEMBER;

        if (POS_LEADER.equals(pos)
                && userDepartmentRepository.existsByDepartment_IdAndPosition(departmentId, POS_LEADER)) {
            throw new CustomApiException(HttpStatus.CONFLICT,
                    "Phòng ban này đã có Leader. Hãy xoá Leader cũ trước khi gán Leader mới.");
        }

        String actor = currentUsername();
        UserDepartmentEntity ud = UserDepartmentEntity.builder()
                .user(user)
                .department(dept)
                .position(pos)
                .assignedBy(actor)
                .build();
        userDepartmentRepository.save(ud);
        log.info("User {} added to department {} as {} by {}", userId, departmentId, pos, actor);
        return getById(departmentId);
    }

    // ── Remove member ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void removeMember(Long departmentId, Long userId) {
        requireAdminOrManager();
        if (!departmentRepository.existsById(departmentId)) {
            throw new NotFoundEntityException("Không tìm thấy phòng ban id=" + departmentId);
        }
        userDepartmentRepository.deleteByUser_IdAndDepartment_Id(userId, departmentId);
        log.info("User {} removed from department {}", userId, departmentId);
    }

    // ── Build response ────────────────────────────────────────────────────────

    private DepartmentResponse buildResponse(DepartmentEntity dept, boolean withMembers) {
        List<UserDepartmentEntity> udList = withMembers
                ? userDepartmentRepository.findByDepartmentIdWithUser(dept.getId())
                : userDepartmentRepository.findByDepartmentIdWithUser(dept.getId()); // always load for leaderName

        String leaderName = udList.stream()
                .filter(ud -> POS_LEADER.equals(ud.getPosition()))
                .findFirst()
                .map(ud -> {
                    UserEntity u = ud.getUser();
                    String fullName = (u.getUserInfo() != null) ? u.getUserInfo().getFullName() : null;
                    return fullName != null ? fullName : u.getUsername();
                })
                .orElse(null);

        List<DepartmentMemberResponse> memberResponses = udList.stream()
                .map(DepartmentMemberResponse::fromEntity)
                .collect(Collectors.toList());

        DepartmentResponse r = DepartmentResponse.fromEntity(dept);
        r.setMemberCount(memberResponses.size());
        r.setLeaderName(leaderName);
        if (withMembers) {
            r.setMembers(memberResponses);
        }
        return r;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String encodePermissions(Set<Integer> codes) {
        if (codes == null || codes.isEmpty()) return null;
        return codes.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String currentUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}
