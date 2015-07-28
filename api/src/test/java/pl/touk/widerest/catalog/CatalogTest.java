package pl.touk.widerest.catalog;

import org.springframework.boot.test.SpringApplicationConfiguration;
import pl.touk.widerest.Application;
import pl.touk.widerest.base.ApiTestBase;

/**
 * Created by mst on 28.07.15.
 */
@SpringApplicationConfiguration(classes = Application.class)
public class CatalogTest extends ApiTestBase {


    /* (mst) Tests involving the entire catalog (eg: create a category -> create a product -> add it to the category ->
             add 2 additional SKUS -> ...
             go here
     */
}
