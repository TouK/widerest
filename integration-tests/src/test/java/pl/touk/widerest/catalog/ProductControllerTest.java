package pl.touk.widerest.catalog;

import org.apache.commons.collections.CollectionUtils;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXref;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.dto.BundleItemDto;
import pl.touk.widerest.api.catalog.dto.MediaDto;
import pl.touk.widerest.api.catalog.dto.ProductAttributeDto;
import pl.touk.widerest.api.catalog.dto.ProductBundleDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.api.catalog.dto.SkuProductOptionValueDto;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.base.DtoTestType;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class ProductControllerTest extends ApiTestBase {

    @javax.annotation.Resource(name="blCurrencyService")
    protected BroadleafCurrencyService crrencyService;

    @Before
    public void initProductTests() {
        //serverPort = String.valueOf(8080);
        cleanupProductTests();
    }

     /* ----------------------------- PRODUCT RELATED TESTS----------------------------- */
    @Test
    public void addingNewProductIncreasesProductsCountAndSavedValuesAreValidTest() {

        // when: adding a new product without a specified category
        final long currentProductsCount = getRemoteTotalProductsCount();

        final ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);
        final ResponseEntity<?> remoteAddProductEntity = addNewTestProduct(productDto);
        final long productId = getIdFromLocationUrl(remoteAddProductEntity.getHeaders().getLocation().toString());

        assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentProductsCount + 1));

        final ResponseEntity<ProductDto> receivedProductEntity = restTemplate.exchange(
                PRODUCT_BY_ID_URL,
                HttpMethod.GET, getHttpJsonRequestEntity(), ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        final ProductDto receivedProductDto = receivedProductEntity.getBody();

        // then: all of the provided product values should have been saved/set correctly
        assertThat(receivedProductDto.getName(), equalTo(productDto.getName()));
        assertThat(receivedProductDto.getDescription(), equalTo(productDto.getDescription()));
        assertThat(receivedProductDto.getModel(), equalTo(productDto.getModel()));
        assertThat(receivedProductDto.getDefaultSku().getSalePrice().longValue(), equalTo(productDto.getDefaultSku().getSalePrice().longValue()));
        assertThat(receivedProductDto.getDefaultSku().getQuantityAvailable(), equalTo(productDto.getDefaultSku().getQuantityAvailable()));
    }

    @Ignore("considering allowing duplicate names")
    @Test
    public void addingDuplicateProductDoesNotIncreaseProductsCount() {
        // when: adding the same (new) product twice
        final long currentProductCount = getRemoteTotalProductsCount();
        final ProductDto testProduct = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);
        final ResponseEntity<?> retEntity = addNewTestProduct(testProduct);
        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        // then: API should return 4xx code and only one copy of the product should be saved
        try {
            addNewTestProduct(testProduct);
            fail();
        } catch (HttpClientErrorException httpClientException) {
            assertTrue(httpClientException.getStatusCode().is4xxClientError());
            assertThat(getRemoteTotalProductsCount(), equalTo(currentProductCount + 1));
        }

    }

    @Test
    public void successfullyDeletingNewlyCreatedProductTest() {
        // when: adding a new product and then deleting it
        final long currentProductsCount = getRemoteTotalProductsCount();
        final ProductDto defaultProduct = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);
        final ResponseEntity<?> retEntity = addNewTestProduct(defaultProduct);
        final long productId = getIdFromLocationUrl(retEntity.getHeaders().getLocation().toString());

        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentProductsCount + 1));

        oAuth2AdminRestTemplate().delete(retEntity.getHeaders().getLocation().toString());

        // then: the deleted product should no longer exist (HTTP code returned by API => 404)
        try {
            restTemplate.exchange(PRODUCT_BY_ID_URL,
                    HttpMethod.GET,
                    getHttpJsonRequestEntity(), ProductDto.class, serverPort, productId);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
        }

        assertThat(getRemoteTotalProductsCount(), equalTo(currentProductsCount));
    }

    @Test
    public void modifyingExistingProductDoesNotCreateANewOneInsteadTest() {
        // when: adding a new product and then modifying its values
        final ResponseEntity<?> retEntity = addNewTestProduct(DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT));
        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long productId = getIdFromLocationUrl(retEntity.getHeaders().getLocation().toString());

        final long currentGlobalProductsCount = getRemoteTotalProductsCount();
        final  ProductDto modifiedProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        oAuth2AdminRestTemplate().put(PRODUCT_BY_ID_URL, modifiedProductDto, serverPort, productId);

        // then: no new product gets created
        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductsCount));
    }

    @Test
    public void modifyingExistingProductDoesActuallyModifyItsValuesTest() {
        // when: adding a test new product and then modifying its values
        final ResponseEntity<?> retEntity = addNewTestProduct(DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT));
        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long productId = getIdFromLocationUrl(retEntity.getHeaders().getLocation().toString());

        final ProductDto modifiedProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        oAuth2AdminRestTemplate().put(PRODUCT_BY_ID_URL, modifiedProductDto, serverPort, productId);

        // then: the modified fields get updated in test product
        final ResponseEntity<ProductDto> receivedProductEntity = restTemplate.exchange(PRODUCT_BY_ID_URL,
                HttpMethod.GET,
                getHttpJsonRequestEntity(),
                ProductDto.class, serverPort, productId);

        final ProductDto receivedProductDto = receivedProductEntity.getBody();

        assertThat(modifiedProductDto.getName(), equalTo(receivedProductDto.getName()));
        assertThat(modifiedProductDto.getLongDescription(), equalTo(receivedProductDto.getLongDescription()));
        assertThat(modifiedProductDto.getDescription(), equalTo(receivedProductDto.getDescription()));
        assertThat(modifiedProductDto.getModel(), equalTo(receivedProductDto.getModel()));
        assertThat(modifiedProductDto.getManufacturer(), equalTo(receivedProductDto.getManufacturer()));
    }

    @Test
    @Ignore("considering allowing duplicate names")
    public void updatingProductsNameWithAnExistingOneCausesExceptionTest() {
        // when: creating two new products and then updating the first one with second one's name
        final ProductDto productDto1 = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        final ProductDto productDto2 = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        final ResponseEntity<?> retEntity1 = addNewTestProduct(productDto1);
        assertThat(retEntity1.getStatusCode(), equalTo(HttpStatus.CREATED));
        final long productId1 = getIdFromLocationUrl(retEntity1.getHeaders().getLocation().toString());

        final ResponseEntity<?> retEntity2 = addNewTestProduct(productDto2);
        assertThat(retEntity2.getStatusCode(), equalTo(HttpStatus.CREATED));

        productDto1.setName(productDto2.getName());

        // then: API should return HTTP.CONFLICT
        try {
            oAuth2AdminRestTemplate().put(PRODUCT_BY_ID_URL, productDto1, serverPort, productId1);
            fail();
        } catch(HttpClientErrorException httpCleintErrorException) {
            assertThat(httpCleintErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
        }
    }


    /* ----------------------------- PRODUCT RELATED TESTS----------------------------- */

    /* -----------------------------SKUS TESTS----------------------------- */

    @Test
    public void addingNewProductWihoutDefaultSKUCausesExceptionTest() {
        long currentProductsCount = getRemoteTotalProductsCount();
        ProductDto productWihtoutDefaultSkuDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        productWihtoutDefaultSkuDto.setDefaultSku(null);

        try {
            //when
            addNewTestProduct(productWihtoutDefaultSkuDto);
            fail();
        } catch (HttpClientErrorException httpClientException) {
            //then
            assertThat(httpClientException.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
            assertThat(currentProductsCount, equalTo(getRemoteTotalProductsCount()));
        }
    }

    @Test
    @Transactional
    public void addingNewSkuAfterCreatingProductWithDefaultSku() {
        ProductDto productWithDefaultSKU = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        ResponseEntity<?> addedProductEntity = addNewTestProduct(productWithDefaultSKU);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        String createdProductUrlString = addedProductEntity.getHeaders().getLocation().toString();
        long productId = getIdFromLocationUrl(createdProductUrlString);

        assertThat(getRemoteTotalSkusForProductCount(productId), equalTo(1L));


        SkuDto additionalSkuDto = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

        addNewTestSKUToProduct(productId, additionalSkuDto);

        assertThat(getLocalTotalSkusForProductCount(productId), equalTo(2L));
    }

    @Test
    public void changingDefaultSkuModifiesValuesCorrectlyTest() {

    }

    @Test
    public void partiallyUpdatingSkuDoesNotRemoveAlreadySetValuesTest() {
        ProductDto productWithDefaultSKU = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        ResponseEntity<?> addedProductEntity = addNewTestProduct(productWithDefaultSKU);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());

        SkuDto additionalSkuDto = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

        ResponseEntity<?> addedSkuEntity = addNewTestSKUToProduct(productId, additionalSkuDto);
        assertThat(addedSkuEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long skuId = getIdFromLocationUrl(addedSkuEntity.getHeaders().getLocation().toString());


        additionalSkuDto.setDescription("New Sku Description");
        additionalSkuDto.setQuantityAvailable(4);


        final HttpEntity<SkuDto> requestEntity = new HttpEntity<>(additionalSkuDto);

        OAuth2RestTemplate adminRestTemplate = oAuth2AdminRestTemplate();
        adminRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        try {
            adminRestTemplate.exchange(PRODUCT_BY_ID_SKU_BY_ID, HttpMethod.PATCH,
                    requestEntity, Void.class, serverPort, productId, skuId);
        } catch(RestClientException ex) {
            System.out.println(ex.getMessage() + ex.getCause() + ex.getLocalizedMessage() + ex.getStackTrace());
        }

        ResponseEntity<SkuDto> receivedSkuEntity =
                restTemplate.getForEntity(PRODUCT_BY_ID_SKU_BY_ID, SkuDto.class,
                        serverPort, productId, skuId);

        assertThat(receivedSkuEntity.getStatusCode(), equalTo(HttpStatus.OK));

        SkuDto receivedSkuDto = receivedSkuEntity.getBody();

        assertNotNull(receivedSkuDto.getName());
        assertNotNull(receivedSkuDto.getActiveStartDate());
        assertNotNull(receivedSkuDto.getTaxCode());
        assertNotNull(receivedSkuDto.getSalePrice());

        assertThat(receivedSkuDto.getDescription(), equalTo(additionalSkuDto.getDescription()));
        assertThat(receivedSkuDto.getQuantityAvailable(), equalTo(additionalSkuDto.getQuantityAvailable()));

    }

    @Test
    public void skuAddedWithoutCurrencyGetsADefaultOneTest() {
        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        productDto.getDefaultSku().setCurrencyCode(null);

        ResponseEntity<?> addedProductEntity = addNewTestProduct(productDto);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());

        ResponseEntity<ProductDto> receivedProductEntity = restTemplate.exchange(PRODUCT_BY_ID_URL,
                HttpMethod.GET,
                getHttpJsonRequestEntity(),
                ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        String defaultCurrencyCode = crrencyService.findDefaultBroadleafCurrency().getCurrencyCode();

        assertThat(receivedProductEntity.getBody().getDefaultSku().getCurrencyCode(), equalTo(defaultCurrencyCode));

    }

    @Test
    public void whenSkuAndProductNamesDifferThenProductsNameGetsChosenTest() {
        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        String newProductName = "This name should be chosen";

        productDto.setName(newProductName);

        ResponseEntity<?> addedProductEntity = addNewTestProduct(productDto);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());

        ResponseEntity<ProductDto> receivedProductEntity = restTemplate.exchange(PRODUCT_BY_ID_URL,
                HttpMethod.GET,
                getHttpJsonRequestEntity(),
                ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        assertThat(receivedProductEntity.getBody().getDefaultSku().getName(), equalTo(newProductName));


    }






    @Test
    @Transactional
    public void addingNewSkuMediaInsertsAllValuesCorrectlyTest() {
        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        ResponseEntity<?> addedProductEntity = addNewTestProduct(productDto);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());

        ResponseEntity<?> addedAdditionalSkuEntity = addNewTestSKUToProduct(productId, DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT));
        assertThat(addedAdditionalSkuEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long skuId = getIdFromLocationUrl(addedAdditionalSkuEntity.getHeaders().getLocation().toString());

        MediaDto testSkuMedia = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);

        addOrUpdateNewTestSkuMediaToProductSku(productId, skuId, "alt1", testSkuMedia);
//        assertThat(addedSkuMediaEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
//        long skuMediaId = getIdFromLocationUrl(addedSkuMediaEntity.getHeaders().getLocation().toString());


        SkuMediaXref skuMediaXref = catalogService.findSkuById(skuId).getSkuMediaXref().get("alt1");

        Media receivedMedia = skuMediaXref.getMedia();

        assertThat(receivedMedia.getAltText(), equalTo(testSkuMedia.getAltText()));
        assertThat(receivedMedia.getTags(), equalTo(testSkuMedia.getTags()));
        assertThat(receivedMedia.getTitle(), equalTo(testSkuMedia.getTitle()));
        assertThat(receivedMedia.getUrl(), equalTo(testSkuMedia.getUrl()));
    }

