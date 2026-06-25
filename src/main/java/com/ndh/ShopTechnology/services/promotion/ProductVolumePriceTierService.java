package com.ndh.ShopTechnology.services.promotion;

import com.ndh.ShopTechnology.dto.request.promotion.VolumePriceTierItemRequest;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;

import java.util.List;

public interface ProductVolumePriceTierService {

    List<VolumePriceTierResponse> listByVariant(Long productId, Long variantId);

    /** Tất cả bậc giá của mọi biến thể thuộc một sản phẩm. */
    List<VolumePriceTierResponse> listByProduct(Long productId);

    /** Tất cả bậc giá (mọi sản phẩm/biến thể) cho trang tổng quan. */
    List<VolumePriceTierResponse> listAll();

    List<VolumePriceTierResponse> replaceTiers(Long productId, Long variantId, List<VolumePriceTierItemRequest> tiers);
}
