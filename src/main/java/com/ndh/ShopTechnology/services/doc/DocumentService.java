package com.ndh.ShopTechnology.services.doc;

import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import org.springframework.stereotype.Service;

import java.util.List;

public interface DocumentService {

    DocumentEntity createDocument(DocumentEntity document);

    DocumentEntity getDocumentById(Long id);

    List<DocumentEntity> getAllDocuments();

    DocumentEntity updateDocument(Long id, DocumentEntity document);

    void deleteDocument(Long id);
}
