package pl.touk.widerest.api;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXref;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.products.BundleItemDto;
import pl.touk.widerest.api.products.ProductBundleDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.skus.SkuDto;
import pl.touk.widerest.api.products.skus.SkuProductOptionValueDto;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.base.ApiTestUtils;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.security.oauth2.Scope;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static pl.touk.widerest.base.ApiTestUrls.PRODUCT_BY_ID_URL;
import static pl.touk.widerest.base.DtoTestFactory.products;

@RunWith(SpringJUnit4ClassRunner.class)
public class ProductControllerTest extends AbstractTest {

    @Resource
    protected BroadleafCurrencyService currencyService;

    @Before
    public void initProductTests() {
        cleanupProductTests();
    }

    @Test
    public void addingNewProductIncreasesProductsCountAndSavedValuesAreValidTest() {

        // when: adding a new product without a specified category
        final long currentProductsCount = catalogOperationsLocal.getTotalProductsCount();

        final ProductDto productDto = DtoTestFactory.products().getTestProductWithoutDefaultCategory();

        final MediaDto mediaDto1 = DtoTestFactory.getTestSkuMedia();
        final MediaDto mediaDto2 = DtoTestFactory.getTestSkuMedia();
        final MediaDto mediaDto3 = DtoTestFactory.getTestSkuMedia();


        productDto.setRetailPrice(BigDecimal.valueOf(99.33f));
        productDto.setCurrencyCode(null);

        productDto.setMedia(new HashMap<>());
        productDto.getMedia().put("primary", mediaDto1);
        productDto.getMedia().put("alt1", mediaDto2);
        productDto.getMedia().put("alt2", mediaDto3);


        final ResponseEntity<?> remoteAddProductEntity = catalogOperationsRemote.addProduct(productDto);
        final long productId = ApiTestUtils.getIdFromLocationUrl(remoteAddProductEntity.getHeaders().getLocation().toString());

        assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentProductsCount + 1));

        final ResponseEntity<ProductDto> receivedProductEntity = backofficeRestTemplate.exchange(
                ApiTestUrls.PRODUCT_BY_ID_URL,
                HttpMethod.GET, null, ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        final ProductDto receivedProductDto = receivedProductEntity.getBody();

        // then: all of the provided product values should have been saved/set correctly
        assertThat(receivedProductDto.getName(), equalTo(productDto.getName()));
        assertThat(receivedProductDto.getLongDescription(), equalTo(productDto.getLongDescription()));
        assertThat(receivedProductDto.getDescription(), equalTo(productDto.getDescription()));
        assertNull(receivedProductDto.getCategoryName());
        assertThat(receivedProductDto.getTaxCode(), equalTo(productDto.getTaxCode()));
        assertNotNull(receivedProductDto.getCurrencyCode());
        assertThat(receivedProductDto.getModel(), equalTo(productDto.getModel()));
        assertThat(receivedProductDto.getManufacturer(), equalTo(productDto.getManufacturer()));
        assertThat(receivedProductDto.getValidFrom(), equalTo(productDto.getValidFrom()));
        assertThat(receivedProductDto.getSalePrice(), equalTo(productDto.getSalePrice()));
        assertThat(receivedProductDto.getQuantityAvailable(), equalTo(productDto.getQuantityAvailable()));
        assertThat(receivedProductDto.getRetailPrice().longValue(), equalTo(productDto.getRetailPrice().longValue()));


        final Map<String, MediaDto> receivedDefaultSkuMediaDto = receivedProductDto.getMedia();
        assertThat(receivedDefaultSkuMediaDto.size(), equalTo(3));

        assertThat(receivedDefaultSkuMediaDto.get("primary").getTitle(), equalTo(mediaDto1.getTitle()));
        assertThat(receivedDefaultSkuMediaDto.get("primary").getUrl(), equalTo(mediaDto1.getUrl()));
        assertThat(receivedDefaultSkuMediaDto.get("alt1").getTitle(), equalTo(mediaDto2.getTitle()));
        assertThat(receivedDefaultSkuMediaDto.get("alt1").getUrl(), equalTo(mediaDto2.getUrl()));
        assertThat(receivedDefaultSkuMediaDto.get("alt2").getTitle(), equalTo(mediaDto3.getTitle()));
        assertThat(receivedDefaultSkuMediaDto.get("alt2").getUrl(), equalTo(mediaDto3.getUrl()));
    }

    @Test
    @Ignore("considering allowing duplicate names")
    public void addingDuplicateProductDoesNotIncreaseProductsCount() {
        // when: adding the same (new) product twice
        final long currentProductCount = catalogOperationsLocal.getTotalProductsCount();
        final ProductDto testProduct = DtoTestFactory.products().getTestProductWithoutDefaultCategory();
        final ResponseEntity<?> retEntity = catalogOperationsRemote.addProduct(testProduct);

        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        // then: API should return 4xx code and only one copy of the product should be saved
        try {
            catalogOperationsRemote.addProduct(testProduct);
            fail();
        } catch (HttpClientErrorException httpClientException) {
            assertTrue(httpClientException.getStatusCode().is4xxClientError());
            assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentProductCount + 1));
        }

    }

    @Test
    public void successfullyDeletingNewlyCreatedProductTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: adding a new product and then deleting it
            final long currentProductsCount = catalogOperationsLocal.getTotalProductsCount();
            final ProductDto defaultProduct = DtoTestFactory.products().getTestProductWithoutDefaultCategory();
            final ResponseEntity<?> retEntity = catalogOperationsRemote.addProduct(defaultProduct);
            final long productId = ApiTestUtils.getIdFromLocationUrl(retEntity.getHeaders().getLocation().toString());

            assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
            assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentProductsCount + 1));

            adminRestTemplate.delete(retEntity.getHeaders().getLocation().toString());

            // then: the deleted product should no longer exist (HTTP code returned by API => 404)
            try {
                backofficeRestTemplate.exchange(ApiTestUrls.PRODUCT_BY_ID_URL,
                        HttpMethod.GET,
                        null, ProductDto.class, serverPort, productId);
                fail();
            } catch (HttpClientErrorException httpClientErrorException) {
                assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
            }

            assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentProductsCount));
        });
    }

    @Test
    public void modifyingExistingProductDoesNotCreateANewOneInsteadTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: adding a new product and then modifying its values
            final ResponseEntity<?> retEntity = catalogOperationsRemote.addProduct(DtoTestFactory.products().getTestProductWithoutDefaultCategory());
            assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
            final long productId = ApiTestUtils.getIdFromLocationUrl(retEntity.getHeaders().getLocation().toString());

            final long currentGlobalProductsCount = catalogOperationsLocal.getTotalProductsCount();
            final ProductDto modifiedProductDto = DtoTestFactory.products().getTestProductWithoutDefaultCategory();

            adminRestTemplate.put(ApiTestUrls.PRODUCT_BY_ID_URL, modifiedProductDto, serverPort, productId);

            // then: no new product gets created
            assertThat(catalogOperationsLocal.getTotalProductsCount(), equalTo(currentGlobalProductsCount));
        });
    }

    @Test
    public void modifyingExistingProductDoesActuallyModifyItsValuesTest() throws Throwable {

        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: adding a test new product and then modifying its values
            final ResponseEntity<?> retEntity = catalogOperationsRemote.addProduct(DtoTestFactory.products().getTestProductWithoutDefaultCategory());
            assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
            final long productId = ApiTestUtils.getIdFromLocationUrl(retEntity.getHeaders().getLocation().toString());

            final ProductDto modifiedProductDto = DtoTestFactory.products().getTestProductWithoutDefaultCategory();

            adminRestTemplate.put(ApiTestUrls.PRODUCT_BY_ID_URL, modifiedProductDto, serverPort, productId);

            // then: the modified fields get updated in test product
            final ResponseEntity<ProductDto> receivedProductEntity = backofficeRestTemplate.exchange(ApiTestUrls.PRODUCT_BY_ID_URL,
                    HttpMethod.GET,
                    null,
                    ProductDto.class, serverPort, productId);

            final ProductDto receivedProductDto = receivedProductEntity.getBody();

            assertThat(modifiedProductDto.getName(), equalTo(receivedProductDto.getName()));
            assertThat(modifiedProductDto.getLongDescription(), equalTo(receivedProductDto.getLongDescription()));
            assertThat(modifiedProductDto.getDescription(), equalTo(receivedProductDto.getDescription()));
            assertThat(modifiedProductDto.getModel(), equalTo(receivedProductDto.getModel()));
            assertThat(modifiedProductDto.getManufacturer(), equalTo(receivedProductDto.getManufacturer()));
        });
    }

    @Test
    @Ignore("considering allowing duplicate names")
    public void updatingProductsNameWithAnExistingOneCausesExceptionTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: creating two new products and then updating the first one with second one's name
            final ProductDto productDto1 = DtoTestFactory.products().getTestProductWithoutDefaultCategory();
            final ProductDto productDto2 = DtoTestFactory.products().getTestProductWithoutDefaultCategory();

            final ResponseEntity<?> retEntity1 = catalogOperationsRemote.addProduct(productDto1);
            assertThat(retEntity1.getStatusCode(), equalTo(HttpStatus.CREATED));
            final long productId1 = ApiTestUtils.getIdFromLocationUrl(retEntity1.getHeaders().getLocation().toString());

            final ResponseEntity<?> retEntity2 = catalogOperationsRemote.addProduct(productDto2);
            assertThat(retEntity2.getStatusCode(), equalTo(HttpStatus.CREATED));

            productDto1.setName(productDto2.getName());

            // then: API should return HTTP.CONFLICT
            try {
                adminRestTemplate.put(ApiTestUrls.PRODUCT_BY_ID_URL, productDto1, serverPort, productId1);
                fail();
            } catch (HttpClientErrorException httpCleintErrorException) {
                assertThat(httpCleintErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
            }
        });
    }


    @Test
    public void shouldThrowIfProductDoesNotHaveDefaultSku() {
        final ProductDto productWihtoutDefaultSkuDto = products().getTestProductWithoutDefaultCategory();
        productWihtoutDefaultSkuDto.setSalePrice(null);
        productWihtoutDefaultSkuDto.setRetailPrice(null);

        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> catalogOperationsRemote.addProduct(productWihtoutDefaultSkuDto))
                .has(ApiTestHttpConditions.httpUnprocessableEntityCondition);
    }

    @Test
    public void shouldThrowIfProductDoesNotHaveDefaultSku2() {
        final long currentProductsCount = catalogOperationsLocal.getTotalProductsCount();
        final ProductDto productWihtoutDefaultSkuDto = products().getTestProductWithoutDefaultCategory();
        //productWihtoutDefaultSkuDto.setDefaultSku(null);

        productWihtoutDefaultSkuDto.setSalePrice(null);
        productWihtoutDefaultSkuDto.setRetailPrice(null);

        // then: API should return HTTP.BAD_REQUEST code and the product should not be added
        try {

            catalogOperationsRemote.addProduct(productWihtoutDefaultSkuDto);
            fail();
        } catch (HttpClientErrorException httpClientException) {
            assertThat(httpClientException.getStatusCode(), Matchers.anyOf(equalTo(HttpStatus.BAD_REQUEST), equalTo(HttpStatus.UNPROCESSABLE_ENTITY)));
            assertThat(currentProductsCount, equalTo(catalogOperationsLocal.getTotalProductsCount()));
        }
    }

    @Test
    @Transactional
    public void addingNewSkuAfterCreatingProductWithDefaultSkuIncreasesSkusCountForThatProductTest() {
        // when: adding new SKU to a product
        final ProductDto productWithDefaultSKU = DtoTestFactory.products().getTestProductWithoutDefaultCategory();
        final ResponseEntity<?> addedProductEntity = catalogOperationsRemote.addProduct(productWithDefaultSKU);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final String createdProductUrlString = addedProductEntity.getHeaders().getLocation().toString();
        final long productId = ApiTestUtils.getIdFromLocationUrl(createdProductUrlString);
        assertThat(catalogOperationsLocal.getTotalSkusForProductCount(productId), equalTo(1L));

        final SkuDto additionalSkuDto = DtoTestFactory.products().testAdditionalSkuDto();
        final ResponseEntity<?> addedAdditionalSkuEntity = catalogOperationsRemote.addTestSKUToProduct(productId, additionalSkuDto);
        assertThat(addedAdditionalSkuEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        em.clear();

        // then: total number of SKUs for that product should increase
        assertThat(catalogOperationsLocal.getTotalSkusForProductCount(productId), equalTo(2L));
    }


    @Ignore("dropped PATCH support")
    @Test
    public void partiallyUpdatingSkuDoesNotRemoveAlreadySetValuesTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: modifying certain values (via PATCH) of SKU
            final ProductDto productWithDefaultSKU = DtoTestFactory.products().getTestProductWithoutDefaultCategory();

            final ResponseEntity<?> addedProductEntity = catalogOperationsRemote.addProduct(productWithDefaultSKU);
            assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
            final long productId = ApiTestUtils.getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());

            final SkuDto additionalSkuDto = DtoTestFactory.products().testAdditionalSkuDto();

            final ResponseEntity<?> addedSkuEntity = catalogOperationsRemote.addTestSKUToProduct(productId, additionalSkuDto);
            assertThat(addedSkuEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
            final long skuId = ApiTestUtils.getIdFromLocationUrl(addedSkuEntity.getHeaders().getLocation().toString());

            additionalSkuDto.setDescription("New Sku Description");
            additionalSkuDto.setQuantityAvailable(4);

            final HttpEntity<SkuDto> requestEntity = new HttpEntity<>(additionalSkuDto);
            adminRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());


            try {
                adminRestTemplate.exchange(ApiTestUrls.PRODUCT_BY_ID_SKU_BY_ID, HttpMethod.PATCH,
                        requestEntity, Void.class, serverPort, productId, skuId);
            } catch (RestClientException ex) {
                System.out.println(ex.getMessage() + ex.getCause() + ex.getLocalizedMessage() + ex.getStackTrace());
            }

            final ResponseEntity<SkuDto> receivedSkuEntity =
                    backofficeRestTemplate.getForEntity(ApiTestUrls.PRODUCT_BY_ID_SKU_BY_ID, SkuDto.class,
                            serverPort, productId, skuId);

            assertThat(receivedSkuEntity.getStatusCode(), equalTo(HttpStatus.OK));

            // then: only modified values are affected, the others remain unchanged
            final SkuDto receivedSkuDto = receivedSkuEntity.getBody();

            assertNotNull(receivedSkuDto.getName());
            assertNotNull(receivedSkuDto.getValidFrom());
            assertNotNull(receivedSkuDto.getTaxCode());
            assertNotNull(receivedSkuDto.getRetailPrice());

            assertThat(receivedSkuDto.getDescription(), equalTo(additionalSkuDto.getDescription()));
            assertThat(receivedSkuDto.getQuantityAvailable(), equalTo(additionalSkuDto.getQuantityAvailable()));
        });
    }

    @Test
    public void skuAddedWithoutCurrencyGetsADefaultOneTest() {
        // when: adding product without currency specified
        final ProductDto productDto = DtoTestFactory.products().getTestProductWithoutDefaultCategory();

        productDto.setCurrencyCode(null);

        final ResponseEntity<?> addedProductEntity = catalogOperationsRemote.addProduct(productDto);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long productId = ApiTestUtils.getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());

        final ResponseEntity<ProductDto> receivedProductEntity = backofficeRestTemplate.exchange(ApiTestUrls.PRODUCT_BY_ID_URL,
                HttpMethod.GET,
                null,
                ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        // then: the default API currency is being used
        assertThat(receivedProductEntity.getBody().getCurrencyCode(), equalTo(
                Optional.ofNullable(currencyService.findDefaultBroadleafCurrency())
                        .map(BroadleafCurrency::getCurrencyCode)
                        .orElse(Money.defaultCurrency().getCurrencyCode())
        ));
    }

    @Test
    @Ignore("DefaultSku is no longer used in Product DTO")
    public void whenSkuAndProductNamesDifferThenProductsNameGetsChosenTest() {
        // when: newly created product and its default SKU names differ
        final ProductDto productDto = DtoTestFactory.products().getTestProductWithoutDefaultCategory();
        final String newProductName = "This name should be chosen";

        productDto.setName(newProductName);

        final ResponseEntity<?> addedProductEntity = catalogOperationsRemote.addProduct(productDto);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long productId = ApiTestUtils.getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());

        // then: default SKU's name should be chosen far a product name
        final ResponseEntity<ProductDto> receivedProductEntity = backofficeRestTemplate.exchange(ApiTestUrls.PRODUCT_BY_ID_URL,
                HttpMethod.GET,
                null,
                ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedProductEntity.getBody().getName(), equalTo(newProductName));
    }

    @Test
    @Transactional
    public void addingNewSkuMediaInsertsAllValuesCorrectlyTest() {
        // when: creating new product with 1 Media Object
        final ResponseEntity<?> addedProductEntity = catalogOperationsRemote.addProduct(DtoTestFactory.products().testProductWithDefaultSKUandCategory());
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long productId = ApiTestUtils.getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());

        final ResponseEntity<?> addedAdditionalSkuEntity = catalogOperationsRemote.addTestSKUToProduct(productId, DtoTestFactory.products().testAdditionalSkuDto());
        assertThat(addedAdditionalSkuEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long skuId = ApiTestUtils.getIdFromLocationUrl(addedAdditionalSkuEntity.getHeaders().getLocation().toString());

        final MediaDto testSkuMedia = DtoTestFactory.getTestSkuMedia();

        catalogOperationsRemote.addTestMediaToSku(productId, skuId, "alt1", testSkuMedia);

        // then: Media object gets added properly
        final SkuMediaXref skuMediaXref = catalogService.findSkuById(skuId).getSkuMediaXref().get("alt1");
        final Media receivedMedia = skuMediaXref.getMedia();

        assertThat(receivedMedia.getAltText(), equalTo(testSkuMedia.getAltText()));
        assertThat(receivedMedia.getTags(), equalTo(testSkuMedia.getTags()));
        assertThat(receivedMedia.getTitle(), equalTo(testSkuMedia.getTitle()));
        assertThat(receivedMedia.getUrl(), equalTo(testSkuMedia.getUrl()));
    }

