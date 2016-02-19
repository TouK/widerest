package pl.touk.widerest.base;

import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.catalog.CatalogUtils;

import javax.annotation.Resource;

@Component
public class ApiTestCatalogLocal implements ApiTestCatalog {

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @Override
    public long getTotalCategoriesCount() {
        return catalogService.findAllCategories().stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();
    }

    @Override
    public long getTotalProductsCount() {
        return catalogService.findAllProducts().stream()
                .filter(CatalogUtils::archivedProductFilter)
                .count();
    }

    @Override
    public long getTotalProductsInCategoryCount(final long categoryId) {
        return catalogService.findCategoryById(categoryId).getAllProductXrefs().stream()
                    .map(CategoryProductXref::getProduct)
                    .filter(CatalogUtils::archivedProductFilter)
                    .count();
    }


}
