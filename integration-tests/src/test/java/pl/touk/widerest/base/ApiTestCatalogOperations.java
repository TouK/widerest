package pl.touk.widerest.base;

import org.springframework.http.ResponseEntity;
import pl.touk.widerest.api.catalog.categories.dto.CategoryDto;
import pl.touk.widerest.api.catalog.products.dto.ProductDto;

public interface ApiTestCatalogOperations {
    ResponseEntity<?> addTestCategory(final CategoryDto categoryDto);
    ResponseEntity<?> addTestProduct(final ProductDto productDto);
    ResponseEntity<?> addCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId);
    ResponseEntity<?> addProductToCategoryReference(final long categoryId, final long productId);

    void removeCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId);
    void removeProductToCategoryReference(final long categoryId, final long productId);
}
