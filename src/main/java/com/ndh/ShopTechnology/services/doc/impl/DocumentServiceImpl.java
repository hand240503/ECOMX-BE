package com.ndh.ShopTechnology.services.doc.impl;

import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import com.ndh.ShopTechnology.repository.DocumentRepository;
import com.ndh.ShopTechnology.services.doc.DocumentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;

    public DocumentServiceImpl(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public DocumentEntity createDocument(DocumentEntity document) {
        document.setEntityId(-1L);
        document.setEntityType(-1);
        return documentRepository.save(document);
    }

    @Override
    public DocumentEntity getDocumentById(Long id) {
        return null;
    }

    @Override
    public List<DocumentEntity> getAllDocuments() {
        return null;
    }

    @Override
    public DocumentEntity updateDocument(Long id, DocumentEntity document) {
        return null;
    }

    @Override
    public void deleteDocument(Long id) {

    }
}
