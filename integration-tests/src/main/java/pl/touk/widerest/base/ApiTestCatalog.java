package pl.touk.widerest.base;

public interface ApiTestCatalog {
    long getTotalCategoriesCount();
    long getTotalProductsCount();
    long getTotalSkusCount();

    long getTotalProductsInCategoryCount(final long categoryId);
    long getTotalCategoriesForProductCount(final long productId);
    long getTotalSkusForProductCount(final long productId);
}
