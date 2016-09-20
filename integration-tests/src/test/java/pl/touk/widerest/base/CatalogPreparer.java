package pl.touk.widerest.base;

import javaslang.control.Try;
import pl.touk.widerest.api.categories.CategoryDto;

import java.util.function.Supplier;

public class CatalogPreparer {

    private CatalogOperationsLocal catalogOperationsLocal;
    private CatalogOperationsRemote catalogOperationsRemote;

    public CatalogPreparer(final CatalogOperationsLocal catalogOperationsLocal, final CatalogOperationsRemote catalogOperationsRemote) {
        this.catalogOperationsLocal = catalogOperationsLocal;
        this.catalogOperationsRemote = catalogOperationsRemote;
    }

    public void categoryReference(final Long categoryId, final Long subcategoryId) throws Throwable {
        catalogOperationsRemote.addCategoryToCategoryReference(categoryId, subcategoryId);
    }

    public void localCategoriesCount(final Try.CheckedConsumer<Long> consumer) throws Throwable {
        consumer.accept(catalogOperationsLocal.getTotalCategoriesCount());
    }

    public void localProductsCount(final Try.CheckedConsumer<Long> consumer) throws Throwable {
        consumer.accept(catalogOperationsLocal.getTotalProductsCount());
    }

    public void category(final Try.CheckedConsumer<Long> consumer) throws Throwable {
        consumer.accept(nextCategory.get());
    }

    public void category(final CategoryDto categoryDto, final Try.CheckedConsumer<Long> consumer) throws Throwable {
        consumer.accept(ApiTestUtils.getIdFromEntity(catalogOperationsRemote.addCategory(categoryDto)));
    }

    private Supplier<Long> nextCategory = () ->
            ApiTestUtils.getIdFromEntity(catalogOperationsRemote.addCategory(DtoTestFactory.categories().testCategoryDto()));
}
