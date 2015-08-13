package pl.touk.widerest.catalog;

import org.hamcrest.number.IsCloseTo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;


/**
 * Created by mst on 28.07.15.
 */
@SpringApplicationConfiguration(classes = Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class CatalogTest extends ApiTestBase {


    /* (mst) Tests involving the entire catalog (eg: create a category -> create a product -> add it to the category ->
             add 2 additional SKUS -> ...
             go here
     */

    @Before
    public void initCatalogTests() {
        /* uncomment the following for "local" testing */
        //serverPort = String.valueOf(8080);

        cleanupCatalogTests();
    }

    @Test
    @Transactional
    public void exemplaryCatalogFlow1Test() {

        long currentGlobalProductCount = getRemoteTotalProductsCount();
        long currentGlobalCategoriesCount = getRemoteTotalCategoriesCount();

        //add test category
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(
                CATEGORIES_URL,
                categoryDto, null, serverPort);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        long currentProductsInCategoryRemoteCount = getRemoteTotalProductsInCategoryCount(testCategoryId);
        assertThat(currentProductsInCategoryRemoteCount, equalTo(0L));

        //add test product with default sku into category

        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);

        productDto.setCategoryName(categoryDto.getName());

        ResponseEntity<ProductDto> remoteAddProduct1Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testProductId1 = getIdFromLocationUrl(remoteAddProduct1Entity.getHeaders().getLocation().toString());


        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 1));
        assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount + 1));

        //validate default sku

        ResponseEntity<SkuDto> receivedSkuEntity = restTemplate.exchange(
                PRODUCT_BY_ID_SKUS_DEFAULT, HttpMethod.GET,
                getHttpJsonRequestEntity(), SkuDto.class, serverPort, testProductId1);

        assertThat(receivedSkuEntity.getStatusCode(), equalTo(HttpStatus.OK));

        SkuDto receivedSkuDto = receivedSkuEntity.getBody();
        SkuDto defaultTestSku = DtoTestFactory.getTestDefaultSku();

        assertThat(receivedSkuDto.getName(), containsString(defaultTestSku.getName()));
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
        assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount + 1));


        //remove both products

        oAuth2AdminRestTemplate().delete(PRODUCTS_URL + "/" + testProductId1, serverPort);
        oAuth2AdminRestTemplate().delete(PRODUCTS_URL + "/" + testProductId2, serverPort);

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount));
        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount));

        //assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo(0L));

        //remove category

        oAuth2AdminRestTemplate().delete(CATEGORIES_URL + "/" + testCategoryId, serverPort);

        /* TODO: (mst) maybe few other checks after removal */

        assertThat(getRemoteTotalCategoriesCount(), equalTo(currentGlobalCategoriesCount));
    }

    @Test
    @Transactional
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
            assertThat(getLocalTotalProductsInCategoryCount(newCategoriesIds.get(i)), equalTo(0L));
        }


        // create a single product

        long currentTotalProductsCount = getRemoteTotalProductsCount();

        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

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
    @Transactional
    public void deletingProductRemovesAllSkusAndCategoriesReferencesTest() {
        long currentGlobalProductCount = getRemoteTotalProductsCount();
        long currentGlobalCategoryCount = getRemoteTotalCategoriesCount();

        //add test category
        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(
                CATEGORIES_URL,
                categoryDto, null, serverPort);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalCategoriesCount(), equalTo(currentGlobalCategoryCount + 1));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());
        assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo(0L));

        // create a product and assign it to that category

        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        //productDto.setCategoryName(categoryDto.getName());
        ResponseEntity<ProductDto> remoteAddProduct1Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 1));

        long testProductId = getIdFromLocationUrl(remoteAddProduct1Entity.getHeaders().getLocation().toString());

        oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null , serverPort, testCategoryId, testProductId);


        try {
            oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId, testProductId);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 1));
            assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
        }

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

        // delete product

        oAuth2AdminRestTemplate().delete(PRODUCT_BY_ID_URL, serverPort, testProductId);

        // validate catalog state after removal

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount));
        assertThat(getRemoteTotalSkusForProductCount(testProductId), equalTo(currentSkusForProductCount));

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

    }

    @Test
    @Transactional
    public void modifyingExistingCategoryDoesNotAffectItsProductsTest() {

        CategoryDto testCategory = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        long currentGlobalCategoryCount = getRemoteTotalCategoriesCount();

        ResponseEntity<?> newCategoryEntity = addNewTestCategory(testCategory);

        assertThat(newCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalCategoriesCount(), equalTo(currentGlobalCategoryCount + 1));

        long testCategoryId = getIdFromLocationUrl(newCategoryEntity.getHeaders().getLocation().toString());

        final int PRODUCT_COUNT = 4;

        List<Long> newProductsIds = new ArrayList<>();

        for(int i = 0; i < PRODUCT_COUNT; i++) {

            ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
            productDto.setCategoryName(testCategory.getName());

            ResponseEntity<ProductDto> remoteAddProductEntity = oAuth2AdminRestTemplate().postForEntity(
                    PRODUCTS_URL,
                    productDto, null, serverPort);

            assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

            newProductsIds.add(getIdFromLocationUrl(remoteAddProductEntity.getHeaders().getLocation().toString()));
        }



        assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo((long)PRODUCT_COUNT));

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);
        categoryDto.setDescription("ModifiedTestCategoryDescription");
        categoryDto.setName("ModifiedTestCategoryName");
        categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

        oAuth2AdminRestTemplate().put(newCategoryEntity.getHeaders().getLocation().toString(), categoryDto, serverPort);

        assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo((long)PRODUCT_COUNT));
    }


    /* ------------------ HELPER METHODS -------------------*/

    private void cleanupCatalogTests() {
        removeLocalTestProducts();
        removeLocalTestCategories();
    }


    public long getRemoteTotalCategoriesByProductCount(long productId) {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_BY_PRODUCT_BY_ID_COUNT,
                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    private long getRemoteTotalSkusForProductCount(long productId) {

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(SKUS_COUNT_URL,
                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }



}
