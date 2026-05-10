package com.ndh.ShopTechnology.services.promotion.impl;

import com.ndh.ShopTechnology.dto.request.promotion.VolumePriceTierItemRequest;
import com.ndh.ShopTechnology.dto.response.promotion.VolumePriceTierResponse;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.promotion.ProductVolumePriceTierEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.ProductVolumePriceTierRepository;
import com.ndh.ShopTechnology.services.promotion.ProductVolumePriceTierService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductVolumePriceTierServiceImpl implements ProductVolumePriceTierService {

    private final ProductVolumePriceTierRepository tierRepository;
    private final ProductRepository productRepository;

    public ProductVolumePriceTierServiceImpl(
            ProductVolumePriceTierRepository tierRepository,
            ProductRepository productRepository) {
        this.tierRepository = tierRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolumePriceTierResponse> listByProductId(Long productId) {
        ensureProduct(productId);
        return tierRepository.findByProduct_IdOrderByMinQuantityAsc(productId).stream()
                .map(ProductVolumePriceTierServiceImpl::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<VolumePriceTierResponse> replaceTiers(Long productId, List<VolumePriceTierItemRequest> tiers) {
        ProductEntity product = ensureProduct(productId);
        if (tiers == null) {
            tiers = List.of();
        }
        Set<Integer> seen = new HashSet<>();
        for (VolumePriceTierItemRequest t : tiers) {
            if (!seen.add(t.getMinQuantity())) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Duplicate minQuantity in tier list: " + t.getMinQuantity());
            }
        }
        List<ProductVolumePriceTierEntity> existing = tierRepository.findByProduct_IdOrderByMinQuantityAsc(productId);
        tierRepository.deleteAll(existing);

        List<ProductVolumePriceTierEntity> saved = new ArrayList<>();
        for (VolumePriceTierItemRequest t : tiers) {
            ProductVolumePriceTierEntity e = ProductVolumePriceTierEntity.builder()
                    .product(product)
                    .minQuantity(t.getMinQuantity())
                    .unitPrice(t.getUnitPrice())
                    .enabled(t.getEnabled() != null ? t.getEnabled() : true)
                    .build();
            saved.add(tierRepository.save(e));
        }
        return saved.stream().map(ProductVolumePriceTierServiceImpl::toResponse).collect(Collectors.toList());
    }

    private ProductEntity ensureProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + productId));
    }

    private static VolumePriceTierResponse toResponse(ProductVolumePriceTierEntity e) {
        return VolumePriceTierResponse.builder()
                .id(e.getId())
                .productId(e.getProduct().getId())
                .minQuantity(e.getMinQuantity())
                .unitPrice(e.getUnitPrice())
                .enabled(e.getEnabled())
                .build();
    }
}
