import org.apache.commons.httpclient.HttpClientError;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.catalog.dto.CategoryDto;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
//@ContextConfiguration(locations = {"classpath:/blc-config/site/bl-*-applicationContext.xml"})
//@WebAppConfiguration
public class CategoryControllerTest extends ApiTestBase {

    private static final String CATEGORIES_COUNT_URL = "http://localhost:{port}/catalog/categories/count";

    private HttpHeaders httpRequestHeader;

    @Before
    public void initCategoryTests() {
        this.httpRequestHeader = new HttpHeaders();
    }

    private long getRemoteTotalCountValue() {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> httpRequestEntity = new HttpEntity<>(null, httpRequestHeader);

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_COUNT_URL,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }

    private ResponseEntity<?> addNewTestCategory() {

        CategoryDto categoryDto = (CategoryDto) DtoTestFactory.getDtoTestObject(DtoTestType.CATEGORY_DTO);

        ResponseEntity<CategoryDto> remoteAddCategoryEntity = restTemplate.postForEntity(CATEGORIES_URL, categoryDto, null, serverPort);

        return remoteAddCategoryEntity;
    }

   
    @Test
    public void localAndRemoteCountValuesAreEqualTest() {
        assertThat(getRemoteTotalCountValue(), equalTo((long)catalogService.findAllCategories().size()));
    }

    @Test
    public void addingNewCategoryIncreasesTotalCountNumber() {
        long currentCategoryCount = getRemoteTotalCountValue();

        addNewTestCategory();

        assertThat(getRemoteTotalCountValue(), equalTo(currentCategoryCount + 1));
    }

    @Test(expected = HttpClientErrorException.class)
    public void addingDuplicateCategoryDoesNotIncreaseTotalCountNumber() {

        addNewTestCategory();

        long currentCategoryCount = getRemoteTotalCountValue();

        addNewTestCategory();

        assertThat(getRemoteTotalCountValue(), equalTo(currentCategoryCount));

    }



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
