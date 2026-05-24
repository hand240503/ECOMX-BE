package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetailEntity, Long> {

    @EntityGraph(attributePaths = "product")
    List<OrderDetailEntity> findByOrder_IdOrderByIdAsc(Long orderId);

    /**
     * Kiểm tra user đã mua sản phẩm thành công chưa (đơn hàng ở trạng thái {@code status}).
     * Dùng để xác thực quyền comment/đánh giá sản phẩm.
     *
     * @param userId    ID của user cần kiểm tra
     * @param productId ID sản phẩm cần kiểm tra
     * @param status    Trạng thái đơn hàng (thường là {@link com.ndh.ShopTechnology.constants.OrderConstants#STATUS_COMPLETED})
     * @return {@code true} nếu user có ít nhất 1 đơn hàng ở trạng thái {@code status} chứa sản phẩm {@code productId}
     */
    @Query("SELECT COUNT(od) > 0 FROM OrderDetail od " +
           "WHERE od.order.user.id = :userId " +
           "AND od.product.id = :productId " +
           "AND od.order.status = :status")
    boolean existsByOrderUserIdAndProductIdAndOrderStatus(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("status") Integer status);
}
