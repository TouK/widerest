package pl.touk.widerest.base;

import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.Application;
import pl.touk.widerest.BroadleafApplicationContextInitializer;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringApplicationConfiguration(classes = Application.class, initializers = BroadleafApplicationContextInitializer.class)
@WebIntegrationTest({
        "server.port:0"
})
public abstract class ApiTestBase {

    /* Categories */
    public static final String CATEGORIES_URL = "http://localhost:{port}/catalog/categories";
    public static final String CATEGORIES_COUNT_URL = CATEGORIES_URL + "/count";
    public static final String PRODUCTS_IN_CATEGORY_URL = CATEGORIES_URL + "/{categoryId}/products";
    public static final String PRODUCTS_IN_CATEGORY_COUNT_URL = PRODUCTS_IN_CATEGORY_URL + "/count";

    /* Products */
    public static final String PRODUCTS_URL = "http://localhost:{port}/catalog/products";
    public static final String PRODUCTS_COUNT_URL = PRODUCTS_URL + "/count";
    public static final String PRODUCT_BY_ID_SKUS = PRODUCTS_URL + "/{productId}/skus";
    public static final String PRODUCT_BY_ID_SKU_BY_ID = PRODUCT_BY_ID_SKUS + "/{skuId}";
    public static final String SKUS_COUNT_URL = PRODUCT_BY_ID_SKUS + "/count";


    /* Orders */
    public static final String ORDERS_URL = "http://localhost:{port}/catalog/orders";

    public static final String LOGIN_URL = "http://localhost:{port}/login";


    public static final String OAUTH_AUTHORIZATION = "http://localhost:{port}/oauth/authorize?client_id=test&response_type=token&redirect_uri=/";

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    protected RestTemplate restTemplate = new RestTemplate();

    @Value("${local.server.port}")
    protected String serverPort;


    /* This is the way to access admin related REST API!
     *
     * TODO: inject constants instead of hardcoding them
     *
     */
    protected OAuth2RestTemplate oAuth2AdminRestTemplate() {

            List<String> scopes = new ArrayList<>();
            scopes.add("site");
            scopes.add("backoffice");

            ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
            resourceDetails.setGrantType("password");
            resourceDetails.setAccessTokenUri("http://localhost:" + serverPort + "/oauth/token");
            resourceDetails.setClientId("test");
            resourceDetails.setScope(scopes);

            resourceDetails.setUsername("backoffice/admin");
            resourceDetails.setPassword("admin");

            return new OAuth2RestTemplate(resourceDetails);
    }

}
