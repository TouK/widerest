package pl.touk.widerest.base;

import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.skus.SkuDto;

public interface CatalogOperations {

    default long getTotalCategoriesCount() { throw new UnsupportedOperationException(); };
    default long getTotalProductsCount() { throw new UnsupportedOperationException(); };
    default long getTotalSkusCount() { throw new UnsupportedOperationException(); };

    default long getTotalProductsInCategoryCount(final long categoryId) { throw new UnsupportedOperationException(); };
    default long getTotalCategoriesForProductCount(final long productId) { throw new UnsupportedOperationException(); };
    default long getTotalSkusForProductCount(final long productId) { throw new UnsupportedOperationException(); };

    default ResponseEntity<?> addCategory(final CategoryDto categoryDto) { throw new UnsupportedOperationException(); };
    default void modifyCategory(final long categoryId, final CategoryDto categoryDto) { throw new UnsupportedOperationException(); };
    default ResponseEntity<?> removeCategory(final long categoryId) { throw new UnsupportedOperationException(); };
    default CategoryDto getCategory(final long categoryId) { throw new UnsupportedOperationException(); }
    default Resources<CategoryDto> getSubcategories(final long categoryId) { throw new UnsupportedOperationException(); }

    default ResponseEntity<?> addProduct(final ProductDto productDto) { throw new UnsupportedOperationException(); };
    default ResponseEntity<?> addTestSKUToProduct(final long productId, final SkuDto skuDto) { throw new UnsupportedOperationException(); };
    default ResponseEntity<?> addTestMediaToSku(final long productId, final long skuId, final String key, final MediaDto mediaDto) { throw new UnsupportedOperationException(); };
    default ResponseEntity<?> addCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId) { throw new UnsupportedOperationException(); };
    default ResponseEntity<?> addProductToCategoryReference(final long categoryId, final long productId) { throw new UnsupportedOperationException(); };

    default ResponseEntity<?> removeCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId) { throw new UnsupportedOperationException(); };
    default ResponseEntity<?> removeProductToCategoryReference(final long categoryId, final long productId) { throw new UnsupportedOperationException(); };
}
