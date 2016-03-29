package pl.touk.widerest.base;

import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Try;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.orders.DiscreteOrderItemDto;
import pl.touk.widerest.api.orders.OrderDto;
import pl.touk.widerest.api.orders.OrderItemDto;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.security.oauth2.Scope;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public abstract class ApiTestBase {

    @Resource
    ApplicationContext applicationContext;

    @PersistenceContext(unitName="blPU")
    protected EntityManager em;

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @Value("${local.server.port}")
    protected String serverPort;

    protected RestTemplate backofficeRestTemplate;

    protected CatalogOperationsLocal catalogOperationsLocal;

    protected CatalogOperationsRemote catalogOperationsRemote;

    @Before
    public void init() throws IOException {
        AuthorizationServerClient authorizationServerClient = authorizationServerClient();
        authorizationServerClient.logIn("backoffice", "admin", "admin");
        this.backofficeRestTemplate = authorizationServerClient.requestAuthorization(Scope.STAFF);
        this.catalogOperationsRemote = new CatalogOperationsRemote(backofficeRestTemplate, serverPort);
        this.catalogOperationsLocal = new CatalogOperationsLocal(catalogService);
    }

    protected ResponseEntity<ProductDto> getRemoteTestProductByIdEntity(final long productId) {
        final ResponseEntity<ProductDto> receivedProductEntity =
                backofficeRestTemplate.getForEntity(ApiTestUrls.PRODUCT_BY_ID_URL, ProductDto.class, serverPort, productId);

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

    /* --------------------------------  ORDER METHODS -------------------------------- */

    protected URI addItemToOrder(final RestTemplate restTemplate, final URI orderUrl, final long skuId, final int quantity) {
        final OrderItemDto template = new OrderItemDto();
        template.setQuantity(quantity);
        template.setSkuId(skuId);

        return restTemplate.postForLocation(orderUrl.toASCIIString() + "/items-old", template);
    }

    protected long getRemoteTotalOrdersCountValue(final RestTemplate restTemplate) {
        final Long ordersCount = restTemplate.getForObject(ApiTestUrls.ORDERS_COUNT, Long.class, serverPort);

        assertNotNull(ordersCount);

        return ordersCount;
    }

    protected Integer getRemoteItemsInOrderCount(final RestTemplate restTemplate, final URI orderUrl) {
        return restTemplate.getForObject(orderUrl.toASCIIString() + "/items/count", Integer.class);
    }

    protected List<DiscreteOrderItemDto> getItemsFromCart(RestTemplate restTemplate, final URI orderUrl) {

        final Resources<DiscreteOrderItemDto> orderItems =
                restTemplate.exchange(orderUrl.toASCIIString() + "/items", HttpMethod.GET, null, new ParameterizedTypeReference<Resources<DiscreteOrderItemDto>>() {}).getBody();

        return new ArrayList<>(orderItems.getContent());
    }

    protected DiscreteOrderItemDto getItemDetailsFromCart(final RestTemplate restTemplate, final URI itemHref) {
        final HttpEntity<DiscreteOrderItemDto> response = restTemplate.exchange(itemHref,
                HttpMethod.GET, null, DiscreteOrderItemDto.class);

        return response.getBody();
    }


    protected Collection<OrderDto> getAllOrders(RestTemplate restTemplate) {
        return restTemplate.exchange(ApiTestUrls.ORDERS_URL, HttpMethod.GET, null,
                        new ParameterizedTypeReference<Resources<OrderDto>>() {
                        }, serverPort).getBody().getContent();

    }

    protected OrderStatus getOrderStatus(RestTemplate restTemplate, final Integer orderId) {
        return restTemplate.getForObject(ApiTestUrls.ORDERS_URL + "/" + orderId + "/status", OrderStatus.class, serverPort);
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
            if (Scope.STAFF.equals(scope)) {
                whenLoggedInBackoffice(authorizationServerClient, Tuple.of("admin", "admin"));
            }
            whenAuthorizationRequestedFor(authorizationServerClient, scope, thens);
        });
    }


    protected Supplier<AuthorizationServerClient> givenAuthorizationServerClient() {
        return this::authorizationServerClient;
    }

    protected AuthorizationServerClient authorizationServerClient() {
        return applicationContext.getBean(AuthorizationServerClient.class);
    }

    protected void whenAuthorizationRequestedFor(AuthorizationServerClient authorizationServerClient, final Scope scope, Try.CheckedConsumer<OAuth2RestTemplate>... thens) throws Throwable {
        when(() -> authorizationServerClient.requestAuthorization(scope), thens);
    }

    protected URI createNewOrder(RestTemplate oAuth2RestTemplate) {
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

    protected void whenOrderItemAdded(RestTemplate restTemplate, final URI orderUri, final URI productHref, Try.CheckedConsumer<URI>... thens) throws Throwable {
        when(() -> addOrderItem(restTemplate, CatalogUtils.getIdFromUrl(orderUri), productHref, 1), thens);
    }

    protected URI addOrderItem(RestTemplate restTemplate, final long orderId, final URI productHref, final int quantity) {
        final OrderItemDto dto = OrderItemDto.builder()
                .productHref(productHref.toASCIIString())
                .quantity(quantity)
                .build();
        return restTemplate.postForLocation(ApiTestUrls.ORDERS_BY_ID_ITEMS, dto, serverPort, orderId);
    }

    protected void whenOrderItemDeleted(RestTemplate restTemplate, final URI orderItemHref, Try.CheckedConsumer<Void>... thens) throws Throwable {
        when(() -> { restTemplate.delete(orderItemHref); return null; }, thens);
    }

    protected void whenOrderFulfillmentsRetrieved(RestTemplate oAuth2RestTemplate, final URI orderUri, Try.CheckedConsumer<Collection<FulfillmentDto>>... thens) throws Throwable {
        when(() -> retrieveOrderFulfillments(oAuth2RestTemplate, CatalogUtils.getIdFromUrl(orderUri)), thens);
    }

    protected Collection<FulfillmentDto> retrieveOrderFulfillments(RestTemplate oAuth2RestTemplate, final long orderId) {
        final ResponseEntity<Resources<FulfillmentDto>> receivedOriginalFulfillmentGroupEntity =
                oAuth2RestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Resources<FulfillmentDto>>() {
                        },
                        serverPort,
                        orderId
                );
        return receivedOriginalFulfillmentGroupEntity.getBody().getContent();
    }

    protected void whenSingleOrderFulfillmentRetrieved(RestTemplate restTemplate, URI fulfillmentHref, Try.CheckedConsumer<FulfillmentDto>... thens) throws Throwable {
        when(() -> restTemplate.getForObject(fulfillmentHref, FulfillmentDto.class), thens);
    }



}
