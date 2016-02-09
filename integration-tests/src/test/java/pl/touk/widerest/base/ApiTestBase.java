package pl.touk.widerest.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.CategoryProductXref;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.AnnotationRelProvider;
import org.springframework.hateoas.core.DefaultRelProvider;
import org.springframework.hateoas.core.DelegatingRelProvider;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RestTemplate;
import pl.touk.multitenancy.MultiTenancyConfig;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.BroadleafApplicationContextInitializer;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.dto.MediaDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.paypal.gateway.PayPalSession;
import pl.touk.widerest.security.oauth2.OutOfBandUriHandler;
import pl.touk.widerest.security.oauth2.Scope;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@SpringApplicationConfiguration(classes = Application.class, initializers = BroadleafApplicationContextInitializer.class)
@WebIntegrationTest({
        "server.port:0", "auth0.domain:false", "management.port:0"
})
public abstract class ApiTestBase {

    public static final String API_BASE_URL = "http://localhost:{port}/v1";

    /* Categories */
    public static final String CATEGORIES_URL = API_BASE_URL + "/categories";
    public static final String CATEGORIES_FLAT_URL = API_BASE_URL + "/categories?flat=true";
    public static final String CATEGORY_BY_ID_URL = CATEGORIES_URL + "/{categoryId}";
    public static final String CATEGORIES_COUNT_URL = CATEGORIES_URL + "/count";
    public static final String PRODUCTS_IN_CATEGORY_URL = CATEGORIES_URL + "/{categoryId}/products";
    public static final String PRODUCTS_IN_CATEGORY_BY_ID_URL = PRODUCTS_IN_CATEGORY_URL + "/{productId}";
    public static final String ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL = PRODUCTS_IN_CATEGORY_URL + "?href=";
    public static final String PRODUCTS_IN_CATEGORY_COUNT_URL = PRODUCTS_IN_CATEGORY_URL + "/count";
    public static final String CATEGORY_AVAILABILITY_BY_ID_URL = CATEGORY_BY_ID_URL + "/availability";
    public static final String SUBCATEGORY_IN_CATEGORY_BY_ID_URL = CATEGORY_BY_ID_URL + "/subcategories";
    public static final String ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL = SUBCATEGORY_IN_CATEGORY_BY_ID_URL + "?href=";


    /* Products */
    public static final String PRODUCTS_URL = API_BASE_URL + "/products";
    public static final String PRODUCT_BY_ID_URL = PRODUCTS_URL + "/{productId}";
    public static final String PRODUCTS_COUNT_URL = PRODUCTS_URL + "/count";
    public static final String PRODUCT_BY_ID_SKUS = PRODUCTS_URL + "/{productId}/skus";
    public static final String PRODUCT_BY_ID_SKU_BY_ID = PRODUCT_BY_ID_SKUS + "/{skuId}";
    public static final String PRODUCT_BY_ID_SKUS_DEFAULT = PRODUCT_BY_ID_SKUS + "/default";
    public static final String CATEGORIES_BY_PRODUCT_BY_ID_COUNT = PRODUCT_BY_ID_URL + "/categories/count";
    public static final String SKUS_COUNT_URL = PRODUCT_BY_ID_SKUS + "/count";
    public static final String MEDIA_BY_KEY_URL = PRODUCT_BY_ID_SKU_BY_ID + "/media/{key}";
    public static final String BUNDLES_URL = PRODUCTS_URL + "/bundles";
    public static final String BUNDLE_BU_ID_URL = BUNDLES_URL + "/{bundleId}";
    public static final String PRODUCT_BY_ID_ATTRIBUTES_URL = PRODUCT_BY_ID_URL + "/attributes";
    public static final String PRODUCT_BY_ID_ATTRIBUTE_BY_NAME_URL = PRODUCT_BY_ID_ATTRIBUTES_URL + "/{attributeName}";

    /* Orders */
    public static final String ORDERS_URL = API_BASE_URL + "/orders";
    public static final String ORDER_BY_ID_URL = ORDERS_URL + "{orderId}";
    public static final String ORDERS_COUNT = ORDERS_URL+"/count";
    public static final String ORDERS_BY_ID_ITEMS = ORDER_BY_ID_URL + "/items";

