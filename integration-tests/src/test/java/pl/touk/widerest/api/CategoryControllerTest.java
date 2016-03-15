package pl.touk.widerest.api;

import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.base.ApiTestUtils;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.base.DtoTestType;
import pl.touk.widerest.security.oauth2.Scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
public class CategoryControllerTest extends AbstractTest {

    @Before
    public void initCategoryTests() {
        cleanupCategoryTests();
    }

    protected long addNewTestCategory() {
        final ResponseEntity<?> newTestCategoryEntity = catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT));
        //oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL, DtoTestFactory.getTestCategory(DtoTestType.NEXT), null, serverPort);
        assertThat(newTestCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        return ApiTestUtils.getIdFromLocationUrl(newTestCategoryEntity.getHeaders().getLocation().toString());
    }

    @Test
    @Transactional
    public void newlyCreatedCategoryDoesNotContainAnyProductTest() {
        // when: creating a new category
        final long categoryId = addNewTestCategory();

        // then: newly created category does not contain any products
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(categoryId), equalTo(0L));
    }

    @Test
    @Transactional
    public void localAndRemoteProductsCountValuesInCategoryAreEqualTest() {
        // when: 1) creating a new category 2) creating a new product and adding it to that category
        final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        final ResponseEntity<?> newCategoryResponseHeaders = catalogOperationsRemote.addTestCategory(categoryDto);
        assertThat(newCategoryResponseHeaders.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long categoryId = ApiTestUtils.getIdFromLocationUrl(newCategoryResponseHeaders.getHeaders().getLocation().toString());

        final ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        productDto.setCategoryName(categoryDto.getName());
        final ResponseEntity<?> newProductInTestCategoryEntity = catalogOperationsRemote.addTestProduct(productDto);
        assertThat(newProductInTestCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        // then: the number of products in that category equals 1
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(categoryId), equalTo(1L));
    }

    @Test
    public void addingNewCategoryIncreasesTotalCategoriesCountTest() {
        // when: creating a new category
        final long currentCategoriesCount = catalogOperationsLocal.getTotalCategoriesCount();
        final long newCategoryId = addNewTestCategory();

        // then: total categories number increases by 1
        assertThat(catalogOperationsLocal.getTotalCategoriesCount(), equalTo(currentCategoriesCount + 1));
    }

    @Test
    @Ignore("Considering allowing duplicate names")
    public void addingDuplicateCategoryDoesNotIncreaseTotalCountNumberTest() {

        // when: 1) creating a new category
        final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        catalogOperationsRemote.addTestCategory(categoryDto);

        final long currentCategoryCount = catalogOperationsLocal.getTotalCategoriesCount();

        try {
            // when: 2) adding the same category again
            catalogOperationsRemote.addTestCategory(categoryDto);
            fail();
        } catch (HttpClientErrorException httpClientErrorException) {
            // then: API should return HTTP.CONFLICT code and the total number of categories should
            //       not change
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
            assertThat(catalogOperationsLocal.getTotalCategoriesCount(), equalTo(currentCategoryCount));
        }
    }

    @Test
    //@Ignore("This test is probably useless")
    public void numberOfRemotelyRetrievedCategoriesEqualsLocalyStoredCountTest() {
        // when: retrieving all categories
        final ResponseEntity<Resources<CategoryDto>> receivedCategoriesEntity =
                backofficeRestTemplate.exchange(ApiTestUrls.CATEGORIES_FLAT_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort);
        assertThat(receivedCategoriesEntity.getStatusCode(), equalTo(HttpStatus.OK));

        // then: the number of remotely retrieved categories should equal the number of locally retrieved ones
        assertThat((long) receivedCategoriesEntity.getBody().getContent().size(), equalTo(catalogOperationsLocal.getTotalCategoriesCount()));
    }

    @Test
    public void addingCategoryWithNoNameResultsIn400ErrorCodeTest() {
        // when: creating a category with no/empty name
        final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        categoryDto.setName(null);

        try {
            catalogOperationsRemote.addTestCategory(categoryDto);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            // then: API returns 4xx error code
            assertTrue(httpClientErrorException.getStatusCode().is4xxClientError());
        }
    }

    @Test
    public void remotelyRetrievedCategoryEqualsLocalyRetrievedOneTest() {
        // when: picking a random category via local service
        final List<Long> localCategoryIds = catalogService.findAllCategories().stream()
                .filter(entity -> ((Status) entity).getArchived() == 'N')
                .map(Category::getId).collect(Collectors.toList());

        if(localCategoryIds.isEmpty()) {
            return;
        }

        final Random rnd = new Random();

        final int pickedCategoryIndex = rnd.nextInt(localCategoryIds.size());
        final long pickedCategoryId = localCategoryIds.get(pickedCategoryIndex);

        final ResponseEntity<CategoryDto> receivedCategoryEntity =
                backofficeRestTemplate.getForEntity(ApiTestUrls.CATEGORIES_URL + "/" + pickedCategoryId, CategoryDto.class, serverPort);

        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

        final CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();

        // then: locally retrieved random category should be equal to the remotely retrieved one
        final Category localCategoryEntity = catalogService.findCategoryById(pickedCategoryId);

        assertTrue(receivedCategoryDto.getName().equals(localCategoryEntity.getName()) &&
                receivedCategoryDto.getDescription().equals(localCategoryEntity.getDescription()));
    }

    @Test
    public void createNewCategoryAndCheckIfValuesAreValidTest() {
        // when: creating a new category
        final CategoryDto testCategoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        final ResponseEntity<?> newCategoryResponseHeaders = catalogOperationsRemote.addTestCategory(testCategoryDto);
        assertThat(newCategoryResponseHeaders.getStatusCode(), equalTo(HttpStatus.CREATED));

        final ResponseEntity<CategoryDto> receivedCategoryEntity =
                backofficeRestTemplate.getForEntity(newCategoryResponseHeaders.getHeaders().getLocation().toString(), CategoryDto.class, serverPort);

        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

        // then: all the values of the newly created category should be properly set/saved
        final CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
        assertThat(testCategoryDto.getName(), equalTo(receivedCategoryDto.getName()));
        assertThat(testCategoryDto.getDescription(), equalTo(receivedCategoryDto.getDescription()));
        assertThat(testCategoryDto.getLongDescription(), equalTo(receivedCategoryDto.getLongDescription()));
    }

    @Test
    public void successfullyDeletingANewlyCreatedCategoryTest() {
        // when: 1) creating a new category
        final ResponseEntity<?> createdCategoryResponse = catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT));
        assertThat(createdCategoryResponse.getStatusCode(), equalTo(HttpStatus.CREATED));

        final String createdCategoryLocationUri = createdCategoryResponse.getHeaders().getLocation().toString();
        assertNotNull(createdCategoryLocationUri);
        assertTrue(!createdCategoryLocationUri.isEmpty());

        // when: 2) deleting the newly created category
        catalogOperationsRemote.removeTestCategory(ApiTestUtils.strapSuffixId(createdCategoryLocationUri));

        // then: deleted category should no longer exist
        try {
            backofficeRestTemplate.getForEntity(createdCategoryLocationUri, CategoryDto.class, serverPort);
        } catch (HttpClientErrorException httpClientErrorException) {
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
        }
    }


    @Test
    public void modifyingExistingCategoryDoesNotCreateANewOneInsteadTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: 1) creating a new category
            final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
            final ResponseEntity<?> addNewCategoryResponse = catalogOperationsRemote.addTestCategory(categoryDto);
            assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);

            final String createdCategoryLocationUri = addNewCategoryResponse.getHeaders().getLocation().toString();
            final long currentCategoriesCount = catalogOperationsLocal.getTotalCategoriesCount();

            // when: 2) modifying the newly created category
            categoryDto.setDescription("ModifiedTestCategoryDescription");
            categoryDto.setName("ModifiedTestCategoryName2");
            categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

            adminRestTemplate.put(createdCategoryLocationUri, categoryDto, serverPort);

            // then: no new category should be created
            assertThat(catalogOperationsLocal.getTotalCategoriesCount(), equalTo(currentCategoriesCount));
        });
    }

    @Test
    public void modifyingExistingCategoryDoesActuallyModifyItsValuesTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            // when: 1) creating a new category
            final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
            final ResponseEntity<?> addNewCategoryResponse = catalogOperationsRemote.addTestCategory(categoryDto);
            assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);

            final String createdCategoryLocationUri = addNewCategoryResponse.getHeaders().getLocation().toString();

            // when: 2) modifying the newly created category
            categoryDto.setDescription("ModifiedTestCategoryDescription");
            categoryDto.setName("ModifiedTestCategoryName");
            categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

            adminRestTemplate.put(createdCategoryLocationUri, categoryDto, serverPort);

            // then: the modified category should actually get changed
            final ResponseEntity<CategoryDto> receivedCategoryEntity =
                    backofficeRestTemplate.getForEntity(createdCategoryLocationUri, CategoryDto.class, serverPort);
            assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

            final CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
            assertThat(categoryDto.getName(), equalTo(receivedCategoryDto.getName()));
            assertThat(categoryDto.getDescription(), equalTo(receivedCategoryDto.getDescription()));
            assertThat(categoryDto.getLongDescription(), equalTo(receivedCategoryDto.getLongDescription()));
        });
    }

    @Test
    @Transactional
    public void addingNewProductWithDefaultSKUToExistingCategoryIncreasesProductsCountTest() {
        // when: 1) adding a new category
        final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        final ResponseEntity<?> remoteAddCategoryEntity = catalogOperationsRemote.addTestCategory(categoryDto);
        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testCategoryId = ApiTestUtils.getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        // then: 1) the number of products in added category is equal to 0 (by default)
        final long currentProductsInCategoryRemoteCount = catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId);
        assertThat(currentProductsInCategoryRemoteCount, equalTo(0L));

        // when: 2) adding a new product to newly created category
        final ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        productDto.setCategoryName(categoryDto.getName());
        final ResponseEntity<?> remoteAddProduct1Entity = catalogOperationsRemote.addTestProduct(productDto);
        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        em.clear();

        // then: 2) total number of products in that category increases by 1
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount + 1));
    }

    @Test
    @Transactional
    public void deletingCategoryDoesNotRemoveProductPreviouslyAssociatedWithItTest() {
        // when: 1) creating a new category and inserting a new product into it
        final long currentTotalProductsCount = catalogOperationsLocal.getTotalProductsCount();

        final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        final ResponseEntity<?> newCategoryResponseHeaders = catalogOperationsRemote.addTestCategory(categoryDto);
        assertThat(newCategoryResponseHeaders.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long categoryId = ApiTestUtils.getIdFromLocationUrl(newCategoryResponseHeaders.getHeaders().getLocation().toString());

        final ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        productDto.setCategoryName(categoryDto.getName());

        final ResponseEntity<?> newProductInTestCategoryEntity = catalogOperationsRemote.addTestProduct(productDto);
        assertThat(newProductInTestCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(categoryId), equalTo(1L));

        // when: 1) deleting the newly created category
        catalogOperationsRemote.removeTestCategory(categoryId);

        // then: deleting the category does not remove its products
        try {
            backofficeRestTemplate.getForEntity(ApiTestUrls.CATEGORY_BY_ID_URL, Void.class, serverPort, categoryId);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
        }

        assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentTotalProductsCount + 1));
    }


