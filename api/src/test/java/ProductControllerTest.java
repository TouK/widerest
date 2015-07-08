import org.broadleafcommerce.core.catalog.domain.Product;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.ProductDto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = Application.class)
//@WebAppConfiguration
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = Application.class)
///@WebAppConfiguration
public class ProductControllerTest extends ApiTestBase {

    public final String PRODUCTS_URL = "http://localhost:{port}/catalog/product/";
    @Test
    public void readProductsTest() {

        //when
        ResponseEntity<ProductDto[]> receivedProductsEntity =
                restTemplate.getForEntity(PRODUCTS_URL, ProductDto[].class, serverPort);


        assertNotNull(receivedProductsEntity);
        assertTrue("List of products not found", receivedProductsEntity.getStatusCode().value() == 200);

        ProductDto[] receivedProducts = receivedProductsEntity.getBody();

        /* Enable Spring! */
        List<ProductDto> localProducts = catalogService.findAllProducts().stream()
                .map(DtoConverters.productEntityToDto)
                .collect(Collectors.toList());


        assertTrue(Arrays.deepEquals(receivedProducts, localProducts.toArray()));
    }

    @Test
    public void readProductsByIdTest() {
        ResponseEntity<ProductDto[]> receivedProductsEntity =
                restTemplate.getForEntity(PRODUCTS_URL, ProductDto[].class, serverPort);

        assertNotNull(receivedProductsEntity);
        assertTrue("List of products not found", receivedProductsEntity.getStatusCode().value() == 200);
        assertTrue(receivedProductsEntity.getBody().length >= 1);

        ProductDto receivedProductSingleEntity = receivedProductsEntity.getBody()[1];
        Product localProduct = catalogService.findProductById(receivedProductSingleEntity.getProductId());

//        org.broadleafcommerce.core.catalog.domain.Product localProduct = catalogService.findProductById(receivedProductSingleEntity.getId());

        assertTrue(receivedProductSingleEntity.equals(localProduct));

    }

}
