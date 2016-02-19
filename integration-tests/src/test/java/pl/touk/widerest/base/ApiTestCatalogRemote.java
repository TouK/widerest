package pl.touk.widerest.base;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

import static org.junit.Assert.assertNotNull;

@Component
@WebIntegrationTest({
        "server.port:0"
})
public class ApiTestCatalogRemote implements ApiTestCatalog {

    @Value("${local.server.port}")
    protected String serverPort;

    private final RestTemplate restTemplate =
            new RestTemplate(Lists.newArrayList(new MappingJackson2HttpMessageConverter()));


    @PostConstruct
    protected void init() {

    }


    @Override
    public long getTotalProductsInCategoryCount(final long categoryId) {
//        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(ApiTestBase.PRODUCTS_IN_CATEGORY_COUNT_URL,
//                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort, categoryId);
//
//        assertNotNull(remoteCountEntity);
//
//        return remoteCountEntity.getBody();

        throw new UnsupportedOperationException();
    }

    @Override
    public long getTotalCategoriesCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTotalProductsCount() {
        throw new UnsupportedOperationException();
    }
}
