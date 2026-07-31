package com.king_sparkon_tracker.backend.specification;

import java.util.ArrayList;
import java.util.List;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> filter(
        Long businessId,
        ProductCategory category,
        ProductStatus status,
        String search) {

    return Specification.where(hasBusiness(businessId))
            .and(hasCategory(category))
            .and(hasStatus(status))
            .and(search(search));
}

    public static Specification<Product> hasBusiness(Long businessId) {
        return (root, query, cb) ->
                cb.equal(root.get("business").get("id"), businessId);
    }

    public static Specification<Product> hasCategory(ProductCategory category) {
        return (root, query, cb) ->
                category == null
                        ? cb.conjunction()
                        : cb.equal(root.get("category"), category);
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> search(String search) {

        return (root, query, cb) -> {

            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }

            query.distinct(true);

            String value = "%" + search.toLowerCase().trim() + "%";

            // ⭐ THIS IS WHERE IT GOES
            Join<Product, ProductBarcode> barcode =
                    root.join("barcodes", JoinType.LEFT);

            Predicate searchPredicate = cb.or(

                    cb.like(cb.lower(root.get("name")), value),

                    cb.like(cb.lower(root.get("productBarcode")), value),

                    cb.like(cb.lower(barcode.get("barcode")), value),

                    cb.like(cb.lower(barcode.get("unitCode")), value)

            );

            return searchPredicate;
        };
    }
}
