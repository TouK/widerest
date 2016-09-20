package pl.touk.widerest.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.skus.SkuDto;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.base.ApiTestUtils;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.security.oauth2.Scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
public class CatalogTest extends AbstractTest {


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
    public void exemplaryCatalogFlow1Test() throws Throwable {

        final long currentGlobalProductCount = catalogOperationsLocal.getTotalProductsCount();
        final long currentGlobalCategoriesCount = catalogOperationsLocal.getTotalCategoriesCount();

        // when: 1) adding a new test category
        final CategoryDto categoryDto = DtoTestFactory.categories().testCategoryDto();

        final ResponseEntity<?> remoteAddCategoryEntity = catalogOperationsRemote.addCategory(categoryDto);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testCategoryId = ApiTestUtils.getIdFromEntity(remoteAddCategoryEntity);

        // then: 1) the new category should not have any products
        final long currentProductsInCategoryRemoteCount = catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId);
        assertThat(currentProductsInCategoryRemoteCount, equalTo(0L));

        // when: 2) adding a new test product (with default SKU) into the category
        final ProductDto productDto = DtoTestFactory.products().testProductWithDefaultCategory(categoryDto.getName());

        final ResponseEntity<?> remoteAddProduct1Entity = catalogOperationsRemote.addProduct(productDto);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testProductId1 = ApiTestUtils.getIdFromEntity(remoteAddProduct1Entity);

        em.clear();