////    @Test
////    public void addingNewSkuMediaWithInvalidOrNoKeyCausesAnException() {
////        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory();
////        ResponseEntity<?> addedProductEntity = apiTestCatalogManager.addProduct(productDto);
////        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
////        long productId = ApiTestUtils.getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());
////
////        ResponseEntity<?> addedAdditionalSkuEntity = addNewTestSKUToProduct(productId, DtoTestFactory.products().testAdditionalSkuDto());
////        assertThat(addedAdditionalSkuEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
////        long skuId = ApiTestUtils.getIdFromLocationUrl(addedAdditionalSkuEntity.getHeaders().getLocation().toString());
////
////        SkuMediaDto testSkuMedia = DtoTestFactory.getTestSkuMedia();
////
////        try {
////            addOrUpdateNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
////            fail();
////        } catch(HttpClientErrorException httpClientErrorException) {
////            assertTrue(httpClientErrorException.getStatusCode().is4xxClientError());
////        }
////
////        testSkuMedia.setKey("randomKey");
////
////        try {
////            addOrUpdateNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
////            fail();
////        } catch(HttpClientErrorException httpClientErrorException) {
////            assertTrue(httpClientErrorException.getStatusCode().is4xxClientError());
////        }
////
////        testSkuMedia.setKey("primary");
////
////        ResponseEntity<?> addedSkuMediaEntity = addOrUpdateNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
////        assertThat(addedSkuMediaEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
////        long skuMediaId = ApiTestUtils.getIdFromLocationUrl(addedSkuMediaEntity.getHeaders().getLocation().toString());
////
////    }

    @Test
    public void addingProductWithAttributesSavesAttributesCorrectlyTest() {
        final ImmutableMap<String, String> PRODUCT_ATTRIBUTES = ImmutableMap.<String, String>builder()
                .put("size", String.valueOf(99))
                .put("color", "red")
                .put("length", String.valueOf(12.222))
                .build();

        final ProductDto testProductWithoutDefaultCategory = products().getTestProductWithoutDefaultCategory();
        testProductWithoutDefaultCategory.setAttributes(PRODUCT_ATTRIBUTES);

        final long productId = ApiTestUtils.getIdFromEntity(catalogOperationsRemote.addProduct(testProductWithoutDefaultCategory));
        final ProductDto receivedProductEntity = retrieveProduct(productId);

        org.assertj.core.api.Assertions.assertThat(receivedProductEntity.getAttributes())
                .isNotEmpty()
                .hasSize(PRODUCT_ATTRIBUTES.size())
                .containsAllEntriesOf(PRODUCT_ATTRIBUTES);
    }


    @Test
    public void attemptingToRemoveDefaultSkuCausesExceptionTest() throws Throwable {

        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: creating a new test product and then trying to delete its default SKU
            final ResponseEntity<?> responseEntity = catalogOperationsRemote.addProduct(DtoTestFactory.products().getTestProductWithoutDefaultCategory());
            final long productId = ApiTestUtils.getIdFromEntity(responseEntity);

            final ResponseEntity<ProductDto> receivedProductEntity =
                    adminRestTemplate.exchange(
                            ApiTestUrls.PRODUCT_BY_ID_URL,
                            HttpMethod.GET,
                            null,
                            ProductDto.class, serverPort, productId);

            final Link defaultSkuLink = receivedProductEntity.getBody().getLink("default-sku");

            assertNotNull(defaultSkuLink);

            final long defaultSkuId = ApiTestUtils.getIdFromLocationUrl(defaultSkuLink.getHref());

            // then: API should not allow to do that and return 4xx error
            try {
                adminRestTemplate.delete(ApiTestUrls.PRODUCT_BY_ID_SKU_BY_ID, serverPort, productId, defaultSkuId);
                fail();
            } catch (HttpClientErrorException httpClientErrorException) {
                assertTrue(httpClientErrorException.getStatusCode().is4xxClientError());
            }
        });
    }



    @Test
    @Transactional
    public void addingComplexProductSavesAllValuesProperly() {
        // when: creating a new test category
        final CategoryDto testCategory = DtoTestFactory.categories().testCategoryDto();
        final ResponseEntity<?> responseEntity1 = catalogOperationsRemote.addCategory(testCategory);
        final long testCategoryId = ApiTestUtils.getIdFromLocationUrl(responseEntity1.getHeaders().getLocation().toString());

        // when: creating a new test product with: default SKU, 2 additional SKUs and 3 medias
        final ProductDto complexProductDto = DtoTestFactory.products().testProductWithDefaultSKUandCategory();
        final SkuDto additionalSku1 = DtoTestFactory.products().testAdditionalSkuDto();
        final SkuDto additionalSku2 = DtoTestFactory.products().testAdditionalSkuDto();

        final MediaDto mediaDto1 = DtoTestFactory.getTestSkuMedia();
        final MediaDto mediaDto2 = DtoTestFactory.getTestSkuMedia();
        final MediaDto mediaDto3 = DtoTestFactory.getTestSkuMedia();

        complexProductDto.setCategoryName(testCategory.getName());

        // set additional default SKU options
//        complexProductDto.setValidFrom(ApiTestUtils.addNDaysToDate(complexProductDto.getValidFrom(), 30));
        complexProductDto.setRetailPrice(new BigDecimal("19.99"));


        final Set<SkuProductOptionValueDto> additionalSku1Options = new HashSet<>();
        additionalSku1Options.add(new SkuProductOptionValueDto("OPTION_NAME1", "OPTION_VALUE2"));

        final Set<SkuProductOptionValueDto> additionalSku2Options = new HashSet<>();
        additionalSku2Options.add(new SkuProductOptionValueDto("OPTION_NAME1", "OPTION_VALUE1"));

        additionalSku1.setMedia(new HashMap<>());
        additionalSku1.getMedia().put("primary", mediaDto1);
        additionalSku1.getMedia().put("alt1", mediaDto2);
        additionalSku1.setRetailPrice(new BigDecimal("29.99"));
        additionalSku1.setValidTo(ApiTestUtils.addNDaysToDate(additionalSku1.getValidFrom(), 10));
        additionalSku1.setCurrencyCode("USD");
        additionalSku1.setAvailability("CHECK_QUANTITY");
        additionalSku1.setSkuProductOptionValues(additionalSku1Options);

        additionalSku2.setMedia(new HashMap<>());
        additionalSku2.getMedia().put("primary", mediaDto3);
        additionalSku2.setRetailPrice(new BigDecimal("19.99"));
        additionalSku2.setValidTo(ApiTestUtils.addNDaysToDate(additionalSku1.getValidFrom(), 2));
        additionalSku2.setCurrencyCode("EUR");
        additionalSku2.setAvailability("CHECK_QUANTITY");
        additionalSku2.setSkuProductOptionValues(additionalSku2Options);

        final Map<String, String> productAttributes = new HashMap<>();
        productAttributes.put("size", String.valueOf(99));
        productAttributes.put("color", "red");

        complexProductDto.setSkus(Arrays.asList(additionalSku1, additionalSku2));
        complexProductDto.setAttributes(productAttributes);

        // then: all fields should be properly saved
        final ResponseEntity<?> responseEntity = catalogOperationsRemote.addProduct(complexProductDto);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long idFromLocationUrl = ApiTestUtils.getIdFromLocationUrl(responseEntity.getHeaders().getLocation().toString());

        assertThat(catalogOperationsLocal.getTotalProductsInCategoryCount(testCategoryId), equalTo(1L));

        final ProductDto receivedProduct = getRemoteTestProductByIdDto(idFromLocationUrl);

        em.clear();

        // validate Product Attributes
        final Map<String, String> attributes = receivedProduct.getAttributes();

        assertThat(attributes.size(), equalTo(productAttributes.size()));
        assertThat(attributes.get("size"), equalTo(String.valueOf(99)));
        assertThat(attributes.get("color"), equalTo("red"));

        // validate Additional SKUS
        final List<SkuDto> additionalSkus = receivedProduct.getSkus();

        assertThat(additionalSkus.size(), equalTo(2));

        final SkuDto receivedAdditionalSku1, receivedAdditionalSku2;

        if(additionalSkus.get(0).getMedia().size() == 2) {
            receivedAdditionalSku1 = additionalSkus.get(0);
            receivedAdditionalSku2 = additionalSkus.get(1);
        } else {
            receivedAdditionalSku1 = additionalSkus.get(1);
            receivedAdditionalSku2 = additionalSkus.get(0);
        }

        // validate addtional Sku #1
        final Map<String, MediaDto> additionalSku1Media = receivedAdditionalSku1.getMedia();

        assertThat(additionalSku1Media.size(), equalTo(2));
        assertTrue(CollectionUtils.isEqualCollection(additionalSku1Media.entrySet(), additionalSku1.getMedia().entrySet()));

        receivedAdditionalSku1.setMedia(null);
        additionalSku1.setMedia(null);

        assertThat(receivedAdditionalSku1, equalTo(additionalSku1));

        // validate addtional Sku #2
        final Map<String, MediaDto> additionalSku2Media = receivedAdditionalSku2.getMedia();

        assertThat(additionalSku2Media.size(), equalTo(1));
        assertThat(additionalSku2Media.get(0), equalTo(additionalSku2.getMedia().get(0)));

        assertThat(receivedAdditionalSku2, equalTo(additionalSku2));

        // validate product's default SKU
        assertThat(receivedProduct.getTaxCode(), equalTo(complexProductDto.getTaxCode()));
        assertThat(receivedProduct.getName(), equalTo(complexProductDto.getName()));
        assertThat(receivedProduct.getSalePrice(), equalTo(complexProductDto.getSalePrice()));
        assertThat(receivedProduct.getRetailPrice(), equalTo(complexProductDto.getRetailPrice()));
        assertThat(receivedProduct.getQuantityAvailable(), equalTo(complexProductDto.getQuantityAvailable()));
        assertThat(receivedProduct.getAvailability(), equalTo("CHECK_QUANTITY"));
        assertThat(receivedProduct.getCurrencyCode(), equalTo(
                Optional.ofNullable(currencyService.findDefaultBroadleafCurrency())
                        .map(BroadleafCurrency::getCurrencyCode)
                        .orElse(Money.defaultCurrency().getCurrencyCode())
        ));
        assertThat(receivedProduct.getValidFrom(), equalTo(complexProductDto.getValidFrom()));
        assertThat(receivedProduct.getValidTo(), equalTo(complexProductDto.getValidTo()));

        // validate product
        assertThat(receivedProduct.getManufacturer(), equalTo(complexProductDto.getManufacturer()));
        assertThat(receivedProduct.getModel(), equalTo(complexProductDto.getModel()));
        assertThat(receivedProduct.getName(), equalTo(complexProductDto.getName()));
        assertThat(receivedProduct.getDescription(), equalTo(complexProductDto.getDescription()));
        assertThat(receivedProduct.getLongDescription(), equalTo(complexProductDto.getLongDescription()));
    }




    @Test
    @Ignore("Commented out Bundle endpoints for now")
    public void creatingProductBundleSavesPotentialSavingsProperlyTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: creating a bundle out of two products with a given prices
            final ProductDto testProductDto1 = DtoTestFactory.products().getTestProductWithoutDefaultCategory();
            final ProductDto testProductDto2 = DtoTestFactory.products().getTestProductWithoutDefaultCategory();

            final ResponseEntity<?> responseEntityProduct1 = catalogOperationsRemote.addProduct(testProductDto1);
            final ResponseEntity<?> responseEntityProduct2 = catalogOperationsRemote.addProduct(testProductDto2);

            final long productId1 = ApiTestUtils.getIdFromEntity(responseEntityProduct1);
            final long productId2 = ApiTestUtils.getIdFromEntity(responseEntityProduct2);

            final ResponseEntity<ProductDto> receivedProduct1Entity =
                    adminRestTemplate.exchange(
                            ApiTestUrls.PRODUCT_BY_ID_URL,
                            HttpMethod.GET,
                            null,
                            ProductDto.class, serverPort, productId1);

            assertThat(receivedProduct1Entity.getStatusCode(), equalTo(HttpStatus.OK));

            final ResponseEntity<ProductDto> receivedProduct2Entity =
                    adminRestTemplate.exchange(
                            ApiTestUrls.PRODUCT_BY_ID_URL,
                            HttpMethod.GET,
                            null,
                            ProductDto.class, serverPort, productId2);

            assertThat(receivedProduct2Entity.getStatusCode(), equalTo(HttpStatus.OK));

            final ProductDto remoteTestProductByIdDto1 = receivedProduct1Entity.getBody();
            final ProductDto remoteTestProductByIdDto2 = receivedProduct2Entity.getBody();

            final long defaultSkuId1 = ApiTestUtils.getIdFromLocationUrl(remoteTestProductByIdDto1.getLink("default-sku").getHref());
            final long defaultSkuId2 = ApiTestUtils.getIdFromLocationUrl(remoteTestProductByIdDto2.getLink("default-sku").getHref());

            final ProductBundleDto testBundle = DtoTestFactory.getTestBundle();

            final BundleItemDto bundleItemDto1 = BundleItemDto.builder()
                    .quantity(2)
                    .salePrice(new BigDecimal("3.99"))
                    .skuId(defaultSkuId1)
                    .build();

            final BundleItemDto bundleItemDto2 = BundleItemDto.builder()
                    .quantity(1)
                    .salePrice(new BigDecimal("2.99"))
                    .skuId(defaultSkuId2)
                    .build();


            testBundle.setBundleItems(Arrays.asList(bundleItemDto1, bundleItemDto2));

            final ResponseEntity<?> objectResponseEntity = adminRestTemplate.postForEntity(ApiTestUrls.BUNDLES_URL, testBundle, null, serverPort);

            assertThat(objectResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

            final long testBundleId = ApiTestUtils.getIdFromEntity(objectResponseEntity);

            final ResponseEntity<ProductBundleDto> receivedBundleEntity =
                    adminRestTemplate.getForEntity(ApiTestUrls.BUNDLE_BU_ID_URL, ProductBundleDto.class, serverPort, testBundleId);

            assertThat(receivedBundleEntity.getStatusCode(), equalTo(HttpStatus.OK));

            // then: total prices and potential savings should be calculated correctly

            final ProductBundleDto receivedBundleDto = receivedBundleEntity.getBody();

            final BigDecimal totalNormalPriceForSku1 = remoteTestProductByIdDto1.getRetailPrice()
                    .multiply(new BigDecimal(receivedBundleDto.getBundleItems().get(0).getQuantity()));

            final BigDecimal totalNormalPriceForSku2 = remoteTestProductByIdDto2.getRetailPrice()
                    .multiply(new BigDecimal(receivedBundleDto.getBundleItems().get(1).getQuantity()));

            final BigDecimal totalNormalPriceForBundle = totalNormalPriceForSku1.add(totalNormalPriceForSku2);

            final BigDecimal potentialSavings = totalNormalPriceForBundle.subtract(receivedBundleDto.getBundleRetailPrice());

            assertThat(potentialSavings, equalTo(receivedBundleDto.getPotentialSavings()));
        });
    }


    @Test
    @Ignore("Commented out Bundle endpoints for now")
    public void creatingABundleFromNonExistingSkusThrowsExceptionTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            // when: creating a bundle with any of its SKU ids refering to a non existing SKU
            final long NON_EXISTING_SKU_ID = 9999999;

            final ProductDto testProductDto1 = DtoTestFactory.products().getTestProductWithoutDefaultCategory();
            final ResponseEntity<?> responseEntityProduct1 = catalogOperationsRemote.addProduct(testProductDto1);
            final long productId1 = ApiTestUtils.getIdFromEntity(responseEntityProduct1);

            final ResponseEntity<ProductDto> receivedProductEntity =
                    adminRestTemplate.exchange(
                            ApiTestUrls.PRODUCT_BY_ID_URL,
                            HttpMethod.GET,
                            null,
                            ProductDto.class, serverPort, productId1);

            assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

            final ProductDto remoteTestProductByIdDto1 = receivedProductEntity.getBody();

            final long defaultSkuId1 = ApiTestUtils.getIdFromLocationUrl(remoteTestProductByIdDto1.getLink("default-sku").getHref());

            final ProductBundleDto testBundle = DtoTestFactory.getTestBundle();

            final BundleItemDto bundleItemDto1 = BundleItemDto.builder()
                    .quantity(2)
                    .salePrice(new BigDecimal("3.99"))
                    .skuId(defaultSkuId1)
                    .build();

            final BundleItemDto bundleItemDto2 = BundleItemDto.builder()
                    .quantity(1)
                    .salePrice(new BigDecimal("2.99"))
                    .skuId(NON_EXISTING_SKU_ID)
                    .build();

            testBundle.setBundleItems(Arrays.asList(bundleItemDto1, bundleItemDto2));

            // then: API should return HTTP.CONFLICT code
            try {
                adminRestTemplate.postForEntity(ApiTestUrls.BUNDLES_URL, testBundle, null, serverPort);
                fail();
            } catch (HttpClientErrorException httpClientErrorException) {
                assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
            }
        });
    }
