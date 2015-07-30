package pl.touk.widerest.catalog;

import org.hamcrest.number.IsCloseTo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.base.DtoTestType;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * Created by mst on 28.07.15.
 */
@SpringApplicationConfiguration(classes = Application.class)
public class CatalogTest extends ApiTestBase {


    /* (mst) Tests involving the entire catalog (eg: create a category -> create a product -> add it to the category ->
             add 2 additional SKUS -> ...
             go here
     */

    private HttpHeaders httpRequestHeader;
    private HttpEntity<String> httpRequestEntity;

    @Before
    public void initCatalogTests() {
        this.httpRequestHeader = new HttpHeaders();
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestEntity = new HttpEntity<>(null, httpRequestHeader);
        /* uncomment the following for "local" testing */
        serverPort = String.valueOf(8080);
        cleanupCatalogTests();
    }

    @Test
    public void exemplaryCatalogFlow1Test() {

        long currentGlobalProductCount = getRemoteTotalProductsCount();

        //add test category
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(
                CATEGORIES_URL,
                categoryDto, null, serverPort);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        long currentTotalProductsInTestCategoryCount = getRemoteTotalProductsInCategorCount(testCategoryId);

        assertThat(currentTotalProductsInTestCategoryCount, equalTo(0L));

        //add test product with default sku into category

        ProductDto productDto1 = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.SAME);

