package com.ndh.ShopTechnology.repository.spec;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Search: each whitespace-separated token must appear in product name, description, or tag (OR per
 * field, AND across tokens). So "Smart Phone" matches "Smartphone" (both "smart" and "phone" occur).
 */
public final class ProductSearchSpecifications {

  private ProductSearchSpecifications() {}

  public static Specification<ProductEntity> isActiveOrNullStatus(int activeStatus) {
    return (root, query, cb) -> cb.or(
        cb.isNull(root.get("status")),
        cb.equal(root.get("status"), activeStatus));
  }

  public static Specification<ProductEntity> allTokensMatchNameDescriptionTag(List<String> tokensLowercase) {
    if (tokensLowercase == null || tokensLowercase.isEmpty()) {
      return (root, query, cb) -> cb.disjunction();
    }
    return (root, query, cb) -> {
      List<Predicate> tokenAnd = new ArrayList<>();
      for (String token : tokensLowercase) {
        if (token == null || token.isEmpty()) {
          continue;
        }
        String pattern = "%" + token + "%";
        var name = cb.lower(cb.coalesce(root.get("productName"), ""));
        var desc = cb.lower(cb.coalesce(root.get("description"), ""));
        var tag = cb.lower(cb.coalesce(root.get("tag"), ""));
        tokenAnd.add(cb.or(
            cb.like(name, pattern),
            cb.like(desc, pattern),
            cb.like(tag, pattern)));
      }
      if (tokenAnd.isEmpty()) {
        return cb.disjunction();
      }
      return cb.and(tokenAnd.toArray(Predicate[]::new));
    };
  }
}