//
    @Test
    public void addingSameAttributeWithAnotherValueUpdatesPreviousValueTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: adding an attribute to a product and then adding it once again but with a different value
            final ProductDto productDto = DtoTestFactory.products().getTestProductWithoutDefaultCategory();
            final ResponseEntity<?> productResponseEntity = catalogOperationsRemote.addProduct(productDto);
            URI productUrl = productResponseEntity.getHeaders().getLocation();

            { // when attribute set
                final ProductDto receivedProductDto =
                        backofficeRestTemplate.getForObject(productUrl, ProductDto.class);
                receivedProductDto.setAttributes(new HashMap<>());
                receivedProductDto.getAttributes().put("Range", "Long");
                adminRestTemplate.put(productUrl, receivedProductDto);
            }

            { // when attribute set
                final ProductDto receivedProductDto =
                        backofficeRestTemplate.getForObject(productUrl, ProductDto.class);
                receivedProductDto.getAttributes().put("Range", "Short");
                adminRestTemplate.put(productUrl, receivedProductDto);
            }

            { // then it is available
                final ProductDto receivedProductDto =
                        backofficeRestTemplate.getForObject(productUrl, ProductDto.class);
                assertThat(receivedProductDto.getAttributes().size(), equalTo(1));
                assertThat(receivedProductDto.getAttributes().get("Range"), equalTo("Short"));
            }

        });
    }

    @Test
    public void addingAttributeAndRemovingItWorksAsExpectedTest() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

            // when: adding an attribute to a product and then deleting it
            final ProductDto productDto = DtoTestFactory.products().getTestProductWithoutDefaultCategory();
            final ResponseEntity<?> productResponseEntity = catalogOperationsRemote.addProduct(productDto);
            URI productUrl = productResponseEntity.getHeaders().getLocation();

            { // when attribute set
                final ProductDto receivedProductDto =
                        backofficeRestTemplate.getForObject(productUrl, ProductDto.class);
                receivedProductDto.setAttributes(new HashMap<>());
                receivedProductDto.getAttributes().put("Range", "Long");
                adminRestTemplate.put(productUrl, receivedProductDto);
            }

            { // then it is available
                final ProductDto receivedProductDto =
                        backofficeRestTemplate.getForObject(productUrl, ProductDto.class);
                assertThat(receivedProductDto.getAttributes().get("Range"), equalTo("Long"));
            }

            { // when the attribute deleted
                final ProductDto receivedProductDto =
                        backofficeRestTemplate.getForObject(productUrl, ProductDto.class);
                receivedProductDto.getAttributes().remove("Range");
                adminRestTemplate.put(productUrl, receivedProductDto);
            }

            { // then the attribute no longer exists
                final ProductDto receivedProductDto =
                        backofficeRestTemplate.getForObject(productUrl, ProductDto.class);
                assertThat(receivedProductDto.getAttributes(), nullValue());
            }

        });
    }


    private ProductDto retrieveProduct(final long productId) {
        return backofficeRestTemplate.getForObject(PRODUCT_BY_ID_URL, ProductDto.class, serverPort, productId);
    }

    private void cleanupProductTests() {
        removeLocalTestProducts();
    }
}