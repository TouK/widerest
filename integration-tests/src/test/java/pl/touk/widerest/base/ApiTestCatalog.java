package pl.touk.widerest.base;

public interface ApiTestCatalog {
    long getTotalCategoriesCount();
    long getTotalProductsCount();

    long getTotalProductsInCategoryCount(final long categoryId);
}
