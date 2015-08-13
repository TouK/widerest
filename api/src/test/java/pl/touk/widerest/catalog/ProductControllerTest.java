package pl.touk.widerest.catalog;

import org.broadleafcommerce.common.currency.service.BroadleafCurrencyService;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
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
    @Ignore
    public void addingNewProductIncreasesProductsCountAndSavedValuesAreValidTest() {

        long currentProductsCount = getRemoteTotalProductsCount();
        ProductDto productDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.SAME);

        //when
        ResponseEntity<?> remoteAddProductEntity = addNewTestProduct(productDto);

        assertThat(remoteAddProductEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(getRemoteTotalProductsCount(), equalTo(currentProductsCount + 1));


        ResponseEntity<ProductDto> receivedProductEntity = restTemplate.exchange(
                remoteAddProductEntity.getHeaders().getLocation().toString(),
                HttpMethod.GET, getHttpJsonRequestEntity(), ProductDto.class, serverPort);

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
        removeLocalTestProducts();
    }
}
