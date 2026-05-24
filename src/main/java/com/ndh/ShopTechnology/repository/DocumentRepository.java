package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE document d SET d.main = false WHERE d.entityId = :entityId AND d.entityType = :entityType")
    int clearMainFlagsForEntity(@Param("entityId") Long entityId, @Param("entityType") Integer entityType);

    @Query(
            """
                    SELECT d FROM document d
                    WHERE d.entityId IN :entityIds AND d.entityType IN :entityTypes
                    ORDER BY d.entityId ASC, d.id ASC
                    """)
    List<DocumentEntity> findForProductDocuments(
            @Param("entityIds") Collection<Long> entityIds, @Param("entityTypes") Collection<Integer> entityTypes);

    /**
     * Lấy ảnh đại diện (is_main = true, type = IMAGE) của một entity.
     * Dùng cho category thumbnail, brand logo, v.v.
     */
    @Query("""
            SELECT d FROM document d
            WHERE d.entityId = :entityId AND d.entityType = :entityType AND d.main = true
            ORDER BY d.id ASC
            """)
    Optional<DocumentEntity> findMainByEntityIdAndEntityType(
            @Param("entityId") Long entityId, @Param("entityType") Integer entityType);

    /**
     * Lấy tất cả ảnh (type = IMAGE) của một entity, ảnh main lên trước.
     */
    @Query("""
            SELECT d FROM document d
            WHERE d.entityId = :entityId AND d.entityType = :entityType
            ORDER BY d.main DESC, d.id ASC
            """)
    List<DocumentEntity> findAllByEntityIdAndEntityType(
            @Param("entityId") Long entityId, @Param("entityType") Integer entityType);
}
