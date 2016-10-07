package pl.touk.widerest.base;

import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class CategoryExpectations {

    @Resource
    private CatalogService catalogService;

    private CatalogOperationsLocal catalogOperationsLocal;

    @PostConstruct
    public void init() {
        this.catalogOperationsLocal = new CatalogOperationsLocal(catalogService);
    }

    public void isActive(final long categoryId) {
        assertThat(catalogService.findCategoryById(categoryId))
                .isNotNull()
                .hasFieldOrPropertyWithValue("active", true);
    }

    public void totalCountEquals(final long categoriesCount) {
        assertThat(catalogOperationsLocal.getTotalCategoriesCount()).isEqualTo(categoriesCount);
    }

    @Transactional
    public void doesNotContainProducts(final long categoryId) {
        containsProductsCount(categoryId, 0);
    }

    @Transactional
    public void containsProductsCount(final long categoryId, final long productsInCategoryCount) {
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(categoryId))
                .isEqualTo(productsInCategoryCount);
    }

    @Transactional
    public void doesNotHaveSubcategories(final long categoryId) {
        hasSubcategories(categoryId, 0);
    }

    @Transactional
    public void hasSubcategories(final long categoryId, final int subcategoriesCount) {
        assertThat(catalogService.findCategoryById(categoryId).getChildCategoryXrefs())
                .hasSize(subcategoriesCount);
    }
}
