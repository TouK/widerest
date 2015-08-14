package pl.touk.widerest.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.broadleafcommerce.common.media.domain.MediaDto;
import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.AnnotationRelProvider;
import org.springframework.hateoas.core.DefaultRelProvider;
import org.springframework.hateoas.core.DelegatingRelProvider;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.Application;
import pl.touk.widerest.BroadleafApplicationContextInitializer;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.dto.CategoryDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@SpringApplicationConfiguration(classes = Application.class, initializers = BroadleafApplicationContextInitializer.class)
@WebIntegrationTest({
        "server.port:0"
})
public abstract class ApiTestBase {

    /* Categories */
    public static final String CATEGORIES_URL = "http://localhost:{port}/catalog/categories";
    public static final String CATEGORY_BY_ID_URL = CATEGORIES_URL + "/{categoryId}";
    public static final String CATEGORIES_COUNT_URL = CATEGORIES_URL + "/count";
    public static final String PRODUCTS_IN_CATEGORY_URL = CATEGORIES_URL + "/{categoryId}/products";
    public static final String PRODUCTS_IN_CATEGORY_BY_ID_URL = PRODUCTS_IN_CATEGORY_URL + "/{productId}";
    public static final String PRODUCTS_IN_CATEGORY_COUNT_URL = PRODUCTS_IN_CATEGORY_URL + "/count";

    /* Products */
    public static final String PRODUCTS_URL = "http://localhost:{port}/catalog/products";
    public static final String PRODUCT_BY_ID_URL = PRODUCTS_URL + "/{productId}";
    public static final String PRODUCTS_COUNT_URL = PRODUCTS_URL + "/count";
    public static final String PRODUCT_BY_ID_SKUS = PRODUCTS_URL + "/{productId}/skus";
    public static final String PRODUCT_BY_ID_SKU_BY_ID = PRODUCT_BY_ID_SKUS + "/{skuId}";
    public static final String PRODUCT_BY_ID_SKUS_DEFAULT = PRODUCT_BY_ID_SKUS + "/default";
    public static final String CATEGORIES_BY_PRODUCT_BY_ID_COUNT = PRODUCT_BY_ID_URL + "/categories/count";
    public static final String SKUS_COUNT_URL = PRODUCT_BY_ID_SKUS + "/count";


    /* Orders */
    public static final String ORDERS_URL = "http://localhost:{port}/orders";

    public static final String LOGIN_URL = "http://localhost:{port}/login";


    public static final String OAUTH_AUTHORIZATION = "http://localhost:{port}/oauth/authorize?client_id=test&response_type=token&redirect_uri=/";

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    protected RestTemplate restTemplate = new RestTemplate();
    /* HATEOAS Rest Template */
    private List<HttpMessageConverter<?>> httpMessageConverters = new ArrayList<>();
    private RestTemplate hateoasRestTemplate;

    @Value("${local.server.port}")
    protected String serverPort;


    private HttpHeaders httpJsonRequestHeaders;
    private HttpEntity<String> httpJsonRequestEntity;

    private HttpHeaders httpXmlRequestHeaders;
    private HttpEntity<String> httpXmlRequestEntity;

    public HttpEntity<String> getHttpJsonRequestEntity() {
        if(httpJsonRequestHeaders == null) {
            httpJsonRequestHeaders = new HttpHeaders();
            httpJsonRequestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        }

        if(httpJsonRequestEntity == null) {
            httpJsonRequestEntity= new HttpEntity<>(httpJsonRequestHeaders);
        }

        return httpJsonRequestEntity;
    }

    public HttpEntity<String> getHttpXmlRequestEntity() {
        if(httpXmlRequestHeaders == null) {
            httpXmlRequestHeaders = new HttpHeaders();
            httpXmlRequestHeaders.set("Accept", MediaType.APPLICATION_XML_VALUE);
        }

        if(httpXmlRequestEntity == null) {
            httpXmlRequestEntity= new HttpEntity<>(httpXmlRequestHeaders);
        }

        return httpXmlRequestEntity;
    }




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

    protected RestTemplate hateoasRestTemplate() {
        if(hateoasRestTemplate == null) {
            httpMessageConverters.add(getHalConverter());
            hateoasRestTemplate = new RestTemplate();
            hateoasRestTemplate.setMessageConverters(httpMessageConverters);
        }

        return hateoasRestTemplate;
    }

    private DefaultRelProvider getDefaultRelProvider() {
        return new DefaultRelProvider();
    }

    private AnnotationRelProvider getAnnotationRelProvider() {
        return new AnnotationRelProvider();
    }

    private MappingJackson2HttpMessageConverter getHalConverter() {
        RelProvider defaultRelProvider = getDefaultRelProvider();
        RelProvider annotationRelProvider = getAnnotationRelProvider();

        OrderAwarePluginRegistry<RelProvider, Class<?>> relProviderPluginRegistry = OrderAwarePluginRegistry
                .create(Arrays.asList(defaultRelProvider, annotationRelProvider));

        DelegatingRelProvider delegatingRelProvider = new DelegatingRelProvider(relProviderPluginRegistry);

        ObjectMapper halObjectMapper = new ObjectMapper();
        halObjectMapper.registerModule(new Jackson2HalModule());
        halObjectMapper
                .setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(delegatingRelProvider, null));

        MappingJackson2HttpMessageConverter halConverter = new MappingJackson2HttpMessageConverter();
        halConverter.setSupportedMediaTypes(ImmutableList.of(new MediaType("*", "json", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET), new MediaType("*", "javascript", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET)));

