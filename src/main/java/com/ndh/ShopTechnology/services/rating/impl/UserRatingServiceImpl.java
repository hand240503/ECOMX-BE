package com.ndh.ShopTechnology.services.rating.impl;

import com.ndh.ShopTechnology.dto.request.rating.CreateUserRatingRequest;
import com.ndh.ShopTechnology.dto.request.rating.UpdateUserRatingRequest;
import com.ndh.ShopTechnology.dto.response.rating.UserRatingResponse;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.rating.UserRatingEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.UserRatingRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.rating.UserRatingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserRatingServiceImpl implements UserRatingService {

    private final UserRatingRepository userRatingRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public UserRatingServiceImpl(UserRatingRepository userRatingRepository,
                                 UserRepository userRepository,
                                 ProductRepository productRepository) {
        this.userRatingRepository = userRatingRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public UserRatingResponse createRating(CreateUserRatingRequest request) {
        // Check if rating already exists
        userRatingRepository.findByUserIdAndProductId(request.getUserId(), request.getProductId())
                .ifPresent(rating -> {
                    throw new IllegalArgumentException("User has already rated this product. Use update instead.");
                });

        // Verify user exists
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundEntityException("User not found with id: " + request.getUserId()));

        // Verify product exists
        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundEntityException("Product not found with id: " + request.getProductId()));

        UserRatingEntity rating = UserRatingEntity.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        rating = userRatingRepository.save(rating);
        return UserRatingResponse.fromEntity(rating);
    }

    @Override
    public UserRatingResponse getRatingById(Long id) {
        UserRatingEntity rating = userRatingRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Rating not found with id: " + id));
        return UserRatingResponse.fromEntity(rating);
    }

    @Override
    public UserRatingResponse getRatingByUserAndProduct(Long userId, Long productId) {
        UserRatingEntity rating = userRatingRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new NotFoundEntityException(
                        "Rating not found for user id: " + userId + " and product id: " + productId));
        return UserRatingResponse.fromEntity(rating);
    }

    @Override
    public List<UserRatingResponse> getRatingsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundEntityException("User not found with id: " + userId);
        }
        List<UserRatingEntity> ratings = userRatingRepository.findByUserId(userId);
        return ratings.stream()
                .map(UserRatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRatingResponse> getRatingsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundEntityException("Product not found with id: " + productId);
        }
        List<UserRatingEntity> ratings = userRatingRepository.findByProductId(productId);
        return ratings.stream()
                .map(UserRatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRatingResponse> getAllRatings() {
        List<UserRatingEntity> ratings = userRatingRepository.findAll();
        return ratings.stream()
                .map(UserRatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserRatingResponse updateRating(Long id, UpdateUserRatingRequest request) {
        UserRatingEntity rating = userRatingRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Rating not found with id: " + id));

        if (request.getRating() != null) {
            rating.setRating(request.getRating());
        }

        if (request.getComment() != null) {
            rating.setComment(request.getComment());
        }

        rating = userRatingRepository.save(rating);
        return UserRatingResponse.fromEntity(rating);
    }

    @Override
    @Transactional
    public void deleteRating(Long id) {
        UserRatingEntity rating = userRatingRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Rating not found with id: " + id));
        userRatingRepository.delete(rating);
    }

    @Override
    public Double getAverageRatingByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundEntityException("Product not found with id: " + productId);
        }
        Double average = userRatingRepository.getAverageRatingByProductId(productId);
        return average != null ? average : 0.0;
    }

    @Override
    public Long getRatingCountByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundEntityException("Product not found with id: " + productId);
        }
        return userRatingRepository.countByProductId(productId);
    }
}
