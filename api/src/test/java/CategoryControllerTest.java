import org.broadleafcommerce.core.catalog.domain.Category;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import pl.touk.widerest.Application;
import pl.touk.widerest.BroadleafApplicationContextInitializer;
import pl.touk.widerest.api.catalog.DtoConverters;
import pl.touk.widerest.api.catalog.dto.CategoryDto;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
//@WebAppConfiguration
public class CategoryControllerTest extends ApiTestBase {

    @Test
    public void readCategoriesTest() {

        ResponseEntity<CategoryDto[]> receivedCategoriesEntity =
                restTemplate.getForEntity(CATEGORIES_URL, CategoryDto[].class, serverPort);

        assertNotNull(receivedCategoriesEntity);
        assertTrue("List of categories not found", receivedCategoriesEntity.getStatusCode().value() == 200);
        assertTrue(receivedCategoriesEntity.getBody().length >= 1);
        assertTrue(receivedCategoriesEntity.getBody().length ==
                catalogService.findAllCategories().size());

    }

    @Test
    public void readCategoriesByIdTest() {

        List<Long> localCategoryIds = catalogService.findAllCategories().stream()
                .map(id -> id.getId()).collect(Collectors.toList());

        Random rnd = new Random();

        int pickedCategoryIndex = rnd.nextInt(localCategoryIds.size());
        long pickedCategoryId = localCategoryIds.get(pickedCategoryIndex);

        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORIES_URL + "/" + pickedCategoryId, CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryEntity);
        assertTrue("List of categories not found", receivedCategoryEntity.getStatusCode().value() == 200);

        Category localCategoryEntity = catalogService.findCategoryById(pickedCategoryId);
        CategoryDto receivedCategoryDto = receivedCategoryEntity.getBody();

        assertTrue(receivedCategoryDto.getName().equals(localCategoryEntity.getName()) &&
                receivedCategoryDto.getDescription().equals(localCategoryEntity.getDescription()));
    }

    @Test
    public void createReadDeleteTest() {

        CategoryDto categoryDto = CategoryDto.builder().name("testcategory").description("testcategory").build();

        ResponseEntity<CategoryDto> createdCategoryResponse = restTemplate.postForEntity(CATEGORIES_URL, categoryDto, null, serverPort);

        System.out.println("Location: " + createdCategoryResponse.getHeaders().getLocation());

        assertTrue(createdCategoryResponse.getStatusCode() == HttpStatus.CREATED);

        String createdCategoryLocationUri = createdCategoryResponse.getHeaders().getLocation().toString();

        assertNotNull(createdCategoryLocationUri);
        assertTrue(!createdCategoryLocationUri.isEmpty());

        ResponseEntity<CategoryDto> receivedCategoryDto = restTemplate.getForEntity(
                createdCategoryLocationUri, CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryDto);

        assertTrue(categoryDto.getName().equals(receivedCategoryDto.getBody().getName()) &&
                categoryDto.getDescription().equals(receivedCategoryDto.getBody().getDescription()));


        restTemplate.delete(createdCategoryLocationUri, 1);

        ResponseEntity<CategoryDto> receivedCategoryDtoAfterDelete = restTemplate.getForEntity(
                createdCategoryLocationUri, CategoryDto.class, serverPort);

        assertNotNull(receivedCategoryDtoAfterDelete);

        if(receivedCategoryDtoAfterDelete.getBody() != null) {
            assertTrue(!receivedCategoryDtoAfterDelete.getBody().getName().equals(categoryDto.getName()));
        }
    }

}
