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
import pl.touk.widerest.api.cart.orders.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.dto.MediaDto;
import pl.touk.widerest.api.catalog.products.dto.ProductDto;
import pl.touk.widerest.api.catalog.products.dto.SkuDto;
import pl.touk.widerest.api.catalog.categories.dto.CategoryDto;
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

    protected final RestTemplate restTemplate = new RestTemplate(Lists.newArrayList(new MappingJackson2HttpMessageConverter()));
    protected final RestTemplate restTemplateForHalJsonHandling = new RestTemplate(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

    protected final BasicCookieStore cookieStore = new BasicCookieStore();
    protected final CloseableHttpClient authorizationServerClient = HttpClients.custom().setDefaultCookieStore(cookieStore).disableRedirectHandling().build();
    protected final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(new BaseOAuth2ProtectedResourceDetails());


    /* HATEOAS Rest Template */
    private final List<HttpMessageConverter<?>> httpMessageConverters = new ArrayList<>();
    private RestTemplate hateoasRestTemplate;


    /* (mst) Http Request 'Accept' Format to be used while testing */
    protected final TestHttpRequestEntity testHttpRequestEntity = new HalHttpRequestEntity();


    @Before
    public void clearSession() {
        cookieStore.clear();
    }

    /* This is the way to access admin related REST API!
     *
     *
     */
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

//    private MappingJackson2HttpMessageConverter getHalConverter() {
//        final RelProvider defaultRelProvider = getDefaultRelProvider();
//        final RelProvider annotationRelProvider = getAnnotationRelProvider();
//
//        final OrderAwarePluginRegistry<RelProvider, Class<?>> relProviderPluginRegistry = OrderAwarePluginRegistry
//                .create(Arrays.asList(defaultRelProvider, annotationRelProvider));
//
//        final DelegatingRelProvider delegatingRelProvider = new DelegatingRelProvider(relProviderPluginRegistry);
//
//        final ObjectMapper halObjectMapper = new ObjectMapper();
//        halObjectMapper.registerModule(new Jackson2HalModule());
//        halObjectMapper
//                .setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(delegatingRelProvider, null, null));
//
//        final MappingJackson2HttpMessageConverter halConverter = new MappingJackson2HttpMessageConverter();
//        halConverter.setSupportedMediaTypes(ImmutableList.of(new MediaType("*", "json",  MappingJackson2HttpMessageConverter.DEFAULT_CHARSET), new MediaType(" * ", "javascript", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET), MediaTypes.HAL_JSON));
//        halConverter.setObjectMapper(halObjectMapper);
//        return halConverter;
//    }

    private MappingJackson2HttpMessageConverter getHalConverter() {
        final RelProvider defaultRelProvider = getDefaultRelProvider();
        final RelProvider annotationRelProvider = getAnnotationRelProvider();

        final OrderAwarePluginRegistry<RelProvider, Class<?>> relProviderPluginRegistry = OrderAwarePluginRegistry
                .create(Arrays.asList(defaultRelProvider, annotationRelProvider));

        final DelegatingRelProvider delegatingRelProvider = new DelegatingRelProvider(relProviderPluginRegistry);

        final ObjectMapper halObjectMapper = new ObjectMapper();
        halObjectMapper.registerModule(new Jackson2HalModule());
        halObjectMapper
                .setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(delegatingRelProvider, null, null));

        final MappingJackson2HttpMessageConverter halConverter = new MappingJackson2HttpMessageConverter();
        halConverter.setSupportedMediaTypes(ImmutableList.of(
                new MediaType("application", "hal+json"),
                new MediaType("*", "json",  MappingJackson2HttpMessageConverter.DEFAULT_CHARSET),
                new MediaType("*", "javascript", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET)
            )
        );
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

    protected long getIdFromEntity(final ResponseEntity responseEntity) {
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

    protected long getLocalTotalProductsInCategoryCount(final long categoryId) {
        return catalogService.findCategoryById(categoryId).getAllProductXrefs().stream()
                .map(CategoryProductXref::getProduct)
                .filter(CatalogUtils::archivedProductFilter)
                .count();
    }



    protected long getRemoteTotalProductsCount() {
        final HttpEntity<Long> remoteCountEntity = restTemplate.exchange(PRODUCTS_COUNT_URL,
                HttpMethod.GET, testHttpRequestEntity.getTestHttpRequestEntity(), Long.class, serverPort);

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

    protected long getRemoteTotalSkusForProductCount(final long productId) {

        final HttpEntity<Long> remoteCountEntity = restTemplate.exchange(SKUS_COUNT_URL,
                HttpMethod.GET, testHttpRequestEntity.getTestHttpRequestEntity(), Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    protected long getRemoteTotalCategoriesForProductCount(final long productId) {
        final HttpEntity<Long> remoteCountEntity = restTemplate.exchange(CATEGORIES_BY_PRODUCT_BY_ID_COUNT,
                HttpMethod.GET, testHttpRequestEntity.getTestHttpRequestEntity(), Long.class, serverPort, productId);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    protected long getLocalTotalCategoriesForProductCount(final long productId) {
        return catalogService.findProductById(productId)
                .getAllParentCategoryXrefs().stream()
                .map(CategoryProductXref::getCategory)
                .filter(CatalogUtils::archivedCategoryFilter)
                .count();
    }


    protected long getLocalTotalSkusForProductCount(final long productId) {
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
        oAuth2AdminRestTemplate().put(MEDIA_BY_KEY_URL, mediaDto, serverPort, productId, skuId, key);
    }

    protected ResponseEntity<CategoryDto> getRemoteTestCategoryByIdEntity(final long categoryId) {
        final ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, categoryId);

        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

        return receivedCategoryEntity;
    }

    protected CategoryDto getRemoteTestCategoryByIdDto(final long categoryId) {
        return getRemoteTestCategoryByIdEntity(categoryId).getBody();
    }


    protected ResponseEntity<ProductDto> getRemoteTestProductByIdEntity(final long productId) {
        final ResponseEntity<ProductDto> receivedProductEntity =
                restTemplate.getForEntity(PRODUCT_BY_ID_URL, ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));
        return receivedProductEntity;
    }

    protected ProductDto getRemoteTestProductByIdDto(final long productId) {
        return getRemoteTestProductByIdEntity(productId).getBody();
    }

    protected ResponseEntity<?> addNewTestSKUToProduct(final long productId, final SkuDto skuDto) {
        return oAuth2AdminRestTemplate().postForEntity(PRODUCT_BY_ID_SKUS, skuDto, null, serverPort, productId);
    }

    protected ResponseEntity<?> addCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId) {
        return oAuth2AdminRestTemplate().postForEntity(ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL, null, null,
                serverPort, rootCategoryId, serverPort, childCategoryId);
    }

    protected void removeCategoryToCategoryReference(final long rootCategoryId, final long childCategoryId) {
        oAuth2AdminRestTemplate().delete(ADD_SUBCATEGORY_IN_CATEGORY_BY_ID_URL + CATEGORY_BY_ID_URL,
                serverPort, rootCategoryId, serverPort, childCategoryId);
    }

    protected ResponseEntity<?> addProductToCategoryReference(final long categoryId, final long productId) {
        return oAuth2AdminRestTemplate().postForEntity(ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL + PRODUCT_BY_ID_URL, null, null, serverPort, categoryId, serverPort, productId);
    }

    protected void removeProductToCategoryReference(final long categoryId, final long productId) {
        oAuth2AdminRestTemplate().delete(ADD_PRODUCTS_IN_CATEGORY_BY_ID_URL + PRODUCT_BY_ID_URL, serverPort, categoryId, serverPort, productId);
    }

    /* --------------------------------  CLEANUP METHODS -------------------------------- */

    protected void removeRemoteTestCategories() {

        final ResponseEntity<CategoryDto[]> receivedCategoriesEntity =
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
        final ResponseEntity<ProductDto[]> receivedProductEntity = hateoasRestTemplate().exchange(PRODUCTS_URL,
                HttpMethod.GET, testHttpRequestEntity.getTestHttpRequestEntity(), ProductDto[].class, serverPort);

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

    protected String getAccessTokenFromLocationUrl(final String locationUrl) throws URISyntaxException {
        final String accessTokenUrl = locationUrl.replace("#", "?");
        final List<NameValuePair> authorizationParams = URLEncodedUtils.parse(new URI(accessTokenUrl), "UTF-8");

        return authorizationParams.stream()
                .filter(x -> x.getName().equals("access_token"))
                .collect(Collectors.toList()).get(0).getValue();
    }

    protected Date addNDaysToDate(final Date date, final int N) {
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

    protected HttpHeaders prepareJsonHttpHeadersWithToken(final String token) {
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + token);
        return requestHeaders;
    }

    protected HttpEntity<?> getProperEntity(final String token) {
        return new HttpEntity<>(prepareJsonHttpHeadersWithToken(token));
    }


    /* --------------------------------  ORDER METHODS -------------------------------- */

    private final HttpHeaders httpRequestHeader = new HttpHeaders();

    protected Integer createNewOrder(final String token) {
        final ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, getProperEntity(token), HttpHeaders.class, serverPort);

        return strapSufixId(anonymousOrderHeaders.getHeaders().getLocation().toString());
    }

    protected Integer strapSufixId(final String url) {
        // Assuming it is */df/ab/{sufix}
        final String[] tab = StringUtils.split(url, "/");
        return Integer.parseInt(tab[tab.length - 1]);
    }

    protected Pair generateAnonymousUser() throws URISyntaxException {
        final RestTemplate restTemplate = new RestTemplate();
        final URI FirstResponseUri = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null, serverPort);
        return Pair.of(restTemplate, strapToken(FirstResponseUri));
    }

    protected Pair generateAdminUser() throws URISyntaxException {
        final OAuth2RestTemplate adminRestTemplate = oAuth2AdminRestTemplate();
        final URI adminUri = adminRestTemplate.postForLocation(LOGIN_URL, null, serverPort);
        return Pair.of(adminRestTemplate, strapToken(adminUri));
    }

    protected ResponseEntity<HttpHeaders> deleteRemoveOrderItem(final RestTemplate restTemplate, final String token,
                                                              final Integer orderId, final Integer orderItemId) {

        final HttpEntity httpRequestEntity = new HttpEntity(prepareJsonHttpHeadersWithToken(token));

        return restTemplate.exchange(ORDERS_URL + "/" + orderId + "/items/" + orderItemId,
                HttpMethod.DELETE, httpRequestEntity, HttpHeaders.class, serverPort);
    }

    protected ResponseEntity<HttpHeaders> addItemToOrder(final long skuId, final Integer quantity, final String location, final String token, final RestTemplate restTemplate) {
        final OrderItemDto template = new OrderItemDto();
        template.setQuantity(quantity);
        template.setSkuId(skuId);

        final HttpEntity<OrderItemDto> httpRequestEntity = new HttpEntity(template, prepareJsonHttpHeadersWithToken(token));

        return restTemplate.exchange(location, HttpMethod.POST, httpRequestEntity, HttpHeaders.class, serverPort);
    }

    protected long getRemoteTotalOrdersCountValue(final String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(prepareJsonHttpHeadersWithToken(token));

        final HttpEntity<Long> remoteCountEntity = restTemplate.exchange(ORDERS_COUNT,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }


    protected Boolean givenOrderIdIsCancelled(final String adminToken, final Long orderId) {
        final HttpEntity<?> adminHttpEntity = getProperEntity(adminToken);
        final ResponseEntity<OrderDto[]> allOrders =
                oAuth2AdminRestTemplate().getForEntity(ORDERS_URL, OrderDto[].class, serverPort, adminHttpEntity);

        return new ArrayList<>(Arrays.asList(allOrders.getBody())).stream()
                .filter(x -> x.getOrderId() == orderId)
                .findAny()
                .map(e -> e.getStatus().equals(OrderStatus.CANCELLED))
                .orElse(false);
    }

    protected Integer getRemoteItemsInOrderCount(final Integer orderId, final String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(prepareJsonHttpHeadersWithToken(token));

        final HttpEntity<Integer> remoteCountEntity = restTemplate.exchange(ORDERS_URL + "/" + orderId + "/items/count",
                HttpMethod.GET, httpRequestEntity, Integer.class, serverPort);

        return remoteCountEntity.getBody();
    }

    protected List<DiscreteOrderItemDto> getItemsFromCart(final Integer orderId, final String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(prepareJsonHttpHeadersWithToken(token));

        final HttpEntity<DiscreteOrderItemDto[]> response = restTemplate.exchange(ORDERS_URL+"/"+orderId+"/items",
                HttpMethod.GET, httpRequestEntity, DiscreteOrderItemDto[].class, serverPort);

        return new ArrayList<>(Arrays.asList(response.getBody()));
    }

    protected DiscreteOrderItemDto getItemDetailsFromCart(final Integer orderId, final Long itemId, final String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(prepareJsonHttpHeadersWithToken(token));

        final HttpEntity<DiscreteOrderItemDto> response = restTemplate.exchange(ORDERS_URL+"/"+orderId+"/items/"+itemId,
                HttpMethod.GET, httpRequestEntity, DiscreteOrderItemDto.class, serverPort);

        return response.getBody();
    }

    protected OrderStatus getOrderStatus(Integer orderId, String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(prepareJsonHttpHeadersWithToken(token));

        final HttpEntity<OrderStatus> response = restTemplate.exchange(ORDERS_URL + "/" + orderId + "/status",
                HttpMethod.GET, httpRequestEntity, OrderStatus.class, serverPort);

        return response.getBody();

    }

    protected void whenRegistrationPerformed(final String username, final String password, final String email) {
        final MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("email", email);
        map.add("username", username);
        map.add("password", password);
        map.add("passwordConfirm", password);
        final HttpEntity requestEntity = new HttpEntity(map, new HttpHeaders());
        oAuth2RestTemplate.postForEntity(API_BASE_URL + "/customers/register", requestEntity, HttpHeaders.class, serverPort);
    }

    protected void whenLoggedIn(final String usertype, final String username, final String password) throws IOException {
        final HttpUriRequest request = RequestBuilder
                .post()
                .setUri("http://localhost:" + serverPort + "/login")
                .addParameter("usertype", usertype)
                .addParameter("username", username)
                .addParameter("password", password)
                .build();
        try (CloseableHttpResponse response = authorizationServerClient.execute(request)) {
        }
    }

    protected void whenAuthorizationRequestedFor(final Scope scope) throws IOException {
        final HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(authorizationServerClient);
        final ClientHttpRequest request = httpRequestFactory.createRequest(
                URI.create("http://localhost:" + serverPort + "/oauth/authorize?client_id=default&response_type=token&redirect_uri=" + OutOfBandUriHandler.OOB_URI + (scope != null ? "&scope=" + scope : "")),
                HttpMethod.GET
        );

        try (ClientHttpResponse response = request.execute()) {
            final HttpMessageConverterExtractor<Map> e = new HttpMessageConverterExtractor(Map.class, Arrays.asList(new MappingJackson2HttpMessageConverter()));
            final Map<String, String> map = e.extractData(response);
            final Optional<String> accessToken = Optional.ofNullable(map.get("access_token"));
            oAuth2RestTemplate.getOAuth2ClientContext().setAccessToken(accessToken.map(DefaultOAuth2AccessToken::new).orElse(null));
        }
    }
}
