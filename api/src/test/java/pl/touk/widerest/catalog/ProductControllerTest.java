package pl.touk.widerest.catalog;

import org.apache.commons.collections.CollectionUtils;
import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.core.catalog.domain.SkuMediaXref;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.hateoas.Link;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.catalog.dto.*;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.Application;
import pl.touk.widerest.base.DtoTestType;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

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
    public void localAndRemoteProductCountValuesAreEqualTest() {

        long remoteTotalProductCount = getRemoteTotalProductsCount();

        assertThat(remoteTotalProductCount, equalTo(getLocalTotalProductsCount()));
    }

    @Test
    public void addingNewProductIncreasesProductsCountAndSavedValuesAreValidTest() {

        long currentProductsCount = getRemoteTotalProductsCount();
        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);

        //when
        ResponseEntity<?> remoteAddProductEntity = addNewTestProduct(productDto);

        assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentProductsCount + 1));

        long productId = getIdFromLocationUrl(remoteAddProductEntity.getHeaders().getLocation().toString());

        ResponseEntity<ProductDto> receivedProductEntity = restTemplate.exchange(
                PRODUCT_BY_ID_URL,
                HttpMethod.GET, getHttpJsonRequestEntity(), ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        ProductDto receivedProductDto = receivedProductEntity.getBody();

        //then
        assertThat(receivedProductDto.getName(), equalTo(productDto.getName()));
        assertThat(receivedProductDto.getDescription(), equalTo(productDto.getDescription()));
        assertThat(receivedProductDto.getModel(), equalTo(productDto.getModel()));
        assertThat(receivedProductDto.getDefaultSku().getSalePrice().longValue(), equalTo(productDto.getDefaultSku().getSalePrice().longValue()));
        assertThat(receivedProductDto.getDefaultSku().getQuantityAvailable(), equalTo(productDto.getDefaultSku().getQuantityAvailable()));
        /* ... */

    }

    @Test
    public void addingDuplicateProductDoesNotIncreaseProductsCount() {
        long currentProductCount = getRemoteTotalProductsCount();

        ProductDto testProduct = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);

        ResponseEntity<?> retEntity = addNewTestProduct(testProduct);
        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        try {
            addNewTestProduct(testProduct);
            fail();
        } catch (HttpClientErrorException httpClientException) {
            assertThat(httpClientException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
            assertThat(getRemoteTotalProductsCount(), equalTo(currentProductCount + 1));
        }

    }

    @Test
    public void successfullyDeletingNewlyCreatedProductTest() {
        long currentProductsCount = getRemoteTotalProductsCount();
        ProductDto defaultProduct = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.SAME);

        ResponseEntity<?> retEntity = addNewTestProduct(defaultProduct);

        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentProductsCount + 1));

        long productId = getIdFromLocationUrl(retEntity.getHeaders().getLocation().toString());

        //when
        oAuth2AdminRestTemplate().delete(retEntity.getHeaders().getLocation().toString());

        //then

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
        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        ResponseEntity<?> retEntity = addNewTestProduct(productDto);
        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromLocationUrl(retEntity.getHeaders().getLocation().toString());

        long currentGlobalProductsCount = getRemoteTotalProductsCount();

        ProductDto modifiedProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        oAuth2AdminRestTemplate().put(PRODUCT_BY_ID_URL, modifiedProductDto, serverPort, productId);

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductsCount));

    }

    @Test
    public void modifyingExistingProductDoesActuallyModifyItsValuesTest() {
        ProductDto productDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        ResponseEntity<?> retEntity = addNewTestProduct(productDto);
        assertThat(retEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromLocationUrl(retEntity.getHeaders().getLocation().toString());

        ProductDto modifiedProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        oAuth2AdminRestTemplate().put(PRODUCT_BY_ID_URL, modifiedProductDto, serverPort, productId);

        ResponseEntity<ProductDto> receivedProductEntity = restTemplate.exchange(PRODUCT_BY_ID_URL,
                HttpMethod.GET,
                getHttpJsonRequestEntity(),
                ProductDto.class, serverPort, productId);

        ProductDto receivedProductDto = receivedProductEntity.getBody();

        assertThat(modifiedProductDto.getName(), equalTo(receivedProductDto.getName()));
        assertThat(modifiedProductDto.getLongDescription(), equalTo(receivedProductDto.getLongDescription()));
        assertThat(modifiedProductDto.getDescription(), equalTo(receivedProductDto.getDescription()));
        assertThat(modifiedProductDto.getModel(), equalTo(receivedProductDto.getModel()));
        assertThat(modifiedProductDto.getManufacturer(), equalTo(receivedProductDto.getManufacturer()));

    }

    @Test
    public void updatingProductsNameWithAnExistingOneCausesExceptionTest() {
        ProductDto productDto1 = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        ProductDto productDto2 = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        ResponseEntity<?> retEntity1 = addNewTestProduct(productDto1);
        assertThat(retEntity1.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId1 = getIdFromLocationUrl(retEntity1.getHeaders().getLocation().toString());

        ResponseEntity<?> retEntity2 = addNewTestProduct(productDto2);
        assertThat(retEntity2.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId2 = getIdFromLocationUrl(retEntity2.getHeaders().getLocation().toString());

        productDto1.setName(productDto2.getName());

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

        SkuMediaDto testSkuMedia = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);

        testSkuMedia.setKey("alt1");

        ResponseEntity<?> addedSkuMediaEntity = addNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
        assertThat(addedSkuMediaEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long skuMediaId = getIdFromLocationUrl(addedSkuMediaEntity.getHeaders().getLocation().toString());

        System.out.println("just added: " + skuMediaId);

        SkuMediaXref skuMediaXref = catalogService.findSkuById(skuId).getSkuMediaXref().get("alt1");

        Media receivedMedia = skuMediaXref.getMedia();

        assertThat(receivedMedia.getAltText(), equalTo(testSkuMedia.getAltText()));
        assertThat(receivedMedia.getTags(), equalTo(testSkuMedia.getTags()));
        assertThat(receivedMedia.getTitle(), equalTo(testSkuMedia.getTitle()));
        assertThat(receivedMedia.getUrl(), equalTo(testSkuMedia.getUrl()));
    }

    @Test
    public void addingNewSkuMediaWithInvalidOrNoKeyCausesAnException() {
        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        ResponseEntity<?> addedProductEntity = addNewTestProduct(productDto);
        assertThat(addedProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromLocationUrl(addedProductEntity.getHeaders().getLocation().toString());

        ResponseEntity<?> addedAdditionalSkuEntity = addNewTestSKUToProduct(productId, DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT));
        assertThat(addedAdditionalSkuEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long skuId = getIdFromLocationUrl(addedAdditionalSkuEntity.getHeaders().getLocation().toString());

        SkuMediaDto testSkuMedia = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);

        try {
            addNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertTrue(httpClientErrorException.getStatusCode().is4xxClientError());
        }

        testSkuMedia.setKey("randomKey");

        try {
            addNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
            fail();
        } catch(HttpClientErrorException httpClientErrorException) {
            assertTrue(httpClientErrorException.getStatusCode().is4xxClientError());
        }

        testSkuMedia.setKey("primary");

        ResponseEntity<?> addedSkuMediaEntity = addNewTestSkuMediaToProductSku(productId, skuId, testSkuMedia);
        assertThat(addedSkuMediaEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long skuMediaId = getIdFromLocationUrl(addedSkuMediaEntity.getHeaders().getLocation().toString());

    }


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
    public void addingComplexProductSavesAllValuesProperly() {

        CategoryDto testCategory = DtoTestFactory.getTestCategory(DtoTestType.NEXT);
        ResponseEntity<?> responseEntity1 = addNewTestCategory(testCategory);
        long testCategoryId = getIdFromLocationUrl(responseEntity1.getHeaders().getLocation().toString());

        ProductDto complexProductDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        SkuDto additionalSku1 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);
        SkuDto additionalSku2 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

        SkuMediaDto skuMediaDto1 = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);
        SkuMediaDto skuMediaDto2 = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);
        SkuMediaDto skuMediaDto3 = DtoTestFactory.getTestSkuMedia(DtoTestType.NEXT);



        complexProductDto.setCategoryName(testCategory.getName());

        // set additional default SKU options
        complexProductDto.getDefaultSku().setActiveEndDate(addNDaysToDate(complexProductDto.getDefaultSku().getActiveStartDate(), 30));
        complexProductDto.getDefaultSku().setRetailPrice(new BigDecimal("19.99"));


        Set<SkuProductOptionValueDto> additionalSku1Options = new HashSet<>();
        additionalSku1Options.add(new SkuProductOptionValueDto("TESTOPTION", "test1"));

        Set<SkuProductOptionValueDto> additionalSku2Options = new HashSet<>();
        additionalSku2Options.add(new SkuProductOptionValueDto("TESTOPTION", "test2"));

        skuMediaDto1.setKey("primary");
        skuMediaDto2.setKey("alt1");
        additionalSku1.setSkuMedia(Arrays.asList(skuMediaDto1, skuMediaDto2));
        additionalSku1.setRetailPrice(new BigDecimal("29.99"));
        additionalSku1.setActiveEndDate(addNDaysToDate(additionalSku1.getActiveStartDate(), 10));
        additionalSku1.setCurrencyCode("USD");
        additionalSku1.setAvailability("CHECK_QUANTITY");
        additionalSku1.setSkuProductOptionValues(additionalSku1Options);

        skuMediaDto3.setKey("primary");
        additionalSku2.setSkuMedia(Arrays.asList(skuMediaDto3));
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


        assertThat(getRemoteTotalProductsInCategoryCount(testCategoryId), equalTo(1L));


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

        List<SkuMediaDto> additionalSku1Media = receivedAdditionalSku1.getSkuMedia();

        assertThat(additionalSku1Media.size(), equalTo(2));
        assertTrue(CollectionUtils.isEqualCollection(additionalSku1Media, additionalSku1.getSkuMedia()));

        receivedAdditionalSku1.setSkuMedia(null);
        additionalSku1.setSkuMedia(null);


        assertThat(receivedAdditionalSku1, equalTo(additionalSku1));


        // validate addtional Sku #2
        List<SkuMediaDto> additionalSku2Media = receivedAdditionalSku2.getSkuMedia();

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







    /* -----------------------------END OF TESTS----------------------------- */
    private void cleanupProductTests() {
        removeLocalTestProducts();
    }
}
