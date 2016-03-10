package pl.touk.widerest.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Try;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.Resources;
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
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.api.categories.CategoryDto;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.orders.DiscreteOrderItemDto;
import pl.touk.widerest.api.orders.OrderDto;
import pl.touk.widerest.api.orders.OrderItemDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.security.oauth2.Scope;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public abstract class ApiTestBase {

    @PersistenceContext(unitName="blPU")
    protected EntityManager em;

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @Value("${local.server.port}")
    protected String serverPort;

    @Deprecated
    protected final RestTemplate restTemplate = new RestTemplate(Lists.newArrayList(new MappingJackson2HttpMessageConverter()));

    @Deprecated
    protected final RestTemplate restTemplateForHalJsonHandling = new RestTemplate(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));




    /* HATEOAS Rest Template */
    private final List<HttpMessageConverter<?>> httpMessageConverters = new ArrayList<>();
    private RestTemplate hateoasRestTemplate;


    /* (mst) Http Request 'Accept' Format to be used while testing */
    protected final TestHttpRequestEntity testHttpRequestEntity = new HalHttpRequestEntity();

    @Autowired
    protected ApiTestCatalogLocal apiTestCatalogLocal;

    @Autowired
    protected ApiTestCatalogRemote apiTestCatalogRemote;

    protected ApiTestCatalogOperations apiTestCatalogManager;

    @Autowired
    protected HttpHeadersWithTokenFactory httpHeadersWithTokenFactory;

    @PostConstruct
    public void init() {
        apiTestCatalogManager = new ApiTestCatalogManager(serverPort);
    }

    /* This is the way to access admin related REST API!
     *
     *
     */
    protected OAuth2RestTemplate oAuth2AdminRestTemplate() {

        final ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        resourceDetails.setGrantType("password");
        resourceDetails.setAccessTokenUri("http://localhost:" + serverPort + "/oauth/token");
        resourceDetails.setClientId("default");
        resourceDetails.setScope(Arrays.asList("staff"));
        resourceDetails.setUsername("backoffice/admin");
        resourceDetails.setPassword("admin");

        final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);
        oAuth2RestTemplate.setMessageConverters(Lists.newArrayList(new MappingJackson2HttpMessageConverter()));
        return oAuth2RestTemplate;
    }

    protected OAuth2RestTemplate oAuth2AdminHalRestTemplate() {

        final ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        resourceDetails.setGrantType("password");
        resourceDetails.setAccessTokenUri("http://localhost:" + serverPort + "/oauth/token");
        resourceDetails.setClientId("default");
        resourceDetails.setScope(Arrays.asList("staff"));
        resourceDetails.setUsername("backoffice/admin");
        resourceDetails.setPassword("admin");

        final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);
        oAuth2RestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));
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


    protected long addNewTestCategory() {
        final ResponseEntity<?> newTestCategoryEntity = apiTestCatalogManager.addTestCategory(DtoTestFactory.getTestCategory(DtoTestType.NEXT));
                //oAuth2AdminRestTemplate().postForEntity(CATEGORIES_URL, DtoTestFactory.getTestCategory(DtoTestType.NEXT), null, serverPort);
        assertThat(newTestCategoryEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        return ApiTestUtils.getIdFromLocationUrl(newTestCategoryEntity.getHeaders().getLocation().toString());
    }

    protected ResponseEntity<CategoryDto> getRemoteTestCategoryByIdEntity(final long categoryId) {
        final ResponseEntity<CategoryDto> receivedCategoryEntity =
                restTemplate.getForEntity(ApiTestUrls.CATEGORY_BY_ID_URL, CategoryDto.class, serverPort, categoryId);

        assertThat(receivedCategoryEntity.getStatusCode(), equalTo(HttpStatus.OK));

        return receivedCategoryEntity;
    }


    protected ResponseEntity<ProductDto> getRemoteTestProductByIdEntity(final long productId) {
        final ResponseEntity<ProductDto> receivedProductEntity =
                restTemplate.getForEntity(ApiTestUrls.PRODUCT_BY_ID_URL, ProductDto.class, serverPort, productId);

        assertThat(receivedProductEntity.getStatusCode(), equalTo(HttpStatus.OK));
        return receivedProductEntity;
    }

    protected ProductDto getRemoteTestProductByIdDto(final long productId) {
        return getRemoteTestProductByIdEntity(productId).getBody();
    }

    /* --------------------------------  CLEANUP METHODS -------------------------------- */

    protected void removeLocalTestCategories() {
        catalogService.findAllCategories().stream()
                .filter(CatalogUtils.nonArchivedCategory)
                .filter(x -> x.getName().contains(DtoTestFactory.TEST_CATEGORY_DEFAULT_NAME))
                .forEach(catalogService::removeCategory);
    }


    protected void removeLocalTestProducts() {
        catalogService.findAllProducts().stream()
                .filter(CatalogUtils.nonArchivedProduct)
                .filter(x -> x.getName().contains(DtoTestFactory.TEST_PRODUCT_DEFAULT_NAME))
                .forEach(catalogService::removeProduct);
    }

    protected HttpEntity<?> getProperEntity(final String token) {
        return new HttpEntity<>(httpHeadersWithTokenFactory.getHalHttpHeadersWithToken(token));
    }


    /* --------------------------------  ORDER METHODS -------------------------------- */

    protected int createNewOrder(final String token) {
        final ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ApiTestUrls.ORDERS_URL, getProperEntity(token), HttpHeaders.class, serverPort);

        return ApiTestUtils.strapSuffixId(anonymousOrderHeaders.getHeaders().getLocation().toString());
    }

    @Deprecated
    protected Pair<RestTemplate, String> generateAnonymousUser() throws URISyntaxException {
        final RestTemplate restTemplate = new RestTemplate();
        final URI FirstResponseUri = restTemplate.postForLocation(ApiTestUrls.OAUTH_AUTHORIZATION, null, serverPort);
        return Pair.of(restTemplate, ApiTestUtils.strapTokenFromURI(FirstResponseUri));
    }

    protected Pair generateAdminUser() throws URISyntaxException {
        final OAuth2RestTemplate adminRestTemplate = oAuth2AdminHalRestTemplate();
        final URI adminUri = adminRestTemplate.postForLocation(ApiTestUrls.LOGIN_URL, null, serverPort);
        return Pair.of(adminRestTemplate, ApiTestUtils.strapTokenFromURI(adminUri));
    }

    protected ResponseEntity<HttpHeaders> deleteRemoveOrderItem(final RestTemplate restTemplate, final String token,
                                                              final Integer orderId, final Integer orderItemId) {

        final HttpEntity httpRequestEntity = new HttpEntity(httpHeadersWithTokenFactory.getHalHttpHeadersWithToken(token));

        return restTemplate.exchange(ApiTestUrls.ORDERS_URL + "/" + orderId + "/items/" + orderItemId,
                HttpMethod.DELETE, httpRequestEntity, HttpHeaders.class, serverPort);
    }

    protected ResponseEntity<HttpHeaders> addItemToOrder(final long skuId, final int quantity, final String location, final String token, final RestTemplate restTemplate) {
        final OrderItemDto template = new OrderItemDto();
        template.setQuantity(quantity);
        template.setSkuId(skuId);

        final HttpEntity<OrderItemDto> httpRequestEntity = new HttpEntity(template, httpHeadersWithTokenFactory.getHalHttpHeadersWithToken(token));

        return restTemplate.exchange(location, HttpMethod.POST, httpRequestEntity, HttpHeaders.class, serverPort);
    }

    protected long getRemoteTotalOrdersCountValue(final String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(httpHeadersWithTokenFactory.getHalHttpHeadersWithToken(token));

        final HttpEntity<Long> remoteCountEntity = restTemplate.exchange(ApiTestUrls.ORDERS_COUNT,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody();
    }

    protected Boolean givenOrderIdIsCancelled(final String adminToken, final Long orderId) {
        final HttpEntity<?> adminHttpEntity = getProperEntity(adminToken);
//        final ResponseEntity<OrderDto[]> allOrders =
//                oAuth2AdminRestTemplate().getForEntity(ORDERS_URL, OrderDto[].class, serverPort, adminHttpEntity);

        final ResponseEntity<Resources<OrderDto>> allOrders =
                oAuth2AdminRestTemplate().exchange(ApiTestUrls.ORDERS_URL, HttpMethod.GET, adminHttpEntity, new ParameterizedTypeReference<Resources<OrderDto>>() {}, serverPort);


        return new ArrayList<>(allOrders.getBody().getContent()).stream()
                    .filter(x -> ApiTestUtils.strapSuffixId(x.getLink("self").getHref()) == orderId)
                    .findAny()
                    .map(e -> e.getStatus().equals(OrderStatus.CANCELLED))
                    .orElse(false);
    }

    protected Integer getRemoteItemsInOrderCount(final Integer orderId, final String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(httpHeadersWithTokenFactory.getHalHttpHeadersWithToken(token));

        final HttpEntity<Integer> remoteCountEntity = restTemplate.exchange(ApiTestUrls.ORDERS_URL + "/" + orderId + "/items/count",
                HttpMethod.GET, httpRequestEntity, Integer.class, serverPort);

        return remoteCountEntity.getBody();
    }

    protected List<DiscreteOrderItemDto> getItemsFromCart(final Integer orderId, final String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(httpHeadersWithTokenFactory.getHalHttpHeadersWithToken(token));

        final ResponseEntity<Resources<DiscreteOrderItemDto>> receivedProductAttributeEntity =
                restTemplateForHalJsonHandling.exchange(ApiTestUrls.ORDERS_URL+"/"+orderId+"/items", HttpMethod.GET, httpRequestEntity, new ParameterizedTypeReference<Resources<DiscreteOrderItemDto>>() {}, serverPort);

        assertThat(receivedProductAttributeEntity.getStatusCode(), equalTo(HttpStatus.OK));

        return new ArrayList<>(receivedProductAttributeEntity.getBody().getContent());
    }

    protected DiscreteOrderItemDto getItemDetailsFromCart(final Integer orderId, final Long itemId, final String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(httpHeadersWithTokenFactory.getHalHttpHeadersWithToken(token));

        final HttpEntity<DiscreteOrderItemDto> response = restTemplate.exchange(ApiTestUrls.ORDERS_URL+"/"+orderId+"/items/"+itemId,
                HttpMethod.GET, httpRequestEntity, DiscreteOrderItemDto.class, serverPort);

        return response.getBody();
    }

    protected OrderStatus getOrderStatus(final Integer orderId, final String token) {
        final HttpEntity httpRequestEntity = new HttpEntity(httpHeadersWithTokenFactory.getHalHttpHeadersWithToken(token));

        final HttpEntity<OrderStatus> response = restTemplate.exchange(ApiTestUrls.ORDERS_URL + "/" + orderId + "/status",
                HttpMethod.GET, httpRequestEntity, OrderStatus.class, serverPort);

        return response.getBody();

    }

    /* BDD */

    protected <R> void when(Try.CheckedSupplier<R> r, Try.CheckedConsumer<R>... thens) throws Throwable {
        Try<R> result = Try.of(r);
        for (Try.CheckedConsumer<R> then : thens) {
            then.accept(result.get());
        }
    }

    protected void givenAuthorizationServerClient(Try.CheckedConsumer<AuthorizationServerClient> consumer) throws Throwable {
        consumer.accept(authorizationServerClient());
    }

    protected void givenAuthorizationFor(final Scope scope, Try.CheckedConsumer<OAuth2RestTemplate>... thens) throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenAuthorizationRequestedFor(authorizationServerClient, scope, thens);
        });
    }


    protected Supplier<AuthorizationServerClient> givenAuthorizationServerClient() {
        return this::authorizationServerClient;
    }

    protected AuthorizationServerClient authorizationServerClient() {
        return new AuthorizationServerClient(this.serverPort);
    }

    protected void whenAuthorizationRequestedFor(AuthorizationServerClient authorizationServerClient, final Scope scope, Try.CheckedConsumer<OAuth2RestTemplate>... thens) throws Throwable {
        when(() -> authorizationServerClient.requestAuthorization(scope), thens);
    }

    protected URI createNewOrder(OAuth2RestTemplate oAuth2RestTemplate) {
        return oAuth2RestTemplate.postForLocation(ApiTestUrls.ORDERS_URL, null, serverPort);
    }

    protected void whenNewOrderCreated(OAuth2RestTemplate oAuth2RestTemplate, Try.CheckedConsumer<URI>... thens) throws Throwable {
        when(() -> createNewOrder(oAuth2RestTemplate), thens);
    }

    protected Tuple2<String, String> performRegistration(OAuth2RestTemplate oAuth2RestTemplate) {

        final String username = RandomStringUtils.random(32, "haskellCurry");
        final String password = "uncurry";
        final String email = RandomStringUtils.random(32, "haskellCurry") + "@curry.org";

        final MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("email", email);
        map.add("username", username);
        map.add("password", password);
        map.add("passwordConfirm", password);
        final HttpEntity requestEntity = new HttpEntity(map, new HttpHeaders());
        oAuth2RestTemplate.postForEntity(ApiTestUrls.API_BASE_URL + "/customers/register", requestEntity, HttpHeaders.class, serverPort);

        return Tuple.of(username, password);
    }

    protected void whenRegistrationPerformed(OAuth2RestTemplate oAuth2RestTemplate, Try.CheckedConsumer<Tuple2<String, String>> then) throws Throwable {
        when(() -> performRegistration(oAuth2RestTemplate), then);
    }

    protected void whenLoggedInSite(AuthorizationServerClient authorizationServerClient, final Tuple2<String, String> usernameAndPassword) throws Throwable {
        when(() -> { authorizationServerClient.logIn("site", usernameAndPassword._1, usernameAndPassword._2); return null; });
    }

    protected void whenLoggedInBackoffice(AuthorizationServerClient authorizationServerClient, final Tuple2<String, String> usernameAndPassword) throws Throwable {
        when(() -> { authorizationServerClient.logIn("backoffice", usernameAndPassword._1, usernameAndPassword._2); return null; });
    }

    protected void thenAuthorized(OAuth2RestTemplate oAuth2RestTemplate, boolean value) {
        Assert.assertEquals(
                value,
                oAuth2RestTemplate.getOAuth2ClientContext().getAccessToken() != null
        );
    }
    protected void thenAuthorized(OAuth2RestTemplate oAuth2RestTemplate) {
        thenAuthorized(oAuth2RestTemplate, true);
    }

    public void thenNotAuthorized(OAuth2RestTemplate oAuth2RestTemplate) {
        thenAuthorized(oAuth2RestTemplate, false);
    }


}
