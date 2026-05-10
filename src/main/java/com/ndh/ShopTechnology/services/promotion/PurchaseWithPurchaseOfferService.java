package com.ndh.ShopTechnology.services.promotion;

import com.ndh.ShopTechnology.dto.request.promotion.UpsertPurchaseWithPurchaseRequest;
import com.ndh.ShopTechnology.dto.response.promotion.PurchaseWithPurchaseOfferResponse;

import java.util.List;

public interface PurchaseWithPurchaseOfferService {

    List<PurchaseWithPurchaseOfferResponse> listAll();

    PurchaseWithPurchaseOfferResponse create(UpsertPurchaseWithPurchaseRequest request);

    PurchaseWithPurchaseOfferResponse update(long id, UpsertPurchaseWithPurchaseRequest request);

    void delete(long id);
}
