package pl.touk.widerest.cart;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import pl.touk.widerest.Application;
import pl.touk.widerest.api.cart.customers.dto.AddressDto;
import pl.touk.widerest.api.cart.orders.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.orders.dto.FulfillmentDto;
import pl.touk.widerest.api.cart.orders.dto.OrderDto;
import pl.touk.widerest.api.cart.orders.dto.OrderItemDto;
import pl.touk.widerest.api.cart.orders.dto.OrderItemOptionDto;
import pl.touk.widerest.api.catalog.products.dto.ProductAttributeDto;
import pl.touk.widerest.api.catalog.products.dto.ProductDto;
import pl.touk.widerest.api.catalog.products.dto.SkuDto;
import pl.touk.widerest.api.catalog.products.dto.SkuProductOptionValueDto;
import pl.touk.widerest.base.*;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class OrderControllerTest extends ApiTestBase {

    @Before
    public void initTests() {
    }

    @Test
    public void CreateAnonymousUserTest() throws URISyntaxException {
        // Given nothing

        // When I send POST request
        RestTemplate restTemplate = new RestTemplate();
        URI FirstResponseUri = restTemplate.postForLocation(ApiTestUrls.OAUTH_AUTHORIZATION, null, serverPort);

        // Then the token should be generated
        assertNotNull(FirstResponseUri);
        assertFalse(ApiTestUtils.strapTokenFromURI(FirstResponseUri).equals(""));

    }

    @Test
    @Transactional
    public void shouldChangeOrderItemQuantity() throws URISyntaxException {
        // Given anonymous user with one item in order
        Pair<RestTemplate, String> user = generateAnonymousUser();
        RestTemplate restTemplate = user.getKey();
        String accessToken = user.getValue();
        Integer orderId = createNewOrder(accessToken);
        String orderUrl = ApiTestUrls.ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + orderId;
        ResponseEntity<HttpHeaders> orderItemResponse =
                addItemToOrder(10, 5, orderUrl+"/items", accessToken, restTemplate);

        // When PUT /orders/{orderId}/items/{itemId}/quantity
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
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
    @Transactional
    public void shouldReturnFulfillmentAddressAndOption() throws URISyntaxException {
        // Given anonymous user with order with 1 item
        Pair<RestTemplate, String> user = generateAnonymousUser();
        RestTemplate restTemplate = user.getKey();
        String accessToken = user.getValue();
        Integer orderId = createNewOrder(accessToken);
        String orderUrl = ApiTestUrls.ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + orderId;
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
        requestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
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
    @Transactional
    public void CreateEmptyOrderTest() throws URISyntaxException {
        // Given anonymous user
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();

        // When POSTing to create an order
        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ApiTestUrls.ORDERS_URL, getProperEntity(accessToken), HttpHeaders.class, serverPort);

        // Then it shouldn't be null and its ID must be > 0
        assertNotNull(anonymousOrderHeaders);
        assert(ApiTestUtils.strapSuffixId(anonymousOrderHeaders.getHeaders().getLocation().toString()) > 0);
    }

    @Test
    @Transactional
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
    @Transactional
    public void AnonymousCreateAndDeleteOrder() throws URISyntaxException {

        // Given anonymous user and created cart
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();
        long orderId = createNewOrder(accessToken);

        em.clear();


        // When sending DELETE message
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + accessToken);
        HttpEntity httpRequestEntity = new HttpEntity(null, requestHeaders);
        ResponseEntity<HttpHeaders> response = restTemplate.exchange(ApiTestUrls.ORDERS_URL + "/" + orderId,
                HttpMethod.DELETE, httpRequestEntity, HttpHeaders.class, serverPort);

        // Then the status code should be 200
        assert(response.getStatusCode().value() == 200);

        em.clear();

        // Then the cart shouldn't exist
        //assertNull(orderService.findOrderById(Long.valueOf(orderId)));
        Pair<OAuth2RestTemplate, String> adminUser = generateAdminUser();
        final OAuth2RestTemplate adminRestTemplate = adminUser.getKey();

        final ResponseEntity<Resources<OrderDto>> allOrders =
                adminRestTemplate.exchange(ApiTestUrls.ORDERS_URL, HttpMethod.GET, null, new ParameterizedTypeReference<Resources<OrderDto>>() {}, serverPort);

        assertThat(allOrders.getStatusCode(), equalTo(HttpStatus.OK));

        assertFalse(new ArrayList<>(allOrders.getBody().getContent()).stream()
                .filter(x -> x.getOrderId() == orderId)
                .findAny()
                .map(e -> e.getStatus().equals(OrderStatus.CANCELLED))
                .orElse(false));
    }

    @Test
    @Transactional
    public void AccessingItemsFromOrderTest() throws URISyntaxException {
        // Given anonymous user and cart
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();
        Integer orderId = createNewOrder(accessToken);
        String orderUrl = ApiTestUrls.ORDERS_URL+"/"+orderId;

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
    @Transactional
    public void AnonymousUserAddingItemsToCartTest() throws URISyntaxException {

        // Given an anonymous user/token
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();

        //Given an order
        Integer orderId = createNewOrder(accessToken);


        // When I add 3 different items to order
        ResponseEntity<HttpHeaders> itemAddResponse =
                addItemToOrder(10, 5, ApiTestUrls.ORDERS_URL+"/"+orderId+"/items", accessToken, restTemplate);
        // Then 1st item should be added (amount: 5)
        assert(itemAddResponse.getStatusCode().value() == 201);

        itemAddResponse =
                addItemToOrder(11, 3, ApiTestUrls.ORDERS_URL+"/"+orderId+"/items", accessToken, restTemplate);
        // Then 2nd item should be added (amount: 3)
        assert(itemAddResponse.getStatusCode().value() == 201);

        itemAddResponse =
                addItemToOrder(12, 4, ApiTestUrls.ORDERS_URL+"/"+orderId+"/items", accessToken, restTemplate);
        // Then 3rd item should be added (amount: 4)
        assert(itemAddResponse.getStatusCode().value() == 201);

        // Then I have a total amount of 12
        assert(getRemoteItemsInOrderCount(orderId, accessToken) == 12);

        // When I remove the last added item
        ResponseEntity<HttpHeaders> itemRemovalResponse =
                deleteRemoveOrderItem(restTemplate, accessToken, orderId, ApiTestUtils.strapSuffixId(itemAddResponse.getHeaders().getLocation().toString()));

        // Then I should receive 200 status code
        assert(itemRemovalResponse.getStatusCode().value() == 200);
        // Then the item amount should decrease by 4
        assert(getRemoteItemsInOrderCount(orderId, accessToken) == 8);

    }

    @Test
    @Transactional
    public void OrderAccessTest() throws URISyntaxException {

        // Given 2 anonymous users
        final Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        final RestTemplate restTemplate = firstUser.getKey();
        restTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));
        final String accessFirstAnonymousToken = firstUser.getValue();

        final Pair<RestTemplate, String> secondUser = generateAnonymousUser();
        final RestTemplate restTemplate1 = secondUser.getKey();
        restTemplate1.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));
        final String accessSecondAnonymousToken = secondUser.getValue();

        // Given admin user
        final Pair<OAuth2RestTemplate, String> adminUser = generateAdminUser();
        final OAuth2RestTemplate adminRestTemplate = adminUser.getKey();
        final String accessLoggedToken = adminUser.getValue();

        // When receiving tokens
        // Then they should be different
        assertFalse(accessFirstAnonymousToken.equals(accessSecondAnonymousToken));
        assertFalse(accessFirstAnonymousToken.equals(accessLoggedToken));
        assertFalse(accessSecondAnonymousToken.equals(accessLoggedToken));


        // When creating order for 1st user
        HttpEntity<?> anonymousFirstHttpEntity = getProperEntity(accessFirstAnonymousToken);
        ResponseEntity<HttpHeaders> anonymousOrderHeaders =
                restTemplate.postForEntity(ApiTestUrls.ORDERS_URL, anonymousFirstHttpEntity, HttpHeaders.class, serverPort);

        em.clear();

        // Then it should succeed
        assert(anonymousOrderHeaders.getStatusCode().is2xxSuccessful());

        // When user added order
        String orderLocation = anonymousOrderHeaders.getHeaders().getLocation().toASCIIString();

        final ResponseEntity<Resources<OrderDto>> allOrders =
                adminRestTemplate.exchange(ApiTestUrls.ORDERS_URL, HttpMethod.GET, null,
                        new ParameterizedTypeReference<Resources<OrderDto>>() {}, serverPort);

        assertThat(allOrders.getStatusCode(), equalTo(HttpStatus.OK));

