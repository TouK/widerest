package cart;

import base.ApiTestBase;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restlet.data.Method;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.cart.dto.OrderItemDto;
import pl.touk.widerest.api.cart.service.OrderServiceProxy;
import pl.touk.widerest.api.catalog.dto.CategoryDto;


import javax.persistence.Tuple;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;


//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = Application.class)
public class OrderControllerTest extends ApiTestBase {

    @Before
    public void initTests() {
        serverPort = String.valueOf(8080);
    }

    @Test
    public void CreateAnonymousUserTest() throws URISyntaxException {
        // Given nothing

        // When I send POST request
        RestTemplate restTemplate = new RestTemplate();
        URI FirstResponseUri = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null);

        // Then the token should be generated
        assertNotNull(FirstResponseUri);
        assertFalse(strapToken(FirstResponseUri).equals(""));

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
        OAuth2RestTemplate adminRestTemplate = oAuth2AdminRestTemplate();
        URI adminUri = adminRestTemplate.postForLocation(LOGIN_URL, null);
        assertNotNull(adminUri);
        String accessLoggedToken = strapToken(adminUri);

        // When receiving tokens
        // Then they should be different
        assertFalse(accessFirstAnonymousToken.equals(accessLoggedToken));
        assertFalse(accessSecondAnonymousToken.equals(accessLoggedToken));


        // When creating order for 1st user
        HttpEntity<?> anonymousFirstHttpEntity = getProperEntity(accessFirstAnonymousToken);
        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, anonymousFirstHttpEntity, HttpHeaders.class);

        // Then it should succeed
        assertNotNull(anonymousOrderHeaders);

        // When user added order
        URI orderLocation = anonymousOrderHeaders.getHeaders().getLocation();
        ResponseEntity<OrderDto[]> allOrders =
                adminRestTemplate.getForEntity(ORDERS_URL, OrderDto[].class, serverPort);

        OrderDto goodOne = new ArrayList<>(Arrays.asList(allOrders.getBody())).stream()
                .filter(x -> (ORDERS_URL + "/" + x.getOrderId()).equals(orderLocation.toString()))
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
        adminRestTemplate.delete(ORDERS_URL + "/" + goodOne.getOrderId(), 1);

        // Then it should exist anymore
        assertFalse(givenOrderIdExists(accessLoggedToken, goodOne.getOrderId()));

    }


    @Test
    public void AnonymousUserOrderUsageTest() throws URISyntaxException {

        // Given an anonymous user/token
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getLeft();
        String accessToken = firstUser.getRight();

        HttpEntity<?> anonymousFirstHttpEntity = getProperEntity(accessToken);

        //Given an order
        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, anonymousFirstHttpEntity, HttpHeaders.class);

        Integer orderId = strapSufixId(anonymousOrderHeaders.getHeaders().getLocation().toString());


        // When I add 3 different items to order
        ResponseEntity<HttpHeaders> itemAddResponse =
                addItemToOrder(10, 5, anonymousOrderHeaders.getHeaders().getLocation()+"/items", accessToken, restTemplate);
        // Then 1st item should be added (amount: 5)
        assert(itemAddResponse.getStatusCode().value() == 201);

        itemAddResponse =
                addItemToOrder(11, 3, anonymousOrderHeaders.getHeaders().getLocation()+"/items", accessToken, restTemplate);
        // Then 2nd item should be added (amount: 3)
        assert(itemAddResponse.getStatusCode().value() == 201);

        itemAddResponse =
                addItemToOrder(12, 4, anonymousOrderHeaders.getHeaders().getLocation()+"/items", accessToken, restTemplate);
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

    private HttpHeaders httpRequestHeader = new HttpHeaders();

    private final String ORDERS_COUNT = ORDERS_URL+"/count";

    private Integer strapSufixId(String url) {
        // Assuming it is /df/ab/{sufix}
        String[] tab = StringUtils.split(url, "/");
        return Integer.parseInt(tab[tab.length - 1]);
    }

    private Pair generateAnonymousUser() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI FirstResponseUri = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null);
        return new ImmutablePair<RestTemplate, String>(restTemplate, strapToken(FirstResponseUri));
    }

    private ResponseEntity<HttpHeaders> deleteRemoveOrderItem(RestTemplate restTemplate, String token,
                                                              Integer orderId, Integer orderItemId) {

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, requestHeaders);

        return restTemplate.exchange(ORDERS_URL + "/" + orderId + "/items/" + orderItemId,
                HttpMethod.DELETE, httpRequestEntity, HttpHeaders.class);

    }

    private ResponseEntity<HttpHeaders> addItemToOrder(long skuId, Integer quantity, String location, String token, RestTemplate restTemplate) {
        OrderItemDto template = new OrderItemDto();
        template.setQuantity(quantity);
        template.setSkuId(skuId);


        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(template, requestHeaders);

        return restTemplate.exchange(location, HttpMethod.POST, httpRequestEntity, HttpHeaders.class);
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


    private Boolean givenOrderIdExists(String adminToken, Long orderId) {
        HttpEntity<?> adminHttpEntity = getProperEntity(adminToken);
        ResponseEntity<OrderDto[]> allOrders =
                oAuth2AdminRestTemplate().getForEntity(ORDERS_URL, OrderDto[].class, adminHttpEntity);

        OrderDto goodOne = new ArrayList<>(Arrays.asList(allOrders.getBody())).stream()
                .filter(x -> x.getOrderId() == orderId)
                .findAny()
                .orElse(null);

        return goodOne==null?false:true;
    }

    private Integer getRemoteItemsInOrderCount(Integer orderId, String token) {
        httpRequestHeader.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpRequestHeader.set("Authorization", "Bearer " + token);
        HttpEntity httpRequestEntity = new HttpEntity(null, httpRequestHeader);

        HttpEntity<Integer> remoteCountEntity = restTemplate.exchange(ORDERS_URL + "/" + orderId + "/items/count",
                HttpMethod.GET, httpRequestEntity, Integer.class, serverPort);

        assertNotNull(remoteCountEntity);

        return remoteCountEntity.getBody().intValue();
    }

}