package pl.touk.widerest.base;

import javaslang.control.Try;
import org.springframework.http.ResponseEntity;
import pl.touk.widerest.api.categories.CategoryDto;

import java.util.function.Supplier;

public class CategoryBehaviour extends ApiBaseBehaviour {

    private CatalogOperationsRemote catalogOperationsRemote;

    public CategoryBehaviour(final CatalogOperationsRemote catalogOperationsRemote) {
        this.catalogOperationsRemote = catalogOperationsRemote;
    }

    public void created(final CategoryDto categoryDto, Try.CheckedConsumer<ResponseEntity<?>>... thens) throws Throwable {
        when(() -> catalogOperationsRemote.addCategory(categoryDto), thens);
    }

    public void created(final Try.CheckedConsumer<Long>... thens) throws Throwable {
        when(nextCategory::get, thens);
    }

    public void retrieved(final long categoryId, Try.CheckedConsumer<CategoryDto>... thens) throws Throwable {
        when(() -> catalogOperationsRemote.getCategory(categoryId), thens);
    }

    public void deleted(final Long categoryId, final Try.CheckedConsumer<ResponseEntity<?>>... thens) throws Throwable {
        when(() -> catalogOperationsRemote.removeCategory(categoryId), thens);
    }

    public void modified(final Long categoryId, final CategoryDto categoryDto, final Try.CheckedConsumer<Void>... thens) throws Throwable {
        when(() -> {
            catalogOperationsRemote.modifyCategory(categoryId, categoryDto);
            return null;
        }, thens);
    }

    public void referenceCreated(final Long categoryId, final Long subcategoryId, final Try.CheckedConsumer<ResponseEntity<?>>... thens) throws Throwable {
        when(() -> catalogOperationsRemote.addCategoryToCategoryReference(categoryId, subcategoryId), thens);
    }

    public void referenceDeleted(final Long categoryId, final Long subcategoryId, final Try.CheckedConsumer<ResponseEntity<?>>... thens) throws Throwable {
        when(() -> catalogOperationsRemote.removeCategoryToCategoryReference(categoryId, subcategoryId), thens);
    }

    private Supplier<Long> nextCategory = () ->
            ApiTestUtils.getIdFromEntity(catalogOperationsRemote.addCategory(DtoTestFactory.categories().testCategoryDto()));
}
