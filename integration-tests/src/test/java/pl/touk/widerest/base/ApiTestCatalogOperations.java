package pl.touk.widerest.base;

import org.springframework.http.ResponseEntity;
import pl.touk.widerest.api.catalog.categories.dto.CategoryDto;
import pl.touk.widerest.api.catalog.products.dto.MediaDto;
import pl.touk.widerest.api.catalog.products.dto.ProductDto;
import pl.touk.widerest.api.catalog.products.dto.SkuDto;

public interface ApiTestCatalogOperations {
    ResponseEntity<?> addTestCategory(final CategoryDto categoryDto);
    ResponseEntity<?> addTestProduct(final ProductDto productDto);
    ResponseEntity<?> addTestSKUToProduct(final long productId, final SkuDto skuDto);
    ResponseEntity<?> addTestMediaToSku(final long productId, final long skuId, final String key, final MediaDto mediaDto);
    ResponseEntity<?> addCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId);
    ResponseEntity<?> addProductToCategoryReference(final long categoryId, final long productId);

    ResponseEntity<?> removeCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId);
    ResponseEntity<?> removeProductToCategoryReference(final long categoryId, final long productId);
}
