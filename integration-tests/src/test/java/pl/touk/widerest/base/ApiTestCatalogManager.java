package pl.touk.widerest.base;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.multitenancy.MultiTenancyConfig;
import pl.touk.widerest.api.catalog.categories.dto.CategoryDto;
import pl.touk.widerest.api.catalog.products.dto.MediaDto;
import pl.touk.widerest.api.catalog.products.dto.ProductDto;
import pl.touk.widerest.api.catalog.products.dto.SkuDto;

import java.util.Arrays;


public class ApiTestCatalogManager implements ApiTestCatalogOperations {

    private final String serverPort;


    public ApiTestCatalogManager(final String serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public ResponseEntity<?> addTestCategory(final CategoryDto categoryDto) throws HttpClientErrorException{
        return oAuth2AdminRestTemplate().postForEntity(ApiTestUrls.CATEGORIES_URL, categoryDto, null, serverPort);
    }

    @Override
    public ResponseEntity<?> addTestProduct(final ProductDto productDto) {
        return oAuth2AdminRestTemplate().postForEntity(ApiTestUrls.PRODUCTS_URL, productDto, null, serverPort);
    }

    @Override
    public ResponseEntity<?> addTestSKUToProduct(final long productId, final SkuDto skuDto) {
        return oAuth2AdminRestTemplate().postForEntity(ApiTestUrls.PRODUCT_BY_ID_SKUS, skuDto, null, serverPort, productId);
    }

    @Override
    public ResponseEntity<?> addTestMediaToSku(final long productId, final long skuId, final String key, final MediaDto mediaDto) {
        return oAuth2AdminRestTemplate().exchange(ApiTestUrls.MEDIA_BY_KEY_URL, HttpMethod.PUT,
                new HttpEntity<>(mediaDto), Void.class,
                serverPort, productId, skuId, key);
    }

    @Override
    public ResponseEntity<?> addCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId) {
        return oAuth2AdminRestTemplate().postForEntity(ApiTestUrls.ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + ApiTestUrls.CATEGORY_BY_ID_URL,
                null, null, serverPort, rootCategoryId, serverPort, childCategoryId);
    }

    @Override
    public ResponseEntity<?> removeCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId) {
        return oAuth2AdminRestTemplate().exchange(ApiTestUrls.ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + ApiTestUrls.CATEGORY_BY_ID_URL,
                HttpMethod.DELETE, null, Void.class, serverPort, rootCategoryId, serverPort, childCategoryId);
    }

    @Override
    public ResponseEntity<?> addProductToCategoryReference(final long categoryId, final long productId) {
        return oAuth2AdminRestTemplate().postForEntity(ApiTestUrls.ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL + ApiTestUrls.PRODUCT_BY_ID_URL,
                null, null, serverPort, categoryId, serverPort, productId);
    }

    @Override
    public ResponseEntity<?> removeTestCategory(final long categoryId) {
        return oAuth2AdminRestTemplate().exchange(ApiTestUrls.CATEGORY_BY_ID_URL,
                HttpMethod.DELETE, null, Void.class, serverPort, categoryId);
    }

    @Override
    public ResponseEntity<?> removeProductToCategoryReference(final long categoryId, final long productId) {
        return oAuth2AdminRestTemplate().exchange(ApiTestUrls.ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL + ApiTestUrls.PRODUCT_BY_ID_URL,
                HttpMethod.DELETE, null, Void.class, serverPort, categoryId, serverPort, productId);
    }

    protected OAuth2RestTemplate oAuth2AdminRestTemplate() {

        final ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        resourceDetails.setGrantType("password");
        resourceDetails.setAccessTokenUri("http://localhost:" + serverPort + "/oauth/token");
        resourceDetails.setClientId(MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER);
        resourceDetails.setScope(Arrays.asList("staff"));
        resourceDetails.setUsername("backoffice/admin");
        resourceDetails.setPassword("admin");

        final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);
        oAuth2RestTemplate.setMessageConverters(Lists.newArrayList(new MappingJackson2HttpMessageConverter()));
        return oAuth2RestTemplate;
    }
}