    /* PayPal */
    public static final String SYSTEM_PROPERTIES_URL = API_BASE_URL + "/settings";
    public static final String PAYPAL_CREDENTIALS_ID_URL = SYSTEM_PROPERTIES_URL + "/" + PayPalSession.CLIENT_ID;
    public static final String PAYPAL_CREDENTIALS_SECRET_URL = SYSTEM_PROPERTIES_URL + "/" + PayPalSession.SECRET;

    /* Customer */
    public static final String CUSTOMERS_URL = API_BASE_URL + "/customers";

    public static final String LOGIN_URL = "http://localhost:{port}/login";

    public static final String OAUTH_AUTHORIZATION = "http://localhost:{port}/oauth/authorize?client_id=" + MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER + "&scope=customer&response_type=token&redirect_uri=/";


    public static final String SETTINGS_URL = API_BASE_URL + "/settings";
    public static final String SETTINGS_BY_NAME_URL = SETTINGS_URL + "/{settingName}";

    @PersistenceContext(unitName="blPU")
    protected EntityManager em;

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @Value("${local.server.port}")
    protected String serverPort;

    protected RestTemplate restTemplate = new RestTemplate(Lists.newArrayList(new MappingJackson2HttpMessageConverter()));
    protected RestTemplate restTemplateForHalJsonHandling = new RestTemplate(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

    protected BasicCookieStore cookieStore = new BasicCookieStore();
    protected CloseableHttpClient authorizationServerClient = HttpClients.custom().setDefaultCookieStore(cookieStore).disableRedirectHandling().build();
    protected OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(new BaseOAuth2ProtectedResourceDetails());


    /* HATEOAS Rest Template */
    private List<HttpMessageConverter<?>> httpMessageConverters = new ArrayList<>();
    private RestTemplate hateoasRestTemplate;

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



    @Before
    public void clearSession() {
        cookieStore.clear();
    }

    /* This is the way to access admin related REST API!
     *
     *
     */
    protected OAuth2RestTemplate oAuth2AdminRestTemplate() {

        ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        resourceDetails.setGrantType("password");
        resourceDetails.setAccessTokenUri("http://localhost:" + serverPort + "/oauth/token");
        resourceDetails.setClientId(MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER);
        resourceDetails.setScope(Arrays.asList("staff"));

        resourceDetails.setUsername("backoffice/admin");
        resourceDetails.setPassword("admin");

        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);
        oAuth2RestTemplate.setMessageConverters(Lists.newArrayList(new MappingJackson2HttpMessageConverter()));
        return oAuth2RestTemplate;
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
                .setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(delegatingRelProvider, null, null));

        MappingJackson2HttpMessageConverter halConverter = new MappingJackson2HttpMessageConverter();
        halConverter.setSupportedMediaTypes(ImmutableList.of(new MediaType("*", "json",  MappingJackson2HttpMessageConverter.DEFAULT_CHARSET), new MediaType(" * ", "javascript", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET), MediaTypes.HAL_JSON));
        halConverter.setObjectMapper(halObjectMapper);
        return halConverter;
    }

    protected long getIdFromLocationUrl(final String locationUrl) {
        if(locationUrl != null && org.apache.commons.lang.StringUtils.isNotEmpty(locationUrl)) {
            return Long.parseLong(locationUrl.substring(locationUrl.lastIndexOf('/') + 1));
        } else {
            return -1;
        }

    }

    protected long getIdFromEntity(ResponseEntity responseEntity) {
        return getIdFromLocationUrl(responseEntity.getHeaders().getLocation().toString());
    }

    /* ---------------- TEST HELPER/COMMON METHODS ---------------- */
//    public long getRemoteTotalCategoriesCount() {
//        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_COUNT_URL,
//                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort);
//
//        assertNotNull(remoteCountEntity);
//
//        return remoteCountEntity.getBody();
//    }

    public long getLocalTotalCategoriesCount() {
        return catalogService.findAllCategories().stream()
                .filter(entity -> ((Status) entity).getArchived() == 'N')
                .count();
    }

