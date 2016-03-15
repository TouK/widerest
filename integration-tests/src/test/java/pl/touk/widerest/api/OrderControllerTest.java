package pl.touk.widerest.api;

import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.api.orders.DiscreteOrderItemDto;
import pl.touk.widerest.api.orders.OrderDto;
import pl.touk.widerest.api.orders.OrderItemDto;
import pl.touk.widerest.api.products.ProductDto;
import pl.touk.widerest.api.products.skus.SkuDto;
import pl.touk.widerest.api.products.skus.SkuProductOptionValueDto;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.base.ApiTestUtils;
import pl.touk.widerest.base.DtoTestFactory;
import pl.touk.widerest.base.DtoTestType;
import pl.touk.widerest.security.oauth2.Scope;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
public class OrderControllerTest extends AbstractTest {

    @Test
    public void shouldChangeOrderItemQuantity() throws Throwable {

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            // Given an item in order
            URI orderUrl = createNewOrder(restTemplate);
            URI orderItemUrl = addItemToOrder(restTemplate, orderUrl, 10, 5);

            // When PUT /orders/{orderId}/items/{itemId}/quantity
            restTemplate.put(orderItemUrl.toASCIIString() + "/quantity", 10);

            // When GETting orderItem details
            DiscreteOrderItemDto orderItemDto = restTemplate.getForObject(orderItemUrl, DiscreteOrderItemDto.class);

            // Then orderItem quantity should be changed
            assertThat(orderItemDto.getQuantity(), equalTo(10));

        });
    }

    @Test
    public void CreateEmptyOrderTest() throws Throwable {

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            URI newOrderUrl = createNewOrder(restTemplate);

            // Then it shouldn't be null and its ID must be > 0
            assertNotNull(newOrderUrl);

            Collection<OrderDto> allOrdersSeenByUser = getAllOrders(restTemplate);
            assertTrue(allOrdersSeenByUser.stream()
                    .anyMatch(x -> x.getLink("self").getHref().contains(newOrderUrl.toASCIIString())));

        });
    }

    @Test
    public void shouldStartWithInProcessStatus() throws Throwable {

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            URI newOrderUrl = createNewOrder(restTemplate);

            // When GETting order status
            OrderStatus status = getOrderStatus(restTemplate, ApiTestUtils.strapSuffixId(newOrderUrl.toASCIIString()));

            // Then it should be "IN_PROCESS"
            assertThat(status.getType(), equalTo("IN_PROCESS"));
        });
    }

    @Test
    public void shouldHaveStatusCancelledVisibleToStaffWhenDeleted() throws Throwable {

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            URI newOrderUrl = createNewOrder(restTemplate);

            // When sending DELETE message
            restTemplate.delete(newOrderUrl);


            givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
                // Then the cart shouldn't exist
                Collection<OrderDto> allOrders = getAllOrders(restTemplate);

                assertFalse(allOrders.stream()
                        .filter(x -> newOrderUrl.toASCIIString().equals(x.getLink("self").getHref()))
                        .findAny()
                        .map(e -> e.getStatus().equals(OrderStatus.CANCELLED))
                        .orElse(false)
                );
            });
        });
    }

    @Test
    public void AccessingItemsFromOrderTest() throws Throwable {
        // Given anonymous user and cart
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            URI orderUrl = createNewOrder(restTemplate);

            // Given items added to cart
            addItemToOrder(restTemplate, orderUrl, 10, 5);
            addItemToOrder(restTemplate, orderUrl, 11, 3);
            addItemToOrder(restTemplate, orderUrl, 12, 1);
            addItemToOrder(restTemplate, orderUrl, 13, 8);

            // When GETting items from cart
            List<DiscreteOrderItemDto> remoteItems = getItemsFromCart(restTemplate, orderUrl);

            // Then all these items should be seen
            assertNotNull(remoteItems);
            assertThat(remoteItems, hasSize(4));

            // When GETting details about one item

            DiscreteOrderItemDto remoteItem = getItemDetailsFromCart(restTemplate, URI.create(remoteItems.get(0).getLink(Link.REL_SELF).getHref()));

            // Then they should be available and not null
            assertNotNull(remoteItem);
            assertThat(remoteItem, equalTo(remoteItems.get(0)));
        });
    }

    @Test
    public void AnonymousUserAddingItemsToCartTest() throws Throwable {

        // Given an anonymous user/token
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            //Given an order
            URI orderUrl = createNewOrder(restTemplate);

            // When I add 3 different items to order
            addItemToOrder(restTemplate, orderUrl, 10, 5);
            addItemToOrder(restTemplate, orderUrl, 11, 3);
            URI lastItemUrl = addItemToOrder(restTemplate, orderUrl, 12, 4);

            // Then I have a total amount of 12
            assertThat(getRemoteItemsInOrderCount(restTemplate, orderUrl), equalTo(12));

            // When I remove the last added item
            restTemplate.delete(lastItemUrl);

            // Then the item amount should decrease by 4
            assertThat(getRemoteItemsInOrderCount(restTemplate, orderUrl), equalTo(8));
        });
    }

    @Test
    public void OrderAccessTest() throws Throwable {

        // Given 2 anonymous users
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            // When creating order for 1st user
            URI orderUrl = createNewOrder(restTemplate);

            // Then one user should have 1 cart created
            assertThat(getRemoteTotalOrdersCountValue(restTemplate), equalTo(1l));

            givenAuthorizationFor(Scope.CUSTOMER, otherCustomerRestTemplate -> {
                // Then the other user can't access it
                Collection<OrderDto> allOrdersSeenByAdmin = getAllOrders(otherCustomerRestTemplate);
                assertTrue(allOrdersSeenByAdmin.stream()
                        .noneMatch(x -> x.getLink("self").getHref().contains(orderUrl.toASCIIString())));

                // Then the other one doesn't have any cart neither sees 1st user cart
                assertThat(getRemoteTotalOrdersCountValue(otherCustomerRestTemplate), equalTo(0l));
            });

            givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {

                // Then admin should see the order
                Collection<OrderDto> allOrdersSeenByAdmin = getAllOrders(adminRestTemplate);
                assertTrue(allOrdersSeenByAdmin.stream()
                        .anyMatch(x -> x.getLink("self").getHref().contains(orderUrl.toASCIIString())));

                // When admin deletes the user's cart
                adminRestTemplate.delete(orderUrl);

                // Then it should not exist anymore
                Collection<OrderDto> allOrdersSeenByUser = getAllOrders(restTemplate);
                assertTrue(allOrdersSeenByUser.stream()
                        .noneMatch(x -> x.getLink("self").getHref().contains(orderUrl.toASCIIString())));

            });
        });
    }

    @Test
    public void shouldNotModifyOrderItemQuantity() throws Throwable {
        givenAuthorizationFor(Scope.STAFF, adminRestTemplate -> {
            // Given sku with limited quantity
            // assuming that there is product with id 10 and skuId 10
            adminRestTemplate.put(ApiTestUrls.PRODUCT_BY_ID_SKU_BY_ID + "/availability", "CHECK_QUANTITY", serverPort, 10L, 10L);
            adminRestTemplate.put(ApiTestUrls.PRODUCT_BY_ID_SKU_BY_ID + "/quantity", 100, serverPort, 10L, 10L);
        });

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            URI newOrderUrl = createNewOrder(restTemplate);
            URI orderItemUri = addItemToOrder(restTemplate, newOrderUrl, 10L, 90);

            // Then user shouldn't be able to change quantity
            // When trying to change quantity to too big
            thrown.expect(HttpClientErrorException.class);
            restTemplate.put(orderItemUri.toASCIIString() + "/quantity", 101);
        });
    }

    @Test
    public void creatingNewProductAndAddingItToOrderSavesAllValuesCorrectlyTest() throws Throwable {

        final ProductDto testProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);
        final SkuDto additionalSkuDto = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

        testProductDto.setSkus(Arrays.asList(additionalSkuDto));
        testProductDto.setValidTo(ApiTestUtils.addNDaysToDate(testProductDto.getValidFrom(), 10));

        final ResponseEntity<?> newProductResponseEntity = catalogOperationsRemote.addTestProduct(testProductDto);
        assertThat(newProductResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = ApiTestUtils.getIdFromEntity(newProductResponseEntity);


        //ResponseEntity<ProductDto> remoteTestProductByIdEntity = getRemoteTestProductByIdEntity(productId);

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            final ResponseEntity<ProductDto> remoteTestProductByIdEntity =
                    restTemplate.exchange(
                            ApiTestUrls.PRODUCT_BY_ID_URL,
                            HttpMethod.GET,
                            null,
                            ProductDto.class, serverPort, productId);

            assertThat(remoteTestProductByIdEntity.getStatusCode(), equalTo(HttpStatus.OK));


            ProductDto receivedProductDto= remoteTestProductByIdEntity.getBody();
            long skuId = ApiTestUtils.getIdFromLocationUrl(receivedProductDto.getLink("skus").getHref());


            URI newOrderUrl = createNewOrder(restTemplate);

            URI orderItemUrl = addItemToOrder(restTemplate, newOrderUrl, skuId, 2);
            assertNotNull(orderItemUrl);

            getItemDetailsFromCart(restTemplate, orderItemUrl);
        });
    }

    @Test
    public void addingSkusWithNotEnoughQuantityAvailableThrowsAnExceptionTest() throws Throwable {


        final int TEST_QUANTITY = 3;

        ProductDto testProductDto = DtoTestFactory.getTestProductWithoutDefaultCategory(DtoTestType.NEXT);

        SkuDto additionalSkuDto = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

        additionalSkuDto.setQuantityAvailable(TEST_QUANTITY);
        additionalSkuDto.setAvailability("CHECK_QUANTITY");

        testProductDto.setSkus(Arrays.asList(additionalSkuDto));
        testProductDto.setValidTo(ApiTestUtils.addNDaysToDate(testProductDto.getValidFrom(), 10));

        ResponseEntity<?> newProductResponseEntity = catalogOperationsRemote.addTestProduct(testProductDto);
        assertThat(newProductResponseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        long productId = ApiTestUtils.getIdFromEntity(newProductResponseEntity);

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {

            final ResponseEntity<ProductDto> remoteTestProductByIdEntity =
                    restTemplate.exchange(
                            ApiTestUrls.PRODUCT_BY_ID_URL,
                            HttpMethod.GET,
                            null,
                            ProductDto.class, serverPort, productId);

            assertThat(remoteTestProductByIdEntity.getStatusCode(), equalTo(HttpStatus.OK));

            //ResponseEntity<ProductDto> remoteTestProductByIdEntity = getRemoteTestProductByIdEntity(productId);
            ProductDto receivedProductDto= remoteTestProductByIdEntity.getBody();
            long skuId = ApiTestUtils.getIdFromLocationUrl(receivedProductDto.getLink("skus").getHref());


            URI newOrderUrl = createNewOrder(restTemplate);
            try {
                addItemToOrder(restTemplate, newOrderUrl, skuId, TEST_QUANTITY + 2);
                fail();
            } catch(HttpStatusCodeException httpStatusCodeException) {
                assertTrue(httpStatusCodeException.getStatusCode().is5xxServerError());
            }

            // this should add correctly
            URI orderItemUrl = addItemToOrder(restTemplate, newOrderUrl, skuId, TEST_QUANTITY);
            assertNotNull(orderItemUrl);

            getItemDetailsFromCart(restTemplate, orderItemUrl);
        });

    }

    @Test
    public void shouldAddItemToOrderByProductOptionsTest() throws Throwable {

        final ProductDto newProductDto = DtoTestFactory.getTestProductWithDefaultSKUandCategory(DtoTestType.NEXT);
        final SkuDto additionalSku1 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);
        final SkuDto additionalSku2 = DtoTestFactory.getTestAdditionalSku(DtoTestType.NEXT);

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

        final ResponseEntity<?> responseEntity = catalogOperationsRemote.addTestProduct(newProductDto);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final String productHref = responseEntity.getHeaders().getLocation().toString();

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            URI newOrderUrl = createNewOrder(restTemplate);

            final OrderItemDto orderItemDto = new OrderItemDto();
            orderItemDto.setQuantity(2);
            orderItemDto.setProductHref(productHref);

            orderItemDto.setSelectedOptions(
                    Arrays.asList(Pair.of("TESTOPTION", "test2")).stream().collect(Collectors.toMap(Pair::getLeft, Pair::getRight))
            );

            URI orderItemUrl = restTemplate.postForLocation(newOrderUrl.toASCIIString() + "/items", orderItemDto, serverPort);
            assertNotNull(orderItemUrl);

            final List<DiscreteOrderItemDto> itemDetailsFromCart = getItemsFromCart(restTemplate, newOrderUrl);

        /* (mst) Make sure that the correct SKU has been selected */
            assertTrue(!itemDetailsFromCart.isEmpty());
            assertThat(itemDetailsFromCart.size(), equalTo(1));
            assertThat(itemDetailsFromCart.get(0).getRetailPrice().getAmount(), equalTo(additionalSku2.getRetailPrice()));
        });
    }

    @Test
    @Transactional
    public void shouldAddItemToOrderByProductOptions2Test() throws Throwable {
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

        final ResponseEntity<?> responseEntity = catalogOperationsRemote.addTestProduct(newProductDto);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));
        final String productHref = responseEntity.getHeaders().getLocation().toString();

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            URI newOrderUrl = createNewOrder(restTemplate);

        /* (mst) Add first product with given options */
            final OrderItemDto orderItemDto = new OrderItemDto();
            orderItemDto.setQuantity(2);
            orderItemDto.setProductHref(productHref);

            orderItemDto.setSelectedOptions(
                    Arrays.asList(Pair.of("TESTOPTION", "test2")).stream().collect(Collectors.toMap(Pair::getLeft, Pair::getRight))
            );

            URI orderItemUrl = restTemplate.postForLocation(newOrderUrl.toASCIIString() + "/items", orderItemDto, serverPort);
            assertNotNull(orderItemUrl);

        /* (mst) Add second product with given options */
            final OrderItemDto orderItemDto2 = new OrderItemDto();
            orderItemDto2.setQuantity(2);
            orderItemDto2.setProductHref(productHref);

            orderItemDto2.setSelectedOptions(
                    Arrays.asList(Pair.of("TESTOPTION", "test1")).stream().collect(Collectors.toMap(Pair::getLeft, Pair::getRight))
            );

            URI orderItemUrl2 = restTemplate.postForLocation(newOrderUrl.toASCIIString() + "/items", orderItemDto2, serverPort);
            assertNotNull(orderItemUrl2);

        /* (mst) Verify that both SKUs have been properly added */
            final List<DiscreteOrderItemDto> itemDetailsFromCart = getItemsFromCart(restTemplate, newOrderUrl);

        /* (mst) Make sure that the correct SKU has been selected */
            assertThat(itemDetailsFromCart.size(), equalTo(2));

            DiscreteOrderItemDto firstDiscreteOrderItemDto = itemDetailsFromCart.get(0);

            if (firstDiscreteOrderItemDto.getRetailPrice().getAmount().compareTo(additionalSku1.getRetailPrice()) == 0) {
                assertThat(itemDetailsFromCart.get(1).getRetailPrice().getAmount(), equalTo(additionalSku2.getRetailPrice()));
            } else {
                assertThat(itemDetailsFromCart.get(1).getRetailPrice().getAmount(), equalTo(additionalSku1.getRetailPrice()));
                assertThat(firstDiscreteOrderItemDto.getRetailPrice().getAmount(), equalTo(additionalSku2.getRetailPrice()));
            }
        });
    }
}