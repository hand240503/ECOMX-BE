package com.ndh.ShopTechnology.services.doc.impl;

import com.ndh.ShopTechnology.constants.DocumentEntityType;
import com.ndh.ShopTechnology.constants.DocumentKind;
import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.BrandRepository;
import com.ndh.ShopTechnology.repository.CategoryRepository;
import com.ndh.ShopTechnology.repository.DocumentRepository;
import com.ndh.ShopTechnology.repository.OrderRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.doc.DocumentService;
import com.ndh.ShopTechnology.services.storage.CloudinaryService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final OrderRepository orderRepository;
    private final CloudinaryService cloudinaryService;

    public DocumentServiceImpl(
            DocumentRepository documentRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            OrderRepository orderRepository,
            CloudinaryService cloudinaryService) {
        this.documentRepository = documentRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.orderRepository = orderRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    @Transactional
    public List<DocumentEntity> persistUploadedFiles(
            List<MultipartFile> files,
            Long entityId,
            Integer entityType,
            Integer mainFileIndex) throws IOException {
        if (entityId != null && entityType != null) {
            validateEntityBinding(entityId, entityType);
        }
        List<DocumentEntity> documentEntities = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            CloudinaryService.UploadResult uploaded = cloudinaryService.uploadDocument(file);
            String original = file.getOriginalFilename();
            String safeName = original != null ? StringUtils.cleanPath(original) : null;
            String displayName = (safeName == null || safeName.isBlank())
                    ? FilenameUtils.getName(uploaded.url())
                    : safeName;
            int kind = DocumentKind.resolve(file);
            DocumentEntity documentEntity = DocumentEntity.builder()
                    .fileName(displayName)
                    .fileSize(String.valueOf(file.getSize()))
                    .filePath(uploaded.url())
                    .cloudinaryPublicId(uploaded.publicId())
                    .type(kind)
                    .main(false)
                    .build();
            if (entityId != null && entityType != null) {
                documentEntity.setEntityId(entityId);
                documentEntity.setEntityType(entityType);
            }
            documentEntities.add(createDocument(documentEntity));
        }
        if (mainFileIndex != null
                && entityId != null
                && entityType != null
                && mainFileIndex >= 0
                && mainFileIndex < documentEntities.size()) {
            DocumentEntity chosen = documentEntities.get(mainFileIndex);
            if (chosen.getType() != DocumentKind.IMAGE) {
                throw new CustomApiException(
                        HttpStatus.BAD_REQUEST,
                        "mainFileIndex must point to an image (document.type="
                                + DocumentKind.IMAGE
                                + "); index "
                                + mainFileIndex
                                + " has type "
                                + chosen.getType());
            }
            documentRepository.clearMainFlagsForEntity(entityId, entityType);
            chosen.setMain(true);
            documentRepository.save(chosen);
        }
        return documentEntities;
    }

    private void validateEntityBinding(Long entityId, int entityType) {
        if (entityType == DocumentEntityType.ID_DOCUMENT_ENTITY_UNASSIGNED) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "entityType must not be ID_DOCUMENT_ENTITY_UNASSIGNED (-1) when entityId is set");
        }
        if (!DocumentEntityType.isRegistered(entityType) && entityType != 1) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Unknown entityType: " + entityType);
        }
        switch (entityType) {
            case DocumentEntityType.ID_DOCUMENT_ENTITY_PRODUCT, 1 -> {
                if (!productRepository.existsById(entityId)) {
                    throw new NotFoundEntityException("Product not found with id: " + entityId);
                }
            }
            case DocumentEntityType.ID_DOCUMENT_ENTITY_PRODUCT_VARIANT -> {
                if (!productVariantRepository.existsById(entityId)) {
                    throw new NotFoundEntityException("Product variant not found with id: " + entityId);
                }
            }
            case DocumentEntityType.ID_DOCUMENT_ENTITY_USER -> {
                if (!userRepository.existsById(entityId)) {
                    throw new NotFoundEntityException("User not found with id: " + entityId);
                }
            }
            case DocumentEntityType.ID_DOCUMENT_ENTITY_CATEGORY -> {
                if (!categoryRepository.existsById(entityId)) {
                    throw new NotFoundEntityException("Category not found with id: " + entityId);
                }
            }
            case DocumentEntityType.ID_DOCUMENT_ENTITY_BRAND -> {
                if (!brandRepository.existsById(entityId)) {
                    throw new NotFoundEntityException("Brand not found with id: " + entityId);
                }
            }
            case DocumentEntityType.ID_DOCUMENT_ENTITY_ORDER -> {
                if (!orderRepository.existsById(entityId)) {
                    throw new NotFoundEntityException("Order not found with id: " + entityId);
                }
            }
            default -> throw new CustomApiException(HttpStatus.BAD_REQUEST, "Unsupported entityType: " + entityType);
        }
    }

    @Override
    public DocumentEntity createDocument(DocumentEntity document) {
        if (document.getEntityId() == null) {
            document.setEntityId((long) DocumentEntityType.ID_DOCUMENT_ENTITY_UNASSIGNED);
        }
        if (document.getEntityType() == null) {
            document.setEntityType(DocumentEntityType.ID_DOCUMENT_ENTITY_UNASSIGNED);
        }
        return documentRepository.save(document);
    }

    @Override
    public DocumentEntity getDocumentById(Long id) {
        if (id == null) {
            return null;
        }
        return documentRepository.findById(id).orElse(null);
    }

    @Override
    public List<DocumentEntity> getAllDocuments() {
        return null;
    }

    @Override
    @Transactional
    public DocumentEntity updateDocument(Long id, Map<String, String> fields) {
        DocumentEntity doc = documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Document not found with id: " + id));
        if (fields == null) {
            return doc;
        }
        if (fields.containsKey("description")) {
            doc.setDescription(fields.get("description"));
        }
        if (fields.containsKey("fileDes")) {
            doc.setFileDes(fields.get("fileDes"));
        }
        return documentRepository.save(doc);
    }

    @Override
    @Transactional
    public DocumentEntity replaceDocumentFile(Long id, MultipartFile newFile) throws IOException {
        if (newFile == null || newFile.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "New file must not be empty");
        }
        DocumentEntity doc = documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Document not found with id: " + id));

        String oldPublicId = doc.getCloudinaryPublicId();

        CloudinaryService.UploadResult uploaded = cloudinaryService.uploadDocument(newFile);

        if (oldPublicId != null && !oldPublicId.isBlank()) {
            cloudinaryService.deleteByPublicId(oldPublicId);
        }

        String original = newFile.getOriginalFilename();
        String safeName = original != null ? StringUtils.cleanPath(original) : null;
        String displayName = (safeName == null || safeName.isBlank())
                ? FilenameUtils.getName(uploaded.url())
                : safeName;

        doc.setFilePath(uploaded.url());
        doc.setCloudinaryPublicId(uploaded.publicId());
        doc.setFileName(displayName);
        doc.setFileSize(String.valueOf(newFile.getSize()));
        doc.setType(DocumentKind.resolve(newFile));

        return documentRepository.save(doc);
    }

    @Override
    @Transactional
    public DocumentEntity setMainDocument(Long documentId) {
        if (documentId == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "documentId is required");
        }
        DocumentEntity doc =
                documentRepository.findById(documentId).orElseThrow(() -> new NotFoundEntityException(
                        "Document not found with id: " + documentId));

        Long entityId = doc.getEntityId();
        Integer entityTypeBoxed = doc.getEntityType();
        if (entityId == null || entityTypeBoxed == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Document is not bound to an entity");
        }
        long unassigned = DocumentEntityType.ID_DOCUMENT_ENTITY_UNASSIGNED;
        if (Objects.equals(entityId, unassigned) || Objects.equals(entityTypeBoxed, (int) unassigned)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Cannot set main on unassigned document");
        }

        int kind = doc.getType();
        if (kind != DocumentKind.IMAGE && kind != 0) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Only image documents can be main (expected type "
                            + DocumentKind.IMAGE
                            + " or legacy 0, got "
                            + kind
                            + ")");
        }

        int entityType = entityTypeBoxed;
        validateEntityBinding(entityId, entityType);
        documentRepository.clearMainFlagsForEntity(entityId, entityType);
        doc.setMain(true);
        return documentRepository.save(doc);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        if (id == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Document id is required");
        }
        DocumentEntity doc = documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Document not found with id: " + id));
        String pid = doc.getCloudinaryPublicId();
        if (pid != null && !pid.isBlank()) {
            cloudinaryService.deleteByPublicId(pid);
        }
        documentRepository.delete(doc);
    }
}
