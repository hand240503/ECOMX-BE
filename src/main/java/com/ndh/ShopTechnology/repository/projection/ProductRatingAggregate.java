package com.ndh.ShopTechnology.repository.projection;

public interface ProductRatingAggregate {

  Long getProductId();

  Double getAverageRating();

  Long getRatingCount();
}
