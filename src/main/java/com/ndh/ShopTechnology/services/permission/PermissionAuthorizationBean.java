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

    public boolean checkDocumentUpload(Integer entityType) {
        if (check(PermissionCode.CREATE_DOCUMENT)) {
            return true;
        }
        if (entityType == null) {
            return false;
        }
        return isCatalogueBoundEntity(entityType) && check(PermissionCode.CREATE_PRODUCT);
    }

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
