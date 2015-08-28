package pl.touk.widerest.catalog;

import org.hamcrest.number.IsCloseTo;
import org.junit.Before;
import org.junit.Ignore;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    /*
     *  1. Adds a new category
     *  2. Adds a new product to that category
     *  3. Validates that the default SKU, that comes with that product, contains proper values
     *  4. Adds a new product without category
     *  5. Removes both products and a category + validates everything is in the same state as at the beginning
     */
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

        assertThat(getRemoteTotalCategoriesCount(), equalTo(currentGlobalCategoriesCount));
    }


    /* 1. Adds TEST_CATEGORIES_COUNT new categories
       2. Adds a new product and inserts it into all test categories
     * 3. Removes test categories from catalog and checks:
     *                 a) if the test product still exists
     *                 b) if the categories count referenced by test product decreases
     */
    @Test
    @Transactional
    public void removingCategoriesFromCatalogDoesNotRemoveProductThatIsInThemTest() {
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

        //add product to all N Categories

        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, newCategoriesIds.get(i).longValue(), testProductId);
        }

        assertThat(getRemoteTotalCategoriesForProductCount(testProductId), equalTo(TEST_CATEGORIES_COUNT));

        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            oAuth2AdminRestTemplate().delete(CATEGORY_BY_ID_URL, serverPort, newCategoriesIds.get(i).longValue());

            ResponseEntity<ProductDto> receivedProductEntity = restTemplate.exchange(
                    PRODUCT_BY_ID_URL,
                    HttpMethod.GET, getHttpJsonRequestEntity(), ProductDto.class, serverPort, testProductId);

            assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));
            assertThat(getRemoteTotalCategoriesForProductCount(testProductId), equalTo(TEST_CATEGORIES_COUNT - (i + 1)));
        }

        assertThat(getRemoteTotalCategoriesForProductCount(testProductId), equalTo(0L));

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

        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId), equalTo(0L));


        oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId, testProductId);

        try {
            oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId, testProductId);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 1));
            assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
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
        /* (msT) we cant get access to SKUs via REST API after deleting the product therefore we
                 will you the local service
         */
        //assertThat(getLocalTotalSkusForProductCount(testProductId), equalTo(currentSkusForProductCount));

        // Category is still "there" with no reference to Test Product
        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

        assertNotNull(receivedCategoryEntity);
        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId), equalTo(0L));
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

        for(int i = 0; i < PRODUCT_COUNT; i++) {

            ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
            productDto.setCategoryName(testCategory.getName());

            ResponseEntity<ProductDto> remoteAddProductEntity = oAuth2AdminRestTemplate().postForEntity(
                    PRODUCTS_URL,
                    productDto, null, serverPort);

            assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));


        }



        assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo((long)PRODUCT_COUNT));

        CategoryDto categoryDto = DtoTestFactory.getTestCategory(DtoTestType.SAME);
        categoryDto.setDescription("ModifiedTestCategoryDescription");
        categoryDto.setName("ModifiedTestCategoryName");
        categoryDto.setLongDescription("ModifiedTestCategoryLongDescription");

        oAuth2AdminRestTemplate().put(newCategoryEntity.getHeaders().getLocation().toString(), categoryDto, serverPort);

        assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo((long)PRODUCT_COUNT));
    }

    @Test
    @Transactional
    public void modifyingfExistingCategoryDoesNotBreakReferencesToAndFromProductsTest() {
        CategoryDto testCategory = DtoTestFactory.getTestCategory(DtoTestType.NEXT);

        long currentGlobalCategoryCount = getRemoteTotalCategoriesCount();

        ResponseEntity<?> newCategoryEntity = addNewTestCategory(testCategory);

        assertThat(newCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long testCategoryId = getIdFromLocationUrl(newCategoryEntity.getHeaders().getLocation().toString());

        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);

        productDto.setCategoryName(testCategory.getName());

        ResponseEntity<ProductDto> remoteAddProduct1Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testProductId1 = getIdFromLocationUrl(remoteAddProduct1Entity.getHeaders().getLocation().toString());

        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
        assertThat(getRemoteTotalCategoriesForProductCount(testProductId1), equalTo(1L));

        testCategory.setDescription("ModifiedTestCategoryDescription2");
        testCategory.setName("ModifiedTestCategoryName2");
        testCategory.setLongDescription("ModifiedTestCategoryLongDescription2");

        Map<String, String> categoryAttributes = new HashMap<>();
        categoryAttributes.put("size", String.valueOf(99));
        categoryAttributes.put("color", "red");
        categoryAttributes.put("length", String.valueOf(12.222));

        testCategory.setAttributes(categoryAttributes);

        oAuth2AdminRestTemplate().put(CATEGORY_BY_ID_URL, testCategory, serverPort, testCategoryId);

        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
        assertThat(getRemoteTotalCategoriesForProductCount(testProductId1), equalTo(1L));
    }


    @Test
    @Transactional
    @Ignore
    public void creatingAndDeletingCategoriesReferencesDoesNotAffectActualEntitiesTest() {
        CategoryDto testCategory1 = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        ResponseEntity<?> newCategoryEntity1 = addNewTestCategory(testCategory1);
        assertThat(newCategoryEntity1.getStatusCode(), equalTo(HttpStatus.CREATED));
        long testCategoryId1 = getIdFromLocationUrl(newCategoryEntity1.getHeaders().getLocation().toString());

        CategoryDto testCategory2 = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        ResponseEntity<?> newCategoryEntity2 = addNewTestCategory(testCategory2);
        assertThat(newCategoryEntity2.getStatusCode(), equalTo(HttpStatus.CREATED));
        long testCategoryId2 = getIdFromLocationUrl(newCategoryEntity2.getHeaders().getLocation().toString());

        final List<Long> newProductsIds = new ArrayList<>();

        for(int i = 0; i < 3; i++) {
            ResponseEntity<ProductDto> remoteAddProductEntity= oAuth2AdminRestTemplate().postForEntity(
                    PRODUCTS_URL,
                    DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT), null, serverPort);

            assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

            newProductsIds.add(getIdFromLocationUrl(remoteAddProductEntity.getHeaders().getLocation().toString()));
        }

        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId1), equalTo(0L));
        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId2), equalTo(0L));

        // first product references to the first category + should throw exception trying to "add" this product twice
        oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId1, newProductsIds.get(0));

        try {
            oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId1, newProductsIds.get(0));
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId1), equalTo(1L));
        }

        // second product references to first category
        oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId1, newProductsIds.get(1));
        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId1), equalTo(2L));

        assertThat(getRemoteTotalCategoriesForProductCount(newProductsIds.get(0)), equalTo(1L));
        assertThat(getRemoteTotalCategoriesForProductCount(newProductsIds.get(1)), equalTo(1L));


        // third product references to both categories
        oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId1, newProductsIds.get(2));
        oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId2, newProductsIds.get(2));

        assertThat(getRemoteTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(2L));

        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId1), equalTo(3L));
        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId2), equalTo(1L));

        // remove reference to second product from first category + "remove non existing reference check"
        oAuth2AdminRestTemplate().delete(PRODUCTS_IN_CATEGORY_BY_ID_URL, serverPort, testCategoryId1, newProductsIds.get(1));
        try {
            oAuth2AdminRestTemplate().delete(PRODUCTS_IN_CATEGORY_BY_ID_URL, serverPort, testCategoryId1, newProductsIds.get(1));
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId1), equalTo(2L));
        }

        assertThat(getLocalTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(2L));
        assertThat(getLocalTotalCategoriesForProductCount(newProductsIds.get(0)), equalTo(1L));
        assertThat(getLocalTotalCategoriesForProductCount(newProductsIds.get(1)), equalTo(0L));


        // remove reference to first product from first category
        oAuth2AdminRestTemplate().delete(PRODUCTS_IN_CATEGORY_BY_ID_URL, serverPort, testCategoryId1, newProductsIds.get(0));

        // add first and second products to second category
        oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId2, newProductsIds.get(0));
        oAuth2AdminRestTemplate().put(PRODUCTS_IN_CATEGORY_BY_ID_URL, null, serverPort, testCategoryId2, newProductsIds.get(1));


        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId1), equalTo(1L));
        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId2), equalTo(3L));

        assertThat(getLocalTotalCategoriesForProductCount(newProductsIds.get(0)), equalTo(1L));
        assertThat(getLocalTotalCategoriesForProductCount(newProductsIds.get(1)), equalTo(1L));
        assertThat(getLocalTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(2L));

        // remove 3rd product from first category
        oAuth2AdminRestTemplate().delete(PRODUCTS_IN_CATEGORY_BY_ID_URL, serverPort, testCategoryId1, newProductsIds.get(2));
        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId1), equalTo(0L));
        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId2), equalTo(3L));

        assertThat(getRemoteTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(1L));
    }




    /* ------------------ HELPER METHODS -------------------*/

    private void cleanupCatalogTests() {
        removeLocalTestProducts();
        removeLocalTestCategories();
    }

}