//        ResponseEntity<OrderDto[]> allOrders =
//                adminRestTemplate.getForEntity(ORDERS_URL, OrderDto[].class, serverPort);

        OrderDto goodOne = new ArrayList<>(allOrders.getBody().getContent()).stream()
                .filter(x -> x.getLink("self").toString().contains(orderLocation))
                .findAny()
                .orElse(null);

        // Then admin should see it
        assertNotNull(goodOne);

        final ResponseEntity<Resources<OrderDto>> allSecondOrders =
                restTemplate1.exchange(ApiTestUrls.ORDERS_URL, HttpMethod.GET, null,
                        new ParameterizedTypeReference<Resources<OrderDto>>() {}, serverPort);

        assertThat(allSecondOrders.getStatusCode(), equalTo(HttpStatus.OK));

//        ResponseEntity<OrderDto[]> allSecondOrders =
//                restTemplate1.getForEntity(ORDERS_URL, OrderDto[].class, serverPort);

        // Then the other user can't access it
        assert(new ArrayList<>(allSecondOrders.getBody().getContent()).isEmpty());


        // When checking orders amount

        // Then one user should have 1 cart created
        assert(getRemoteTotalOrdersCountValue(accessFirstAnonymousToken) == Long.valueOf(1));

        // Then the other one doesn't have any cart neither sees 1st user cart
        assert(getRemoteTotalOrdersCountValue(accessSecondAnonymousToken) == Long.valueOf(0));


        // When admin deletes the user's cart
        adminRestTemplate.delete(orderLocation);

        // Then it should exist anymore
