package pl.touk.widerest.catalog;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
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
            if(categoryTestDto.getName().equals(testCategory.getName()) && categoryTestDto.getDescription().equals(testCategory.getDescription())) {
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
            if(productTestDto.getName().equals(testProduct.getName())) {
                oAuth2AdminRestTemplate().delete(testProduct.getId().getHref(), 1);
            }
        }
    }

}
