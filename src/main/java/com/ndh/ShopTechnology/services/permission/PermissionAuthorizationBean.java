package com.ndh.ShopTechnology.services.permission;

import com.ndh.ShopTechnology.constants.DocumentEntityType;
import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import com.ndh.ShopTechnology.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Bean được expose dưới tên {@code @perm} để dùng trong SpEL của {@code @PreAuthorize}:
 *
 * <pre>
 *     // Yêu cầu quyền tạo Product (100001) hoặc system-wide CREATE_ALL (101)
 *     &#64;PreAuthorize("@perm.check(100001)")
 *     public ResponseEntity&lt;...&gt; createProduct(...) { ... }
 * </pre>
 *
 * <p>Upload / metadata / replace / xoá <b>tài liệu gắn catalogue</b> (ảnh SKU, variant, danh mục, thương hiệu)
 * chấp nhận luôn các mã nhóm {@code 100xxx} tương ứng (CREATE/UPDATE/DELETE — xem {@link #checkCatalogueDocumentAlter}
 * và {@link #checkDocumentUpload}) không bắt buộc quyền module Document {@code 300xxx} riêng.
 *
 * <p><b>Lưu ý:</b> với "check any" / "check all" tổng quát thì gọi {@code PermissionService.requireAnyPermission}
 * trong method; không dùng SpEL vararg.
 */
@Component("perm")
@RequiredArgsConstructor
public class PermissionAuthorizationBean {

    private final PermissionService permissionService;
    private final DocumentRepository documentRepository;

    public boolean check(int code) {
        String username = currentUsername();
        if (username == null) return false;
        return permissionService.hasPermission(username, code);
    }

    /**
     * Upload kèm entity: nếu chưa gắn entity (chưa gửi type) thì vẫn cần {@link PermissionCode#CREATE_DOCUMENT}.
     * Khi gắn sản phẩm / variant / danh mục / hãng (và legacy type sản phẩm) thì {@link PermissionCode#CREATE_PRODUCT} đủ.
     */
    public boolean checkDocumentUpload(Integer entityType) {
        if (check(PermissionCode.CREATE_DOCUMENT)) {
            return true;
        }
        if (entityType == null) {
            return false;
        }
        return isCatalogueBoundEntity(entityType) && check(PermissionCode.CREATE_PRODUCT);
    }

    /**
     * Thao tác trên bản ghi document theo id: chấp nhận quyền doc ({@code docPermission}) hoặc quyền catalogue
     * ({@code productPermission}) nếu document gắn thực thể catalogue.
     */
    public boolean checkCatalogueDocumentAlter(int docPermission, int productPermission, Long documentId) {
        if (documentId == null) {
            return false;
        }
        if (check(docPermission)) {
            return true;
        }
        return documentRepository.findById(documentId)
                .map(DocumentEntity::getEntityType)
                .filter(this::isCatalogueBoundEntity)
                .isPresent()
                && check(productPermission);
    }

    private boolean isCatalogueBoundEntity(Integer entityType) {
        if (entityType == null) {
            return false;
        }
        int t = entityType;
        if (t == DocumentEntityType.ID_DOCUMENT_ENTITY_UNASSIGNED) {
            return false;
        }
        return t == DocumentEntityType.ID_DOCUMENT_ENTITY_PRODUCT
                || t == 1
                || t == DocumentEntityType.ID_DOCUMENT_ENTITY_PRODUCT_VARIANT
                || t == DocumentEntityType.ID_DOCUMENT_ENTITY_CATEGORY
                || t == DocumentEntityType.ID_DOCUMENT_ENTITY_BRAND;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String name = auth.getName();
        return (name == null || name.isBlank()) ? null : name;
    }
}
