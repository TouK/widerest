import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.catalog.service.CatalogServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.context.web.WebAppConfiguration;
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

    public static final String CATEGORIES_URL = "http://localhost:{port}/catalog/categories";
    public static final String PRODUCTS_URL = "http://localhost:{port}/catalog/products";
    public static final String ORDERS_URL = "http://localhost:8080/catalog/orders";
    public static final String LOGIN_URL = "http://localhost:8080/login";

    public static final String OAUTH_AUTHORIZATION = "http://localhost:8080/oauth/authorize?client_id=test&response_type=token&redirect_uri=/";

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
            resourceDetails.setAccessTokenUri("http://localhost:" + /*serverPort*/ "8080" + "/oauth/token");
            resourceDetails.setClientId("test");
            resourceDetails.setScope(scopes);

            resourceDetails.setUsername("backoffice/admin");
            resourceDetails.setPassword("admin");

            return new OAuth2RestTemplate(resourceDetails);
    }

}
