package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.doc.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

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
}
