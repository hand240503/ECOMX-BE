package com.ndh.ShopTechnology.services.doc;

import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DocumentService {

    DocumentEntity createDocument(DocumentEntity document);

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

    DocumentEntity updateDocument(Long id, Map<String, String> fields);

    DocumentEntity replaceDocumentFile(Long id, MultipartFile newFile) throws IOException;

    void deleteDocument(Long id);

    DocumentEntity setMainDocument(Long documentId);
}
