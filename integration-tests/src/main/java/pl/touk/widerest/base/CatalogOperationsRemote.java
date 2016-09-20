package pl.touk.widerest.base;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.skus.SkuDto;

import java.io.IOException;

import static pl.touk.widerest.base.ApiTestUrls.CATEGORY_BY_ID_URL;

public class CatalogOperationsRemote implements CatalogOperations {

    protected final String serverPort;

    protected final RestTemplate restTemplate;

    public CatalogOperationsRemote(final RestTemplate restTemplate, final String serverPort) throws IOException {
        this.serverPort = serverPort;
        this.restTemplate = restTemplate;
    }

    @Override
    public ResponseEntity<?> addCategory(final CategoryDto categoryDto) throws HttpClientErrorException{
        return restTemplate.postForEntity(ApiTestUrls.CATEGORIES_URL, categoryDto, null, serverPort);
    }

    @Override
    public void modifyCategory(final long categoryId, final CategoryDto categoryDto) {
        restTemplate.put(ApiTestUrls.CATEGORY_BY_ID_URL, categoryDto, serverPort, categoryId);
    }

    @Override
    public CategoryDto getCategory(long categoryId) {
        return restTemplate.getForObject(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, categoryId);
    }

    @Override
    public ResponseEntity<?> addProduct(final ProductDto productDto) {
        return restTemplate.postForEntity(ApiTestUrls.PRODUCTS_URL, productDto, null, serverPort);
    }

    @Override
    public ResponseEntity<?> addTestSKUToProduct(final long productId, final SkuDto skuDto) {
        return restTemplate.postForEntity(ApiTestUrls.PRODUCT_BY_ID_SKUS, skuDto, null, serverPort, productId);
    }

    @Override
    public ResponseEntity<?> addTestMediaToSku(final long productId, final long skuId, final String key, final MediaDto mediaDto) {
        return restTemplate.exchange(ApiTestUrls.MEDIA_BY_KEY_URL, HttpMethod.PUT,
                new HttpEntity<>(mediaDto), Void.class,
                serverPort, productId, skuId, key);
    }

    @Override
    public ResponseEntity<?> addCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId) {
        return restTemplate.postForEntity(ApiTestUrls.ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL,
                null, null, serverPort, rootCategoryId, serverPort, childCategoryId);
    }

    @Override
    public ResponseEntity<?> removeCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId) {
        return restTemplate.exchange(ApiTestUrls.ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL,
                HttpMethod.DELETE, null, Void.class, serverPort, rootCategoryId, serverPort, childCategoryId);
    }

    @Override
    public ResponseEntity<?> addProductToCategoryReference(final long categoryId, final long productId) {
        return restTemplate.postForEntity(ApiTestUrls.ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL + ApiTestUrls.PRODUCT_BY_ID_URL,
                null, null, serverPort, categoryId, serverPort, productId);
    }

    @Override
    public ResponseEntity<?> removeCategory(final long categoryId) {
        return restTemplate.exchange(CATEGORY_BY_ID_URL,
                HttpMethod.DELETE, null, Void.class, serverPort, categoryId);
    }

    @Override
    public ResponseEntity<?> removeProductToCategoryReference(final long categoryId, final long productId) {
        return restTemplate.exchange(ApiTestUrls.ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL + ApiTestUrls.PRODUCT_BY_ID_URL,
                HttpMethod.DELETE, null, Void.class, serverPort, categoryId, serverPort, productId);
    }

    @Override
    public Resources<CategoryDto> getSubcategories(final long categoryId) {
        return restTemplate.exchange(
                ApiTestUrls.SUBCATEGORY_IN_CATEGORY_BY_ID_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Resources<CategoryDto>>() {
                },
                serverPort,
                categoryId
        ).getBody();
    }

}