//        assertFalse(givenOrderIdIsCancelled(accessLoggedToken, goodOne.getOrderId()));

        final ResponseEntity<Resources<OrderDto>> allOrders3 =
                adminRestTemplate.exchange(ApiTestUrls.ORDERS_URL, HttpMethod.GET, null,
                        new ParameterizedTypeReference<Resources<OrderDto>>() {}, serverPort);

        assertThat(allOrders.getStatusCode(), equalTo(HttpStatus.OK));

        assertFalse(new ArrayList<>(allOrders3.getBody().getContent()).stream()
                        .filter(x -> x.getOrderId() == goodOne.getOrderId())
                        .findAny()
                        .map(e -> e.getStatus().equals(OrderStatus.CANCELLED))
                        .orElse(false)
        );

    }

    @Test
    @Transactional
    public void shouldNotModifyOrderItemQuantity() throws URISyntaxException {
        // Given admin
        Pair<OAuth2RestTemplate, String> adminCredentials = generateAdminUser();
        OAuth2RestTemplate adminRestTemplate = adminCredentials.getKey();

        // Given sku with limited quantity
        // assuming that there is product with id 10 and skuId 10
        HttpEntity<String> requestEntity =  new HttpEntity<>("CHECK_QUANTITY");
        HttpHeaders httpJsonRequestHeaders = new HttpHeaders();

        httpJsonRequestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        httpJsonRequestHeaders.set("Content-Type", "application/hal+json");

        adminRestTemplate.exchange(ApiTestUrls.PRODUCT_BY_ID_SKU_BY_ID + "/availability", HttpMethod.PUT,
                requestEntity, HttpHeaders.class, serverPort, 10L, 10L);
        HttpEntity<Integer> quantityEntity = new HttpEntity<>(100, httpJsonRequestHeaders);
        adminRestTemplate.exchange(ApiTestUrls.PRODUCT_BY_ID_SKU_BY_ID + "/quantity", HttpMethod.PUT,
                quantityEntity, HttpHeaders.class, serverPort, 10L, 10L);


        // Given anonymous user
        Pair<OAuth2RestTemplate, String> userCredentials = generateAnonymousUser();
        RestTemplate userRestTemplate = userCredentials.getKey();
        String userAccessToken = userCredentials.getValue();
        Integer orderId = createNewOrder(userAccessToken);
        String orderUrl = ApiTestUrls.ORDERS_URL+"/"+orderId;

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
    @Transactional
    public void shouldNotAcceptInvalidCountryNameInAddress() throws URISyntaxException {
        // Given anonymous user with 1 item in order
        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();
        Integer orderId = createNewOrder(accessToken);
        String orderUrl = ApiTestUrls.ORDERS_URL+"/"+orderId;
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
        httpJsonRequestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
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
    @Transactional
    public void creatingNewProductAndAddingItToOrderSavesAllValuesCorrectlyTest() throws URISyntaxException {

        final ProductDto testProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        final SkuDto additionalSkuDto = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

        testProductDto.setSkus(Arrays.asList(additionalSkuDto));
        testProductDto.setValidTo(ApiTestUtils.addNDaysToDate(testProductDto.getValidFrom(), 10));

        final ResponseEntity<?> newProductResponseEntity = apiTestCatalogManager.addTestProduct(testProductDto);
        assertThat(newProductResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = ApiTestUtils.getIdFromEntity(newProductResponseEntity);


        //ResponseEntity<ProductDto> remoteTestProductByIdEntity = getRemoteTestProductByIdEntity(productId);

        final ResponseEntity<ProductDto> remoteTestProductByIdEntity =
                hateoasRestTemplate().exchange(
                        ApiTestUrls.PRODUCT_BY_ID_URL,
                        HttpMethod.GET,
                        testHttpRequestEntity.getTestHttpRequestEntity(),
                        ProductDto.class, serverPort, productId);

        assertThat(remoteTestProductByIdEntity.getStatusCode(), equalTo(HttpStatus.OK));


        ProductDto receivedProductDto= remoteTestProductByIdEntity.getBody();
        long skuId = ApiTestUtils.getIdFromLocationUrl(receivedProductDto.getLink("skus").getHref());


        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();

        Integer orderId = createNewOrder(accessToken);

        ResponseEntity<HttpHeaders> itemAddResponse =
                addItemToOrder(skuId, 2, ApiTestUrls.ORDERS_URL + "/" + orderId + "/items", accessToken, restTemplate);

        assertThat(itemAddResponse.getStatusCode(), equalTo(HttpStatus.CREATED));

        long addedItemId = ApiTestUtils.getIdFromEntity(itemAddResponse);

        getItemDetailsFromCart(orderId, addedItemId, accessToken);
    }

    @Test
    @Transactional
    public void addingSkusWithNotEnoughQuantityAvailableThrowsAnExceptionTest() throws URISyntaxException {


        final int TEST_QUANTITY = 3;

        ProductDto testProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        SkuDto additionalSkuDto = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

        additionalSkuDto.setQuantityAvailable(TEST_QUANTITY);
        additionalSkuDto.setAvailability("CHECK_QUANTITY");

        testProductDto.setSkus(Arrays.asList(additionalSkuDto));
        testProductDto.setValidTo(ApiTestUtils.addNDaysToDate(testProductDto.getValidFrom(), 10));

        ResponseEntity<?> newProductResponseEntity = apiTestCatalogManager.addTestProduct(testProductDto);
        assertThat(newProductResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = ApiTestUtils.getIdFromEntity(newProductResponseEntity);

        final ResponseEntity<ProductDto> remoteTestProductByIdEntity =
                hateoasRestTemplate().exchange(
                        ApiTestUrls.PRODUCT_BY_ID_URL,
                        HttpMethod.GET,
                        testHttpRequestEntity.getTestHttpRequestEntity(),
                        ProductDto.class, serverPort, productId);

        assertThat(remoteTestProductByIdEntity.getStatusCode(), equalTo(HttpStatus.OK));

        //ResponseEntity<ProductDto> remoteTestProductByIdEntity = getRemoteTestProductByIdEntity(productId);
        ProductDto receivedProductDto= remoteTestProductByIdEntity.getBody();
        long skuId = ApiTestUtils.getIdFromLocationUrl(receivedProductDto.getLink("skus").getHref());


        Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        RestTemplate restTemplate = firstUser.getKey();
        String accessToken = firstUser.getValue();

        Integer orderId = createNewOrder(accessToken);

        try {
            addItemToOrder(skuId, TEST_QUANTITY + 2, ApiTestUrls.ORDERS_URL + "/" + orderId + "/items", accessToken, restTemplate);
            fail();
        } catch(HttpStatusCodeException httpStatusCodeException) {
            assertTrue(httpStatusCodeException.getStatusCode().is5xxServerError());
        }

        // this should add correctly
        final ResponseEntity<HttpHeaders> itemAddResponse = addItemToOrder(skuId, TEST_QUANTITY, ApiTestUrls.ORDERS_URL + "/" + orderId + "/items", accessToken, restTemplate);

        assertThat(itemAddResponse.getStatusCode(), equalTo(HttpStatus.CREATED));

        long addedItemId = ApiTestUtils.getIdFromEntity(itemAddResponse);

        getItemDetailsFromCart(orderId, addedItemId, accessToken);
    }

    @Test
    @Transactional
    public void shouldAddItemToOrderByProductOptionsTest() throws URISyntaxException {
        /* (mst) Prepare a single product with 2 'options' assigned to different SKUs */
        final ProductDto newProductDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        final SkuDto additionalSku1 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);
        final SkuDto additionalSku2 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

//        newProductDto.getDefaultSku().setActiveEndDate(addNDaysToDate(newProductDto .getDefaultSku().getActiveStartDate(), 30));
//        newProductDto.getDefaultSku().setRetailPrice(new BigDecimal("19.99"));
//
        newProductDto.setValidFrom(ApiTestUtils.addNDaysToDate(newProductDto.getValidFrom(), 30));
        newProductDto.setRetailPrice(new BigDecimal("19.99"));

        final Set<SkuProductOptionValueDto> additionalSku1Options = new HashSet<>();
        additionalSku1Options.add(new SkuProductOptionValueDto("TESTOPTION", "test1"));

        final Set<SkuProductOptionValueDto> additionalSku2Options = new HashSet<>();
        additionalSku2Options.add(new SkuProductOptionValueDto("TESTOPTION", "test2"));

        additionalSku1.setRetailPrice(new BigDecimal("29.99"));
        additionalSku1.setActiveEndDate(ApiTestUtils.addNDaysToDate(additionalSku1.getActiveStartDate(), 10));
        additionalSku1.setCurrencyCode("USD");
        additionalSku1.setAvailability("ALWAYS_AVAILABLE");
        additionalSku1.setSkuProductOptionValues(additionalSku1Options);

        additionalSku2.setRetailPrice(new BigDecimal("49.99"));
        additionalSku2.setActiveEndDate(ApiTestUtils.addNDaysToDate(additionalSku1.getActiveStartDate(), 2));
        additionalSku2.setCurrencyCode("USD");
        additionalSku2.setAvailability("ALWAYS_AVAILABLE");
        additionalSku2.setSkuProductOptionValues(additionalSku2Options);

        newProductDto.setSkus(Arrays.asList(additionalSku1, additionalSku2));

        final ResponseEntity<?> responseEntity = apiTestCatalogManager.addTestProduct(newProductDto);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final String productHref = responseEntity.getHeaders().getLocation().toString();

        /* (mst) Place an order */
        final Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        final RestTemplate restTemplate = firstUser.getKey();
        final String accessToken = firstUser.getValue();

        final Integer orderId = createNewOrder(accessToken);

        final OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setQuantity(2);
        orderItemDto.setProductHref(productHref);

        final OrderItemOptionDto orderItemOptionDto = OrderItemOptionDto.builder()
                .optionName("TESTOPTION")
                .optionValue("test2")
                .build();

        orderItemDto.setSelectedProductOptions(Arrays.asList(orderItemOptionDto));

        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + accessToken);
        final HttpEntity httpRequestEntity = new HttpEntity(orderItemDto, requestHeaders);

        final ResponseEntity<HttpHeaders> placeOrderResponseEntity = restTemplate.exchange(ApiTestUrls.ORDERS_URL + "/" + orderId + "/itemsp", HttpMethod.POST, httpRequestEntity, HttpHeaders.class, serverPort);

        assertThat(placeOrderResponseEntity .getStatusCode(), equalTo(HttpStatus.CREATED));

        final List<DiscreteOrderItemDto> itemDetailsFromCart = getItemsFromCart(orderId, accessToken);

        /* (mst) Make sure that the correct SKU has been selected */
        assertTrue(!itemDetailsFromCart.isEmpty());
        assertThat(itemDetailsFromCart.size(), equalTo(1));
        assertThat(itemDetailsFromCart.get(0).getRetailPrice().getAmount(), equalTo(additionalSku2.getRetailPrice()));
    }

    @Test
    @Transactional
    public void shouldAddItemToOrderByProductOptions2Test() throws URISyntaxException {
        /* (mst) Prepare a single product with 2 'options' assigned to different SKUs */
        final ProductDto newProductDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        final SkuDto additionalSku1 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);
        final SkuDto additionalSku2 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

        newProductDto.setValidTo(ApiTestUtils.addNDaysToDate(newProductDto.getValidFrom(), 30));
        newProductDto.setRetailPrice(new BigDecimal("19.99"));

        final Set<SkuProductOptionValueDto> additionalSku1Options = new HashSet<>();
        additionalSku1Options.add(new SkuProductOptionValueDto("TESTOPTION", "test1"));

        final Set<SkuProductOptionValueDto> additionalSku2Options = new HashSet<>();
        additionalSku2Options.add(new SkuProductOptionValueDto("TESTOPTION", "test2"));

        additionalSku1.setRetailPrice(new BigDecimal("29.99"));
        additionalSku1.setActiveEndDate(ApiTestUtils.addNDaysToDate(additionalSku1.getActiveStartDate(), 10));
        additionalSku1.setCurrencyCode("USD");
        additionalSku1.setAvailability("ALWAYS_AVAILABLE");
        additionalSku1.setSkuProductOptionValues(additionalSku1Options);

        additionalSku2.setRetailPrice(new BigDecimal("49.99"));
        additionalSku2.setActiveEndDate(ApiTestUtils.addNDaysToDate(additionalSku1.getActiveStartDate(), 2));
        additionalSku2.setCurrencyCode("USD");
        additionalSku2.setAvailability("ALWAYS_AVAILABLE");
        additionalSku2.setSkuProductOptionValues(additionalSku2Options);

        newProductDto.setSkus(Arrays.asList(additionalSku1, additionalSku2));

        final ResponseEntity<?> responseEntity = apiTestCatalogManager.addTestProduct(newProductDto);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final String productHref = responseEntity.getHeaders().getLocation().toString();

        /* (mst) Place an order */
        final Pair<RestTemplate, String> firstUser = generateAnonymousUser();
        final RestTemplate restTemplate = firstUser.getKey();
        final String accessToken = firstUser.getValue();

        final Integer orderId = createNewOrder(accessToken);

        /* (mst) Add first product with given options */
        final OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setQuantity(2);
        orderItemDto.setProductHref(productHref);

        final OrderItemOptionDto orderItemOptionDto = OrderItemOptionDto.builder()
                .optionName("TESTOPTION")
                .optionValue("test2")
                .build();

        orderItemDto.setSelectedProductOptions(Arrays.asList(orderItemOptionDto));

        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + accessToken);
        final HttpEntity httpRequestEntity = new HttpEntity(orderItemDto, requestHeaders);

        final ResponseEntity<HttpHeaders> placeOrderResponseEntity = restTemplate.exchange(ApiTestUrls.ORDERS_URL + "/" + orderId + "/itemsp", HttpMethod.POST, httpRequestEntity, HttpHeaders.class, serverPort);

        assertThat(placeOrderResponseEntity .getStatusCode(), equalTo(HttpStatus.CREATED));

        /* (mst) Add second product with given options */
        final OrderItemDto orderItemDto2 = new OrderItemDto();
        orderItemDto2.setQuantity(2);
        orderItemDto2.setProductHref(productHref);

        final OrderItemOptionDto orderItemOptionDto2 = OrderItemOptionDto.builder()
                .optionName("TESTOPTION")
                .optionValue("test1")
                .build();

        orderItemDto2.setSelectedProductOptions(Arrays.asList(orderItemOptionDto2));

        final HttpEntity httpRequestEntity2 = new HttpEntity(orderItemDto2, requestHeaders);

        final ResponseEntity<HttpHeaders> placeOrderResponseEntity2 = restTemplate.exchange(ApiTestUrls.ORDERS_URL + "/" + orderId + "/itemsp", HttpMethod.POST, httpRequestEntity2, HttpHeaders.class, serverPort);

        assertThat(placeOrderResponseEntity2 .getStatusCode(), equalTo(HttpStatus.CREATED));

        /* (mst) Verify that both SKUs have been properly added */
        final List<DiscreteOrderItemDto> itemDetailsFromCart = getItemsFromCart(orderId, accessToken);

        /* (mst) Make sure that the correct SKU has been selected */
        assertTrue(!itemDetailsFromCart.isEmpty());
        assertThat(itemDetailsFromCart.size(), equalTo(2));

        DiscreteOrderItemDto firstDiscreteOrderItemDto = itemDetailsFromCart.get(0);

        if(firstDiscreteOrderItemDto.getRetailPrice().getAmount().compareTo(additionalSku1.getRetailPrice()) == 0) {
            assertThat(itemDetailsFromCart.get(1).getRetailPrice().getAmount(), equalTo(additionalSku2.getRetailPrice()));
        } else {
            assertThat(itemDetailsFromCart.get(1).getRetailPrice().getAmount(), equalTo(additionalSku1.getRetailPrice()));
            assertThat(firstDiscreteOrderItemDto.getRetailPrice().getAmount(), equalTo(additionalSku2.getRetailPrice()));
        }
    }
}