        ResponseEntity<ProductDto> remoteAddProduct1Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto1, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testProductId1 = getIdFromLocationUrl(remoteAddProduct1Entity.getHeaders().getLocation().toString());

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 1));
        assertThat(getRemoteTotalProductsInCategorCount(testCategoryId), equalTo(1L));

        //validate default sku

        ResponseEntity<SkuDto> receivedSkuEntity = restTemplate.exchange(
                PRODUCT_BY_ID_SKUS_DEFAULT, HttpMethod.GET,
                httpRequestEntity, SkuDto.class, serverPort, testProductId1);

        assertThat(receivedSkuEntity.getStatusCode(), equalTo(HttpStatus.OK));

        SkuDto receivedSkuDto = receivedSkuEntity.getBody();
        SkuDto defaultTestSku = DtoTestFactory.getTestDefaultSku();

        assertThat(receivedSkuDto.getName(), equalTo(defaultTestSku.getName()));
        assertThat(receivedSkuDto.getQuantityAvailable(), equalTo(defaultTestSku.getQuantityAvailable()));
        //assertTrue(IsCloseTo.closeTo(receivedSkuDto.getSalePrice().doubleValue(), defaultTestSku.getSalePrice().doubleValue()));
        assertThat(receivedSkuDto.getActiveStartDate(), equalTo(defaultTestSku.getActiveStartDate()));

        //add another product without category

        ProductDto productDto2 = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);


        ResponseEntity<ProductDto> remoteAddProduct2Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto2, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testProductId2 = getIdFromLocationUrl(remoteAddProduct2Entity.getHeaders().getLocation().toString());

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 2));
        assertThat(getRemoteTotalProductsInCategorCount(testCategoryId), equalTo(1L));

        //remove both products

        oAuth2AdminRestTemplate().delete(PRODUCTS_URL + "/" + testProductId1, serverPort);
        oAuth2AdminRestTemplate().delete(PRODUCTS_URL + "/" + testProductId2, serverPort);

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount));
        assertThat(getRemoteTotalProductsInCategorCount(testCategoryId), equalTo(0L));

        //remove category

        oAuth2AdminRestTemplate().delete(CATEGORIES_URL + "/" + testCategoryId, serverPort);

        /* TODO: (mst) maybe few other checks after removal */
    }

    @Test
    public void exemplaryCatalogFlow2Test() {
        // create N new Categories
        final long TEST_CATEGORIES_COUNT = 3;

        long currentTotalCategoriesCount = getRemoteTotalCategoriesCount();

        List<Long> newCategoriesIds = new ArrayList<>();

        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(
                    CATEGORIES_URL,
                    DtoTestFactory.getTestCategory(DtoTestType.NEXT), null, serverPort);

            assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

            newCategoriesIds.add(getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString()));
        }

        assertThat(getRemoteTotalCategoriesCount(), equalTo(currentTotalCategoriesCount + TEST_CATEGORIES_COUNT));

        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            assertThat(getRemoteTotalProductsInCategorCount(newCategoriesIds.get(i).longValue()), equalTo(0L));
        }


        // create a single product

        long currentTotalProductsCount = getRemoteTotalProductsCount();

        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);

        ResponseEntity<ProductDto> remoteAddProduct1Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentTotalProductsCount + 1));

        long testProductId = getIdFromLocationUrl(remoteAddProduct1Entity.getHeaders().getLocation().toString());

        //add product to all 3 Categories

        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, newCategoriesIds.get(i).longValue(), testProductId);
        }

        assertThat(getRemoteTotalCategoriesByProductCount(testProductId), equalTo(TEST_CATEGORIES_COUNT));

    }

    @Test
    public void exemplaryCatalogFlow3Test() {
        long currentGlobalProductCount = getRemoteTotalProductsCount();
        long currentGlobalCategoryCount = getRemoteTotalCategoriesCount();

        //add test category
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(
                CATEGORIES_URL,
                categoryDto, null, serverPort);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalCategoriesCount(), equalTo(currentGlobalCategoryCount + 1));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());
        long currentTotalProductsInTestCategoryCount = getRemoteTotalProductsInCategorCount(testCategoryId);

        assertThat(currentTotalProductsInTestCategoryCount, equalTo(0L));

        // create a product and assign it to that category

        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);

        ResponseEntity<ProductDto> remoteAddProduct1Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 1));

        long testProductId = getIdFromLocationUrl(remoteAddProduct1Entity.getHeaders().getLocation().toString());

        oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId, testProductId);

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 1));
        assertThat(getRemoteTotalProductsInCategorCount(testCategoryId), equalTo(currentTotalProductsInTestCategoryCount + 1));

        // create N skus

        final long TEST_SKUS_COUNT = 5;

        long currentSkusForProductCount = getRemoteTotalSkusForProductCount(testProductId);

        for(int i = 0; i < TEST_SKUS_COUNT; i++) {
                oAuth2AdminRestTemplate().postForEntity(
                        PRODUCT_BY_ID_SKUS,
                        DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT),
                        null,
                        serverPort,
                        testProductId);
        }

        assertThat(getRemoteTotalSkusForProductCount(testProductId), equalTo(currentSkusForProductCount + TEST_SKUS_COUNT));

        // validate everything
    }

    private void cleanupCatalogTests() {
        removeRemoteTestProducts();
        removeRemoteTestCategory();
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
    private void removeRemoteTestProducts() {

        ResponseEntity<ProductDto[]> receivedProductEntity = hateoasRestTemplate().exchange(PRODUCTS_URL,
                HttpMethod.GET, httpRequestEntity, ProductDto[].class, serverPort);

        assertNotNull(receivedProductEntity);
        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        for(ProductDto testProduct : receivedProductEntity.getBody()) {
            if(testProduct.getName().startsWith(DtoTestFactory.TEST_PRODUCT_DEFAULT_NAME)) {
                oAuth2AdminRestTemplate().delete(testProduct.getId().getHref(), 1);
            }
        }
    }

    private long getRemoteTotalProductsCount() {

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    private long getRemoteTotalProductsInCategorCount(long categoryId) {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_IN_CATEGORY_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort, categoryId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    public long getRemoteTotalCategoriesCount() {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    public long getRemoteTotalCategoriesByProductCount(long productId) {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_BY_PRODUCT_BY_ID_COUNT,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    private long getRemoteTotalSkusForProductCount(long productId) {

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(SKUS_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

}
