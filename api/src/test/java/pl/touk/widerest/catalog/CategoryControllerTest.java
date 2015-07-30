package pl.touk.widerest.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.core.AnnotationRelProvider;
import org.springframework.hateoas.core.DefaultRelProvider;
import org.springframework.hateoas.core.DelegatingRelProvider;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
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

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

//@RunWith(SpringJUnit4ClassRunner.class)
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
        serverPort = String.valueOf(8080);
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
    public void addingNewCategoryIncreasesTotalCountNumberTest() {
        //when
        long currentCategoryCount = getRemoteTotalCategoriesCountValue();
        addNewTestCategory();
        //then
        assertThat(getRemoteTotalCategoriesCountValue(), equalTo(currentCategoryCount + 1));
    }

    @Test(expected = HttpClientErrorException.class)
    public void addingDuplicateCategoryDoesNotIncreaseTotalCountNumberTest() {
        //when
        addNewTestCategory();
        long currentCategoryCount = getRemoteTotalCategoriesCountValue();
        addNewTestCategory();
        //then
        assertThat(getRemoteTotalCategoriesCountValue(), equalTo(currentCategoryCount));

    }

    @Test
    public void numberOfRemotelyRetrievedCategoriesEqualsLocalyStoredCountTest() {
        //when
        ResponseEntity<CategoryDto[]> receivedCategoriesEntity =
                restTemplate.getForEntity(ApiTestBase.CATEGORIES_URL, CategoryDto[].class, serverPort);
        //then
        assertThat(receivedCategoriesEntity.getStatusCode().value(), equalTo(200));
        assertThat((long) receivedCategoriesEntity.getBody().length, equalTo(getLocalTotalCategoriesCountValue()));

    }



    @Test
    public void remotelyRetrievedCategoryEqualsLocalyRetrievedOneTest() {
        /* retrieve the IDs of all available, non-archived categories */
        List<Long> localCategoryIds = catalogService.findAllCategories().stream()
                .filter(entity -> ((Status) entity).getArchived() == 'N')
                .map(id -> id.getId()).collect(Collectors.toList());

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
        ResponseEntity<?> newCategoryResponseHeaders = addNewTestCategory();

        assertThat(newCategoryResponseHeaders.getStatusCode(), equalTo(HttpStatus.CREATED));

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(newCategoryResponseHeaders.getHeaders().getLocation().toString(), CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode().value(), equalTo(200));
        //then
        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
        assertThat(testCategoryDto.getName(), equalTo(receivedCategoryDto.getName()));
        assertThat(testCategoryDto.getDescription(), equalTo(receivedCategoryDto.getDescription()));
        assertThat(testCategoryDto.getLongDescription(), equalTo(receivedCategoryDto.getLongDescription()));
    }

    @Test(expected = HttpClientErrorException.class)
    public void successfullyDeletingANewlyCreatedCategoryTest() {

        /* Creating a new test category */
        CategoryDto categoryDto = CategoryDto.builder().name("testcategory").description("testcategory").build();

        ResponseEntity<CategoryDto> createdCategoryResponse = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);
        assertTrue(createdCategoryResponse.getStatusCode() == HttpStatus.CREATED);

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
        ResponseEntity<CategoryDto> receivedCategoryDtoAfterDelete = restTemplate.getForEntity(
                createdCategoryLocationUri, CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryDtoAfterDelete);

        assertThat(receivedCategoryDtoAfterDelete.getStatusCode(), equalTo(404));

        if(receivedCategoryDtoAfterDelete.getBody() != null) {
            assertTrue(!receivedCategoryDtoAfterDelete.getBody().getName().equals(categoryDto.getName()));
        }
    }


    @Test
    public void modifyingExistingCategoryDoesNotCreateANewOneInsteadTest() {
        /* Create a test category */
        ResponseEntity<?> addNewCategoryResponse = addNewTestCategory();
        assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);
        String createdCategoryLocationUri = addNewCategoryResponse.getHeaders().getLocation().toString();

        long currentCategoriesCount = getRemoteTotalCategoriesCountValue();

        //when
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);
        categoryDto.setDescription("ModifiedTestCategoryDescription");
        categoryDto.setName("ModifiedTestCategoryName");
        categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

        oAuth2AdminRestTemplate().put(createdCategoryLocationUri, categoryDto, serverPort);

        //then
        assertThat(getRemoteTotalCategoriesCountValue(), equalTo(currentCategoriesCount));

    }

    @Test
    public void modifyingExistingCategoryDoesActuallyModifyItsValuesTest() {
        /* Create a test category */
        ResponseEntity<?> addNewCategoryResponse = addNewTestCategory();
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
        assertThat(receivedCategoryEntity.getStatusCode().value(), equalTo(200));

        //then
        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();
        assertThat(categoryDto.getName(), equalTo(receivedCategoryDto.getName()));
        assertThat(categoryDto.getDescription(), equalTo(receivedCategoryDto.getDescription()));
        assertThat(categoryDto.getLongDescription(), equalTo(receivedCategoryDto.getLongDescription()));

    }

    @Test
    public void modifyingExistingCategoryDoesNotRemoveProductsFromIt() {

    }

    @Test
    @Transactional
    public void addingNewProductWithDefaultSKUToExistingCategoryIncreasesProductsCount() {

        List<Category> allCategories = catalogService.findAllCategories();

        if(allCategories.isEmpty()) {
            fail("No categories found?!");
        }

        Category firstCategory = allCategories.stream().filter(CatalogUtils::archivedCategoryFilter)
                .findFirst().get();

        //long currentProductsInCategoryCount = firstCategory.getAllProductXrefs().stream().count();
        long currentProductsInCategoryRemoteCount = getRemoteTotalProductsInCategorCountValue(firstCategory.getId());

        //assertThat(currentProductsInCategoryCount, equalTo(currentProductsInCategoryRemoteCount));

        ProductDto productToAddDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);

        ResponseEntity<ProductDto> remoteAddProductEntity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_IN_CATEGORY_URL, productToAddDto, null, serverPort, 701);

        assertNotNull(remoteAddProductEntity);
        assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        //long newProductsInCategoryCount = firstCategory.getAllProductXrefs().stream().count();
        long newProductsInCategoryRemoteCount = getRemoteTotalProductsInCategorCountValue(firstCategory.getId());

       // assertThat(newProductsInCategoryCount, equalTo(newProductsInCategoryRemoteCount));
        assertThat(newProductsInCategoryRemoteCount, equalTo(currentProductsInCategoryRemoteCount + 1));

    }

    @Test
    @Ignore("Do not forget to implement me! You should probably move me to ProductControllerTest as well! :)")
    public void addNewProductToNewCategoryAndCheckIfItExists() {

        ResponseEntity<?> addNewCategoryResponse = addNewTestCategory();

        assertTrue(addNewCategoryResponse.getStatusCode() == HttpStatus.CREATED);

        String createdCategoryLocationUri = addNewCategoryResponse.getHeaders().getLocation().toString();

        assertNotNull(createdCategoryLocationUri);
        assertTrue(!createdCategoryLocationUri.isEmpty());

        String[] createdCategoryLocation = StringUtils.split(createdCategoryLocationUri, "/");

        long createdCategoryId = Long.parseLong(createdCategoryLocation[createdCategoryLocation.length - 1]);

        long currentProductsCount = getRemoteTotalProductsInCategorCountValue(createdCategoryId);

        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.SAME);

        ResponseEntity<ProductDto> remoteAddProductEntity = oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL + "/{p}/products", productDto, null, serverPort, createdCategoryId);

        assertNotNull(remoteAddProductEntity);
        assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        assertThat(getRemoteTotalProductsInCategorCountValue(createdCategoryId), equalTo(currentProductsCount + 1));
    }

    @Test /* Duplicate check */
    public void addExistingProductToTheCategoryAndCheckIfCountDoesNotIncrease() {

    }





    /* -----------------------------END OF TESTS----------------------------- */
    private void cleanupCategoryTests() {
        /* If there is still any test row in a database, delete it */
        removeRemoteTestCategory();
    }

    private long getLocalTotalCategoriesCountValue() {
        return catalogService.findAllCategories().stream()
                .filter(entity -> ((Status)entity).getArchived() == 'N')
                .count();
    }


    private long getRemoteTotalProductsInCategorCountValue(long categoryId) {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_IN_CATEGORY_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort, categoryId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    public long getRemoteTotalCategoriesCountValue() {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    private void removeRemoteTestCategory() {

        ResponseEntity<CategoryDto[]> receivedCategoriesEntity =
                restTemplate.getForEntity(ApiTestBase.CATEGORIES_URL, CategoryDto[].class, serverPort);

        assertNotNull(receivedCategoriesEntity);
        assertThat(receivedCategoriesEntity.getStatusCode().value(), equalTo(200));

        for(CategoryDto testCategory : receivedCategoriesEntity.getBody()) {
            if(testCategory.getName().startsWith(DtoTestFactory.TEST_CATEGORY_DEFAULT_NAME)) {
                oAuth2AdminRestTemplate().delete(testCategory.getId().getHref(), 1);
            }
        }
    }


    private ResponseEntity<?> addNewTestCategory() throws HttpClientErrorException {

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);

        return remoteAddCategoryEntity;
    }

    /* (mst) meh.... */
    private ResponseEntity<?> addNewTestProduct(ProductDto productToAddDto) throws HttpClientErrorException {
        ResponseEntity<ProductDto> remoteAddProductEntity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_IN_CATEGORY_URL, productToAddDto, null, serverPort);

        return remoteAddProductEntity;

    }

}
