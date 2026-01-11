package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
}
