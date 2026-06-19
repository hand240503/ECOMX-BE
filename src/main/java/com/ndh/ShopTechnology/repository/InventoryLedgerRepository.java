package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.inventory.InventoryLedgerEntity;
import com.ndh.ShopTechnology.enums.inventory.InventoryMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLedgerRepository extends JpaRepository<InventoryLedgerEntity, Long> {

    /** Dùng cho idempotency: kiểm tra một dòng đơn đã có bút toán loại này chưa. */
    boolean existsByOrderDetail_IdAndMovementType(Long orderDetailId, InventoryMovementType movementType);

    long countByOrderDetail_IdAndMovementType(Long orderDetailId, InventoryMovementType movementType);

    List<InventoryLedgerEntity> findByVariant_IdOrderByIdDesc(Long variantId);

    List<InventoryLedgerEntity> findByOrderDetail_IdOrderByIdAsc(Long orderDetailId);
}
