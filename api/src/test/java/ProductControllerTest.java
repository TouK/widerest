import org.junit.Test;
import org.springframework.http.ResponseEntity;
import pl.touk.widerest.api.catalog.Category;
import pl.touk.widerest.api.catalog.CategoryController;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.Product;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = Application.class)
///@WebAppConfiguration
public class ProductControllerTest extends ApiTestBase {

    public final String PRODUCTS_URL = "http://localhost:{port}/catalog/product/";
    @Test
    public void readProductsTest() {

        //when
        ResponseEntity<Product[]> receivedProductsEntity =
                restTemplate.getForEntity(PRODUCTS_URL, Product[].class, serverPort);


        assertNotNull(receivedProductsEntity);
        assertTrue("List of products not found", receivedProductsEntity.getStatusCode().value() == 200);

        Product[] receivedProducts = receivedProductsEntity.getBody();

        /* Enable Spring! */
        List<Product> localProducts = catalogService.findAllProducts().stream()
                .map(DtoConverters.productEntityToDto)
                .collect(Collectors.toList());


        assertTrue(Arrays.deepEquals(receivedProducts, localProducts.toArray()));
    }

    @Test
    public void readProductsByIdTest() {
        ResponseEntity<Product[]> receivedProductsEntity =
                restTemplate.getForEntity(PRODUCTS_URL, Product[].class, serverPort);

        assertNotNull(receivedProductsEntity);
        assertTrue("List of products not found", receivedProductsEntity.getStatusCode().value() == 200);
        assertTrue(receivedProductsEntity.getBody().length >= 1);

        Product receivedProductSingleEntity = receivedProductsEntity.getBody()[1];
        org.broadleafcommerce.core.catalog.domain.Product localProduct = catalogService.findProductById(receivedProductSingleEntity.getId());

        assertTrue(receivedProductSingleEntity.equals(localProduct));

    }

}