/*
    @Test
    public void partialUpdateCategoryDescriptionAndCheckIfOtherValuesPreserveTest() {
        long currentTotalCategoriesCount = apiTestCatalogLocal.getTotalCategoriesCount();

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestUrls.CATEGORIES_URL, categoryDto, null, serverPort);

        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        assertThat(apiTestCatalogLocal.getTotalCategoriesCount(), equalTo(currentTotalCategoriesCount + 1));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());


        categoryDto.setName("Category Name Changed!");

        final HttpEntity<CategoryDto> requestEntity = new HttpEntity<>(categoryDto);

        */
/* (mst) Those 2 are needed for RestTemplate to work with PATCH requests... *//*

        OAuth2RestTemplate adminRestTemplate = oAuth2AdminRestTemplate();
        adminRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        adminRestTemplate.exchange(CATEGORY_BY_ID_URL, HttpMethod.PATCH, requestEntity, Void.class, serverPort, testCategoryId);

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                backofficeRestTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode().value(), equalTo(200));

        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
        assertThat(categoryDto.getName(), equalTo(receivedCategoryDto.getName()));
        assertThat(categoryDto.getDescription(), equalTo(receivedCategoryDto.getDescription()));
        assertThat(categoryDto.getLongDescription(), equalTo(receivedCategoryDto.getLongDescription()));

    }
*/

    @Test
    public void newlyAddedCategoryHasDefaultAvailabilityTest() {
        // when: adding a new category without availability status
        final long testCategoryId = addNewTestCategory();

        final ResponseEntity<CategoryDto> receivedCategoryEntity =
                backofficeRestTemplate.getForEntity(ApiTestUrls.CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

        final CategoryDto receviedCategoryDto = receivedCategoryEntity.getBody();

        // then: the availability status of the newly created category is ALWAYS_AVAILABLE (by default)
        assertThat(receviedCategoryDto.getProductsAvailability(), equalTo(InventoryType.ALWAYS_AVAILABLE.getType()));
    }

    @Test
    public void addingCategoryWithIncorrectAvailabilitySetsDefaultValueTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: adding a new category with incorrect availability status
            final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
            categoryDto.setProductsAvailability("dasdadasda");
            final ResponseEntity<CategoryDto> remoteAddCategoryEntity = adminRestTemplate.postForEntity(ApiTestUrls.CATEGORIES_URL, categoryDto, null, serverPort);
            assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
            final long testCategoryId = ApiTestUtils.getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

            final ResponseEntity<CategoryDto> receivedCategoryEntity =
                    backofficeRestTemplate.getForEntity(ApiTestUrls.CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

            assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

            final CategoryDto receviedCategoryDto = receivedCategoryEntity.getBody();

            // then: the availability status of the newly created category should be set to ALWAYS_AVAILABLE (by default)
            assertThat(receviedCategoryDto.getProductsAvailability(), equalTo(InventoryType.ALWAYS_AVAILABLE.getType()));
        });
    }

