package pl.touk.widerest.base;

import org.springframework.http.ResponseEntity;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.skus.SkuDto;

public interface ApiTestCatalogOperations {
    ResponseEntity<?> addTestCategory(final CategoryDto categoryDto);
    ResponseEntity<?> addTestProduct(final ProductDto productDto);
    ResponseEntity<?> addTestSKUToProduct(final long productId, final SkuDto skuDto);
    ResponseEntity<?> addTestMediaToSku(final long productId, final long skuId, final String key, final MediaDto mediaDto);
    ResponseEntity<?> addCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId);
    ResponseEntity<?> addProductToCategoryReference(final long categoryId, final long productId);




    ResponseEntity<?> removeTestCategory(final long categoryId);
    ResponseEntity<?> removeCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId);
    ResponseEntity<?> removeProductToCategoryReference(final long categoryId, final long productId);
}
