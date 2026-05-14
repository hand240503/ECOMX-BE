package com.ndh.ShopTechnology.services.doc;

import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DocumentService {

    DocumentEntity createDocument(DocumentEntity document);

    /**
     * Upload từng file lên Cloudinary rồi {@link #createDocument} — dùng chung cho
     * {@code /document/upload} và {@code /admin/document/upload}. Caller chịu trách nhiệm validate danh sách.
     *
     * @param entityId nếu cùng lúc có {@code entityType} (không null) thì gắn vào mỗi bản ghi; cả hai null →
     *                 giữ hành vi cũ ({@code -1} / {@code -1})
     * @param entityType xem {@link com.ndh.ShopTechnology.constants.DocumentEntityType}
     * @param mainFileIndex chỉ số (0-based) trong {@code files} được đánh dấu là ảnh đại diện; cần kèm
     *                    {@code entityId} + {@code entityType}; {@code null} → không đổi cờ main của batch này
     */
    List<DocumentEntity> persistUploadedFiles(
            List<MultipartFile> files,
            Long entityId,
            Integer entityType,
            Integer mainFileIndex) throws IOException;

    default List<DocumentEntity> persistUploadedFiles(
            List<MultipartFile> files,
            Long entityId,
            Integer entityType) throws IOException {
        return persistUploadedFiles(files, entityId, entityType, null);
    }

    default List<DocumentEntity> persistUploadedFiles(List<MultipartFile> files) throws IOException {
        return persistUploadedFiles(files, null, null, null);
    }

    DocumentEntity getDocumentById(Long id);

    List<DocumentEntity> getAllDocuments();

    /**
     * Cập nhật metadata (description, fileDes) của document — không thay file thực tế.
     */
    DocumentEntity updateDocument(Long id, Map<String, String> fields);

    /**
     * Thay thế file thực tế của document: xóa asset cũ trên Cloudinary, upload file mới,
     * cập nhật {@code filePath}, {@code cloudinaryPublicId}, {@code fileName}, {@code fileSize}, {@code type}.
     * Không thay đổi {@code entityId}, {@code entityType}, {@code isMain}.
     */
    DocumentEntity replaceDocumentFile(Long id, MultipartFile newFile) throws IOException;

    /**
     * Xóa document: xóa asset trên Cloudinary (nếu có) rồi xóa bản ghi DB.
     * Nếu document đang là {@code isMain}, cờ main của entity sẽ không có ảnh nào (caller tự xử lý nếu cần).
     *
     * @throws com.ndh.ShopTechnology.exception.NotFoundEntityException nếu không tìm thấy
     */
    void deleteDocument(Long id);

    /**
     * Đặt document làm ảnh đại diện (main) cho đúng cặp {@code entity_id}/{@code entity_type} của nó:
     * gỡ cờ main khắp entity rồi bật cho bản ghi này. Chỉ áp dụng khi {@link DocumentEntity#getType()}
     * là ảnh ({@link com.ndh.ShopTechnology.constants.DocumentKind#IMAGE} hoặc legacy {@code 0}).
     */
    DocumentEntity setMainDocument(Long documentId);
}
