import org.junit.Test;
import org.springframework.http.ResponseEntity;
import pl.touk.widerest.api.catalog.Category;
import pl.touk.widerest.api.catalog.CategoryController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = Application.class)
//@WebAppConfiguration
public class CategoryControllerTest extends ApiTestBase {

    @Test
    public void readCategoriesTest() {

        //when
        ResponseEntity<Category[]> receivedCategoriesEntity =
                restTemplate.getForEntity(CATEGORIES_URL, Category[].class, serverPort);


        assertNotNull(receivedCategoriesEntity);
        assertTrue("List of categories not found", receivedCategoriesEntity.getStatusCode().value() == 200);

        Category[] receivedCategories = receivedCategoriesEntity.getBody();

        /* Enable Spring! */
        List<Category> localCategories = catalogService.findAllCategories().stream()
                .map(CategoryController.categoryEntityToDto)
                .collect(Collectors.toList());


        assertTrue(Arrays.deepEquals(receivedCategories, localCategories.toArray()));
    }

    @Test
    public void readCategoriesByIdTest() {
        ResponseEntity<Category[]> receivedCategoriesEntity =
                restTemplate.getForEntity(CATEGORIES_URL, Category[].class, serverPort);

        assertNotNull(receivedCategoriesEntity);
        assertTrue("List of categories not found", receivedCategoriesEntity.getStatusCode().value() == 200);
        assertTrue(receivedCategoriesEntity.getBody().length >= 1);

        Category receivedCategorySingleEntity = receivedCategoriesEntity.getBody()[1];
        org.broadleafcommerce.core.catalog.domain.Category localCategory = catalogService.findCategoryById(receivedCategorySingleEntity.getId());

        assertTrue(receivedCategorySingleEntity.equals(localCategory));

    }

}