//    @Test
//    public void addingNewSkuMediaWithInvalidOrNoKeyCausesAnException() {
//        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
//        ResponseEntity<?> addedProductEntity = addNewTestProduct(productDto);
//        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
//        long productId = getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());
//
//        ResponseEntity<?> addedAdditionalSkuEntity = addNewTestSKUToProduct(productId, DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT));
//        assertThat(addedAdditionalSkuEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
//        long skuId = getIdFromLocationUrl(addedAdditionalSkuEntity.getHeaders().getLocation().toString());
//
//        SkuMediaDto testSkuMedia = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);
//
//        try {
//            addOrUpdateNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
//            fail();
//        } catch(HttpClientErrorException httpClientErrorException) {
//            assertTrue(httpClientErrorException.getStatusCode().is4xxClientError());
//        }
//
//        testSkuMedia.setKey("randomKey");
//
//        try {
//            addOrUpdateNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
//            fail();
//        } catch(HttpClientErrorException httpClientErrorException) {
//            assertTrue(httpClientErrorException.getStatusCode().is4xxClientError());
//        }
//
//        testSkuMedia.setKey("primary");
//
//        ResponseEntity<?> addedSkuMediaEntity = addOrUpdateNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
//        assertThat(addedSkuMediaEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
//        long skuMediaId = getIdFromLocationUrl(addedSkuMediaEntity.getHeaders().getLocation().toString());
//
//    }


    @Test
    public void addingProductWithAttributesSavesAttributesCorrectlyTest() {
        ProductDto testProductWithoutDefaultCategory = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);


        Map<String, String> productAttributes = new HashMap<>();
        productAttributes.put("size", String.valueOf(99));
        productAttributes.put("color", "red");
        productAttributes.put("length", String.valueOf(12.222));


        testProductWithoutDefaultCategory.setAttributes(productAttributes);

        ResponseEntity<?> responseEntity = addNewTestProduct(testProductWithoutDefaultCategory);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long idFromLocationUrl = getIdFromLocationUrl(responseEntity.getHeaders().getLocation().toString());

        ResponseEntity<ProductDto> receivedProductEntity = getRemoteTestProductByIdEntity(idFromLocationUrl);

        Map<String, String> attributes = receivedProductEntity.getBody().getAttributes();

        assertThat(attributes.size(), equalTo(attributes.size()));
        assertThat(attributes.get("size"), equalTo(String.valueOf(99)));
        assertThat(attributes.get("color"), equalTo("red"));
        assertThat(attributes.get("length"), equalTo(String.valueOf(12.222)));
    }


    @Test
    public void attemptingToRemoveDefaultSkuCausesExceptionTest() {
        ResponseEntity<?> responseEntity = addNewTestProduct(DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT));
        long productId = getIdFromEntity(responseEntity);

        ResponseEntity<ProductDto> receivedProductEntity =
                restTemplate.getForEntity(PRODUCT_BY_ID_URL, ProductDto.class, serverPort, productId);

        Link defaultSkuLink = receivedProductEntity.getBody().getLink("default-sku");

        assertNotNull(defaultSkuLink);

        long defaultSkuId = getIdFromLocationUrl(defaultSkuLink.getHref());

        try {
            oAuth2AdminRestTemplate().delete(PRODUCT_BY_ID_SKU_BY_ID, serverPort, productId, defaultSkuId);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
        }
    }



    @Test
    @Transactional
    public void addingComplexProductSavesAllValuesProperly() {

        CategoryDto testCategory = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        ResponseEntity<?> responseEntity1 = addNewTestCategory(testCategory);
        long testCategoryId = getIdFromLocationUrl(responseEntity1.getHeaders().getLocation().toString());

        ProductDto complexProductDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        SkuDto additionalSku1 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);
        SkuDto additionalSku2 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

        MediaDto mediaDto1 = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);
        MediaDto mediaDto2 = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);
        MediaDto mediaDto3 = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);



        complexProductDto.setCategoryName(testCategory.getName());

        // set additional default SKU options
        complexProductDto.getDefaultSku().setActiveEndDate(addNDaysToDate(complexProductDto.getDefaultSku().getActiveStartDate(), 30));
        complexProductDto.getDefaultSku().setRetailPrice(new BigDecimal("19.99"));


        Set<SkuProductOptionValueDto> additionalSku1Options = new HashSet<>();
        additionalSku1Options.add(new SkuProductOptionValueDto("TESTOPTION", "test1"));

        Set<SkuProductOptionValueDto> additionalSku2Options = new HashSet<>();
        additionalSku2Options.add(new SkuProductOptionValueDto("TESTOPTION", "test2"));

        additionalSku1.setSkuMedia(new HashMap<>());
        additionalSku1.getSkuMedia().put("primary", mediaDto1);
        additionalSku1.getSkuMedia().put("alt1", mediaDto2);
        additionalSku1.setRetailPrice(new BigDecimal("29.99"));
        additionalSku1.setActiveEndDate(addNDaysToDate(additionalSku1.getActiveStartDate(), 10));
        additionalSku1.setCurrencyCode("USD");
        additionalSku1.setAvailability("CHECK_QUANTITY");
        additionalSku1.setSkuProductOptionValues(additionalSku1Options);

        additionalSku2.setSkuMedia(new HashMap<>());
        additionalSku2.getSkuMedia().put("primary", mediaDto3);
        additionalSku2.setRetailPrice(new BigDecimal("19.99"));
        additionalSku2.setActiveEndDate(addNDaysToDate(additionalSku1.getActiveStartDate(), 2));
        additionalSku2.setCurrencyCode("EUR");
        additionalSku2.setAvailability("ALWAYS_AVAILABLE");
        additionalSku2.setSkuProductOptionValues(additionalSku2Options);

        Map<String, String> productAttributes = new HashMap<>();
        productAttributes.put("size", String.valueOf(99));
        productAttributes.put("color", "red");

        complexProductDto.setSkus(Arrays.asList(additionalSku1, additionalSku2));
        complexProductDto.setAttributes(productAttributes);


        ResponseEntity<?> responseEntity = addNewTestProduct(complexProductDto);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long idFromLocationUrl = getIdFromLocationUrl(responseEntity.getHeaders().getLocation().toString());


        assertThat(getLocalTotalProductsInCategoryCount(testCategoryId), equalTo(1L));


        ProductDto receivedProduct = getRemoteTestProductByIdDto(idFromLocationUrl);

        // validate Product Attributes
        Map<String, String> attributes = receivedProduct.getAttributes();

        assertThat(attributes.size(), equalTo(productAttributes.size()));
        assertThat(attributes.get("size"), equalTo(String.valueOf(99)));
        assertThat(attributes.get("color"), equalTo("red"));


        // validate Additional SKUS
        List<SkuDto> additionalSkus = receivedProduct.getSkus();

        assertThat(additionalSkus.size(), equalTo(2));


        SkuDto receivedAdditionalSku1, receivedAdditionalSku2;

        /* (mst): ???? */
        if(additionalSkus.get(0).getSkuMedia().size() == 2) {
            receivedAdditionalSku1 = additionalSkus.get(0);
            receivedAdditionalSku2 = additionalSkus.get(1);
        } else {
            receivedAdditionalSku1 = additionalSkus.get(1);
            receivedAdditionalSku2 = additionalSkus.get(0);
        }

        // validate addtional Sku #1

        Map<String, MediaDto> additionalSku1Media = receivedAdditionalSku1.getSkuMedia();

        assertThat(additionalSku1Media.size(), equalTo(2));
        assertTrue(CollectionUtils.isEqualCollection(additionalSku1Media.entrySet(), additionalSku1.getSkuMedia().entrySet()));

        receivedAdditionalSku1.setSkuMedia(null);
        additionalSku1.setSkuMedia(null);


        assertThat(receivedAdditionalSku1, equalTo(additionalSku1));


        // validate addtional Sku #2
        Map<String, MediaDto> additionalSku2Media = receivedAdditionalSku2.getSkuMedia();

        assertThat(additionalSku2Media.size(), equalTo(1));
        assertThat(additionalSku2Media.get(0), equalTo(additionalSku2.getSkuMedia().get(0)));

        assertThat(receivedAdditionalSku2, equalTo(additionalSku2));


        // validate default SKU
        SkuDto receivedDefaultSku = receivedProduct.getDefaultSku();
        SkuDto localDefaultSku = complexProductDto.getDefaultSku();

        assertThat(receivedDefaultSku.getTaxCode(), equalTo(localDefaultSku.getTaxCode()));
        assertThat(receivedDefaultSku.getName(), equalTo(complexProductDto.getName()));
        assertThat(receivedDefaultSku.getSalePrice(), equalTo(localDefaultSku.getSalePrice()));
        assertThat(receivedDefaultSku.getRetailPrice(), equalTo(localDefaultSku.getRetailPrice()));
        assertThat(receivedDefaultSku.getQuantityAvailable(), equalTo(localDefaultSku.getQuantityAvailable()));
        assertThat(receivedDefaultSku.getAvailability(), equalTo("ALWAYS_AVAILABLE"));
        assertThat(receivedDefaultSku.getCurrencyCode(), equalTo(crrencyService.findDefaultBroadleafCurrency().getCurrencyCode()));
        assertThat(receivedDefaultSku.getActiveStartDate(), equalTo(localDefaultSku.getActiveStartDate()));
        assertThat(receivedDefaultSku.getActiveEndDate(), equalTo(localDefaultSku.getActiveEndDate()));


        // validate product
        assertThat(receivedProduct.getManufacturer(), equalTo(complexProductDto.getManufacturer()));
        assertThat(receivedProduct.getModel(), equalTo(complexProductDto.getModel()));
        assertThat(receivedProduct.getName(), equalTo(complexProductDto.getName()));
        assertThat(receivedProduct.getDescription(), equalTo(complexProductDto.getDescription()));
        assertThat(receivedProduct.getLongDescription(), equalTo(complexProductDto.getLongDescription()));
    }




    @Test
    public void creatingProductBundleSavesPotentialSavingsProperlyTest() {

        ProductDto testProductDto1 = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        ProductDto testProductDto2 = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        ResponseEntity<?> responseEntityProduct1 = addNewTestProduct(testProductDto1);
        ResponseEntity<?> responseEntityProduct2 = addNewTestProduct(testProductDto2);

        long productId1 = getIdFromEntity(responseEntityProduct1);
        long productId2 = getIdFromEntity(responseEntityProduct2);

        ProductDto remoteTestProductByIdDto1 = getRemoteTestProductByIdDto(productId1);
        ProductDto remoteTestProductByIdDto2 = getRemoteTestProductByIdDto(productId2);

        long defaultSkuId1 = getIdFromLocationUrl(remoteTestProductByIdDto1.getLink("default-sku").getHref());
        long defaultSkuId2 = getIdFromLocationUrl(remoteTestProductByIdDto2.getLink("default-sku").getHref());


        ProductBundleDto testBundle = DtoTestFactory.getTestBundle(DtoTestType.NEXT);

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

        ResponseEntity<?> objectResponseEntity = oAuth2AdminRestTemplate().postForEntity(BUNDLES_URL, testBundle, null, serverPort);

        assertThat(objectResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final long testBundleId = getIdFromEntity(objectResponseEntity);


        ResponseEntity<ProductBundleDto> receivedBundleEntity =
                restTemplate.getForEntity(BUNDLE_BU_ID_URL, ProductBundleDto.class, serverPort, testBundleId);

        assertThat(receivedBundleEntity.getStatusCode(), equalTo(HttpStatus.OK));



        final ProductBundleDto receivedBundleDto = receivedBundleEntity.getBody();


        final BigDecimal totalNormalPriceForSku1 = remoteTestProductByIdDto1.getDefaultSku().getRetailPrice()
                .multiply(new BigDecimal(receivedBundleDto.getBundleItems().get(0).getQuantity()));

        final BigDecimal totalNormalPriceForSku2 = remoteTestProductByIdDto2.getDefaultSku().getRetailPrice()
                .multiply(new BigDecimal(receivedBundleDto.getBundleItems().get(1).getQuantity()));

        final BigDecimal totalNormalPriceForBundle = totalNormalPriceForSku1.add(totalNormalPriceForSku2);


        final BigDecimal potentialSavings = totalNormalPriceForBundle.subtract(receivedBundleDto.getBundleRetailPrice());

        assertThat(potentialSavings, equalTo(receivedBundleDto.getPotentialSavings()));

    }


    @Test
    public void creatingABundleFromNonExistingSkusThrowsExceptionTest() {

        final long NON_EXISTING_SKU_ID = 9999999;

        ProductDto testProductDto1 = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        ResponseEntity<?> responseEntityProduct1 = addNewTestProduct(testProductDto1);

        long productId1 = getIdFromEntity(responseEntityProduct1);

        ProductDto remoteTestProductByIdDto1 = getRemoteTestProductByIdDto(productId1);

        long defaultSkuId1 = getIdFromLocationUrl(remoteTestProductByIdDto1.getLink("default-sku").getHref());

        ProductBundleDto testBundle = DtoTestFactory.getTestBundle(DtoTestType.NEXT);

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

        try {
            oAuth2AdminRestTemplate().postForEntity(BUNDLES_URL, testBundle, null, serverPort);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertThat(httpClientErrorException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
        }

    }




    /* Product Attributes Tests */

    @Test
    public void addingSameAttributeWithAnotherValueUpdatesPreviousValueTest() {
        final ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        final ResponseEntity<?> productResponseEntity = addNewTestProduct(productDto);
        final long productId = getIdFromEntity(productResponseEntity);


        final ProductAttributeDto productAttributeDto1 = ProductAttributeDto.builder()
                .attributeName("Range")
                .attributeValue("Long")
                .build();

        final ProductAttributeDto productAttributeDto2 = ProductAttributeDto.builder()
                .attributeName("Range")
                .attributeValue("Short")
                .build();


        final ResponseEntity<?> attributeDtoEntity1 = oAuth2AdminRestTemplate().postForEntity(
                PRODUCT_BY_ID_ATTRIBUTES_URL, productAttributeDto1, null, serverPort, productId);

        assertTrue(attributeDtoEntity1.getStatusCode().is2xxSuccessful());

        final ResponseEntity<?> attributeDtoEntity2 = oAuth2AdminRestTemplate().postForEntity(
                PRODUCT_BY_ID_ATTRIBUTES_URL, productAttributeDto2, null, serverPort, productId);

        assertTrue(attributeDtoEntity2.getStatusCode().is2xxSuccessful());


        Map<String, String> receivedAttributes = restTemplate.getForObject(PRODUCT_BY_ID_ATTRIBUTES_URL, Map.class, serverPort, productId);

        assertThat(receivedAttributes.size(), equalTo(1));
        assertThat(receivedAttributes.get("Range"), equalTo("Short"));


    }

    @Test
    public void addingAttributeAndRemovingItWorksAsExpectedTest() {
        final ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        final ResponseEntity<?> productResponseEntity = addNewTestProduct(productDto);
        final long productId = getIdFromEntity(productResponseEntity);


        final ProductAttributeDto productAttributeDto = ProductAttributeDto.builder()
                .attributeName("Range")
                .attributeValue("Long")
                .build();

        final ResponseEntity<?> attributeDtoEntity1 = oAuth2AdminRestTemplate().postForEntity(
                PRODUCT_BY_ID_ATTRIBUTES_URL, productAttributeDto, null, serverPort, productId);

        assertTrue(attributeDtoEntity1.getStatusCode().is2xxSuccessful());

        oAuth2AdminRestTemplate().delete(PRODUCT_BY_ID_ATTRIBUTE_BY_NAME_URL, serverPort, productId, productAttributeDto.getAttributeName());

        try {
            oAuth2AdminRestTemplate().delete(PRODUCT_BY_ID_ATTRIBUTE_BY_NAME_URL, serverPort, productId, productAttributeDto.getAttributeName());
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertTrue(httpClientErrorException.getStatusCode().is4xxClientError());
        }

        Map<String, String> receivedAttributes = restTemplate.getForObject(PRODUCT_BY_ID_ATTRIBUTES_URL, Map.class, serverPort, productId);

        assertThat(receivedAttributes.size(), equalTo(0));

    }



    /* -----------------------------END OF TESTS----------------------------- */
    private void cleanupProductTests() {
        removeLocalTestProducts();
    }
}