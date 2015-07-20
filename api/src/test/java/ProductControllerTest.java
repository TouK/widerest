import org.broadleafcommerce.core.catalog.domain.Product;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.ProductDto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = Application.class)

public class ProductControllerTest extends ApiTestBase {

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
