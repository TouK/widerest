package pl.touk.widerest.catalog;

import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.inventory.service.type.InventoryType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.base.DtoTestType;

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
@SpringApplicationConfiguration(classes = Application.class)
public class CategoryControllerTest extends ApiTestBase {

    @Before
    public void initCategoryTests() {
        cleanupCategoryTests();
    }

/*
    @Test
    public void localAndRemoteCategoriesCountValuesAreEqualTest() {
        //when
        long remoteTotalCategoriesCount = getRemoteTotalCategoriesCount();
        //then
        assertThat(remoteTotalCategoriesCount, equalTo(getLocalTotalCategoriesCount()));
    }
*/

    @Test
    @Transactional
    public void newlyCreatedCategoryDoesNotContainAnyProductTest() {
        ResponseEntity<?> newCategoryResponseHeaders = addNewTestCategory(DtoTestType.NEXT);
        assertThat(newCategoryResponseHeaders.getStatusCode(), equalTo(HttpStatus.CREATED));
        long categoryId = getIdFromLocationUrl(newCategoryResponseHeaders.getHeaders().getLocation().toString());

        assertThat(getLocalTotalProductsInCategoryCount(categoryId), equalTo(0L));
    }

    @Test
    @Transactional
    public void localAndRemoteProductsCountValuesInCategoryAreEqualTest() {
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        ResponseEntity<?> newCategoryResponseHeaders = addNewTestCategory(categoryDto);
        assertThat(newCategoryResponseHeaders.getStatusCode(), equalTo(HttpStatus.CREATED));
        long categoryId = getIdFromLocationUrl(newCategoryResponseHeaders.getHeaders().getLocation().toString());

        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        productDto.setCategoryName(categoryDto.getName());

        ResponseEntity<?> newProductInTestCategoryEntity = addNewTestProduct(productDto);
        assertThat(newProductInTestCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

//        assertThat(getRemoteTotalProductsInCategoryCount(categoryId), equalTo(getLocalTotalProductsInCategoryCount(categoryId)));
        assertThat(getLocalTotalProductsInCategoryCount(categoryId), equalTo(1L));
    }

    @Test
    public void addingNewCategoryIncreasesTotalCategoriesCountTest() {
        //when
        long currentCategoryCount = getLocalTotalCategoriesCount();
        addNewTestCategory(DtoTestType.NEXT);
        //then
        assertThat(getLocalTotalCategoriesCount(), equalTo(currentCategoryCount + 1));
    }

    @Ignore("considering allowing duplicate names")
    @Test
    public void addingDuplicateCategoryDoesNotIncreaseTotalCountNumberTest() {

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        addNewTestCategory(categoryDto);

        long currentCategoryCount = getLocalTotalCategoriesCount();

        try {
            // when
            addNewTestCategory(categoryDto);
            fail();
        } catch (HttpClientErrorException httpClientErrorException) {
            // then
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
            assertThat(getLocalTotalCategoriesCount(), equalTo(currentCategoryCount));
        }
    }

    @Test
    public void numberOfRemotelyRetrievedCategoriesEqualsLocalyStoredCountTest() {
        //when
        ResponseEntity<Resources<CategoryDto>> receivedCategoriesEntity =
                restTemplate.exchange(ApiTestBase.CATEGORIES_FLAT_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort);
        //then
        assertThat(receivedCategoriesEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat((long) receivedCategoriesEntity.getBody().getContent().size(), equalTo(getLocalTotalCategoriesCount()));

    }

    @Test
    public void addingCategoryWithNoNameResultsIn400ErrorCodeTest() {
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        categoryDto.setName(null);
        try {
            addNewTestCategory(categoryDto);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        }
    }

    @Test
    public void remotelyRetrievedCategoryEqualsLocalyRetrievedOneTest() {
        /* retrieve the IDs of all available, non-archived categories */
        List<Long> localCategoryIds = catalogService.findAllCategories().stream()
                .filter(entity -> ((Status) entity).getArchived() == 'N')
                .map(Category::getId).collect(Collectors.toList());

        Random rnd = new Random();

        /* pick a random category */
        int pickedCategoryIndex = rnd.nextInt(localCategoryIds.size());
        long pickedCategoryId = localCategoryIds.get(pickedCategoryIndex);

        //when
        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(ApiTestBase.CATEGORIES_URL + "/" + pickedCategoryId, CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryEntity);
        assertTrue("List of categories not found", receivedCategoryEntity.getStatusCode().value() == 200);

        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();

        //then
        Category localCategoryEntity = catalogService.findCategoryById(pickedCategoryId);

        assertTrue(receivedCategoryDto.getName().equals(localCategoryEntity.getName()) &&
                receivedCategoryDto.getDescription().equals(localCategoryEntity.getDescription()));
    }

    @Test
    public void createNewCategoryAndCheckIfValuesAreValidTest() {
        CategoryDto testCategoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        //when
        ResponseEntity<?> newCategoryResponseHeaders = addNewTestCategory(testCategoryDto);

        assertThat(newCategoryResponseHeaders.getStatusCode(), equalTo(HttpStatus.CREATED));

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(newCategoryResponseHeaders.getHeaders().getLocation().toString(), CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));
        //then
        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
        assertThat(testCategoryDto.getName(), equalTo(receivedCategoryDto.getName()));
        assertThat(testCategoryDto.getDescription(), equalTo(receivedCategoryDto.getDescription()));
        assertThat(testCategoryDto.getLongDescription(), equalTo(receivedCategoryDto.getLongDescription()));
    }

    @Test
    public void successfullyDeletingANewlyCreatedCategoryTest() {

        /* Creating a new test category */
        CategoryDto categoryDto = CategoryDto.builder().name("testcategory").description("testcategory").build();

        ResponseEntity<CategoryDto> createdCategoryResponse = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);
        assertThat(createdCategoryResponse.getStatusCode(), equalTo(HttpStatus.CREATED));

        String createdCategoryLocationUri = createdCategoryResponse.getHeaders().getLocation().toString();

        assertNotNull(createdCategoryLocationUri);
        assertTrue(!createdCategoryLocationUri.isEmpty());

        /* Reading the newly created category just to make sure it is there */
        ResponseEntity<CategoryDto> receivedCategoryDto = restTemplate.getForEntity(
                createdCategoryLocationUri, CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryDto);

        assertTrue(categoryDto.getName().equals(receivedCategoryDto.getBody().getName()) &&
                categoryDto.getDescription().equals(receivedCategoryDto.getBody().getDescription()));

        //when
        oAuth2AdminRestTemplate().delete(createdCategoryLocationUri, 1);

        //then
        try {
            restTemplate.getForEntity(createdCategoryLocationUri, CategoryDto.class, serverPort);
        } catch (HttpClientErrorException httpClientErrorException) {
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));

        }
    }


    @Test
    public void modifyingExistingCategoryDoesNotCreateANewOneInsteadTest() {

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        /* Create a test category */
        ResponseEntity<?> addNewCategoryResponse = addNewTestCategory(categoryDto);
        assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);
        String createdCategoryLocationUri = addNewCategoryResponse.getHeaders().getLocation().toString();

        long currentCategoriesCount = getLocalTotalCategoriesCount();

        //when

        categoryDto.setDescription("ModifiedTestCategoryDescription");
        categoryDto.setName("ModifiedTestCategoryName2");
        categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

        oAuth2AdminRestTemplate().put(createdCategoryLocationUri, categoryDto, serverPort);

        //then
        assertThat(getLocalTotalCategoriesCount(), equalTo(currentCategoriesCount));

    }

    @Test
    public void modifyingExistingCategoryDoesActuallyModifyItsValuesTest() {
        /* Create a test category */
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        ResponseEntity<?> addNewCategoryResponse = addNewTestCategory(categoryDto);
        assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);
        String createdCategoryLocationUri = addNewCategoryResponse.getHeaders().getLocation().toString();

        long currentCategoriesCount = getLocalTotalCategoriesCount();

        //when

        categoryDto.setDescription("ModifiedTestCategoryDescription");
        categoryDto.setName("ModifiedTestCategoryName");
        categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

        oAuth2AdminRestTemplate().put(createdCategoryLocationUri, categoryDto, serverPort);

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(createdCategoryLocationUri, CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(getLocalTotalCategoriesCount(), equalTo(currentCategoriesCount));

        //then
        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
        assertThat(categoryDto.getName(), equalTo(receivedCategoryDto.getName()));
        assertThat(categoryDto.getDescription(), equalTo(receivedCategoryDto.getDescription()));
        assertThat(categoryDto.getLongDescription(), equalTo(receivedCategoryDto.getLongDescription()));
    }


    @Test
    @Transactional
    public void addingNewProductWithDefaultSKUToExistingCategoryIncreasesProductsCountTest() {

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(
                CATEGORIES_URL,
                categoryDto, null, serverPort);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        long currentProductsInCategoryRemoteCount = getLocalTotalProductsInCategoryCount(testCategoryId);
        assertThat(currentProductsInCategoryRemoteCount, equalTo(0L));

        //when
        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);

        productDto.setCategoryName(categoryDto.getName());
        ResponseEntity<ProductDto> remoteAddProduct1Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto, null, serverPort);
        em.clear();

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        //then
        assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount + 1));
    }

    @Test
    @Transactional
    public void addNewProductToNewCategoryAndCheckIfItExists() {

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);


        ResponseEntity<?> addNewCategoryResponse = addNewTestCategory(categoryDto);

        assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);
        long createdCategoryId = getIdFromLocationUrl(addNewCategoryResponse.getHeaders().getLocation().toString());

        long currentProductsCount = getLocalTotalProductsInCategoryCount(createdCategoryId);

        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        productDto.setCategoryName(categoryDto.getName());



        ResponseEntity<?> remoteAddProductEntity = addNewTestProduct(productDto);

        em.clear();
        assertNotNull(remoteAddProductEntity);
        assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        assertThat(getLocalTotalProductsInCategoryCount(createdCategoryId), equalTo(currentProductsCount + 1));
    }

    @Test
    @Transactional
    public void deletingCategoryDoesNotRemoveProductPreviouslyAssociatedWithItTest() {

        long currentTotalProductsCount = getLocalTotalProductsCount();


        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        ResponseEntity<?> newCategoryResponseHeaders = addNewTestCategory(categoryDto);
        assertThat(newCategoryResponseHeaders.getStatusCode(), equalTo(HttpStatus.CREATED));
        long categoryId = getIdFromLocationUrl(newCategoryResponseHeaders.getHeaders().getLocation().toString());

        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        productDto.setCategoryName(categoryDto.getName());

        ResponseEntity<?> newProductInTestCategoryEntity = addNewTestProduct(productDto);
        assertThat(newProductInTestCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromLocationUrl(newProductInTestCategoryEntity.getHeaders().getLocation().toString());

        assertThat(getLocalTotalProductsInCategoryCount(categoryId), equalTo(1L));


        oAuth2AdminRestTemplate().delete(CATEGORY_BY_ID_URL, serverPort, categoryId);

        try {
            restTemplate.getForEntity(CATEGORY_BY_ID_URL, Void.class, serverPort, categoryId);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
        }

        // our product is still in there!
        assertThat(getLocalTotalProductsCount(), equalTo(currentTotalProductsCount + 1));
    }


/*
    @Test
    public void partialUpdateCategoryDescriptionAndCheckIfOtherValuesPreserveTest() {
        long currentTotalCategoriesCount = getLocalTotalCategoriesCount();

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);

        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        assertThat(getLocalTotalCategoriesCount(), equalTo(currentTotalCategoriesCount + 1));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());


        categoryDto.setName("Category Name Changed!");

        final HttpEntity<CategoryDto> requestEntity = new HttpEntity<>(categoryDto);

        */
/* (mst) Those 2 are needed for RestTemplate to work with PATCH requests... *//*

        OAuth2RestTemplate adminRestTemplate = oAuth2AdminRestTemplate();
        adminRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        adminRestTemplate.exchange(CATEGORY_BY_ID_URL, HttpMethod.PATCH, requestEntity, Void.class, serverPort, testCategoryId);

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

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
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);
        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode().value(), equalTo(200));

        CategoryDto receviedCategoryDto = receivedCategoryEntity.getBody();

        assertThat(receviedCategoryDto.getProductsAvailability(), equalTo(InventoryType.ALWAYS_AVAILABLE.getType()));
    }

    @Test
    public void addingCategoryWithIncorrectAvailabilitySetsDefaultValueTest() {
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        categoryDto.setProductsAvailability("dasdadasda");
        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);
        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode().value(), equalTo(200));

        CategoryDto receviedCategoryDto = receivedCategoryEntity.getBody();

        assertThat(receviedCategoryDto.getProductsAvailability(), equalTo(InventoryType.ALWAYS_AVAILABLE.getType()));
    }

/*
    @Test
    public void updatingAndSettingCategoryAvailabilityWorksTest() {
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        categoryDto.setProductsAvailability(InventoryType.UNAVAILABLE.getType());
        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);
        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode().value(), equalTo(200));

        CategoryDto receviedCategoryDto = receivedCategoryEntity.getBody();

        assertThat(receviedCategoryDto.getProductsAvailability(), equalTo(InventoryType.UNAVAILABLE.getType()));


        // update via PUT endpoint
        oAuth2AdminRestTemplate().put(CATEGORY_AVAILABILITY_BY_ID_URL, InventoryType.CHECK_QUANTITY.getType(), serverPort, testCategoryId);

        ResponseEntity<CategoryDto> receivedCategoryEntity2 =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

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
        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);
        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        ResponseEntity<String> stringResponseEntity = restTemplate.getForEntity(CATEGORY_AVAILABILITY_BY_ID_URL,
                String.class, serverPort, testCategoryId);

        assertNotNull(stringResponseEntity);
        assertThat(stringResponseEntity.getStatusCode().value(), equalTo(200));

        String receivedAvailabilityString = stringResponseEntity.getBody();

        assertThat(receivedAvailabilityString, equalTo(InventoryType.UNAVAILABLE.getType()));
    }
*/

    @Test
    public void addingCategoryWithAttributesSavesThemCorrectlyTest() {
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        Map<String, String> categoryAttributes = new HashMap<>();
        categoryAttributes.put("size", String.valueOf(99));
        categoryAttributes.put("color", "red");
        categoryAttributes.put("length", String.valueOf(12.222));

        categoryDto.setAttributes(categoryAttributes);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);
        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());


        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);
        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
        Map<String, String> receivedAttributes = receivedCategoryDto.getAttributes();

        assertThat(receivedAttributes.size(), equalTo(categoryAttributes.size()));
        assertThat(receivedAttributes.get("size"), equalTo(String.valueOf(99)));
        assertThat(receivedAttributes.get("color"), equalTo("red"));
        assertThat(receivedAttributes.get("length"), equalTo(String.valueOf(12.222)));

    }


    /* ----------------------------- SUBCATEGORIES TESTS ----------------------------- */

    @Test
    public void shouldAddAndDeleteASubcategoryProperlyTest() {
        final ResponseEntity<?> categoryResponseEntity = addNewTestCategory(DtoTestType.NEXT);
        final long testCategoryId = getIdFromLocationUrl(categoryResponseEntity.getHeaders().getLocation().toString());

        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity =
                restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, testCategoryId);

        assertThat(receivedSubcategoriesEntity.getBody().getContent().size(), equalTo(0));

        final CategoryDto subcategoryDto= DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        subcategoryDto.setName("Subcategory");
        subcategoryDto.setDescription("This is a subcategory description");

        final ResponseEntity<?> subcategoryResponseEntity = addNewTestCategory(subcategoryDto);
        final long testSubcategoryId = getIdFromLocationUrl(subcategoryResponseEntity.getHeaders().getLocation().toString());

        final ResponseEntity<Object> addSubcategoryResponseEntity = oAuth2AdminRestTemplate().postForEntity(
                ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL, null, null,
                serverPort, testCategoryId, serverPort, testSubcategoryId);

        assertThat(addSubcategoryResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity2 =
                restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, testCategoryId);

        assertThat(receivedSubcategoriesEntity2.getBody().getContent().size(), equalTo(1));

        CategoryDto category = receivedSubcategoriesEntity2.getBody().iterator().next();
        assertThat(category.getName(), equalTo(subcategoryDto.getName()));
        assertThat(category.getDescription(), equalTo(subcategoryDto.getDescription()));

        oAuth2AdminRestTemplate().delete(ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL,
                serverPort, testCategoryId, serverPort, testSubcategoryId);

        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity3 =
                restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, testCategoryId);

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

        /* (mst) Builds the category tree */
        final long rootCategoryId = getIdFromLocationUrl(addNewTestCategory(DtoTestType.NEXT).getHeaders().getLocation().toString());
        final long rootSubcategoryId = getIdFromLocationUrl(addNewTestCategory(DtoTestType.NEXT).getHeaders().getLocation().toString());
        final long childSubcategory1Id = getIdFromLocationUrl(addNewTestCategory(DtoTestType.NEXT).getHeaders().getLocation().toString());
        final long childSubcategory2Id = getIdFromLocationUrl(addNewTestCategory(DtoTestType.NEXT).getHeaders().getLocation().toString());

        oAuth2AdminRestTemplate().postForEntity(ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL, null, null,
                serverPort, rootCategoryId, serverPort, rootSubcategoryId);

        oAuth2AdminRestTemplate().postForEntity(ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL, null, null,
                serverPort, rootSubcategoryId, serverPort, childSubcategory1Id);

        oAuth2AdminRestTemplate().postForEntity(ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL, null, null,
                serverPort, rootSubcategoryId, serverPort, childSubcategory2Id);

        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity =
                restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootSubcategoryId);

        assertThat(receivedSubcategoriesEntity.getBody().getContent().size(), equalTo(2));

        final ResponseEntity<Resources<CategoryDto>> receivedRootSubcategoriesEntity =
                restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

        assertThat(receivedRootSubcategoriesEntity.getBody().getContent().size(), equalTo(1));

        /* Test the effects of removing the S Node (Category) */
        oAuth2AdminRestTemplate().delete(ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL,
                serverPort, rootCategoryId, serverPort, rootSubcategoryId);

        final ResponseEntity<Resources<CategoryDto>> receivedChildSubcategoriesEntity =
                restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootSubcategoryId);

        assertThat(receivedChildSubcategoriesEntity.getBody().getContent().size(), equalTo(2));

        final ResponseEntity<Resources<CategoryDto>> receivedRootSubcategoriesEntity1 =
                restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

        assertThat(receivedRootSubcategoriesEntity1.getBody().getContent().size(), equalTo(0));
    }

    @Test
    public void shouldThrowExceptionWhenAddingTheSameSubcategoryTwiceTest() {
        final long rootCategoryId = getIdFromLocationUrl(addNewTestCategory(DtoTestType.NEXT).getHeaders().getLocation().toString());
        final long subcategoryId = getIdFromLocationUrl(addNewTestCategory(DtoTestType.NEXT).getHeaders().getLocation().toString());

        final ResponseEntity<Object> addSubcategoryResponseEntity = oAuth2AdminRestTemplate().postForEntity(
                ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL, null, null,
                serverPort, rootCategoryId, serverPort, subcategoryId);

        assertThat(addSubcategoryResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        try {
            oAuth2AdminRestTemplate().postForEntity(
                    ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL, null, null,
                    serverPort, rootCategoryId, serverPort, subcategoryId);
            fail();
        } catch (HttpClientErrorException httpClientErrorException) {
            // then
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));

            final ResponseEntity<Resources<CategoryDto>> receivedRootSubcategoriesEntity =
                    restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

            assertThat(receivedRootSubcategoriesEntity.getBody().getContent().size(), equalTo(1));
        }
    }

    @Test
    public void shouldRemoveAllReferencesInTheParentCategoryAfterDeletingItsSubcategoryTest() {
        final long rootCategoryId = getIdFromLocationUrl(addNewTestCategory(DtoTestType.NEXT).getHeaders().getLocation().toString());
        final long subcategoryId = getIdFromLocationUrl(addNewTestCategory(DtoTestType.NEXT).getHeaders().getLocation().toString());

        final ResponseEntity<Object> addSubcategoryResponseEntity = oAuth2AdminRestTemplate().postForEntity(
                ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL, null, null,
                serverPort, rootCategoryId, serverPort, subcategoryId);

        assertThat(addSubcategoryResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final ResponseEntity<Resources<CategoryDto>> receivedSubcategoriesEntity =
                restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

        assertThat(receivedSubcategoriesEntity.getBody().getContent().size(), equalTo(1));

        oAuth2AdminRestTemplate().delete(CATEGORY_BY_ID_URL, serverPort, subcategoryId);

        final ResponseEntity<Resources<CategoryDto>> receivedRootSubcategoriesEntity2 =
                restTemplate.exchange(ApiTestBase.SUBCATEGORY_IN_CATEGORY_BY_ID_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<CategoryDto>>() {}, serverPort, rootCategoryId);

        assertThat(receivedRootSubcategoriesEntity2.getBody().getContent().size(), equalTo(0));
    }


    /* ----------------------------- SUBCATEGORIES TESTS ----------------------------- */


    /* ----------------------------- HELPER METHODS ----------------------------- */

    private void cleanupCategoryTests() {
        /* If there is still any test row in a database, delete it */
        removeLocalTestCategories();
    }
}