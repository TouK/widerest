package pl.touk.widerest.api;

import javaslang.control.Try;
import org.assertj.core.util.Arrays;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.api.common.AddressDto;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.orders.OrderItemDto;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentGroupDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.security.oauth2.Scope;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class FulfillmentGroupsTest extends ApiTestBase {

    public static final URI SAMPLE_PRODUCT_HREF_1 = URI.create(ApiTestUrls.PRODUCT_BY_ID_URL.substring(ApiTestUrls.API_BASE_URL.length()).replaceFirst("\\{productId\\}", "1"));
    public static final URI SAMPLE_PRODUCT_HREF_2 = URI.create(ApiTestUrls.PRODUCT_BY_ID_URL.substring(ApiTestUrls.API_BASE_URL.length()).replaceFirst("\\{productId\\}", "2"));

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Test
    public void shouldNotCreateAnyFulfillmentForEmptyOrder() throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            whenNewOrderCreated(restTemplate, orderUri ->
                    whenOrderFulfillmentsRetrieved(restTemplate, orderUri, (Collection<FulfillmentGroupDto> fulfillments) -> {
                        // then: order should not have any fulfillment group yet
                        assertTrue(fulfillments.isEmpty());
                    }));
        });
    }

    @Test
    public void shouldNotLeaveEmptyFulfilments() throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate ->
                whenNewOrderCreated(restTemplate, orderUri -> {
                    whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_1, orderItemHref ->
                            whenOrderItemDeleted(restTemplate, orderItemHref));
                    whenOrderFulfillmentsRetrieved(restTemplate, orderUri, (Collection<FulfillmentGroupDto> fulfillments) -> {
                        // then: order should not have any fulfillment group
                        assertTrue(fulfillments.isEmpty());
                    });
                }));
    }

    @Test
    @Transactional
    public void shouldCreateAProperFulfillmentGroupForFirstItemInOrderTest() throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            whenNewOrderCreated(restTemplate, orderUri -> {
                whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_1, orderItemHref -> {
                    whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {
                        assertThat(fulfillments.size(), equalTo(1));
                        final FulfillmentGroupDto fulfillment = fulfillments.iterator().next();
                        assertThat(fulfillment.getItems().size(), equalTo(1));
                        assertThat(fulfillment.getItems().get(0).trim(), equalTo(orderItemHref.toASCIIString()));
                    });
                });
            });
        });
    }


    @Test
    public void shouldAddProperAddressToItemsFulfillmentTest() throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            whenNewOrderCreated(restTemplate, orderUri -> {
                whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_1, orderItemHref -> {

                    final AddressDto addressDto = AddressDto.builder()
                            .firstName("Jan")
                            .lastName("Kowalski")
                            .city("Wroclaw")
                            .postalCode("02-945")
                            .addressLine1("Zakopanska 40")
                            .countryCode("PL")
                            .build();

                    final FulfillmentGroupDto fulfillmentGroupDto = FulfillmentGroupDto.builder()
                            .items(Collections.singletonList(orderItemHref.toASCIIString()))
                            .address(addressDto)
                            .build();

                    whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {
                        final String fulfillmentGroupUrl = fulfillments.iterator().next().getLink("self").getHref();
                        restTemplate.put(
                                fulfillmentGroupUrl,
                                fulfillmentGroupDto
                        );

                    });

                    whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillmentsAfterAddressUpdate -> {
                        // then: address should get changed and its new values should be properly set
                        assertThat(fulfillmentsAfterAddressUpdate.size(), equalTo(1));
                        final FulfillmentGroupDto updatedFulfillmentGroupDto = fulfillmentsAfterAddressUpdate.iterator().next();
                        assertThat(updatedFulfillmentGroupDto, equalTo(fulfillmentGroupDto));

                    });

                });
            });
        });
    }

    @Test
    public void shouldPlaceAllOrderItemsInADefaultFulfillmentGroupTest() throws Throwable {

        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            whenNewOrderCreated(restTemplate, orderUri -> {
                whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_1, orderItem1Href -> {
                    whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_2, orderItem2Href -> {
                        whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {

                            // then: both items should belong to the same fulfillment group
                            assertThat(fulfillments.size(), equalTo(1));
                            final FulfillmentGroupDto receivedFulfillmentGroupDto = fulfillments.iterator().next();
                            assertThat(receivedFulfillmentGroupDto.getItems().size(), equalTo(2));
                            assertTrue(receivedFulfillmentGroupDto.getItems().contains(orderItem1Href.toASCIIString()));
                            assertTrue(receivedFulfillmentGroupDto.getItems().contains(orderItem2Href.toASCIIString()));

                        });
                    });
                });
            });
        });



    }

    @Test
    public void shouldHaveTwoDifferentFulfillmentGroupsForTwoDifferentOrderItemsTest() throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            whenNewOrderCreated(restTemplate, orderUri -> {
                whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_1, orderItem1Href -> {
                    whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_2, orderItem2Href -> {
                        // when: 2) creating 2nd fulfillment group with 1st item in it
                        final FulfillmentGroupDto fulfillmentGroupDto = FulfillmentGroupDto.builder()
                                .items(Collections.singletonList(orderItem1Href.toASCIIString()))
                                .build();

                        final String createdFulfillmentGroupUrl = restTemplate.postForLocation(
                                ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                                fulfillmentGroupDto,
                                serverPort,
                                CatalogUtils.getIdFromUrl(orderUri)
                        ).toASCIIString();

                        // then: 2) both items should be placed in two different fulfillment groups
                        whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {
                            // then: 2.a) there should be two fulfillment groups associated with a created order
                            assertThat(fulfillments, hasSize(2));

                            Iterator<FulfillmentGroupDto> iterator = fulfillments.iterator();
                            FulfillmentGroupDto fulfillment1 = iterator.next();
                            FulfillmentGroupDto fulfillment2 = iterator.next();

                            // both fulfillment groups hold only 1 order item reference
                            assertArrayEquals(fulfillment1.getItems().toArray(), Arrays.array(orderItem2Href.toASCIIString()));
                            assertArrayEquals(fulfillment2.getItems().toArray(), Arrays.array(orderItem1Href.toASCIIString()));

                            // then: 2.b) refering to both fulfillment groups individually, returns "the same" results

                            whenSingleOrderFulfillmentRetrieved(restTemplate, URI.create(fulfillment1.getLink("self").getHref()), fulfillmentGroupDto1 -> {
                                assertArrayEquals(fulfillmentGroupDto1.getItems().toArray(), Arrays.array(orderItem2Href.toASCIIString()));
                            });

                            whenSingleOrderFulfillmentRetrieved(restTemplate, URI.create(fulfillment2.getLink("self").getHref()), fulfillmentGroupDto2 -> {
                                assertArrayEquals(fulfillmentGroupDto2.getItems().toArray(), Arrays.array(orderItem1Href.toASCIIString()));
                            });
                        });
                    });
                });
            });
        });
    }

    @Test
    public void shouldReturnErrorWhenAddingNonExistentItemIntoFulfillmentGroupTest() throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            whenNewOrderCreated(restTemplate, orderUri -> {
                final String NONEXISTENT_ITEM = orderUri + "/items/9999";
                final FulfillmentGroupDto fulfillmentGroupDto = FulfillmentGroupDto.builder()
                        .items(Collections.singletonList(NONEXISTENT_ITEM))
                        .build();

                thrown.expect(HttpClientErrorException.class);
                restTemplate.postForLocation(ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL, fulfillmentGroupDto, serverPort, CatalogUtils.getIdFromUrl(orderUri));
            });
        });
    }

    @Test
    public void shouldReturnErrorWhenUpdatingDefaultFulfillmentGroupWithNonExistingItemTest() throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            whenNewOrderCreated(restTemplate, orderUri -> {
                whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_1);
                whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {
                    final FulfillmentGroupDto fulfillment = fulfillments.iterator().next();
                    final String fulfillmentGroupUrl = fulfillment.getLink("self").getHref();
                    final String NON_EXISTING_ITEM = orderUri + "/items/9999";
                    fulfillment.setItems(Collections.singletonList(NON_EXISTING_ITEM));

                    thrown.expect(HttpClientErrorException.class);
                    restTemplate.put(fulfillmentGroupUrl, fulfillment);
                });
            });
        });
    }

    @Test
    @Ignore("TODO")
    public void shouldReturnErrorWhenUpdatingDefaultFulfillmentGroupWithoutAnyProductTest() {

    }

    @Test
    @Ignore("TODO")
    public void shouldIgnoreDuplicateOrderItemHrefsWhenAddingToFulfillmentGroupTest() {

    }

    @Test
    @Ignore("TODO")
    public void shouldNotSaveAnyOrderItemToAFulfillmentGroupIfOneIsInvalidTest() {

    }

    @Test
    @Ignore("TODO")
    public void shouldHandleMultipleFulfillmentGroupsComplexOperationsProperlyTest() {

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

    private void whenOrderFulfillmentsRetrieved(RestTemplate oAuth2RestTemplate, final URI orderUri, Try.CheckedConsumer<Collection<FulfillmentGroupDto>>... thens) throws Throwable {
        when(() -> retrieveOrderFulfillments(oAuth2RestTemplate, CatalogUtils.getIdFromUrl(orderUri)), thens);
    }

    private Collection<FulfillmentGroupDto> retrieveOrderFulfillments(RestTemplate oAuth2RestTemplate, final long orderId) {
        final ResponseEntity<Resources<FulfillmentGroupDto>> receivedOriginalFulfillmentGroupEntity =
                oAuth2RestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Resources<FulfillmentGroupDto>>() {
                        },
                        serverPort,
                        orderId
                );
        return receivedOriginalFulfillmentGroupEntity.getBody().getContent();
    }

    private void whenSingleOrderFulfillmentRetrieved(RestTemplate restTemplate, URI fulfillmentHref, Try.CheckedConsumer<FulfillmentGroupDto>... thens) throws Throwable {
        when(() -> restTemplate.getForObject(fulfillmentHref, FulfillmentGroupDto.class), thens);
    }

}
