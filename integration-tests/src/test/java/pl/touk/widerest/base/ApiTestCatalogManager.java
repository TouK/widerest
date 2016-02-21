package pl.touk.widerest.base;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.multitenancy.MultiTenancyConfig;
import pl.touk.widerest.api.catalog.categories.dto.CategoryDto;
import pl.touk.widerest.api.catalog.products.dto.ProductDto;

import java.util.Arrays;


public class ApiTestCatalogManager implements ApiTestCatalogOperations {

    private final String serverPort;

    public ApiTestCatalogManager(final String serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public ResponseEntity<?> addTestCategory(final CategoryDto categoryDto) throws HttpClientErrorException{
        return oAuth2AdminRestTemplate().postForEntity(ApiTestBase.CATEGORIES_URL, categoryDto, null, serverPort);
    }

    @Override
    public ResponseEntity<?> addTestProduct(final ProductDto productDto) {
        return oAuth2AdminRestTemplate().postForEntity(ApiTestBase.PRODUCTS_URL, productDto, null, serverPort);
    }

    @Override
    public ResponseEntity<?> addCategoryToCategoryReference(long rootCategoryId, long childCategoryId) {
        return oAuth2AdminRestTemplate().postForEntity(ApiTestBase.ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + ApiTestBase.CATEGORY_BY_ID_URL,
                null, null, serverPort, rootCategoryId, serverPort, childCategoryId);
    }

    @Override
    public void removeCategoryToCategoryReference(long rootCategoryId, long childCategoryId) {
        oAuth2AdminRestTemplate().delete(ApiTestBase.ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + ApiTestBase.CATEGORY_BY_ID_URL,
                serverPort, rootCategoryId, serverPort, childCategoryId);
    }

    @Override
    public ResponseEntity<?> addProductToCategoryReference(long categoryId, long productId) {
        return oAuth2AdminRestTemplate().postForEntity(ApiTestBase.ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL + ApiTestBase.PRODUCT_BY_ID_URL,
                null, null, serverPort, categoryId, serverPort, productId);
    }

    @Override
    public void removeProductToCategoryReference(long categoryId, long productId) {
        oAuth2AdminRestTemplate().delete(ApiTestBase.ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL + ApiTestBase.PRODUCT_BY_ID_URL,
                serverPort, categoryId, serverPort, productId);
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