        // then: 2a) number of products in the test category should increase
        assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount + 1));


        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount + 1));

        final ResponseEntity<ProductDto> receivedProductEntity = backofficeRestTemplate.exchange(
                ApiTestUrls.PRODUCT_BY_ID_URL, HttpMethod.GET,
                null, ProductDto.class, serverPort, testProductId1);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        final Link defaultSkuLink = receivedProductEntity.getBody().getLink("default-sku");

        // then: 2b) product's default SKU should have proper values
        final ResponseEntity<SkuDto> receivedSkuEntity = backofficeRestTemplate.exchange(
                defaultSkuLink.getHref(), HttpMethod.GET,
                null, SkuDto.class, serverPort);

        assertThat(receivedSkuEntity.getStatusCode(), equalTo(HttpStatus.OK));

        final SkuDto receivedSkuDto = receivedSkuEntity.getBody();

        assertThat(receivedSkuDto.getName(), containsString(productDto.getName()));
        assertThat(receivedSkuDto.getQuantityAvailable(), equalTo(productDto.getQuantityAvailable()));
        assertThat(receivedSkuDto.getValidFrom(), equalTo(productDto.getValidFrom()));

        // when: 3) adding another product without category
        final ProductDto productDto2 = DtoTestFactory.products().getTestProductWithoutDefaultCategory();

        final ResponseEntity<?> remoteAddProduct2Entity = catalogOperationsRemote.addProduct(productDto2);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testProductId2 = ApiTestUtils.getIdFromEntity(remoteAddProduct2Entity);

        // then: 3) total number of products should increase BUT the number of products in test category should remain unchanged
        assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount + 2));
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount + 1));

        // when: 4) removing both products from catalog
        backofficeRestTemplate.delete(ApiTestUrls.PRODUCTS_URL + "/" + testProductId1, serverPort);
        backofficeRestTemplate.delete(ApiTestUrls.PRODUCTS_URL + "/" + testProductId2, serverPort);

        em.clear();

        // then: 4) total number of products in catalog should decrease by 2 AND total number of products in the test
        //          category should drop down to 0 (no products in category)
        assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount));
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(currentProductsInCategoryRemoteCount));


        // when: 5) removing test category from catalog
        backofficeRestTemplate.delete(ApiTestUrls.CATEGORIES_URL + "/" + testCategoryId, serverPort);

        // then: 5) the total number of categories should decrease by 1
        em.clear();

        assertThat(catalogOperationsLocal.getTotalCategoriesCount(), equalTo(currentGlobalCategoriesCount));
    }


    /* 1. Adds TEST_CATEGORIES_COUNT new categories
       2. Adds a new product and inserts it into all test categories
     * 3. Removes test categories from catalog and checks:
     *                 a) if the test product still exists
     *                 b) if the categories count referenced by test product decreases
     */
    @Test
    @Transactional
    public void removingCategoriesFromCatalogDoesNotRemoveProductThatIsInThemTest() throws Throwable {

        // when: 1) creating TEST_CATEGORIES_COUNT categories
        final long TEST_CATEGORIES_COUNT = 3;

        final long currentTotalCategoriesCount = catalogOperationsLocal.getTotalCategoriesCount();

        final List<Long> newCategoriesIds = new ArrayList<>();

        ResponseEntity<?> remoteAddCategoryEntity;

        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            remoteAddCategoryEntity = catalogOperationsRemote.addCategory(DtoTestFactory.categories().testCategoryDto());

            assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

            newCategoriesIds.add(ApiTestUtils.getIdFromEntity(remoteAddCategoryEntity));
        }

        // then: 1) total number of categories should increase by TEST_CATEGORIES_COUNT and all of the categories
        //          should not have any products in them
        assertThat(catalogOperationsLocal.getTotalCategoriesCount(), equalTo(currentTotalCategoriesCount + TEST_CATEGORIES_COUNT));

        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(newCategoriesIds.get(i)), equalTo(0L));
        }

        // when: 2) adding a new test product
        final long currentTotalProductsCount = catalogOperationsLocal.getTotalProductsCount();

        final ProductDto productDto = DtoTestFactory.products().getTestProductWithoutDefaultCategory();

        final ResponseEntity<?> remoteAddProduct1Entity = catalogOperationsRemote.addProduct(productDto);

        // then: 2) total number of products should increase
        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentTotalProductsCount + 1));

        final long testProductId = ApiTestUtils.getIdFromEntity(remoteAddProduct1Entity);

        // when: 3) inserting test product into all TEST_CATEGORIES_COUNT categories
        for(int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
            catalogOperationsRemote.addProductToCategoryReference(newCategoriesIds.get(i), testProductId);
        }

        // then: 3) product's categories number should be equal to TEST_CATEGORIES_COUNT (=> it has been inserted into all of them)
        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(testProductId), equalTo(TEST_CATEGORIES_COUNT));

        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            ResponseEntity<ProductDto> receivedProductEntity;

            // when: 4) deleting each of TEST_CATEGORIES_COUNT categories
            for (int i = 0; i < TEST_CATEGORIES_COUNT; i++) {
                adminRestTemplate.delete(ApiTestUrls.CATEGORY_BY_ID_URL, serverPort, newCategoriesIds.get(i));

                em.clear();

                // then: 4a) product's categories number should decrease (by 1) on each category deletion
                receivedProductEntity = backofficeRestTemplate.exchange(
                        ApiTestUrls.PRODUCT_BY_ID_URL,
                        HttpMethod.GET, null, ProductDto.class, serverPort, testProductId);

                assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));
                assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(testProductId), equalTo(TEST_CATEGORIES_COUNT - (i + 1)));
            }

            // then: 4b) product's categories number should equal to 0 after all categories have been removed
            assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(testProductId), equalTo(0L));
        });
    }

    @Test
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deletingProductRemovesAllSkusAndCategoriesReferencesTest() throws Throwable {
        // when: 1) creating a new test category and adding a new test product to it
        final long currentGlobalProductCount = catalogOperationsLocal.getTotalProductsCount();
        final long currentGlobalCategoryCount = catalogOperationsLocal.getTotalCategoriesCount();

        //add test category
        final CategoryDto categoryDto = DtoTestFactory.categories().testCategoryDto();

        final ResponseEntity<?> remoteAddCategoryEntity = catalogOperationsRemote.addCategory(categoryDto);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(catalogOperationsLocal.getTotalCategoriesCount(), equalTo(currentGlobalCategoryCount + 1));

        final long testCategoryId = ApiTestUtils.getIdFromEntity(remoteAddCategoryEntity);

        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(0L));

        final ResponseEntity<?> remoteAddProduct1Entity = catalogOperationsRemote.addProduct(DtoTestFactory.products().getTestProductWithoutDefaultCategory());

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount + 1));

        final long testProductId = ApiTestUtils.getIdFromEntity(remoteAddProduct1Entity);

        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(0L));

        catalogOperationsRemote.addProductToCategoryReference(testCategoryId, testProductId);

        // then: 1) total number of all products as well as products in test category should increase by 1
        try {
            catalogOperationsRemote.addProductToCategoryReference(testCategoryId, testProductId);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            em.clear();
            assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount + 1));
            assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
        }

        // when: 2) creating TEST_SKUS_COUNT additional SKUs and adding them to test product
        final long TEST_SKUS_COUNT = 5;

        final long currentSkusForProductCount = catalogOperationsLocal.getTotalSkusForProductCount(testProductId);

        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            for(int i = 0; i < TEST_SKUS_COUNT; i++) {
                adminRestTemplate.postForEntity(
                        ApiTestUrls.PRODUCT_BY_ID_SKUS,
                        DtoTestFactory.products().testSkuDto(),
                        null,
                        serverPort,
                        testProductId);
            }

            em.clear();

            // then: 2) total number of SKUs for test product should increase by TEST_SKUS_COUNT
            assertThat(catalogOperationsLocal.getTotalSkusForProductCount(testProductId), equalTo(currentSkusForProductCount + TEST_SKUS_COUNT));

            // when: 3) deleting test product
            adminRestTemplate.delete(ApiTestUrls.PRODUCT_BY_ID_URL, serverPort, testProductId);

            em.clear();

            // then: 3) total number of products should decrease by 1 and test category should not reference
            //          any products any longer
            assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentGlobalProductCount));

            final ResponseEntity<CategoryDto> receivedCategoryEntity =
                    adminRestTemplate.getForEntity(ApiTestUrls.CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, testCategoryId);

            assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

            assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(0L));
        });
    }

    @Test
    @Transactional
    public void modifyingExistingCategoryDoesNotAffectItsProductsTest() throws Throwable {

        final int PRODUCT_COUNT = 4;

        // when: 1) adding a test category
        final CategoryDto testCategory = DtoTestFactory.categories().testCategoryDto();

        final long currentGlobalCategoryCount = catalogOperationsLocal.getTotalCategoriesCount();

        final ResponseEntity<?> newCategoryEntity = catalogOperationsRemote.addCategory(testCategory);

        // then: 1) total number of categories should increase
        assertThat(newCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(catalogOperationsLocal.getTotalCategoriesCount(), equalTo(currentGlobalCategoryCount + 1));

        final long testCategoryId = ApiTestUtils.getIdFromEntity(newCategoryEntity);

        // when: 2) creating and inserting PRODUCT_COUNT products into test category
        ProductDto productDto;
        ResponseEntity<?> remoteAddProductEntity;

        for(int i = 0; i < PRODUCT_COUNT; i++) {
            productDto = DtoTestFactory.products().testProductWithDefaultCategory(testCategory.getName());

            remoteAddProductEntity = catalogOperationsRemote.addProduct(productDto);

            assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        }

        // then: 2) total number of products in test category should be equal to PRODUCT_COUNT
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo((long)PRODUCT_COUNT));

        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: 3) modifying test category values
