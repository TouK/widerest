package pl.touk.widerest.catalog;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


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
        CategoryDto categoryDto = DtoTestFactory.getTestCategory();

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = oAuth2AdminRestTemplate().postForEntity(
                CATEGORIES_URL,
                categoryDto, null, serverPort);

        assertThat(remoteAddCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testCategoryId = getIdFromLocationUrl(remoteAddCategoryEntity.getHeaders().getLocation().toString());

        long currentTotalProductsInTestCategoryCount = getRemoteTotalProductsInCategorCountValue(testCategoryId);

        assertThat(currentTotalProductsInTestCategoryCount, equalTo(0L));

        //add test product with default sku into category

        ProductDto productDto1 = DtoTestFactory.getTestProductWithDefaultSKUandCategory();

        ResponseEntity<ProductDto> remoteAddProduct1Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto1, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testProductId1 = getIdFromLocationUrl(remoteAddProduct1Entity.getHeaders().getLocation().toString());

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 1));
        assertThat(getRemoteTotalProductsInCategorCountValue(testCategoryId), equalTo(1L));

        //validate default sku

        ResponseEntity<SkuDto> receivedSkuEntity = restTemplate.exchange(
                PRODUCT_BY_ID_SKUS_DEFAULT, HttpMethod.GET,
                httpRequestEntity, SkuDto.class, serverPort, testProductId1);

        assertThat(receivedSkuEntity.getStatusCode(), equalTo(HttpStatus.OK));

        SkuDto receivedSkuDto = receivedSkuEntity.getBody();
        SkuDto defaultTestSku = DtoTestFactory.getTestDefaultSku();

        assertThat(receivedSkuDto.getName(), equalTo(defaultTestSku.getName()));
        assertThat(receivedSkuDto.getQuantityAvailable(), equalTo(defaultTestSku.getQuantityAvailable()));
        assertThat(receivedSkuDto.getSalePrice(), equalTo(defaultTestSku.getSalePrice()));
        assertThat(receivedSkuDto.getActiveStartDate(), equalTo(defaultTestSku.getActiveStartDate()));

        //add another product without category

        ProductDto productDto2 = DtoTestFactory.getTestProductWithoutDefaultCategory();
        /* (mst) tmp until I update DtoTestFactory methods */
        productDto2.setName(DtoTestFactory.TEST_PRODUCT_DEFAULT_NAME + "1");

        ResponseEntity<ProductDto> remoteAddProduct2Entity = oAuth2AdminRestTemplate().postForEntity(
                PRODUCTS_URL, productDto2, null, serverPort);

        assertThat(remoteAddProduct1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        long testProductId2 = getIdFromLocationUrl(remoteAddProduct2Entity.getHeaders().getLocation().toString());

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount + 2));
        assertThat(getRemoteTotalProductsInCategorCountValue(testCategoryId), equalTo(1L));

        //remove both products

        oAuth2AdminRestTemplate().delete(PRODUCTS_URL + "/" + testProductId1);
        oAuth2AdminRestTemplate().delete(PRODUCTS_URL + "/" + testProductId2);

        assertThat(getRemoteTotalProductsCount(), equalTo(currentGlobalProductCount));
        assertThat(getRemoteTotalProductsInCategorCountValue(testCategoryId), equalTo(0L));

        //remove category

        oAuth2AdminRestTemplate().delete(CATEGORIES_URL + "/" + testCategoryId, 1);

        /* TODO: (mst) maybe few other checks after removal */
    }






    private void cleanupCatalogTests() {
        removeRemoteTestProducts();
        removeRemoteTestCategory();
    }

    private void removeRemoteTestCategory() {

        CategoryDto categoryTestDto = DtoTestFactory.getTestCategory();

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
        ProductDto productTestDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory();


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

    private long getRemoteTotalProductsInCategorCountValue(long categoryId) {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_IN_CATEGORY_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort, categoryId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

}
