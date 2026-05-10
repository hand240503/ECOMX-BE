package com.ndh.ShopTechnology.services.promotion;

import com.ndh.ShopTechnology.dto.request.promotion.VolumePriceTierItemRequest;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;

import java.util.List;

public interface ProductVolumePriceTierService {

    List<VolumePriceTierResponse> listByProductId(Long productId);

    List<VolumePriceTierResponse> replaceTiers(Long productId, List<VolumePriceTierItemRequest> tiers);
}
