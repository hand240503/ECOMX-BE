package com.ndh.ShopTechnology.controller.document;

import com.ndh.ShopTechnology.config.UploadStorageProperties;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import com.ndh.ShopTechnology.services.doc.DocumentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/document")
public class DocumentController {

    private final DocumentService documentService;
    private final UploadStorageProperties uploadStorageProperties;

    public DocumentController(DocumentService documentService, UploadStorageProperties uploadStorageProperties) {
        this.documentService = documentService;
        this.uploadStorageProperties = uploadStorageProperties;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.checkDocumentUpload(#entityType)")
    public ResponseEntity<APIResponse<List<DocumentEntity>>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "entityId", required = false) Long entityId,
            @RequestParam(value = "entityType", required = false) Integer entityType,
            @RequestParam(value = "mainFileIndex", required = false) Integer mainFileIndex
    ) throws Exception {

        // Validate files
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
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }

        // Check for empty files
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
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(response);
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

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * Tải file đã lưu — {@code filename} là phần sau {@code /uploads/}, ví dụ {@code 260510/uuid_....jpg}.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        try {
            Path uploadRoot = uploadStorageProperties.getResolvedRoot();
            Path filePath = uploadRoot.resolve(filename).normalize();
            if (!filePath.startsWith(uploadRoot)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            File file = filePath.toFile();

            if (!file.exists()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            // Đọc nội dung file
            byte[] fileContent = Files.readAllBytes(filePath);

            // Xác định loại nội dung
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileContent);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}