/*
    @Test
    public void updatingAndSettingCategoryAvailabilityWorksTest() {
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        categoryDto.setProductsAvailability(InventoryType.UNAVAILABLE.getType());
        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestUrls.CATEGORIES_URL, categoryDto, null, serverPort);
        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                backofficeRestTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode().value(), equalTo(200));

        CategoryDto receviedCategoryDto = receivedCategoryEntity.getBody();

        assertThat(receviedCategoryDto.getProductsAvailability(), equalTo(InventoryType.UNAVAILABLE.getType()));


        // update via PUT endpoint
        oAuth2AdminRestTemplate().put(CATEGORY_AVAILABILITY_BY_ID_URL, InventoryType.CHECK_QUANTITY.getType(), serverPort, testCategoryId);

        ResponseEntity<CategoryDto> receivedCategoryEntity2 =
                backofficeRestTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertNotNull(receivedCategoryEntity2);
        assertThat(receivedCategoryEntity2.getStatusCode().value(), equalTo(200));

        CategoryDto receviedCategoryDto2 = receivedCategoryEntity2.getBody();

        assertThat(receviedCategoryDto2.getProductsAvailability(), equalTo(InventoryType.CHECK_QUANTITY.getType()));
    }
*/

/*
    @Test
    public void readingAvailabilityOfAddedCategoryViaGetEndpointReturnsCorrectValueTest() {
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        categoryDto.setProductsAvailability(InventoryType.UNAVAILABLE.getType());
        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestUrls.CATEGORIES_URL, categoryDto, null, serverPort);
        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        ResponseEntity<String> stringResponseEntity = backofficeRestTemplate.getForEntity(CATEGORY_AVAILABILITY_BY_ID_URL,
                String.class, serverPort, testCategoryId);

        assertNotNull(stringResponseEntity);
        assertThat(stringResponseEntity.getStatusCode().value(), equalTo(200));

        String receivedAvailabilityString = stringResponseEntity.getBody();

        assertThat(receivedAvailabilityString, equalTo(InventoryType.UNAVAILABLE.getType()));
    }
*/

    @Test
    public void addingCategoryWithAttributesSavesThemCorrectlyTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: adding a new category with 3 attributes
            final CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

            final Map<String, String> categoryAttributes = new HashMap<>();
            categoryAttributes.put("size", String.valueOf(99));
            categoryAttributes.put("color", "red");
            categoryAttributes.put("length", String.valueOf(12.222));

            categoryDto.setAttributes(categoryAttributes);

            final ResponseEntity<CategoryDto> remoteAddCategoryEntity = adminRestTemplate.postForEntity(ApiTestUrls.CATEGORIES_URL, categoryDto, null, serverPort);
            assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
            final long testCategoryId = ApiTestUtils.getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

            // then: category attributes have been set properly
            final ResponseEntity<CategoryDto> receivedCategoryEntity =
                    backofficeRestTemplate.getForEntity(ApiTestUrls.CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);
            assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

            final CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
            final Map<String, String> receivedAttributes = receivedCategoryDto.getAttributes();

            assertThat(receivedAttributes.size(), equalTo(categoryAttributes.size()));
            assertThat(receivedAttributes.get("size"), equalTo(String.valueOf(99)));
            assertThat(receivedAttributes.get("color"), equalTo("red"));
            assertThat(receivedAttributes.get("length"), equalTo(String.valueOf(12.222)));
        });
    }


    /* ----------------------------- SUBCATEGORIES TESTS ----------------------------- */

    @Test
    public void shouldAddAndDeleteASubcategoryProperlyTest() {
        // when: 1) creating a new test category
        final long testCategoryId = addNewTestCategory();

        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity =
                backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, testCategoryId);

        // then: 1) newly created category should not have any subcategories
        assertThat(receivedSubcategoriesEntity.getBody().getContent().size(), equalTo(0));

        // when: 2) adding a new subcategory to test category
        final CategoryDto subcategoryDto= DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        subcategoryDto.setName("Subcategory");
        subcategoryDto.setDescription("This is a subcategory description");

        final ResponseEntity<?> subcategoryResponseEntity = catalogOperationsRemote.addTestCategory(subcategoryDto);
        final long testSubcategoryId = ApiTestUtils.getIdFromLocationUrl(subcategoryResponseEntity.getHeaders().getLocation().toString());

        final ResponseEntity<?> addSubcategoryResponseEntity = catalogOperationsRemote.addCategoryToCategoryReference(testCategoryId, testSubcategoryId);

        assertThat(addSubcategoryResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        // then: 2) test category should have 1 subcategory
        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity2 =
                backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, testCategoryId);

        assertThat(receivedSubcategoriesEntity2.getBody().getContent().size(), equalTo(1));

        final CategoryDto category = receivedSubcategoriesEntity2.getBody().iterator().next();
        assertThat(category.getName(), equalTo(subcategoryDto.getName()));
        assertThat(category.getDescription(), equalTo(subcategoryDto.getDescription()));

        // when: 3) deleting test category's subcategory
        catalogOperationsRemote.removeCategoryToCategoryReference(testCategoryId, testSubcategoryId);

        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity3 =
                backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, testCategoryId);

        // then: 3) test category should not contain any subcategories any more
        assertThat(receivedSubcategoriesEntity3.getBody().getContent().size(), equalTo(0));
    }

    @Test
    public void removingASubcategoryContainingOtherSubcategoriesShouldNotRemoveThemTest() {

        /* (mst) This test operates on a category tree that looks like this:

                            A
                          /
                   P - S
                          \
                           B
         */

        /* Build the category tree */
        final long rootCategoryId = ApiTestUtils.getIdFromLocationUrl(catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT)).getHeaders().getLocation().toString());
        final long rootSubcategoryId = ApiTestUtils.getIdFromLocationUrl(catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT)).getHeaders().getLocation().toString());
        final long childSubcategory1Id = ApiTestUtils.getIdFromLocationUrl(catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT)).getHeaders().getLocation().toString());
        final long childSubcategory2Id = ApiTestUtils.getIdFromLocationUrl(catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT)).getHeaders().getLocation().toString());

        catalogOperationsRemote.addCategoryToCategoryReference(rootCategoryId, rootSubcategoryId);
        catalogOperationsRemote.addCategoryToCategoryReference(rootSubcategoryId, childSubcategory1Id);
        catalogOperationsRemote.addCategoryToCategoryReference(rootSubcategoryId, childSubcategory2Id);

        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity =
                backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootSubcategoryId);

        assertThat(receivedSubcategoriesEntity.getBody().getContent().size(), equalTo(2));

        final ResponseEntity<Resources<CategoryDto>> receivedRootSubcategoriesEntity =
                backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

        assertThat(receivedRootSubcategoriesEntity.getBody().getContent().size(), equalTo(1));

        // when: removing the S Node Category reference from P Node Category
        catalogOperationsRemote.removeCategoryToCategoryReference(rootCategoryId, rootSubcategoryId);

        final ResponseEntity<Resources<CategoryDto>> receivedChildSubcategoriesEntity =
                backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootSubcategoryId);

        // then: S Node category still exists and has all of its subcategories but is no longer "associated" with P Node Category
        assertThat(receivedChildSubcategoriesEntity.getBody().getContent().size(), equalTo(2));

        final ResponseEntity<Resources<CategoryDto>> receivedRootSubcategoriesEntity1 =
                backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

        assertThat(receivedRootSubcategoriesEntity1.getBody().getContent().size(), equalTo(0));
    }

    @Test
    public void shouldThrowExceptionWhenAddingTheSameSubcategoryTwiceTest() {
        // when: 1) adding a new category with a new subcategory
        final long rootCategoryId = ApiTestUtils.getIdFromLocationUrl(catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT)).getHeaders().getLocation().toString());
        final long subcategoryId = ApiTestUtils.getIdFromLocationUrl(catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT)).getHeaders().getLocation().toString());


        final ResponseEntity<?> addSubcategoryResponseEntity = catalogOperationsRemote.addCategoryToCategoryReference(rootCategoryId, subcategoryId);

        assertThat(addSubcategoryResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        // when: 2) adding the same subcategory to the test category
        try {
            catalogOperationsRemote.addCategoryToCategoryReference(rootCategoryId, subcategoryId);

            fail();
        } catch (HttpClientErrorException httpClientErrorException) {
            // then; API should return 4xx code (HTTP.CONFLICT?) and the number of test category's subcategories should remain 1
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));

            final ResponseEntity<Resources<CategoryDto>> receivedRootSubcategoriesEntity =
                    backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

            assertThat(receivedRootSubcategoriesEntity.getBody().getContent().size(), equalTo(1));
        }
    }

    @Test
    public void shouldRemoveAllReferencesInTheParentCategoryAfterDeletingItsSubcategoryTest() {
        // when: 1) adding a new test category with a new subcategory
        final long rootCategoryId = ApiTestUtils.getIdFromLocationUrl(catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT)).getHeaders().getLocation().toString());
        final long subcategoryId = ApiTestUtils.getIdFromLocationUrl(catalogOperationsRemote.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT)).getHeaders().getLocation().toString());

        final ResponseEntity<?> addSubcategoryResponseEntity = catalogOperationsRemote.addCategoryToCategoryReference(rootCategoryId, subcategoryId);

        assertThat(addSubcategoryResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity =
                backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

        assertThat(receivedSubcategoriesEntity.getBody().getContent().size(), equalTo(1));

        // when: 2) deleting the subcategory from test category
        catalogOperationsRemote.removeTestCategory(subcategoryId);

        // then: test category should no longer have any subcategories
        final ResponseEntity<Resources<CategoryDto>> receivedRootSubcategoriesEntity2 =
                backofficeRestTemplate.exchange(ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

        assertThat(receivedRootSubcategoriesEntity2.getBody().getContent().size(), equalTo(0));
    }




    /* ----------------------------- SUBCATEGORIES TESTS ----------------------------- */


    /* ----------------------------- HELPER METHODS ----------------------------- */

    private void cleanupCategoryTests() {
        /* If there is still any test row in a database, delete it */
        removeLocalTestCategories();
    }
}