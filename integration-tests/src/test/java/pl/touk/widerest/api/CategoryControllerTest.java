package pl.touk.widerest.api;

import com.google.common.collect.ImmutableMap;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.base.ApiTestUtils;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.security.oauth2.Scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static pl.touk.widerest.base.DtoTestFactory.categories;
import static pl.touk.widerest.base.DtoTestFactory.products;

@RunWith(SpringJUnit4ClassRunner.class)
public class CategoryControllerTest extends AbstractTest {

    @After
    public void tearDown() throws Exception {
        removeLocalTestCategories();
    }

    @Test
    public void shouldSuccessfullyCreateNewCategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                whenCategory.created(categories().testCategoryDto(), createdCategoryResponseEntity -> {
                    thenHttpStatusReturned(createdCategoryResponseEntity, HttpStatus.CREATED);
                    thenCategory.isActive(ApiTestUtils.getIdFromEntity(createdCategoryResponseEntity));
                })
        );
    }

    @Test
    public void shouldSuccessfullyDeleteCategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                givenCatalog.category(categoryId ->
                        whenCategory.deleted(categoryId, deletedCategoryResponseEntity -> {
                        thenHttpStatusReturned(deletedCategoryResponseEntity, HttpStatus.NO_CONTENT);
                        assertThatExceptionOfType(HttpClientErrorException.class)
                                .isThrownBy(() -> catalogOperationsRemote.getCategory(categoryId))
                                .has(ApiTestHttpConditions.httpNotFoundCondition);
                    })
                )
        );
    }

    @Test
    public void shouldIncreaseTotalCategoriesCountWhenCreatingCategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
            givenCatalog.localCategoriesCount(categoriesCount ->
                    whenCategory.created(categoryId ->
                        thenCategory.totalCountEquals(categoriesCount + 1)
                )
            )
        );
    }

    // TODO: better field by field comparison
    @Test
    public void shouldCreateNewCategoryWithProperlyInitializedFields() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            final CategoryDto categoryDto = categories().testCategoryDto();
            whenCategory.created(categoryDto, createdCategoryResponseEntity -> {
                final long categoryId = ApiTestUtils.getIdFromEntity(createdCategoryResponseEntity);
                final CategoryDto createdCategoryDto = catalogOperationsRemote.getCategory(categoryId);
                final Category createdCategoryEntity = catalogService.findCategoryById(categoryId);
                assertThat(createdCategoryDto).isEqualToComparingOnlyGivenFields(categoryDto, "name", "description", "longDescription");
                assertThat(createdCategoryEntity).isEqualToComparingOnlyGivenFields(categoryDto, "name", "description", "longDescription");
            });
        });
    }

    @Test
    public void shouldNotAssignAnyProductsToCreatedCategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                whenCategory.created(categoryId ->
                        thenCategory.doesNotContainProducts(categoryId)
                )
        );
    }

    @Test
    public void shouldNotAssignAnySubcategoriesToCreatedCategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                whenCategory.created(categoryId ->
                      thenCategory.doesNotHaveSubcategories(categoryId)
                )
        );
    }

    @Test
    public void shouldIncreaseProductsInCategoryCountWhenAddingProductToCategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            final CategoryDto categoryDto = categories().testCategoryDto();
            givenCatalog.category(categoryDto, categoryId -> {
                final ProductDto productDto = products().testProductWithDefaultCategory(categoryDto.getName());
                whenProductCreated(productDto, createdProductResponseEntity ->
                    thenCategory.containsProductsCount(categoryId, 1L)
                );
            });
        });
    }

    @Test
    public void shouldReturn400ErrorCodeWhenAddingCategoryWithoutName() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            final CategoryDto categoryDto = categories().testCategoryDto();
            categoryDto.setName("");

            assertThatExceptionOfType(HttpClientErrorException.class)
                    .isThrownBy(() -> catalogOperationsRemote.addCategory(categoryDto))
                    .is(ApiTestHttpConditions.http4xxStatusCode);
        });
    }

    @Test
    public void modifyingExistingCategoryDoesNotCreateANewOneInstead() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                givenCatalog.category(categoryId ->
                    givenCatalog.localCategoriesCount(categoriesCount -> {
                        whenCategory.modified(categoryId, categories().testCategoryDto());
                        thenCategory.totalCountEquals(categoriesCount);
                })
            )
        );
    }

    @Test
    public void modifyingExistingCategoryDoesActuallyModifyItsValues() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                givenCatalog.category(categoryId -> {
                    final CategoryDto newCategoryDto = categories().testCategoryDto();
                    whenCategory.modified(categoryId, newCategoryDto);

                    final CategoryDto modifiedCategoryDto = catalogOperationsRemote.getCategory(categoryId);
                    assertThat(modifiedCategoryDto).isEqualToComparingOnlyGivenFields(newCategoryDto, "name", "description", "longDescription");
                })
        );
    }

    @Test
    public void shouldAddProductToCategorySuccessfully() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            final CategoryDto categoryDto = categories().testCategoryDto();
            givenCatalog.category(categoryDto, categoryId -> {
                final ProductDto productDto = products().testProductWithDefaultCategory(categoryDto.getName());
                whenProductCreated(productDto, createdProductResponseEntity -> {
                    thenHttpStatusReturned(createdProductResponseEntity, HttpStatus.CREATED);
                });
            });
        });
    }

    @Test
    public void shouldIncreaseProductsCountWhenAddingProductToCategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
            givenCatalog.localProductsCount(productsCount -> {
                final CategoryDto categoryDto = categories().testCategoryDto();
                givenCatalog.category(categoryDto, categoryId -> {
                    final ProductDto productDto = products().testProductWithDefaultCategory(categoryDto.getName());
                    whenProductCreated(productDto, createdProductResponseEntity ->
                        assertThat(catalogOperationsLocal.getTotalProductsCount()).isEqualTo(productsCount + 1)
                    );
                });
            })
        );
    }

    @Test
    public void deletingCategoryDoesNotRemoveProductPreviouslyAssociatedWithIt() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            final CategoryDto categoryDto = categories().testCategoryDto();
            final ProductDto productDto = products().testProductWithDefaultCategory(categoryDto.getName());

            final long categoryId = ApiTestUtils.getIdFromEntity(catalogOperationsRemote.addCategory(categoryDto));
            catalogOperationsRemote.addProduct(productDto);
            final long currentTotalProductsCount = catalogOperationsLocal.getTotalProductsCount();

            catalogOperationsRemote.removeCategory(categoryId);

            assertThat(catalogOperationsLocal.getTotalProductsCount()).isEqualTo(currentTotalProductsCount);
        });
    }

    @Test
    public void shouldAssignDefaultAvailabilityToCreatedCategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                whenCategory.created(categoryId ->
                        whenCategory.retrieved(categoryId, retrievedCategoryDto ->
                            assertThat(retrievedCategoryDto.getProductsAvailability()).isEqualTo(InventoryType.ALWAYS_AVAILABLE.getType())
                    )
                )
        );
    }

    @Test
    public void addingCategoryWithIncorrectAvailabilitySetsDefaultValue() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            final CategoryDto categoryDto = categories().testCategoryDto();
            categoryDto.setProductsAvailability("incorrect_product_availability_value");

            whenCategory.created(categoryDto, createdCategoryResponseEntity ->
                    whenCategory.retrieved(ApiTestUtils.getIdFromEntity(createdCategoryResponseEntity), retrievedCategoryDto ->
                        assertThat(retrievedCategoryDto.getProductsAvailability()).isEqualTo(InventoryType.ALWAYS_AVAILABLE.getType())
                )
            );
        });
    }

    @Test
    public void shouldSetCategoryAvailabilityProperly() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            final CategoryDto categoryDto = categories().testCategoryDto();
            categoryDto.setProductsAvailability(InventoryType.UNAVAILABLE.getType());

            whenCategory.created(categoryDto, createdCategoryResponseEntity ->
                    whenCategory.retrieved(ApiTestUtils.getIdFromEntity(createdCategoryResponseEntity), retrievedCategoryDto ->
                            assertThat(retrievedCategoryDto.getProductsAvailability()).isEqualTo(InventoryType.UNAVAILABLE.getType())
                    )
            );
        });
    }

    @Test
    public void shouldUpdateCategoryAvailabilityProperly() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            final CategoryDto categoryDto = categories().testCategoryDto();
            categoryDto.setProductsAvailability(InventoryType.UNAVAILABLE.getType());

            givenCatalog.category(categoryDto, categoryId -> {
                categoryDto.setProductsAvailability(InventoryType.CHECK_QUANTITY.getType());
                whenCategory.modified(categoryId, categoryDto);

                whenCategory.retrieved(categoryId, retrievedCategoryDto ->
                        assertThat(retrievedCategoryDto.getProductsAvailability()).isEqualTo(InventoryType.CHECK_QUANTITY.getType())
                );
            });
        });
    }

    @Test
    public void shouldSaveCategoryWithAttributesProperly() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            final ImmutableMap<String, String> CATEGORY_ATTRIBUTES = ImmutableMap.<String, String>builder()
                    .put("size", String.valueOf(99))
                    .put("color", "red")
                    .put("length", String.valueOf(12.222))
                    .build();
            final CategoryDto categoryDto = DtoTestFactory.categories().testCategoryDtoWithAttributes(CATEGORY_ATTRIBUTES);
            whenCategory.created(categoryDto, createdCategoryResponseEntity -> {
                final CategoryDto createdCategoryDto = catalogOperationsRemote.getCategory(ApiTestUtils.getIdFromEntity(createdCategoryResponseEntity));
                assertThat(createdCategoryDto.getAttributes())
                        .isNotEmpty()
                        .hasSize(CATEGORY_ATTRIBUTES.size())
                        .containsAllEntriesOf(CATEGORY_ATTRIBUTES);
            });
        });
    }

    @Test
    public void shouldAddSubcategoryToCategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                givenCatalog.category(categoryId ->
                        givenCatalog.category(subcategoryId ->
                            whenCategory.referenceCreated(categoryId, subcategoryId, subcategoryReferenceResponseEntity -> {
                                thenHttpStatusReturned(subcategoryReferenceResponseEntity, HttpStatus.CREATED);
                                thenCategory.hasSubcategories(categoryId, 1);
                    })
                )
            )
        );
    }

    @Test
    public void shouldSaveSubcategoryValuesProperly() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                givenCatalog.category(categoryId -> {
                    final CategoryDto subcategoryDto = categories().testCategoryDto();
                    givenCatalog.category(subcategoryDto, subcategoryId -> {
                        whenCategory.referenceCreated(categoryId, subcategoryId);

                        final CategoryDto categoryDto = catalogOperationsRemote.getSubcategories(categoryId).iterator().next();
                        assertThat(categoryDto)
                                .isEqualToComparingOnlyGivenFields(subcategoryDto, "name", "description", "longDescription");
                    });
                })
        );
    }

    @Test
    public void removingASubcategoryContainingOtherSubcategoriesShouldNotRemoveThem() throws Throwable {
        /* given: a category tree:

                            A
                          /
                   P - S
                          \
                           B
         */
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                givenCatalog.category(rootCategoryId ->
                        givenCatalog.category(rootSubcategoryId ->
                                givenCatalog.category(childSubcategory1Id ->
                                        givenCatalog.category(childSubcategory2Id -> {
                                            givenCatalog.categoryReference(rootCategoryId, rootSubcategoryId);
                                            givenCatalog.categoryReference(rootSubcategoryId, childSubcategory1Id);
                                            givenCatalog.categoryReference(rootSubcategoryId, childSubcategory2Id);

                                        // when: removing the S Node Category reference from P Node Category
                                        whenCategory.referenceDeleted(rootCategoryId, rootSubcategoryId, subcategoryReferenceResponseEntity -> {
                                            // then: S Node category still exists and has all of its subcategories but is no longer "associated" with P Node Category
                                            thenCategory.hasSubcategories(rootSubcategoryId, 2);
                                            thenCategory.doesNotHaveSubcategories(rootCategoryId);
                                        });
                                    })
                                )
                        )
                )
        );
    }

    @Test
    public void shouldThrowExceptionWhenAddingTheSameSubcategoryTwice() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                givenCatalog.category(categoryId ->
                        givenCatalog.category(subcategoryId -> {
                            givenCatalog.categoryReference(categoryId, subcategoryId);

                            thenCategory.hasSubcategories(categoryId, 1);
                            assertThatExceptionOfType(HttpClientErrorException.class)
                                    .isThrownBy(() -> catalogOperationsRemote.addCategoryToCategoryReference(categoryId, subcategoryId))
                                    .is(ApiTestHttpConditions.httpConflictCondition);
                        })
                )
        );
    }

    @Test
    public void shouldRemoveAllReferencesInTheParentCategoryAfterDeletingItsSubcategory() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                givenCatalog.category(categoryId ->
                        givenCatalog.category(subcategoryId -> {
                            givenCatalog.categoryReference(categoryId, subcategoryId);
                                    whenCategory.deleted(subcategoryId, deletedSubcategoryResponseEntity ->
                                            thenCategory.doesNotHaveSubcategories(categoryId)
                                    );
                                })
                )
        );
    }

    @Test
    public void shouldDeleteSubcategoryReference() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate ->
                givenCatalog.category(categoryId ->
                        givenCatalog.category(subcategoryId -> {
                            givenCatalog.categoryReference(categoryId, subcategoryId);
                            whenCategory.referenceDeleted(categoryId, subcategoryId, deleteRefrenceResponseEntity ->
                                    thenCategory.doesNotHaveSubcategories(categoryId)
                            );
                        })
                )
        );
    }
}