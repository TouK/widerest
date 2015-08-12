package pl.touk.widerest.catalog;

import org.springframework.transaction.annotation.Transactional;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;
import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.thymeleaf.util.StringUtils;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.base.DtoTestType;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class CategoryControllerTest extends ApiTestBase {

    private HttpHeaders httpRequestHeader;
    private HttpEntity<String> httpRequestEntity;

    @Before
    public void initCategoryTests() {
        this.httpRequestHeader = new HttpHeaders();
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestEntity = new HttpEntity<>(null, httpRequestHeader);
        /* uncomment the following for "local" testing */
        //serverPort = String.valueOf(8080);


        cleanupCategoryTests();
    }

    @Test
    public void localAndRemoteCountValuesAreEqualTest() {
        //when
        long remoteTotalCategoriesCount = getRemoteTotalCategoriesCountValue();
        //then
        assertThat(remoteTotalCategoriesCount, equalTo(getLocalTotalCategoriesCountValue()));
    }

    @Test
    public void addingNewCategoryIncreasesTotalCategoriesCountTest() {
        //when
        long currentCategoryCount = getRemoteTotalCategoriesCountValue();
        addNewTestCategory(DtoTestType.SAME);
        //then
        assertThat(getRemoteTotalCategoriesCountValue(), equalTo(currentCategoryCount + 1));
    }

    @Test
    public void addingDuplicateCategoryDoesNotIncreaseTotalCountNumberTest() {

        addNewTestCategory(DtoTestType.SAME);
        long currentCategoryCount = getRemoteTotalCategoriesCountValue();

        try {
            // when
            addNewTestCategory(DtoTestType.SAME);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            // then
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
            assertThat(getRemoteTotalCategoriesCountValue(), equalTo(currentCategoryCount));
        }
    }

    @Test
    public void numberOfRemotelyRetrievedCategoriesEqualsLocalyStoredCountTest() {
        //when
        ResponseEntity<CategoryDto[]> receivedCategoriesEntity =
                restTemplate.getForEntity(ApiTestBase.CATEGORIES_URL, CategoryDto[].class, serverPort);
        //then
        assertThat(receivedCategoriesEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat((long) receivedCategoriesEntity.getBody().length, equalTo(getLocalTotalCategoriesCountValue()));

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
        CategoryDto testCategoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);

        //when
        ResponseEntity<?> newCategoryResponseHeaders = addNewTestCategory(DtoTestType.SAME);

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
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));

        }
    }


    @Test
    public void modifyingExistingCategoryDoesNotCreateANewOneInsteadTest() {
        /* Create a test category */
        ResponseEntity<?> addNewCategoryResponse = addNewTestCategory(DtoTestType.SAME);
        assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);
        String createdCategoryLocationUri = addNewCategoryResponse.getHeaders().getLocation().toString();

        long currentCategoriesCount = getRemoteTotalCategoriesCountValue();

        //when
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);
        categoryDto.setDescription("ModifiedTestCategoryDescription");
        categoryDto.setName("ModifiedTestCategoryName2");
        categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

        oAuth2AdminRestTemplate().put(createdCategoryLocationUri, categoryDto, serverPort);

        //then
        assertThat(getRemoteTotalCategoriesCountValue(), equalTo(currentCategoriesCount));

    }

    @Test
    public void modifyingExistingCategoryDoesActuallyModifyItsValuesTest() {
        /* Create a test category */
        ResponseEntity<?> addNewCategoryResponse = addNewTestCategory(DtoTestType.SAME);
        assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);
        String createdCategoryLocationUri = addNewCategoryResponse.getHeaders().getLocation().toString();

        long currentCategoriesCount = getRemoteTotalCategoriesCountValue();

        //when
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);
        categoryDto.setDescription("ModifiedTestCategoryDescription");
        categoryDto.setName("ModifiedTestCategoryName");
        categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

        oAuth2AdminRestTemplate().put(createdCategoryLocationUri, categoryDto, serverPort);

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(createdCategoryLocationUri, CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(getRemoteTotalCategoriesCountValue(), equalTo(currentCategoriesCount));

        //then
        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
        assertThat(categoryDto.getName(), equalTo(receivedCategoryDto.getName()));
        assertThat(categoryDto.getDescription(), equalTo(receivedCategoryDto.getDescription()));
        assertThat(categoryDto.getLongDescription(), equalTo(receivedCategoryDto.getLongDescription()));
    }


    @Test
    @Transactional
    public void addingNewProductWithDefaultSKUToExistingCategoryIncreasesProductsCountTest() {

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(
                CATEGORIES_URL,
                categoryDto, null, serverPort);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        long currentProductsInCategoryRemoteCount = getRemoteTotalProductsInCategoryCountValue(testCategoryId);
        assertThat(currentProductsInCategoryRemoteCount, equalTo(0L));

        //when
        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.SAME);

        ResponseEntity<ProductDto> remoteAddProduct1Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        //then

        long newProductsInCategoryLocalCount = catalogService.findCategoryById(testCategoryId).getAllProductXrefs().size();
        assertThat(newProductsInCategoryLocalCount, equalTo(currentProductsInCategoryRemoteCount + 1));
    }

    @Test
    @Ignore("Do not forget to implement me! You should probably move me to ProductControllerTest as well! :)")
    public void addNewProductToNewCategoryAndCheckIfItExists() {

        ResponseEntity<?> addNewCategoryResponse = addNewTestCategory(DtoTestType.SAME);

        assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);

        String createdCategoryLocationUri = addNewCategoryResponse.getHeaders().getLocation().toString();

        assertNotNull(createdCategoryLocationUri);
        assertTrue(!createdCategoryLocationUri.isEmpty());

        String[] createdCategoryLocation = StringUtils.split(createdCategoryLocationUri, "/");

        long createdCategoryId = Long.parseLong(createdCategoryLocation[createdCategoryLocation.length - 1]);

        long currentProductsCount = getRemoteTotalProductsInCategoryCountValue(createdCategoryId);

        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.SAME);

        ResponseEntity<ProductDto> remoteAddProductEntity = oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL + "/{p}/products", productDto, null, serverPort, createdCategoryId);

        assertNotNull(remoteAddProductEntity);
        assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        assertThat(getRemoteTotalProductsInCategoryCountValue(createdCategoryId), equalTo(currentProductsCount + 1));
    }


    @Test
    @Ignore
    public void partialUpdateCategoryDescriptionAndCheckIfOtherValuesPreserveTest() {
        long currentTotalCategoriesCount = getRemoteTotalCategoriesCountValue();

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);

        assertTrue(remoteAddCategoryEntity.getStatusCode() == HttpStatus.CREATED);
        assertThat(getRemoteTotalCategoriesCountValue(), equalTo(currentTotalCategoriesCount + 1));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());


        categoryDto.setName("Category Name Changed!");

        final HttpEntity<CategoryDto> requestEntity = new HttpEntity<>(categoryDto);

        ResponseEntity<Void> responseCategoryPatchEntity = oAuth2AdminRestTemplate().exchange(
                CATEGORY_BY_ID_URL, HttpMethod.PATCH, requestEntity, Void.class, serverPort, testCategoryId);

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode().value(), equalTo(200));

        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
        assertThat(categoryDto.getName(), equalTo(receivedCategoryDto.getName()));
        assertThat(categoryDto.getDescription(), equalTo(receivedCategoryDto.getDescription()));
        assertThat(categoryDto.getLongDescription(), equalTo(receivedCategoryDto.getLongDescription()));

    }




    /* -----------------------------END OF TESTS----------------------------- */


    /* ----------------------------- HELPER METHODS ----------------------------- */

    private void cleanupCategoryTests() {
        /* If there is still any test row in a database, delete it */
        removeRemoteTestCategories();
    }

    private long getLocalTotalCategoriesCountValue() {
        return catalogService.findAllCategories().stream()
                .filter(entity -> ((Status)entity).getArchived() == 'N')
                .count();
    }


    private long getRemoteTotalProductsInCategoryCountValue(long categoryId) {

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_IN_CATEGORY_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort, categoryId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    public long getRemoteTotalCategoriesCountValue() {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    private void removeRemoteTestCategories() {

        /* (mst) Retrieve all categories */
        ResponseEntity<CategoryDto[]> receivedCategoriesEntity =
                restTemplate.getForEntity(CATEGORIES_URL, CategoryDto[].class, serverPort);

        assertNotNull(receivedCategoriesEntity);
        assertThat(receivedCategoriesEntity.getStatusCode(), equalTo(HttpStatus.OK));

        /* (mst) Remove those, created by tests. Btw: REFACTOR TO LAMBDA */
        for(CategoryDto testCategory : receivedCategoriesEntity.getBody()) {
            if(testCategory.getName().contains(DtoTestFactory.TEST_CATEGORY_DEFAULT_NAME)) {
                oAuth2AdminRestTemplate().delete(testCategory.getId().getHref());
            }
        }
    }

    private ResponseEntity<?> addNewTestCategory(DtoTestType dtoTestType) throws HttpClientErrorException {
        return oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL, DtoTestFactory.getTestCategory(dtoTestType), null, serverPort);
    }
}
