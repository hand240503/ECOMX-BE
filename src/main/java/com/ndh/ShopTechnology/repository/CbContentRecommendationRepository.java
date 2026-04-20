package com.ndh.ShopTechnology.repository;


import com.ndh.ShopTechnology.entities.recommendation.CbContentRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CbContentRecommendationRepository
        extends JpaRepository<CbContentRecommendation, Long> {

    Optional<CbContentRecommendation> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