//            final CategoryDto categoryDto = DtoTestFactory.testCategoryDto(DtoTestType.SAME);
            testCategory.setDescription("ModifiedTestCategoryDescription");
            testCategory.setName("ModifiedTestCategoryName");
            testCategory.setLongDescription("ModifiedTestCategoryLongDescription");

            adminRestTemplate.put(newCategoryEntity.getHeaders().getLocation().toString(), testCategory, serverPort);
        });

        // then: 3) test category does not "lose" its product references
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo((long)PRODUCT_COUNT));
    }

    @Test
    @Transactional
    public void modifyingfExistingCategoryDoesNotBreakReferencesToAndFromProductsTest() throws Throwable {
        // when: 1) adding a new category with a new product
        final CategoryDto testCategory = DtoTestFactory.categories().testCategoryDto();
        final ResponseEntity<?> newCategoryEntity = catalogOperationsRemote.addCategory(testCategory);
        assertThat(newCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testCategoryId = ApiTestUtils.getIdFromEntity(newCategoryEntity);

        final ProductDto productDto = DtoTestFactory.products().testProductWithDefaultCategory(testCategory.getName());

        final ResponseEntity<?> remoteAddProduct1Entity = catalogOperationsRemote.addProduct(productDto);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testProductId1 = ApiTestUtils.getIdFromEntity(remoteAddProduct1Entity);

        // then: 1) Total number of products in test category equals 1
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(testProductId1), equalTo(1L));

        // when: 2) modifying test category and adding 3 attributes to it
        testCategory.setDescription("ModifiedTestCategoryDescription2");
        testCategory.setName("ModifiedTestCategoryName2");
        testCategory.setLongDescription("ModifiedTestCategoryLongDescription2");

        final Map<String, String> categoryAttributes = new HashMap<>();
        categoryAttributes.put("size", String.valueOf(99));
        categoryAttributes.put("color", "red");
        categoryAttributes.put("length", String.valueOf(12.222));
        testCategory.setAttributes(categoryAttributes);

        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            adminRestTemplate.put(ApiTestUrls.CATEGORY_BY_ID_URL, testCategory, serverPort, testCategoryId);
        });

        // then: 2) modification does not change category's products
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(1L));
        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(testProductId1), equalTo(1L));
    }


    @Test
    @Transactional
    public void creatingAndDeletingCategoriesReferencesDoesNotAffectActualEntitiesTest() {
        // when: 1) adding 2 test categories and 3 test products
        final ResponseEntity<?> newCategoryEntity1 = catalogOperationsRemote.addCategory(DtoTestFactory.categories().testCategoryDto());
        assertThat(newCategoryEntity1.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long testCategoryId1 = ApiTestUtils.getIdFromEntity(newCategoryEntity1);

        final ResponseEntity<?> newCategoryEntity2 = catalogOperationsRemote.addCategory(DtoTestFactory.categories().testCategoryDto());
        assertThat(newCategoryEntity2.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long testCategoryId2 = ApiTestUtils.getIdFromEntity(newCategoryEntity2);

        final List<Long> newProductsIds = new ArrayList<>();

        ResponseEntity<?> remoteAddProductEntity;

        for(int i = 0; i < 3; i++) {
            remoteAddProductEntity = catalogOperationsRemote.addProduct(DtoTestFactory.products().getTestProductWithoutDefaultCategory());

            assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

            newProductsIds.add(ApiTestUtils.getIdFromEntity(remoteAddProductEntity));
        }

        // then: 1) both categories do not "include" any of the test products yet
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(0L));
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId2), equalTo(0L));

        // when: 2) adding the 1st product to the 1st category twice
        catalogOperationsRemote.addProductToCategoryReference(testCategoryId1, newProductsIds.get(0));

        em.clear();

        // then: 2) API should only add that product once
        try {
            catalogOperationsRemote.addProductToCategoryReference(testCategoryId1, newProductsIds.get(0));
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(1L));
        }

        // when: 3) adding 2nd product to the 1st category
        catalogOperationsRemote.addProductToCategoryReference(testCategoryId1, newProductsIds.get(1));

        em.clear();

        // then: 3) 1st category should now have 2 product references while both of those products should reference only 1 category
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(2L));

        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(0)), equalTo(1L));
        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(1)), equalTo(1L));


        // when: 4) adding 3rd product to both categories
        catalogOperationsRemote.addProductToCategoryReference(testCategoryId1, newProductsIds.get(2));
        catalogOperationsRemote.addProductToCategoryReference(testCategoryId2, newProductsIds.get(2));

        em.clear();

        // then: 4)
        //         - 3rd product should now reference both categories
        //         - 1st category should reference all 3 products
        //         - 2nd category should reference only 1 product
        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(2L));

        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(3L));
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId2), equalTo(1L));

        // when: 5) removing reference to 2nd product from 1st category twice
        catalogOperationsRemote.removeProductToCategoryReference(testCategoryId1, newProductsIds.get(1));

        em.clear();

        // then: 5)
        //         - 1st category should reference 2 products
        //         - 1st product should reference 1 category
        //         - 2nd product should not reference any category
        //         - 3rd product should reference both categories
        //         - API should throw an error on removing non existent product reference from category
        try {
            catalogOperationsRemote.removeProductToCategoryReference(testCategoryId1, newProductsIds.get(1));
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(2L));
        }

        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(2L));
        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(0)), equalTo(1L));
        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(1)), equalTo(0L));

        // when: 6a) removing reference to 1st product from 1st category
        catalogOperationsRemote.removeProductToCategoryReference(testCategoryId1, newProductsIds.get(0));

        // when: 6b) adding 1st and 2nd product to 2nd category
        catalogOperationsRemote.addProductToCategoryReference(testCategoryId2, newProductsIds.get(0));
        catalogOperationsRemote.addProductToCategoryReference(testCategoryId2, newProductsIds.get(1));

        em.clear();

        // then: 6)
        //        - 1st category should reference only 1 product
        //        - 2nd category should reference all 3 products
        //        - 1st and 2nd products should reference only 1 category
        //        - 3rd product should reference both categories
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(1L));
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId2), equalTo(3L));

        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(0)), equalTo(1L));
        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(1)), equalTo(1L));
        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(2L));

        // when: 7) removing reference to 3rd product from 1st category
        catalogOperationsRemote.removeProductToCategoryReference(testCategoryId1, newProductsIds.get(2));

        em.clear();

        // then: 7)
        //         - 1st category should not have any product references
        //         - 2nd category should reference to all 3 products
        //         - 3rd product should only reference 1 category
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId1), equalTo(0L));
        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId2), equalTo(3L));

        assertThat(catalogOperationsLocal.getTotalCategoriesForProductCount(newProductsIds.get(2)), equalTo(1L));
    }


    /* ------------------ HELPER METHODS -------------------*/

    private void cleanupCatalogTests() {
        removeLocalTestProducts();
        removeLocalTestCategories();
    }

}