public final class ProductSpecification {

    public static Specification<Product> hasBusiness(Long businessId)

    public static Specification<Product> hasCategory(ProductCategory category)

    public static Specification<Product> hasStatus(ProductStatus status)

    public static Specification<Product> search(String search)

}
