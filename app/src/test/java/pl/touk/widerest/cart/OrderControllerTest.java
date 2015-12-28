package pl.touk.widerest.cart;

import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.cart.dto.AddressDto;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.FulfillmentDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.catalog.dto.ProductDto;
import pl.touk.widerest.api.catalog.dto.SkuDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.base.DtoTestType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
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
    public void shouldChangeOrderItemQuantity() throws URISyntaxException {
        // Given anonymous user with one item in order
        Pair<RestTemplate, String> user = generateAnonymousUser();
        RestTemplate restTemplate = user.getKey();
        String accessToken = user.getValue();
        Integer orderId = createNewOrder(accessToken);
        String orderUrl = ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + orderId;
        ResponseEntity<HttpHeaders> orderItemResponse =
                addItemToOrder(10, 5, orderUrl+"/items", accessToken, restTemplate);

        // When PUT /orders/{orderId}/items/{itemId}/quantity
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + accessToken);
        requestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity httpRequestEntity = new HttpEntity(10L, requestHeaders);
        ResponseEntity<HttpHeaders> response = restTemplate.exchange(
                orderItemResponse.getHeaders().getLocation().toString()+"/quantity",
                HttpMethod.PUT, httpRequestEntity, HttpHeaders.class, serverPort);

        // When GETting orderItem details
        httpRequestEntity = new HttpEntity(null, requestHeaders);
        DiscreteOrderItemDto remoteItem = restTemplate.exchange(orderItemResponse.getHeaders().getLocation().toString(),
                HttpMethod.GET, httpRequestEntity, DiscreteOrderItemDto.class, serverPort).getBody();

        // Then orderItem quantity should be changed
        assert(remoteItem.getQuantity() == 10);
    }

    @Test
    public void shouldReturnFulfillmentAddressAndOption() throws URISyntaxException {
        // Given anonymous user with order with 1 item
        Pair<RestTemplate, String> user = generateAnonymousUser();
        RestTemplate restTemplate = user.getKey();
        String accessToken = user.getValue();
        Integer orderId = createNewOrder(accessToken);
        String orderUrl = ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + orderId;
        addItemToOrder(10, 5, orderUrl+"/items", accessToken, restTemplate);

        // Given address and fulfillment option
        AddressDto addressDto = new AddressDto();
        addressDto.setAddressLine1("ul. Warszawska 45");
        addressDto.setAddressLine2("POLSKA");
        addressDto.setCity("Poznan");
        addressDto.setPostalCode("05-134");
        addressDto.setFirstName("Haskell");
        addressDto.setLastName("Curry");
        addressDto.setCountryCode("US");

        // When POST /orders/{orderId}/fulfillment/address
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + accessToken);
        requestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity httpRequestEntity = new HttpEntity(addressDto, requestHeaders);
        ResponseEntity<HttpHeaders> response = restTemplate.exchange(orderUrl+"/fulfillment/address",
        HttpMethod.POST, httpRequestEntity, HttpHeaders.class, serverPort);


        // Then return status should be 201
        assert(response.getStatusCode().value() == 201);


        // When GET /orders/{orderId}/fulfillment/address
        httpRequestEntity = new HttpEntity(null, requestHeaders);
        ResponseEntity<AddressDto> responseAddress = restTemplate.exchange(orderUrl+"/fulfillment/address",
        HttpMethod.GET, httpRequestEntity, AddressDto.class, serverPort);


        // Then return status should be 2xx
        assert(responseAddress.getStatusCode().is2xxSuccessful());
        // Then address should be the same
        assert(responseAddress.getBody().equals(addressDto));

        // When PUT /orders/{orderId}/fulfillment/selectedOption
        httpRequestEntity = new HttpEntity(3L, requestHeaders);
        response = restTemplate.exchange(orderUrl+"/fulfillment/selectedOption",
                HttpMethod.PUT, httpRequestEntity, HttpHeaders.class, serverPort);

        // Then return status should be 200
        assert(response.getStatusCode().value() == 200);

        // When GET /orders/{orderId}/fulfillment/
        httpRequestEntity = new HttpEntity(null, requestHeaders);
        ResponseEntity<FulfillmentDto> responseFulfillment = restTemplate.exchange(orderUrl+"/fulfillment",
                HttpMethod.GET, httpRequestEntity, FulfillmentDto.class, serverPort);

        // Then return status should be 200
        assert(responseFulfillment.getStatusCode().value() == 200);
        // Then fulfillment option and address should be the same as sent
        assert(responseFulfillment.getBody().getAddress().equals(addressDto));
        assert(responseFulfillment.getBody().getSelectedOptionId() == 3);
    }

    @Test
    public void CreateEmptyOrderTest() throws URISyntaxException {
        // Given anonymous user
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();

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
        //RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();
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
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();
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
        String adminToken = adminUser.getValue();
        assertFalse(givenOrderIdIsCancelled(adminToken, orderId.longValue()));
    }

    @Test
    public void AccessingItemsFromOrderTest() throws URISyntaxException {
        // Given anonymous user and cart
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();
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
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();

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
        RestTemplate restTemplate = firstUser.getKey();
        String accessFirstAnonymousToken = firstUser.getValue();

        Pair<RestTemplate, String> secondUser = generateAnonymousUser();
        RestTemplate restTemplate1 = secondUser.getKey();
        String accessSecondAnonymousToken = secondUser.getValue();

        // Given admin user
        Pair<OAuth2RestTemplate, String> adminUser = generateAdminUser();
        OAuth2RestTemplate adminRestTemplate = adminUser.getKey();
        String accessLoggedToken = adminUser.getValue();

        // When receiving tokens
        // Then they should be different
        assertFalse(accessFirstAnonymousToken.equals(accessLoggedToken));
        assertFalse(accessSecondAnonymousToken.equals(accessLoggedToken));


        // When creating order for 1st user
        HttpEntity<?> anonymousFirstHttpEntity = getProperEntity(accessFirstAnonymousToken);
        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ORDERS_URL, anonymousFirstHttpEntity, HttpHeaders.class, serverPort);

        // Then it should succeed
        assert(anonymousOrderHeaders.getStatusCode().is2xxSuccessful());

        // When user added order
        String orderLocation = anonymousOrderHeaders.getHeaders().getLocation().toASCIIString();
        ResponseEntity<OrderDto[]> allOrders =
                adminRestTemplate.getForEntity(ORDERS_URL, OrderDto[].class, serverPort);

        OrderDto goodOne = new ArrayList<>(Arrays.asList(allOrders.getBody())).stream()
                .filter(x -> x.getLink("self").toString().contains(orderLocation))
                .findAny()
                .orElse(null);

        // Then admin should see it
        assertNotNull(goodOne);

        ResponseEntity<OrderDto[]> allSecondOrders =
                restTemplate1.getForEntity(ORDERS_URL, OrderDto[].class, serverPort);

        // Then the other user can't access it
        assert(new ArrayList<OrderDto>(Arrays.asList(allSecondOrders.getBody())).isEmpty());


        // When checking orders amount

        // Then one user should have 1 cart created
        assert(getRemoteTotalOrdersCountValue(accessFirstAnonymousToken) == Long.valueOf(1));

        // Then the other one doesn't have any cart neither sees 1st user cart
        assert(getRemoteTotalOrdersCountValue(accessSecondAnonymousToken) == Long.valueOf(0));


        // When admin deletes the user's cart
        adminRestTemplate.delete(orderLocation);

        // Then it should exist anymore
        assertFalse(givenOrderIdIsCancelled(accessLoggedToken, goodOne.getOrderId()));

    }

    @Test
    public void shouldNotModifyOrderItemQuantity() throws URISyntaxException {
        // Given admin
        Pair<OAuth2RestTemplate, String> adminCredentials = generateAdminUser();
        OAuth2RestTemplate adminRestTemplate = adminCredentials.getKey();

        // Given sku with limited quantity
        // assuming that there is product with id 10 and skuId 10
        HttpEntity<String> requestEntity =  new HttpEntity<>("CHECK_QUANTITY");
        HttpHeaders httpJsonRequestHeaders = new HttpHeaders();
        httpJsonRequestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpJsonRequestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        adminRestTemplate.exchange(PRODUCT_BY_ID_SKU_BY_ID + "/availability", HttpMethod.PUT,
                requestEntity, HttpHeaders.class, serverPort, 10L, 10L);
        HttpEntity<Integer> quantityEntity = new HttpEntity<>(100, httpJsonRequestHeaders);
        adminRestTemplate.exchange(PRODUCT_BY_ID_SKU_BY_ID + "/quantity", HttpMethod.PUT,
                quantityEntity, HttpHeaders.class, serverPort, 10L, 10L);


        // Given anonymous user
        Pair<OAuth2RestTemplate, String> userCredentials = generateAnonymousUser();
        RestTemplate userRestTemplate = userCredentials.getKey();
        String userAccessToken = userCredentials.getValue();
        Integer orderId = createNewOrder(userAccessToken);
        String orderUrl = ORDERS_URL+"/"+orderId;

        // When adding item with not too big quantity
        ResponseEntity<HttpHeaders> itemAddResponse =
            addItemToOrder(10L, 90, orderUrl+"/items", userAccessToken, userRestTemplate);

        // When trying to change quantity to too big
        httpJsonRequestHeaders.set("Authorization", "Bearer " + userAccessToken);
        quantityEntity = new HttpEntity<>(101, httpJsonRequestHeaders);

        // Then user shouldn't be able to change quantity
        try {
           userRestTemplate.exchange(itemAddResponse.getHeaders().getLocation().toString() + "/quantity",
               HttpMethod.PUT, quantityEntity, HttpHeaders.class);
           // Exception should be thrown, if not then test failed
           assert(false);
        } catch (HttpClientErrorException e) {
            assert(e.getStatusCode().is4xxClientError());
        }

    }

    @Test
    public void shouldNotAcceptInvalidCountryNameInAddress() throws URISyntaxException {
        // Given anonymous user with 1 item in order
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();
        Integer orderId = createNewOrder(accessToken);
        String orderUrl = ORDERS_URL+"/"+orderId;
        addItemToOrder(10, 10, orderUrl+"/items", accessToken, restTemplate);

        // When sending wrong address
        AddressDto addressDto = new AddressDto();
        addressDto.setFirstName("Haskell");
        addressDto.setLastName("Curry");
        addressDto.setAddressLine1("Semigroup Valley 12");
        addressDto.setPostalCode("13-337");
        addressDto.setCity("Massachusetts");
        addressDto.setCountryCode("USA");

        HttpHeaders httpJsonRequestHeaders = new HttpHeaders();
        httpJsonRequestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpJsonRequestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        httpJsonRequestHeaders.set("Authorization", "Bearer "+ accessToken);
        HttpEntity<AddressDto> addressEntity = new HttpEntity<>(addressDto, httpJsonRequestHeaders);


        // Then 400 status code should be returned
        try {
            restTemplate.exchange(orderUrl + "/fulfillment/address", HttpMethod.POST,
                    addressEntity, HttpHeaders.class, serverPort);
            fail("Address was checked and should be invalid");
        } catch(HttpClientErrorException e) {
            assert(e.getStatusCode().is4xxClientError());
        }


        // When sending address with correct country code
        addressDto.setCountryCode("PL");
        addressEntity = new HttpEntity<>(addressDto, httpJsonRequestHeaders);


        // Then 2xx status should be returned
        ResponseEntity<HttpHeaders> response = restTemplate.exchange(orderUrl + "/fulfillment/address", HttpMethod.POST,
                addressEntity, HttpHeaders.class, serverPort);
        assert(response.getStatusCode().is2xxSuccessful());

    }


    @Test
    public void creatingNewProductAndAddingItToOrderSavesAllValuesCorrectlyTest() throws URISyntaxException {

        ProductDto testProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        SkuDto additionalSkuDto = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);


        testProductDto.setSkus(Arrays.asList(additionalSkuDto));
        testProductDto.setValidTo(addNDaysToDate(testProductDto.getValidFrom(), 10));

        ResponseEntity<?> newProductResponseEntity = addNewTestProduct(testProductDto);
        assertThat(newProductResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromEntity(newProductResponseEntity);


        ResponseEntity<ProductDto> remoteTestProductByIdEntity = getRemoteTestProductByIdEntity(productId);
        ProductDto receivedProductDto= remoteTestProductByIdEntity.getBody();
        long skuId = getIdFromLocationUrl(receivedProductDto.getLink("skus").getHref());


        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();

        Integer orderId = createNewOrder(accessToken);

        ResponseEntity<HttpHeaders> itemAddResponse =
                addItemToOrder(skuId, 2, ORDERS_URL + "/" + orderId + "/items", accessToken, restTemplate);

        assertThat(itemAddResponse.getStatusCode(), equalTo(HttpStatus.CREATED));

        long addedItemId = getIdFromEntity(itemAddResponse);

        getItemDetailsFromCart(orderId, addedItemId, accessToken);
    }

    @Test
    public void addingSkusWithNotEnoughQuantityAvailableThrowsAnExceptionTest() throws URISyntaxException {


        final int TEST_QUANTITY = 3;

        ProductDto testProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        SkuDto additionalSkuDto = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);



        additionalSkuDto.setQuantityAvailable(TEST_QUANTITY);
        additionalSkuDto.setAvailability("CHECK_QUANTITY");

        testProductDto.setSkus(Arrays.asList(additionalSkuDto));
        testProductDto.setValidTo(addNDaysToDate(testProductDto.getValidFrom(), 10));

        ResponseEntity<?> newProductResponseEntity = addNewTestProduct(testProductDto);
        assertThat(newProductResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = getIdFromEntity(newProductResponseEntity);


        ResponseEntity<ProductDto> remoteTestProductByIdEntity = getRemoteTestProductByIdEntity(productId);
        ProductDto receivedProductDto= remoteTestProductByIdEntity.getBody();
        long skuId = getIdFromLocationUrl(receivedProductDto.getLink("skus").getHref());


        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();

        Integer orderId = createNewOrder(accessToken);

        try {
            addItemToOrder(skuId, TEST_QUANTITY + 2, ORDERS_URL + "/" + orderId + "/items", accessToken, restTemplate);
            fail();
        } catch(HttpStatusCodeException httpStatusCodeException) {
            assertTrue(httpStatusCodeException.getStatusCode().is5xxServerError());
        }

        // this should add correctly
        final ResponseEntity<HttpHeaders> itemAddResponse = addItemToOrder(skuId, TEST_QUANTITY, ORDERS_URL + "/" + orderId + "/items", accessToken, restTemplate);

        assertThat(itemAddResponse.getStatusCode(), equalTo(HttpStatus.CREATED));

        long addedItemId = getIdFromEntity(itemAddResponse);

        getItemDetailsFromCart(orderId, addedItemId, accessToken);


    }

}