package pl.touk.widerest.base;

import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.common.CatalogUtils;

import javax.annotation.Resource;

public class CatalogOperationsLocal implements CatalogOperations {

    protected CatalogService catalogService;

    public CatalogOperationsLocal(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public long getTotalCategoriesCount() {
        return catalogService.findAllCategories().stream()
                .filter(CatalogUtils.nonArchivedCategory)
                .count();
    }

    @Override
    public long getTotalProductsCount() {
        return catalogService.findAllProducts().stream()
                .filter(CatalogUtils.nonArchivedProduct)
                .count();
    }

    @Override
    public long getTotalSkusCount() {
        return catalogService.findAllSkus().stream().count();
    }

    @Override
    public long getTotalProductsInCategoryCount(final long categoryId) {
        return catalogService.findCategoryById(categoryId).getAllProductXrefs().stream()
                    .map(CategoryProductXref::getProduct)
                    .filter(CatalogUtils.nonArchivedProduct)
                    .count();
    }

    @Override
    public long getTotalCategoriesForProductCount(final long productId) {
        return catalogService.findProductById(productId).getAllParentCategoryXrefs().stream()
                    .map(CategoryProductXref::getCategory)
                    .filter(CatalogUtils.nonArchivedCategory)
                    .count();
    }

    @Override
    public long getTotalSkusForProductCount(final long productId) {
        return catalogService.findProductById(productId).getAllSkus().stream()
                .count();
    }


}
