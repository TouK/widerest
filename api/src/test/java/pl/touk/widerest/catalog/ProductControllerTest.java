package pl.touk.widerest.catalog;

import org.broadleafcommerce.core.catalog.domain.Sku;
import org.springframework.web.client.HttpServerErrorException;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.ProductDto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

//@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)

public class ProductControllerTest extends ApiTestBase {

    private HttpHeaders httpRequestHeader;

    @Before
    public void initCategoryTests() {
        this.httpRequestHeader = new HttpHeaders();
        //tmp
        serverPort = String.valueOf(8080);
        //cleanupCategoryTests();
    }

    private long getRemoteTotalProductsCount() {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> httpRequestEntity = new HttpEntity<>(null, httpRequestHeader);

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    private long getLocalTotalSkus() {
        return catalogService.findAllSkus().stream().count();
    }

    private long getRemoteTotalSkusForProductCount(long productId) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> httpRequestEntity = new HttpEntity<>(null, httpRequestHeader);

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_IN_CATEGORY_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    private ResponseEntity<?> addNewTestProduct() throws HttpClientErrorException {

        ProductDto productDto = DtoTestFactory.getTestProduct();

        ResponseEntity<ProductDto> remoteAddProductEntity = oAuth2AdminRestTemplate().postForEntity(ApiTestBase.PRODUCTS_URL, productDto, null, serverPort);

        return remoteAddProductEntity;
    }

    @Test
    public void readProductsTest() {


        //when
        ResponseEntity<ProductDto[]> receivedProductsEntity =
                restTemplate.getForEntity(ApiTestBase.PRODUCTS_URL, ProductDto[].class, serverPort);


        assertNotNull(receivedProductsEntity);
        assertTrue("List of products not found", receivedProductsEntity.getStatusCode().value() == 200);

        for(ProductDto p : receivedProductsEntity.getBody()) {
            System.out.println(p.getName() + ":" + p.getDescription());
        }

        ProductDto[] receivedProducts = receivedProductsEntity.getBody();

        /* Enable Spring! */
        //List<ProductDto> localProducts = catalogService.findAllProducts().stream()
        //        .map(DtoConverters.productEntityToDto)
        //        .collect(Collectors.toList());


       // assertTrue(Arrays.deepEquals(receivedProducts, localProducts.toArray()));
    }

    @Test
    public void readProductsByIdTest() {
        ResponseEntity<ProductDto[]> receivedProductsEntity =
                restTemplate.getForEntity(ApiTestBase.PRODUCTS_URL, ProductDto[].class, serverPort);

        assertNotNull(receivedProductsEntity);
        assertTrue("List of products not found", receivedProductsEntity.getStatusCode().value() == 200);
        assertTrue(receivedProductsEntity.getBody().length >= 1);

        ProductDto receivedProductSingleEntity = receivedProductsEntity.getBody()[1];
        Product localProduct = catalogService.findProductById(receivedProductSingleEntity.getProductId());

//        org.broadleafcommerce.core.catalog.domain.Product localProduct = catalogService.findProductById(receivedProductSingleEntity.getId());

        assertTrue(receivedProductSingleEntity.equals(localProduct));

    }


    @Test
    public void addingNewSKUIncreasesSkusCount() {
        final long totalLocalSkusCount = getLocalTotalSkus();

        ResponseEntity newProductResponseEntity = addNewTestProduct();

       // String newProductLocationUrl = newProductResponseEntity.getHeaders().getLocation().get


        SkuDto skuTestDto = DtoTestFactory.getTestSku();
    try {
        ResponseEntity<HttpHeaders> remoteSkuDtoEntity = restTemplate.postForEntity(
                "http://localhost:8080/catalog/products/skus",
                skuTestDto,
                HttpHeaders.class);

    } catch (HttpServerErrorException e) {
        System.out.println(e.getResponseBodyAsString());
    }


    }

    @Test
    public void addingNewProductIncreasesProductsCount() {

        try {
            ProductDto productDto = DtoTestFactory.getTestProduct();

            ResponseEntity<HttpHeaders> remoteAddProductEntity = restTemplate.postForEntity(ApiTestBase.PRODUCTS_URL, productDto, HttpHeaders.class, serverPort);
            System.out.println("GOT: " + remoteAddProductEntity.getStatusCode());
        }catch (HttpServerErrorException e) {
            System.out.println(e.getResponseBodyAsString());
        }
    }

    /* Duplicate = ??? */
    @Test
    public void addingDuplicateProductDoesNotIncreaseProductsCount() {

    }

    @Test
    public void addingNewSkuAfterCreatingProductWithDefaultSku() {

    }

}
