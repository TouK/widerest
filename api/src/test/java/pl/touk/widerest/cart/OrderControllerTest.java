package pl.touk.widerest.cart;

import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.base.ApiTestBase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class OrderControllerTest extends ApiTestBase {

//    @Before
//    public void initTests() {
//        serverPort = String.valueOf(8080);
//    }

    @Test
    public void CreateAnonymousUserTest() throws URISyntaxException {
        // Given nothing

        // When I send POST request
        RestTemplate restTemplate = new RestTemplate();
        URI FirstResponseUri = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null, serverPort);

        // Then the token should be generated
        assertNotNull(FirstResponseUri);
        assertFalse(strapToken(FirstResponseUri).equals(""));

    }

    @Test
    public void CreateEmptyOrderTest() throws URISyntaxException {
        // Given anonymous user
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getLeft();
        String accessToken = firstUser.getRight();

        // When POSTing to create an order
        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, getProperEntity(accessToken), HttpHeaders.class, serverPort);

        // Then it shouldn't be null and its ID must be > 0
        assertNotNull(anonymousOrderHeaders);
        assert(strapSufixId(anonymousOrderHeaders.getHeaders().getLocation().toString()) > 0);
    }

    @Test
    public void CheckOrderStatusTest() throws URISyntaxException {
        // Given anonymous user and cart
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        //RestTemplate restTemplate = firstUser.getLeft();
        String accessToken = firstUser.getRight();
        Integer orderId = createNewOrder(accessToken);

        // When GETting order status
        OrderStatus status = getOrderStatus(orderId, accessToken);

        // Then it should be "IN_PROCESS"
        assert(status.getType().equals("IN_PROCESS"));
    }

    @Test
    public void AnonymousCreateAndDeleteOrder() throws URISyntaxException {

        // Given anonymous user and created cart
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getLeft();
        String accessToken = firstUser.getRight();
        Integer orderId = createNewOrder(accessToken);

        // When sending DELETE message
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + accessToken);
        HttpEntity httpRequestEntity = new HttpEntity(null, requestHeaders);
        ResponseEntity<HttpHeaders> response = restTemplate.exchange(ORDERS_URL + "/" + orderId,
                HttpMethod.DELETE, httpRequestEntity, HttpHeaders.class, serverPort);

        // Then the status code should be 200
        assert(response.getStatusCode().value() == 200);

        // Then the cart shouldn't exist
        //assertNull(orderService.findOrderById(Long.valueOf(orderId)));
        Pair<?, String> adminUser = generateAdminUser();
        String adminToken = adminUser.getRight();
        assertFalse(givenOrderIdIsCancelled(adminToken, orderId.longValue()));
    }

    @Test
    public void AccessingItemsFromOrderTest() throws URISyntaxException {
        // Given anonymous user and cart
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getLeft();
        String accessToken = firstUser.getRight();
        Integer orderId = createNewOrder(accessToken);
        String orderUrl = ORDERS_URL+"/"+orderId;

        // Given items added to cart
        ArrayList<Long> skuIds = new ArrayList<>();
        Long temp = Long.valueOf(10);
        skuIds.add(temp++);skuIds.add(temp++);skuIds.add(temp++);skuIds.add(temp);
        addItemToOrder(10, 5, orderUrl+"/items", accessToken, restTemplate);
        addItemToOrder(11, 3, orderUrl+"/items", accessToken, restTemplate);
        addItemToOrder(12, 1, orderUrl + "/items", accessToken, restTemplate);
        addItemToOrder(13, 8, orderUrl + "/items", accessToken, restTemplate);


        // When GETting items from cart
        List<DiscreteOrderItemDto> remoteItems = getItemsFromCart(orderId, accessToken);

        // Then all these items should be seen
        assertNotNull(remoteItems);
        assert(remoteItems.size() == 4);
        assert(remoteItems.stream().filter(x -> !skuIds.contains(x.getSkuId())).count() == 0);

        // When GETting details about one item
        DiscreteOrderItemDto remoteItem = getItemDetailsFromCart(orderId, remoteItems.get(0).getItemId(), accessToken);

        // Then they should be available and not null
        assertNotNull(remoteItem);
        assert(remoteItem.equals(remoteItems.get(0)));

    }

    @Test
    public void AnonymousUserAddingItemsToCartTest() throws URISyntaxException {

        // Given an anonymous user/token
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getLeft();
        String accessToken = firstUser.getRight();

        //Given an order
        Integer orderId = createNewOrder(accessToken);


        // When I add 3 different items to order
        ResponseEntity<HttpHeaders> itemAddResponse =
                addItemToOrder(10, 5, ORDERS_URL+"/"+orderId+"/items", accessToken, restTemplate);
        // Then 1st item should be added (amount: 5)
        assert(itemAddResponse.getStatusCode().value() == 201);

        itemAddResponse =
                addItemToOrder(11, 3, ORDERS_URL+"/"+orderId+"/items", accessToken, restTemplate);
        // Then 2nd item should be added (amount: 3)
        assert(itemAddResponse.getStatusCode().value() == 201);

        itemAddResponse =
                addItemToOrder(12, 4, ORDERS_URL+"/"+orderId+"/items", accessToken, restTemplate);
        // Then 3rd item should be added (amount: 4)
        assert(itemAddResponse.getStatusCode().value() == 201);

        // Then I have a total amount of 12
        assert(getRemoteItemsInOrderCount(orderId, accessToken) == 12);

        // When I remove the last added item
        ResponseEntity<HttpHeaders> itemRemovalResponse =
                deleteRemoveOrderItem(restTemplate, accessToken, orderId, strapSufixId(itemAddResponse.getHeaders().getLocation().toString()));

        // Then I should receive 200 status code
        assert(itemRemovalResponse.getStatusCode().value() == 200);
        // Then the item amount should decrease by 4
        assert(getRemoteItemsInOrderCount(orderId, accessToken) == 8);

    }

    @Test
    public void OrderAccessTest() throws URISyntaxException {

        // Given 2 anonymous users
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getLeft();
        String accessFirstAnonymousToken = firstUser.getRight();

        Pair<RestTemplate, String> secondUser = generateAnonymousUser();
        RestTemplate restTemplate1 = secondUser.getLeft();
        String accessSecondAnonymousToken = secondUser.getRight();

        // Given admin user
        Pair<OAuth2RestTemplate, String> adminUser = generateAdminUser();
        OAuth2RestTemplate adminRestTemplate = adminUser.getLeft();
        String accessLoggedToken = adminUser.getRight();

        // When receiving tokens
        // Then they should be different
        assertFalse(accessFirstAnonymousToken.equals(accessLoggedToken));
        assertFalse(accessSecondAnonymousToken.equals(accessLoggedToken));


        // When creating order for 1st user
        HttpEntity<?> anonymousFirstHttpEntity = getProperEntity(accessFirstAnonymousToken);
        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, anonymousFirstHttpEntity, HttpHeaders.class, serverPort);

        // Then it should succeed
        assertNotNull(anonymousOrderHeaders);

        // When user added order
        URI orderLocation = anonymousOrderHeaders.getHeaders().getLocation();
        ResponseEntity<OrderDto[]> allOrders =
                adminRestTemplate.getForEntity(ORDERS_URL, OrderDto[].class, serverPort);

        OrderDto goodOne = new ArrayList<>(Arrays.asList(allOrders.getBody())).stream()
                .filter(x -> (ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + x.getOrderId()).equals(orderLocation.toString()))
                .findAny()
                .orElse(null);

        // Then admin should see it
        assertNotNull(goodOne);

        ResponseEntity<OrderDto[]> allSecondOrders =
                restTemplate1.getForEntity(ORDERS_URL, OrderDto[].class, serverPort);

        // Then the other user can't access it
        assertNull(allSecondOrders.getBody());


        // When checking orders amount

        // Then one user should have 1 cart created
        assert(getRemoteTotalOrdersCountValue(accessFirstAnonymousToken) == Long.valueOf(1));

        // Then the other one doesn't have any cart neither sees 1st user cart
        assert(getRemoteTotalOrdersCountValue(accessSecondAnonymousToken) == Long.valueOf(0));


        // When admin deletes the user's cart
        adminRestTemplate.delete(ORDERS_URL + "/" + goodOne.getOrderId(), serverPort);

        // Then it should exist anymore
        assertFalse(givenOrderIdIsCancelled(accessLoggedToken, goodOne.getOrderId()));

    }

    private HttpHeaders httpRequestHeader = new HttpHeaders();

    private final String ORDERS_COUNT = ORDERS_URL+"/count";


    private Integer createNewOrder(String token) {
        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, getProperEntity(token), HttpHeaders.class, serverPort);

        return strapSufixId(anonymousOrderHeaders.getHeaders().getLocation().toString());
    }

    private Integer strapSufixId(String url) {
        // Assuming it is */df/ab/{sufix}
        String[] tab = StringUtils.split(url, "/");
        return Integer.parseInt(tab[tab.length - 1]);
    }

    private Pair generateAnonymousUser() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI FirstResponseUri = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null, serverPort);
        return new ImmutablePair<RestTemplate, String>(restTemplate, strapToken(FirstResponseUri));
    }

    private Pair generateAdminUser() throws URISyntaxException {
        OAuth2RestTemplate adminRestTemplate = oAuth2AdminRestTemplate();
        URI adminUri = adminRestTemplate.postForLocation(LOGIN_URL, null, serverPort);
        String accessToken = strapToken(adminUri);
        return new ImmutablePair<OAuth2RestTemplate, String>(adminRestTemplate, accessToken);
    }

    private ResponseEntity<HttpHeaders> deleteRemoveOrderItem(RestTemplate restTemplate, String token,
                                                              Integer orderId, Integer orderItemId) {

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, requestHeaders);

        return restTemplate.exchange(ORDERS_URL + "/" + orderId + "/items/" + orderItemId,
                HttpMethod.DELETE, httpRequestEntity, HttpHeaders.class, serverPort);

    }

    private ResponseEntity<HttpHeaders> addItemToOrder(long skuId, Integer quantity, String location, String token, RestTemplate restTemplate) {
        OrderItemDto template = new OrderItemDto();
        template.setQuantity(quantity);
        template.setSkuId(skuId);


        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(template, requestHeaders);

        return restTemplate.exchange(location, HttpMethod.POST, httpRequestEntity, HttpHeaders.class, serverPort);
    }



    private String strapToken(URI response) throws URISyntaxException {
        String authorizationUrl = response.toString().replaceFirst("#", "?");
        List<NameValuePair> authParams = URLEncodedUtils.parse(new URI(authorizationUrl), "UTF-8");

        return authParams.stream()
                .filter(x -> x.getName().equals("access_token"))
                .findFirst()
                .map(NameValuePair::getValue)
                .orElse(null);
    }

    private HttpEntity<?> getProperEntity(String token) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.add("Authorization", "Bearer " + token);
        return new HttpEntity<>(requestHeaders);
    }


    private long getRemoteTotalOrdersCountValue(String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<Long> remoteCountEntity = restTemplate.exchange(ORDERS_COUNT,
                HttpMethod.GET, httpRequestEntity, Long.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().longValue();
    }


    private Boolean givenOrderIdIsCancelled(String adminToken, Long orderId) {
        HttpEntity<?> adminHttpEntity = getProperEntity(adminToken);
        ResponseEntity<OrderDto[]> allOrders =
                oAuth2AdminRestTemplate().getForEntity(ORDERS_URL, OrderDto[].class, serverPort, adminHttpEntity);

        return new ArrayList<>(Arrays.asList(allOrders.getBody())).stream()
                .filter(x -> x.getOrderId() == orderId)
                .findAny()
                .map(e -> e.getStatus().equals(OrderStatus.CANCELLED))
                .orElse(false);
    }

    private Integer getRemoteItemsInOrderCount(Integer orderId, String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<Integer> remoteCountEntity = restTemplate.exchange(ORDERS_URL + "/" + orderId + "/items/count",
                HttpMethod.GET, httpRequestEntity, Integer.class, serverPort);

        return remoteCountEntity.getBody().intValue();
    }

    private List<DiscreteOrderItemDto> getItemsFromCart(Integer orderId, String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<DiscreteOrderItemDto[]> response = restTemplate.exchange(ORDERS_URL+"/"+orderId+"/items",
                HttpMethod.GET, httpRequestEntity, DiscreteOrderItemDto[].class, serverPort);

        return new ArrayList<>(Arrays.asList(response.getBody()));

    }

    private DiscreteOrderItemDto getItemDetailsFromCart(Integer orderId, Long itemId, String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<DiscreteOrderItemDto> response = restTemplate.exchange(ORDERS_URL+"/"+orderId+"/items/"+itemId,
                HttpMethod.GET, httpRequestEntity, DiscreteOrderItemDto.class, serverPort);

        return response.getBody();
    }

    private OrderStatus getOrderStatus(Integer orderId, String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<OrderStatus> response = restTemplate.exchange(ORDERS_URL+"/"+orderId+"/status",
                HttpMethod.GET, httpRequestEntity, OrderStatus.class, serverPort);

        return response.getBody();

    }

}