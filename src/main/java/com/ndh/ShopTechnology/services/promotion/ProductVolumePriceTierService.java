package com.ndh.ShopTechnology.services.promotion;

import com.ndh.ShopTechnology.dto.request.promotion.VolumePriceTierItemRequest;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;

import java.util.List;

public interface ProductVolumePriceTierService {

    List<VolumePriceTierResponse> listByVariant(Long productId, Long variantId);

    List<VolumePriceTierResponse> replaceTiers(Long productId, Long variantId, List<VolumePriceTierItemRequest> tiers);
}
