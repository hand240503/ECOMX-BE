package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.controller.document.DocumentUploadRequestChecks;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import com.ndh.ShopTechnology.services.doc.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;

import java.util.List;
import java.util.Map;

/**
 * Upload tài liệu / media cho luồng Admin — {@code {api.prefix}/admin/document/upload}.
 * Cùng xử lý lưu file với {@link com.ndh.ShopTechnology.controller.document.DocumentController} nhưng chỉ
 * user có quyền vào {@code /admin/**} mới gọi được.
 */
@RestController
@RequestMapping("${api.prefix}/admin/document")
public class AdminDocumentController {

    private final DocumentService documentService;

    public AdminDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(300001)")
    public ResponseEntity<APIResponse<List<DocumentEntity>>> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "entityId", required = false) Long entityId,
            @RequestParam(value = "entityType", required = false) Integer entityType,
            @RequestParam(value = "mainFileIndex", required = false) Integer mainFileIndex
    ) throws Exception {

        if (files == null || files.isEmpty()) {
            APIResponse<List<DocumentEntity>> response = APIResponse.of(
                    false,
                    "No files uploaded",
                    null,
                    List.of(ErrorResponse.builder()
                            .field("files")
                            .message("Files list is empty")
                            .build()),
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                APIResponse<List<DocumentEntity>> response = APIResponse.of(
                        false,
                        "One or more files are empty",
                        null,
                        List.of(ErrorResponse.builder()
                                .field("files")
                                .message("Empty file detected: " + file.getOriginalFilename())
                                .build()),
                        null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }

        if (DocumentUploadRequestChecks.isIncompleteEntityPair(entityId, entityType)) {
            APIResponse<List<DocumentEntity>> response = APIResponse.of(
                    false,
                    "entityId and entityType must be sent together",
                    null,
                    List.of(ErrorResponse.builder()
                            .field("entityId")
                            .message("Provide both entityId and entityType, or omit both")
                            .build()),
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        List<DocumentEntity> documentEntities =
                documentService.persistUploadedFiles(files, entityId, entityType, mainFileIndex);

        APIResponse<List<DocumentEntity>> response = APIResponse.of(
                true,
                "Files uploaded successfully",
                documentEntities,
                null,
                null
        ).withMetadata("totalFiles", documentEntities.size());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Xóa document (xóa asset Cloudinary + bản ghi DB).
     * Quyền: DELETE_DOCUMENT (300004).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check(300004)")
    public ResponseEntity<APIResponse<Void>> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.ok(APIResponse.of(true, "Document deleted successfully", null, null, null));
        } catch (NotFoundEntityException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.of(
                    false, e.getMessage(), null,
                    List.of(ErrorResponse.builder().field("id").message(e.getMessage()).build()),
                    null));
        } catch (CustomApiException e) {
            return ResponseEntity.status(e.getStatus()).body(APIResponse.of(
                    false, e.getMessage(), null,
                    List.of(ErrorResponse.builder().field("id").message(e.getMessage()).build()),
                    null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.of(
                    false, "Failed to delete document: " + e.getMessage(), null,
                    List.of(ErrorResponse.builder().field("id").message(e.getMessage()).build()),
                    null));
        }
    }

    /**
     * Thay thế file ảnh của document: xóa asset cũ trên Cloudinary, upload file mới.
     * Các trường metadata (entityId, entityType, isMain, description…) giữ nguyên.
     * Quyền: UPDATE_DOCUMENT (300003).
     */
    @PutMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.check(300003)")
    public ResponseEntity<APIResponse<DocumentEntity>> replaceDocumentFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.of(
                        false, "File must not be empty", null,
                        List.of(ErrorResponse.builder().field("file").message("File is required").build()),
                        null));
            }
            DocumentEntity updated = documentService.replaceDocumentFile(id, file);
            return ResponseEntity.ok(APIResponse.of(true, "Document file replaced successfully", updated, null, null));
        } catch (NotFoundEntityException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.of(
                    false, e.getMessage(), null,
                    List.of(ErrorResponse.builder().field("id").message(e.getMessage()).build()),
                    null));
        } catch (CustomApiException e) {
            return ResponseEntity.status(e.getStatus()).body(APIResponse.of(
                    false, e.getMessage(), null,
                    List.of(ErrorResponse.builder().field("file").message(e.getMessage()).build()),
                    null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.of(
                    false, "Failed to replace document file: " + e.getMessage(), null,
                    List.of(ErrorResponse.builder().field("file").message(e.getMessage()).build()),
                    null));
        }
    }

    /**
     * Cập nhật metadata (description, fileDes) của document — không thay file thực tế.
     * Quyền: UPDATE_DOCUMENT (300003).
     */
    @PatchMapping("/{id}")
    @PreAuthorize("@perm.check(300003)")
    public ResponseEntity<APIResponse<DocumentEntity>> updateDocumentMeta(
            @PathVariable Long id,
            @RequestBody Map<String, String> fields) {
        try {
            DocumentEntity updated = documentService.updateDocument(id, fields);
            return ResponseEntity.ok(APIResponse.of(true, "Document updated successfully", updated, null, null));
        } catch (NotFoundEntityException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.of(
                    false, e.getMessage(), null,
                    List.of(ErrorResponse.builder().field("id").message(e.getMessage()).build()),
                    null));
        } catch (CustomApiException e) {
            return ResponseEntity.status(e.getStatus()).body(APIResponse.of(
                    false, e.getMessage(), null,
                    List.of(ErrorResponse.builder().field("id").message(e.getMessage()).build()),
                    null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.of(
                    false, "Failed to update document: " + e.getMessage(), null,
                    List.of(ErrorResponse.builder().field("id").message(e.getMessage()).build()),
                    null));
        }
    }

    /**
     * Đặt document đã tồn tại làm ảnh đại diện (main) trong cùng entity — không upload file mới.
     */
    @PostMapping("/{id}/main")
    @PreAuthorize("@perm.check(300001)")
    public ResponseEntity<APIResponse<DocumentEntity>> setMainDocument(@PathVariable Long id) {
        try {
            DocumentEntity updated = documentService.setMainDocument(id);
            APIResponse<DocumentEntity> response = APIResponse.of(
                    true,
                    "Main document updated",
                    updated,
                    null,
                    null);
            return ResponseEntity.ok(response);
        } catch (NotFoundEntityException e) {
            APIResponse<DocumentEntity> response = APIResponse.of(
                    false,
                    e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("id")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (CustomApiException e) {
            APIResponse<DocumentEntity> response = APIResponse.of(
                    false,
                    e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("document")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(e.getStatus()).body(response);
        } catch (Exception e) {
            APIResponse<DocumentEntity> response = APIResponse.of(
                    false,
                    "Failed to set main document: " + e.getMessage(),
                    null,
                    List.of(ErrorResponse.builder()
                            .field("document")
                            .message(e.getMessage())
                            .build()),
                    null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