//    protected long getRemoteTotalProductsInCategoryCount(long categoryId) {
//
//        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_IN_CATEGORY_COUNT_URL,
//                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort, categoryId);
//
//        assertNotNull(remoteCountEntity);
//
//        return remoteCountEntity.getBody();
//    }

    protected long getLocalTotalProductsInCategoryCount(long categoryId) {
        return catalogService.findCategoryById(categoryId).getAllProductXrefs().stream()
                .map(CategoryProductXref::getProduct)
                .filter(CatalogUtils::archivedProductFilter)
                .count();
    }



    protected long getRemoteTotalProductsCount() {
        final HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_COUNT_URL,
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

    protected long getRemoteTotalCategoriesForProductCount(long productId) {
        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_BY_PRODUCT_BY_ID_COUNT,
                HttpMethod.GET, getHttpJsonRequestEntity(), Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    protected long getLocalTotalCategoriesForProductCount(long productId) {
        return catalogService.findProductById(productId)
                .getAllParentCategoryXrefs().stream()
                .map(CategoryProductXref::getCategory)
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();
    }


    protected long getLocalTotalSkusForProductCount(long productId) {
        return catalogService.findProductById(productId).getAllSkus().stream().count();
    }


    protected ResponseEntity<?> addNewTestCategory(final DtoTestType dtoTestType) throws HttpClientErrorException {
        return oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL, DtoTestFactory.getTestCategory(dtoTestType), null, serverPort);
    }

    protected ResponseEntity<?> addNewTestCategory(final CategoryDto categoryDto) throws HttpClientErrorException {

        return oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL, categoryDto, null, serverPort);
    }

    protected ResponseEntity<?> addNewTestProduct(final ProductDto productDto) {
        return oAuth2AdminRestTemplate().postForEntity(PRODUCTS_URL, productDto, null, serverPort);
    }

    protected long addNewTestCategory() {
        final ResponseEntity<?> newTestCategoryEntity = oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL, DtoTestFactory.getTestCategory(DtoTestType.NEXT), null, serverPort);
        assertThat(newTestCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        return getIdFromLocationUrl(newTestCategoryEntity.getHeaders().getLocation().toString());
    }

    protected void addOrUpdateNewTestSkuMediaToProductSku(final long productId, final long skuId, final String key, final MediaDto mediaDto) {
        oAuth2AdminRestTemplate().put(
                MEDIA_BY_KEY_URL,
                mediaDto,
                serverPort,
                productId,
                skuId,
                key);
    }


    protected ResponseEntity<CategoryDto> getRemoteTestCategoryByIdEntity(long categoryId) {
        ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, categoryId);

        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

        return receivedCategoryEntity;
    }

    protected CategoryDto getRemoteTestCategoryByIdDto(long categoryId) {
        return getRemoteTestCategoryByIdEntity(categoryId).getBody();
    }


    protected ResponseEntity<ProductDto> getRemoteTestProductByIdEntity(long productId) {
        ResponseEntity<ProductDto> receivedProductEntity =
                restTemplate.getForEntity(PRODUCT_BY_ID_URL, ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        return receivedProductEntity;
    }

    protected ProductDto getRemoteTestProductByIdDto(long productId) {
        return getRemoteTestProductByIdEntity(productId).getBody();
    }

    protected ResponseEntity<?> addNewTestSKUToProduct(long productId, SkuDto skuDto) {
        return oAuth2AdminRestTemplate().postForEntity(PRODUCT_BY_ID_SKUS, skuDto, null, serverPort, productId);
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

    /* --------------------------------  CLEANUP METHODS -------------------------------- */

    protected void removeRemoteTestCategories() {

        ResponseEntity<CategoryDto[]> receivedCategoriesEntity =
                restTemplate.getForEntity(CATEGORIES_URL, CategoryDto[].class, serverPort);

        assertThat(receivedCategoriesEntity.getStatusCode(), equalTo(HttpStatus.OK));

        Arrays.asList(receivedCategoriesEntity.getBody()).stream()
                .filter(x -> x.getName().contains(DtoTestFactory.TEST_CATEGORY_DEFAULT_NAME))
                .map(x -> {
                    oAuth2AdminRestTemplate().delete(x.getId().getHref());
                    return x;
                });
    }

    protected void removeLocalTestCategories() {
        catalogService.findAllCategories().stream()
                .filter(CatalogUtils::archivedCategoryFilter)
                .filter(x -> x.getName().contains(DtoTestFactory.TEST_CATEGORY_DEFAULT_NAME))
                .forEach(catalogService::removeCategory);
    }


    protected void removeRemoteTestProduct() {
        ResponseEntity<ProductDto[]> receivedProductEntity = hateoasRestTemplate().exchange(PRODUCTS_URL,
                HttpMethod.GET, getHttpJsonRequestEntity(), ProductDto[].class, serverPort);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));

        Arrays.asList(receivedProductEntity.getBody()).stream()
                .filter(x -> x.getName().contains(DtoTestFactory.TEST_PRODUCT_DEFAULT_NAME))
                .map(x -> {
                    oAuth2AdminRestTemplate().delete(x.getId().getHref());
                    return x;
                });
    }


    protected void removeLocalTestProducts() {
        catalogService.findAllProducts().stream()
                .filter(CatalogUtils::archivedProductFilter)
                .filter(x -> x.getName().contains(DtoTestFactory.TEST_PRODUCT_DEFAULT_NAME))
                .forEach(catalogService::removeProduct);
    }


    protected void removeLocalTestSkus() {
       catalogService.findAllSkus().stream()
               .filter(x -> ((x.getName().contains(DtoTestFactory.TEST_ADDITIONAL_SKU_NAME) ||
                            x.getName().contains("Sku"))))
               .forEach(catalogService::removeSku);
    }

      /* --------------------------------  CLEANUP METHODS -------------------------------- */


    /* --------------------------------  HELPER METHODS -------------------------------- */

    protected String getAccessTokenFromLocationUrl(String locationUrl) throws URISyntaxException {
        final String accessTokenUrl = locationUrl.replace("#", "?");
        final List<NameValuePair> authorizationParams = URLEncodedUtils.parse(new URI(accessTokenUrl), "UTF-8");

        return authorizationParams.stream()
                .filter(x -> x.getName().equals("access_token"))
                .collect(Collectors.toList()).get(0).getValue();
    }

    protected Date addNDaysToDate(Date date, int N) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, N);

        return cal.getTime();
    }

    protected String strapToken(final URI response) throws URISyntaxException {
        final String authorizationUrl = response.toString().replaceFirst("#", "?");
        final List<NameValuePair> authParams = URLEncodedUtils.parse(new URI(authorizationUrl), "UTF-8");

        return authParams.stream()
                .filter(x -> x.getName().equals("access_token"))
                .findFirst()
                .map(NameValuePair::getValue)
                .orElse(null);
    }


    /* --------------------------------  ORDER METHODS -------------------------------- */

    private HttpHeaders httpRequestHeader = new HttpHeaders();

    protected Integer createNewOrder(String token) {
        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, getProperEntity(token), HttpHeaders.class, serverPort);

        return strapSufixId(anonymousOrderHeaders.getHeaders().getLocation().toString());
    }

    protected Integer strapSufixId(String url) {
        // Assuming it is */df/ab/{sufix}
        String[] tab = StringUtils.split(url, "/");
        return Integer.parseInt(tab[tab.length - 1]);
    }

    protected Pair generateAnonymousUser() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI FirstResponseUri = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null, serverPort);
        return Pair.of(restTemplate, strapToken(FirstResponseUri));
    }

    protected Pair generateAdminUser() throws URISyntaxException {
        OAuth2RestTemplate adminRestTemplate = oAuth2AdminRestTemplate();
        URI adminUri = adminRestTemplate.postForLocation(LOGIN_URL, null, serverPort);
        String accessToken = strapToken(adminUri);
        return Pair.of(adminRestTemplate, accessToken);

    }

    protected ResponseEntity<HttpHeaders> deleteRemoveOrderItem(RestTemplate restTemplate, String token,
                                                              Integer orderId, Integer orderItemId) {

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, requestHeaders);

        return restTemplate.exchange(ORDERS_URL + "/" + orderId + "/items/" + orderItemId,
                HttpMethod.DELETE, httpRequestEntity, HttpHeaders.class, serverPort);

    }

    protected ResponseEntity<HttpHeaders> addItemToOrder(long skuId, Integer quantity, String location, String token, RestTemplate restTemplate) {
        OrderItemDto template = new OrderItemDto();
        template.setQuantity(quantity);
        template.setSkuId(skuId);


        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(template, requestHeaders);

        return restTemplate.exchange(location, HttpMethod.POST, httpRequestEntity, HttpHeaders.class, serverPort);
    }



    protected HttpEntity<?> getProperEntity(String token) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.add("Authorization", "Bearer " + token);
        return new HttpEntity<>(requestHeaders);
    }


    protected long getRemoteTotalOrdersCountValue(String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(ORDERS_COUNT,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }


    protected Boolean givenOrderIdIsCancelled(String adminToken, Long orderId) {
        HttpEntity<?> adminHttpEntity = getProperEntity(adminToken);
        ResponseEntity<OrderDto[]> allOrders =
                oAuth2AdminRestTemplate().getForEntity(ORDERS_URL, OrderDto[].class, serverPort, adminHttpEntity);

        return new ArrayList<>(Arrays.asList(allOrders.getBody())).stream()
                .filter(x -> x.getOrderId() == orderId)
                .findAny()
                .map(e -> e.getStatus().equals(OrderStatus.CANCELLED))
                .orElse(false);
    }

    protected Integer getRemoteItemsInOrderCount(Integer orderId, String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<Integer> remoteCountEntity = restTemplate.exchange(ORDERS_URL + "/" + orderId + "/items/count",
                HttpMethod.GET, httpRequestEntity, Integer.class, serverPort);

        return remoteCountEntity.getBody();
    }

    protected List<DiscreteOrderItemDto> getItemsFromCart(Integer orderId, String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<DiscreteOrderItemDto[]> response = restTemplate.exchange(ORDERS_URL+"/"+orderId+"/items",
                HttpMethod.GET, httpRequestEntity, DiscreteOrderItemDto[].class, serverPort);

        return new ArrayList<>(Arrays.asList(response.getBody()));

    }

    protected DiscreteOrderItemDto getItemDetailsFromCart(Integer orderId, Long itemId, String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<DiscreteOrderItemDto> response = restTemplate.exchange(ORDERS_URL+"/"+orderId+"/items/"+itemId,
                HttpMethod.GET, httpRequestEntity, DiscreteOrderItemDto.class, serverPort);

        return response.getBody();
    }

    protected OrderStatus getOrderStatus(Integer orderId, String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<OrderStatus> response = restTemplate.exchange(ORDERS_URL + "/" + orderId + "/status",
                HttpMethod.GET, httpRequestEntity, OrderStatus.class, serverPort);

        return response.getBody();

    }

    protected void whenRegistrationPerformed(String username, String password, String email) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("email", email);
        map.add("username", username);
        map.add("password", password);
        map.add("passwordConfirm", password);
        HttpEntity requestEntity = new HttpEntity(map, new HttpHeaders());
        oAuth2RestTemplate.postForEntity(API_BASE_URL + "/customers/register", requestEntity, HttpHeaders.class, serverPort);
    }

    protected void whenLoggedIn(String usertype, String username, String password) throws IOException {
        HttpUriRequest request = RequestBuilder
                .post()
                .setUri("http://localhost:" + serverPort + "/login")
                .addParameter("usertype", usertype)
                .addParameter("username", username)
                .addParameter("password", password)
                .build();
        try (CloseableHttpResponse response = authorizationServerClient.execute(request)) {
        }
    }

    protected void whenAuthorizationRequestedFor(Scope scope) throws IOException {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(authorizationServerClient);
        ClientHttpRequest request = httpRequestFactory.createRequest(
                URI.create("http://localhost:" + serverPort + "/oauth/authorize?client_id=default&response_type=token&redirect_uri=" + OutOfBandUriHandler.OOB_URI + (scope != null ? "&scope=" + scope : "")),
                HttpMethod.GET
        );

        try (ClientHttpResponse response = request.execute()) {
            HttpMessageConverterExtractor<Map> e = new HttpMessageConverterExtractor(Map.class, Arrays.asList(new MappingJackson2HttpMessageConverter()));
            Map<String, String> map = e.extractData(response);
            Optional<String> accessToken = Optional.ofNullable(map.get("access_token"));
            oAuth2RestTemplate.getOAuth2ClientContext().setAccessToken(accessToken.map(DefaultOAuth2AccessToken::new).orElse(null));
        }
    }



}
