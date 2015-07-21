
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private HttpHeaders httpRequestHeader = new HttpHeaders();

    private final String ORDERS_COUNT = ORDERS_URL+"/count";

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

    @Test
    public void OrderAccessTest() throws URISyntaxException {

        // Get anonymous token
        RestTemplate restTemplate = new RestTemplate();
        URI FirstResponseUri = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null);

        assertNotNull(FirstResponseUri);

        String accessFirstAnonymousToken = strapToken(FirstResponseUri);

        // Get another anonymous token
        RestTemplate restTemplate1 = new RestTemplate();
        URI SecondResponseUri = restTemplate1.postForLocation(OAUTH_AUTHORIZATION, null);

        assertNotNull(SecondResponseUri);

        String accessSecondAnonymousToken = strapToken(SecondResponseUri);

        // Get logged token

        OAuth2RestTemplate adminRestTemplate = oAuth2AdminRestTemplate();

        URI adminUri = adminRestTemplate.postForLocation(LOGIN_URL, null);

        assertNotNull(adminUri);

        String accessLoggedToken = strapToken(adminUri);

        // They must be different
        assertFalse(accessFirstAnonymousToken.equals(accessLoggedToken));
        assertFalse(accessSecondAnonymousToken.equals(accessLoggedToken));


        // User makes order, admin can see it and other user not

        HttpEntity<?> anonymousFirstHttpEntity = getProperEntity(accessFirstAnonymousToken);

        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, anonymousFirstHttpEntity, HttpHeaders.class);


        assertNotNull(anonymousOrderHeaders);

        URI orderLocation = anonymousOrderHeaders.getHeaders().getLocation();

        ResponseEntity<OrderDto[]> allOrders =
                adminRestTemplate.getForEntity(ORDERS_URL, OrderDto[].class, serverPort);

        OrderDto goodOne = new ArrayList<>(Arrays.asList(allOrders.getBody())).stream()
                .filter(x -> (ORDERS_URL + "/" + x.getOrderId()).equals(orderLocation.toString()))
                .findAny()
                .orElse(null);

        // Admin can see
        assertNotNull(goodOne);

        ResponseEntity<OrderDto[]> allSecondOrders =
                restTemplate1.getForEntity(ORDERS_URL, OrderDto[].class, serverPort);

        // Other user cant see
        assertNull(allSecondOrders.getBody());


        // /catalog/orders/count

        // Only 1 order was added before
        assert(getRemoteTotalOrdersCountValue(accessFirstAnonymousToken) == Long.valueOf(1));

        // The other user doesn't have any orders
        assert(getRemoteTotalOrdersCountValue(accessSecondAnonymousToken) == Long.valueOf(0));


        // DELETE by admin
        adminRestTemplate.delete(ORDERS_URL + "/" + goodOne.getOrderId(), 1);

        // Check if it still exists
        assertFalse(givenOrderIdExists(accessLoggedToken, goodOne.getOrderId()));

    }

    private ResponseEntity<HttpHeaders> addItemToOrder(long skuId, Integer quantity, String location, String token, RestTemplate restTemplate) {
        OrderItemDto template = new OrderItemDto();
        template.setQuantity(quantity);
        template.setSkuId(skuId);

        return restTemplate.postForEntity(location, template, null, getProperEntity(token));
    }

    @Test
    public void AnonymousUserOrderUsageTest() throws URISyntaxException {
        //TODO: fix addItemToOrder, it doesnt work ok right now

        // Creating user and order
        RestTemplate restTemplate = new RestTemplate();
        URI loginResponseURI = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null);
        assertNotNull(loginResponseURI);
        String accessToken= strapToken(loginResponseURI);

        HttpEntity<?> anonymousFirstHttpEntity = getProperEntity(accessToken);

        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, anonymousFirstHttpEntity, HttpHeaders.class);

        // Add 3 items, check count
        ResponseEntity<HttpHeaders> itemAddResponse =
                addItemToOrder(10, 5, anonymousOrderHeaders.getHeaders().getLocation()+"/items", accessToken, restTemplate);
        assert(itemAddResponse.getStatusCode().value() == 201);

        itemAddResponse = addItemToOrder(11, 3, anonymousOrderHeaders.getHeaders().getLocation()+"/items", accessToken, restTemplate);
        assert(itemAddResponse.getStatusCode().value() == 201);

        itemAddResponse = addItemToOrder(12, 4, anonymousOrderHeaders.getHeaders().getLocation()+"/items", accessToken, restTemplate);
        assert(itemAddResponse.getStatusCode().value() == 201);

        assert(getRemoteTotalOrdersCountValue(accessToken) == 3);

        // Remove 1 item, check count
        restTemplate.delete(anonymousOrderHeaders.getHeaders().getLocation()+"/items/1");
        assert(getRemoteTotalOrdersCountValue(accessToken) == 2);

        // Check order status




    }

/*
    public void Test1() throws URISyntaxException {

        URI orderDtoResponseUri = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null);

        assertNotNull(orderDtoResponseUri);

        String authorizationUrl = orderDtoResponseUri.toString().replaceFirst("#", "?");

        List<NameValuePair> authorizationParams = URLEncodedUtils.parse(new URI(authorizationUrl), "UTF-8");

        /*
        for(NameValuePair v : authorizationParams) {
            System.out.println(v.getName() + " : " + v.getValue());
        }*/

        String access_token = authorizationParams.stream()
                .filter(x -> x.getName().equals("access_token")).collect(Collectors.toList()).get(0).getValue();


        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Authorization", "Bearer " + access_token);
        HttpEntity<String> requestHeadersEntity = new HttpEntity<>(requestHeaders);

        //System.out.println("Token length: " + access_token.length());
        //System.out.println(access_token);

        ResponseEntity<OrderDto> responseOrderEntity = restTemplate.postForEntity(
                ORDERS_URL,
                requestHeadersEntity,
                OrderDto.class);

        assertTrue(responseOrderEntity.getStatusCode() == HttpStatus.CREATED);

        DiscreteOrderItemDto newItem = DiscreteOrderItemDto.builder().skuId(1).quantity(4).build();

        //ResponseEntity<DiscreteOrderItemDto> addNewItemResponseEntity = restTemplate.postForEntity(
        //        ORDERS_URL + "/items",
        //        requestHeadersEntity,
        //        newItem, DiscreteOrderItemDto.class);


    }
*/

}