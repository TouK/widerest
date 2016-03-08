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
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.api.common.AddressDto;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.orders.OrderItemDto;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.security.oauth2.Scope;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
public class OrderFulfillmentsTest extends ApiTestBase {

    public static final URI SAMPLE_PRODUCT_HREF_1 = URI.create(ApiTestUrls.PRODUCT_BY_ID_URL.substring(ApiTestUrls.API_BASE_URL.length()).replaceFirst("\\{productId\\}", "1"));
    public static final URI SAMPLE_PRODUCT_HREF_2 = URI.create(ApiTestUrls.PRODUCT_BY_ID_URL.substring(ApiTestUrls.API_BASE_URL.length()).replaceFirst("\\{productId\\}", "2"));

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Test
    public void shouldNotCreateAnyFulfillmentForEmptyOrder() throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            whenNewOrderCreated(restTemplate, orderUri ->
                    whenOrderFulfillmentsRetrieved(restTemplate, orderUri, (Collection<FulfillmentDto> fulfillments) -> {
                        // then: order should not have any fulfillment group yet
                        assertTrue(fulfillments.isEmpty());
                    }));
        });
    }

    @FunctionalInterface
    interface CheckedConsumer3<T, U, V> {
        void accept(T t, U u, V v) throws Throwable;
    }


    protected void givenAnonymousUserWithAnOrderAndAnItem(CheckedConsumer3<OAuth2RestTemplate, URI, URI> then) throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate ->
                whenNewOrderCreated(restTemplate, orderUri -> {
                    whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_1, orderItemHref -> {
                        then.accept(restTemplate, orderUri, orderItemHref);
                    });
                })
        );
    }

    @Test
    public void shouldNotLeaveEmptyFulfilments() throws Throwable {
        givenAnonymousUserWithAnOrderAndAnItem((restTemplate, orderUri, orderItemHref) -> {
            whenOrderItemDeleted(restTemplate, orderItemHref);
            whenOrderFulfillmentsRetrieved(restTemplate, orderUri, (Collection<FulfillmentDto> fulfillments) -> {
                // then: order should not have any fulfillment group
                assertTrue(fulfillments.isEmpty());
            });
        });
    }

    @Test
    public void shouldCreateAProperFulfillmentGroupForFirstItemInOrderTest() throws Throwable {
        givenAnonymousUserWithAnOrderAndAnItem((restTemplate, orderUri, orderItemHref) -> {
            whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {
                assertThat(fulfillments.size(), equalTo(1));
                final FulfillmentDto fulfillment = fulfillments.iterator().next();
                assertThat(fulfillment.getItems().size(), equalTo(1));
                assertThat(fulfillment.getItems().get(0).trim(), equalTo(orderItemHref.toASCIIString()));
            });
        });
    }


    @Test
    public void shouldAddProperAddressToItemsFulfillmentTest() throws Throwable {
        givenAnonymousUserWithAnOrderAndAnItem((restTemplate, orderUri, orderItemHref) -> {

            final AddressDto addressDto = AddressDto.builder()
                    .firstName("Jan")
                    .lastName("Kowalski")
                    .city("Wroclaw")
                    .postalCode("02-945")
                    .addressLine1("Zakopanska 40")
                    .countryCode("PL")
                    .build();

            final FulfillmentDto fulfillmentDto = FulfillmentDto.builder()
                    .items(Collections.singletonList(orderItemHref.toASCIIString()))
                    .address(addressDto)
                    .build();

            whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {
                final String fulfillmentGroupUrl = fulfillments.iterator().next().getLink("self").getHref();
                restTemplate.put(
                        fulfillmentGroupUrl,
                        fulfillmentDto
                );

            });

            whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillmentsAfterAddressUpdate -> {
                // then: address should get changed and its new values should be properly set
                assertThat(fulfillmentsAfterAddressUpdate.size(), equalTo(1));
                final FulfillmentDto updatedFulfillmentDto = fulfillmentsAfterAddressUpdate.iterator().next();
                assertThat(updatedFulfillmentDto.getAddress(), equalTo(fulfillmentDto.getAddress()));

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
                            final FulfillmentDto receivedFulfillmentDto = fulfillments.iterator().next();
                            assertThat(receivedFulfillmentDto.getItems().size(), equalTo(2));
                            assertTrue(receivedFulfillmentDto.getItems().contains(orderItem1Href.toASCIIString()));
                            assertTrue(receivedFulfillmentDto.getItems().contains(orderItem2Href.toASCIIString()));

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
                        final FulfillmentDto fulfillmentDto = FulfillmentDto.builder()
                                .items(Collections.singletonList(orderItem1Href.toASCIIString()))
                                .build();

                        final String createdFulfillmentGroupUrl = restTemplate.postForLocation(
                                ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                                fulfillmentDto,
                                serverPort,
                                CatalogUtils.getIdFromUrl(orderUri)
                        ).toASCIIString();

                        // then: 2) both items should be placed in two different fulfillment groups
                        whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {
                            // then: 2.a) there should be two fulfillment groups associated with a created order
                            assertThat(fulfillments, hasSize(2));

                            Iterator<FulfillmentDto> iterator = fulfillments.iterator();
                            FulfillmentDto fulfillment1 = iterator.next();
                            FulfillmentDto fulfillment2 = iterator.next();

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
                final FulfillmentDto fulfillmentDto = FulfillmentDto.builder()
                        .items(Collections.singletonList(NONEXISTENT_ITEM))
                        .build();

                thrown.expect(HttpClientErrorException.class);
                restTemplate.postForLocation(ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL, fulfillmentDto, serverPort, CatalogUtils.getIdFromUrl(orderUri));
            });
        });
    }

    @Test
    public void shouldReturnErrorWhenUpdatingDefaultFulfillmentGroupWithNonExistingItemTest() throws Throwable {
        givenAuthorizationFor(Scope.CUSTOMER, restTemplate -> {
            whenNewOrderCreated(restTemplate, orderUri -> {
                whenOrderItemAdded(restTemplate, orderUri, SAMPLE_PRODUCT_HREF_1);
                whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {
                    final FulfillmentDto fulfillment = fulfillments.iterator().next();
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
    public void shouldNotAcceptInvalidCountryNameInAddress() throws Throwable {
        givenAnonymousUserWithAnOrderAndAnItem((restTemplate, orderUri, orderItemHref) -> {
            whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {

                FulfillmentDto fulfillment = fulfillments.iterator().next();
                final String fulfillmentUrl = fulfillment.getLink("self").getHref();

                // When sending wrong address
                AddressDto addressDto = new AddressDto();
                addressDto.setFirstName("Haskell");
                addressDto.setLastName("Curry");
                addressDto.setAddressLine1("Semigroup Valley 12");
                addressDto.setPostalCode("13-337");
                addressDto.setCity("Massachusetts");
                addressDto.setCountryCode("USA");

                fulfillment.setAddress(addressDto);

                try {
                    restTemplate.put(fulfillmentUrl, fulfillment);
                    fail("Address was checked and should be invalid");
                } catch (HttpClientErrorException e) {
                    assertTrue(e.getStatusCode().is4xxClientError());
                }
            });
        });
    }

    @Test
    public void shouldReturnFulfillmentAddressAndOption() throws Throwable {
        givenAnonymousUserWithAnOrderAndAnItem((restTemplate, orderUri, orderItemHref) -> {

            // Given address and fulfillment option
            AddressDto addressDto = new AddressDto();
            addressDto.setAddressLine1("ul. Warszawska 45");
            addressDto.setAddressLine2("POLSKA");
            addressDto.setCity("Poznan");
            addressDto.setPostalCode("05-134");
            addressDto.setFirstName("Haskell");
            addressDto.setLastName("Curry");
            addressDto.setCountryCode("US");

            whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {

                final FulfillmentDto fulfillment = fulfillments.iterator().next();
                final String fulfillmentUrl = fulfillment.getLink("self").getHref();

                assertThat(fulfillment.getSelectedFulfillmentOption(), isEmptyOrNullString());

                fulfillment.setAddress(addressDto);

                String selectedFulfillmentOption = fulfillment.getFulfillmentOptions().entrySet().stream().findFirst().map(e -> e.getKey()).get();
                fulfillment.setSelectedFulfillmentOption(selectedFulfillmentOption);

                restTemplate.put(fulfillmentUrl, fulfillment);

            });
            whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {

                final FulfillmentDto fulfillment = fulfillments.iterator().next();

                assertThat(
                        fulfillment.getAddress(),
                        equalTo(addressDto)
                );
                assertThat(
                        fulfillment.getSelectedFulfillmentOption(),
                        equalTo(fulfillment.getFulfillmentOptions().entrySet().stream().findFirst().map(e -> e.getKey()).get())
                );

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

    private void whenOrderFulfillmentsRetrieved(RestTemplate oAuth2RestTemplate, final URI orderUri, Try.CheckedConsumer<Collection<FulfillmentDto>>... thens) throws Throwable {
        when(() -> retrieveOrderFulfillments(oAuth2RestTemplate, CatalogUtils.getIdFromUrl(orderUri)), thens);
    }

    private Collection<FulfillmentDto> retrieveOrderFulfillments(RestTemplate oAuth2RestTemplate, final long orderId) {
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

    private void whenSingleOrderFulfillmentRetrieved(RestTemplate restTemplate, URI fulfillmentHref, Try.CheckedConsumer<FulfillmentDto>... thens) throws Throwable {
        when(() -> restTemplate.getForObject(fulfillmentHref, FulfillmentDto.class), thens);
    }

}
