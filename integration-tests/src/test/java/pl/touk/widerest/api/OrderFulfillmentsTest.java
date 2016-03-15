package pl.touk.widerest.api;

import org.assertj.core.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.api.common.AddressDto;
import pl.touk.widerest.api.common.CatalogUtils;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentDto;
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
public class OrderFulfillmentsTest extends AbstractTest {

    public static final URI SAMPLE_PRODUCT_HREF_1 = URI.create(ApiTestUrls.PRODUCT_BY_ID_URL.substring(ApiTestUrls.API_BASE_URL.length()).replaceFirst("\\{productId\\}", "1"));
    public static final URI SAMPLE_PRODUCT_HREF_2 = URI.create(ApiTestUrls.PRODUCT_BY_ID_URL.substring(ApiTestUrls.API_BASE_URL.length()).replaceFirst("\\{productId\\}", "2"));

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

        default void init111() {
        }

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
    public void shouldNotLeaveAllowEmptyFulfilmentCreation() throws Throwable {
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
                    .address(addressDto)
                    .build();

            thrown.expect(HttpClientErrorException.class);
            restTemplate.postForLocation(
                    ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                    fulfillmentDto,
                    serverPort,
                    CatalogUtils.getIdFromUrl(orderUri)
            );

        });
    }


    @Test
    public void shouldCreateAProperFulfillmentGroupForFirstItemInOrderTest() throws Throwable {
        givenAnonymousUserWithAnOrderAndAnItem((restTemplate, orderUri, orderItemHref) -> {
            whenOrderFulfillmentsRetrieved(restTemplate, orderUri, fulfillments -> {
                assertThat(fulfillments.size(), equalTo(1));
                final FulfillmentDto fulfillment = fulfillments.iterator().next();
                assertThat(fulfillment.getItemHrefs().size(), equalTo(1));
                assertThat(fulfillment.getItemHrefs().get(0).trim(), equalTo(orderItemHref.toASCIIString()));
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
                    .itemHrefs(Collections.singletonList(orderItemHref.toASCIIString()))
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

            // then there is no problem with adding more items
            whenOrderItemAdded(restTemplate,  orderUri, SAMPLE_PRODUCT_HREF_2, orderItem2Href -> {
                // no error
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
                            assertThat(receivedFulfillmentDto.getItemHrefs().size(), equalTo(2));
                            assertTrue(receivedFulfillmentDto.getItemHrefs().contains(orderItem1Href.toASCIIString()));
                            assertTrue(receivedFulfillmentDto.getItemHrefs().contains(orderItem2Href.toASCIIString()));

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
                                .itemHrefs(Collections.singletonList(orderItem1Href.toASCIIString()))
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
                            assertArrayEquals(fulfillment1.getItemHrefs().toArray(), Arrays.array(orderItem2Href.toASCIIString()));
                            assertArrayEquals(fulfillment2.getItemHrefs().toArray(), Arrays.array(orderItem1Href.toASCIIString()));

                            // then: 2.b) refering to both fulfillment groups individually, returns "the same" results

                            whenSingleOrderFulfillmentRetrieved(restTemplate, URI.create(fulfillment1.getLink("self").getHref()), fulfillmentGroupDto1 -> {
                                assertArrayEquals(fulfillmentGroupDto1.getItemHrefs().toArray(), Arrays.array(orderItem2Href.toASCIIString()));
                            });

                            whenSingleOrderFulfillmentRetrieved(restTemplate, URI.create(fulfillment2.getLink("self").getHref()), fulfillmentGroupDto2 -> {
                                assertArrayEquals(fulfillmentGroupDto2.getItemHrefs().toArray(), Arrays.array(orderItem1Href.toASCIIString()));
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
                        .itemHrefs(Collections.singletonList(NONEXISTENT_ITEM))
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
                    fulfillment.setItemHrefs(Collections.singletonList(NON_EXISTING_ITEM));

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


}
