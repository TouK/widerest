import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.Application;
import pl.touk.widerest.BroadleafApplicationContextInitializer;

import javax.annotation.Resource;

@SpringApplicationConfiguration(classes = Application.class, initializers = BroadleafApplicationContextInitializer.class)
@WebIntegrationTest({
        "server.port:0"
})
public abstract class ApiTestBase {

    public static final String CATEGORIES_URL = "http://localhost:{port}/catalog/categories";
    public static final String SKUS_URL = "http://localhost:{port}/catalog/skus";
    public static final String PRODUCTS_URL = "http://localhost:{port}/catalog/products";

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    protected RestTemplate restTemplate = new RestTemplate();

    @Value("${local.server.port}")
    protected String serverPort;

}