        halConverter.setObjectMapper(halObjectMapper);
        return halConverter;
    }

    protected long getIdFromLocationUrl(String locationUrl) {
        return Long.parseLong(locationUrl.substring(locationUrl.lastIndexOf('/') + 1));
    }



    /* ---------------- TEST HELPER/COMMON METHODS ---------------- */
    public long getRemoteTotalCategoriesCount() {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_COUNT_URL,
                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    public long getLocalTotalCategoriesCount() {
        return catalogService.findAllCategories().stream()
                .filter(entity -> ((Status) entity).getArchived() == 'N')
                .count();
    }

    protected long getRemoteTotalProductsInCategoryCount(long categoryId) {

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_IN_CATEGORY_COUNT_URL,
                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort, categoryId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    protected long getLocalTotalProductsInCategoryCount(long categoryId) {
        return catalogService.findCategoryById(categoryId).getAllProductXrefs().stream()
                .map(CategoryProductXref::getProduct)
                .filter(CatalogUtils::archivedProductFilter)
                .count();
    }



    protected long getRemoteTotalProductsCount() {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_COUNT_URL,
                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    protected long getLocalTotalProductsCount() {
        return catalogService.findAllProducts().stream()
                .filter(CatalogUtils::archivedProductFilter)
                .count();
    }

    protected long getLocalTotalSkus() {
        return catalogService.findAllSkus().stream().count();
    }

    protected long getRemoteTotalSkusForProductCount(long productId) {

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(SKUS_COUNT_URL,
                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    protected long getLocalTotalSkusForProductCount(long productId) {
        return catalogService.findProductById(productId).getAllSkus().stream().count();
    }


    protected ResponseEntity<?> addNewTestCategory(DtoTestType dtoTestType) throws HttpClientErrorException {
        return oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL, DtoTestFactory.getTestCategory(dtoTestType), null, serverPort);
    }

    protected ResponseEntity<?> addNewTestCategory(CategoryDto categoryDto) throws HttpClientErrorException {

        return oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL, categoryDto, null, serverPort);
    }

    protected ResponseEntity<?> addNewTestProduct(ProductDto productDto) {
        return oAuth2AdminRestTemplate().postForEntity(PRODUCTS_URL, productDto, null, serverPort);
    }

    protected ResponseEntity<?> addNewTestSKUToProduct(long productId, SkuDto skuDto) {
        return oAuth2AdminRestTemplate().postForEntity(PRODUCT_BY_ID_SKUS, skuDto, null, serverPort, productId);
    }

    protected ResponseEntity<?> addNewTestMediaToSku(MediaDto mediaDto) {
        throw new RuntimeException("Unimplemented");
    }



    private org.springframework.hateoas.Resource<ProductDto> getProductWithMultipleSkus() {


        ResponseEntity<org.springframework.hateoas.Resource<ProductDto>[]> receivedProductsEntity =
                hateoasRestTemplate().exchange(PRODUCTS_URL,
                        HttpMethod.GET, getHttpJsonRequestEntity(),
                        new ParameterizedTypeReference<org.springframework.hateoas.Resource<ProductDto>[]>() {
                        },
                        serverPort);

        org.springframework.hateoas.Resource<ProductDto> resultProduct = null;

        for (org.springframework.hateoas.Resource<ProductDto> p : receivedProductsEntity.getBody()) {
            if (p.getContent().getSkus().stream().count() >= 2) {
                resultProduct = p;
                break;
            }
        }

        return resultProduct;
    }

    /* ---------------- CLEANUP METHODS ---------------- */
    protected void removeRemoteTestCategories() {

        /* (mst) Retrieve all categories */
        ResponseEntity<CategoryDto[]> receivedCategoriesEntity =
                restTemplate.getForEntity(CATEGORIES_URL, CategoryDto[].class, serverPort);

        assertNotNull(receivedCategoriesEntity);
        assertThat(receivedCategoriesEntity.getStatusCode(), equalTo(HttpStatus.OK));

        /* (mst) Remove those, created by tests. Btw: REFACTOR TO LAMBDA */
        for(CategoryDto testCategory : receivedCategoriesEntity.getBody()) {
            if(testCategory.getName().contains(DtoTestFactory.TEST_CATEGORY_DEFAULT_NAME)) {
                oAuth2AdminRestTemplate().delete(testCategory.getId().getHref());
            }
        }
    }

    protected void removeLocalTestCategories() {
        catalogService.findAllCategories().stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .filter(x -> x.getName().contains(DtoTestFactory.TEST_CATEGORY_DEFAULT_NAME))
                .forEach(catalogService::removeCategory);
    }


    protected void removeLocalTestProducts() {
        catalogService.findAllProducts().stream()
                .filter(CatalogUtils::archivedProductFilter)
                .filter(x -> x.getName().contains(DtoTestFactory.TEST_PRODUCT_DEFAULT_NAME))
                .forEach(catalogService::removeProduct);
    }

    protected void removeRemoteTestPorducts() {
        ResponseEntity<ProductDto[]> receivedProductEntity = hateoasRestTemplate().exchange(PRODUCTS_URL,
                HttpMethod.GET, getHttpJsonRequestEntity(), ProductDto[].class, serverPort);

        assertNotNull(receivedProductEntity);
        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        for (ProductDto testProduct : receivedProductEntity.getBody()) {
            if(testProduct.getName().contains(DtoTestFactory.TEST_PRODUCT_DEFAULT_NAME)) {
                oAuth2AdminRestTemplate().delete(testProduct.getId().getHref());
            }
        }
    }

}
