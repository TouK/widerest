package pl.touk.widerest.catalog;

import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.base.DtoTestType;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

//@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class ProductControllerTest extends ApiTestBase {

    @javax.annotation.Resource(name="blCurrencyService")
    protected BroadleafCurrencyService crrencyService;


    @Before
    public void initProductTests() {
        serverPort = String.valueOf(8080);
        //cleanupProductTests();
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

        /* TODO: (mst) implement a reasonable ProductDto's equalTo() method instead of
         *       doing this bs:
         */
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
            retEntity = addNewTestProduct(testProduct);
            fail();
        } catch (HttpClientErrorException httpClientException) {
            assertThat(httpClientException.getStatusCode(), equalTo(HttpStatus.CONFLICT));
            assertThat(getRemoteTotalProductsCount(), equalTo(currentProductCount + 1));
        }

    }

    @Test
    public void addingNewProductWihoutDefaultSKUCausesExceptionTest() {
        long currentProductsCount = getRemoteTotalProductsCount();
        ProductDto productWihtoutDefaultSkuDto = DtoTestFactory.getTestProductWithoutDefaultSKU();

        try {
            //when
            ResponseEntity<?> retEntity = addNewTestProduct(productWihtoutDefaultSkuDto);
            fail();
        } catch (HttpClientErrorException httpClientException) {
            //then
            assertThat(httpClientException.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
            assertThat(currentProductsCount, equalTo(getRemoteTotalProductsCount()));
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

        /* TODO: (mst) maybe few more checks */
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


    /* -----------------------------END OF TESTS----------------------------- */
    private void cleanupProductTests() {
        removeLocalTestSkus();
        removeLocalTestProducts();
    }
}
