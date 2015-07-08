import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;


public abstract class ApiTestBase {

    public static final String CATEGORIES_URL = "http://localhost:{port}/catalog/categories";


    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    protected RestTemplate restTemplate = new RestTemplate();

    protected final int serverPort = 8080;

}
