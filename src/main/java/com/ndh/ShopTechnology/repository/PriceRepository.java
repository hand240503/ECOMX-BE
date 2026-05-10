package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.PriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<PriceEntity, Long> {

    /** Liệt kê các entry giá của 1 sản phẩm theo id tăng dần (id nhỏ nhất là giá catalog mặc định). */
    List<PriceEntity> findByProduct_IdOrderByIdAsc(Long productId);

    long countByUnit_Id(Long unitId);
}
