package pl.touk.widerest.api;

import com.google.common.collect.Lists;
import org.assertj.core.util.Arrays;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.api.common.AddressDto;
import pl.touk.widerest.api.orders.OrderItemDto;
import pl.touk.widerest.api.orders.fulfillments.FulfillmentGroupDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.base.MappingHalJackson2HttpMessageConverter;
import pl.touk.widerest.security.oauth2.Scope;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.arrayWithSize;
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
    public void shouldNotCreateAnyFulfillmentForEmptyOrder() throws URISyntaxException, IOException {

        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        long orderId = whenNewOrderCreated();
        Collection<FulfillmentGroupDto> fulfillments = whenOrderFulfillmentsRetrieved(orderId);

        // then: order should not have any fulfillment group yet
        assertTrue(fulfillments.isEmpty());
    }

    @Test
    public void shouldNotLeaveEmptyFulfilments() throws URISyntaxException, IOException {

        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        long orderId = whenNewOrderCreated();
        Collection<FulfillmentGroupDto> fulfillments = whenOrderFulfillmentsRetrieved(orderId);

        URI orderItemHref = whenOrderItemAdded(orderId, SAMPLE_PRODUCT_HREF_1);
        whenOrderItemDeleted(orderItemHref);

        // then: order should not have any fulfillment group yet
        assertTrue(fulfillments.isEmpty());
    }

    @Test
    @Transactional
    public void shouldCreateAProperFulfillmentGroupForFirstItemInOrderTest() throws URISyntaxException, IOException {
        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        long orderId = whenNewOrderCreated();

        URI orderItemHref = whenOrderItemAdded(orderId, SAMPLE_PRODUCT_HREF_1);

        Collection<FulfillmentGroupDto> fulfillments = whenOrderFulfillmentsRetrieved(orderId);

        assertThat(fulfillments.size(), equalTo(1));

        final FulfillmentGroupDto fulfillment = fulfillments.iterator().next();

        assertThat(fulfillment.getItems().size(), equalTo(1));
        assertThat(fulfillment.getItems().get(0).trim(), equalTo(orderItemHref.toASCIIString()));
    }


    @Test
    public void shouldAddProperAddressToItemsFulfillmentTest() throws Exception {
        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        long orderId = whenNewOrderCreated();

        URI orderItemHref = whenOrderItemAdded(orderId, SAMPLE_PRODUCT_HREF_1);

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

        Collection<FulfillmentGroupDto> fulfillments = whenOrderFulfillmentsRetrieved(orderId);

        final String fulfillmentGroupUrl = fulfillments.iterator().next().getLink("self").getHref();

        oAuth2RestTemplate.put(
                fulfillmentGroupUrl,
                fulfillmentGroupDto
        );

        // then: address should get changed and its new values should be properly set

        Collection<FulfillmentGroupDto> fulfillmentsAfterAddressUpdate = whenOrderFulfillmentsRetrieved(orderId);

        assertThat(fulfillmentsAfterAddressUpdate.size(), equalTo(1));

        final FulfillmentGroupDto updatedFulfillmentGroupDto = fulfillmentsAfterAddressUpdate.iterator().next();

        assertThat(updatedFulfillmentGroupDto, equalTo(fulfillmentGroupDto));
    }

    @Test
    public void shouldPlaceAllOrderItemsInADefaultFulfillmentGroupTest() throws Exception {

        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        long orderId = whenNewOrderCreated();

        URI orderItem1Href = whenOrderItemAdded(orderId, SAMPLE_PRODUCT_HREF_1);
        URI orderItem2Href = whenOrderItemAdded(orderId, SAMPLE_PRODUCT_HREF_2);

        // then: both items should belong to the same fulfillment group
        Collection<FulfillmentGroupDto> fulfillments = whenOrderFulfillmentsRetrieved(orderId);
        assertThat(fulfillments.size(), equalTo(1));

        final FulfillmentGroupDto receivedFulfillmentGroupDto = fulfillments.iterator().next();

        assertThat(receivedFulfillmentGroupDto.getItems().size(), equalTo(2));
        assertTrue(receivedFulfillmentGroupDto.getItems().contains(orderItem1Href.toASCIIString()));
        assertTrue(receivedFulfillmentGroupDto.getItems().contains(orderItem2Href.toASCIIString()));

    }

    @Test
    public void shouldHaveTwoDifferentFulfillmentGroupsForTwoDifferentOrderItemsTest() throws Exception {
        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        long orderId = whenNewOrderCreated();

        URI orderItem1Href = whenOrderItemAdded(orderId, SAMPLE_PRODUCT_HREF_1);
        URI orderItem2Href = whenOrderItemAdded(orderId, SAMPLE_PRODUCT_HREF_2);

        // when: 2) creating 2nd fulfillment group with 1st item in it
        final FulfillmentGroupDto fulfillmentGroupDto = FulfillmentGroupDto.builder()
                .items(Collections.singletonList(orderItem1Href.toASCIIString()))
                .build();

        final String createdFulfillmentGroupUrl = oAuth2RestTemplate.postForLocation(
                ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                fulfillmentGroupDto,
                serverPort,
                orderId
        ).toASCIIString();


        // then: 2) both items should be placed in two different fulfillment groups
        // then: 2.a) there should be two fulfillment groups associated with a created order
        FulfillmentGroupDto[] fulfillments = whenOrderFulfillmentsRetrieved(orderId).toArray(new FulfillmentGroupDto[0]);
        assertThat(fulfillments, arrayWithSize(2));

        // both fulfillment groups hold only 1 order item reference
        assertArrayEquals(fulfillments[0].getItems().toArray(), Arrays.array(orderItem2Href.toASCIIString()));
        assertArrayEquals(fulfillments[1].getItems().toArray(), Arrays.array(orderItem1Href.toASCIIString()));

        // then: 2.b) refering to both fulfillment groups individually, returns "the same" results

        FulfillmentGroupDto fulfillmentGroupDto1 = whenSingleOrderFulfillmentsRetrieved(URI.create(fulfillments[0].getLink("self").getHref()));
        assertArrayEquals(fulfillmentGroupDto1.getItems().toArray(), Arrays.array(orderItem2Href.toASCIIString()));

        FulfillmentGroupDto fulfillmentGroupDto2 = whenSingleOrderFulfillmentsRetrieved(URI.create(fulfillments[1].getLink("self").getHref()));
        assertArrayEquals(fulfillmentGroupDto2.getItems().toArray(), Arrays.array(orderItem1Href.toASCIIString()));
    }

    @Test
    public void shouldReturnErrorWhenAddingNonExistentItemIntoFulfillmentGroupTest() throws Exception {

        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        long orderId = whenNewOrderCreated();

        final String NONEXISTENT_ITEM = ApiTestUrls.ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + orderId +
                "/items/9999";

        final FulfillmentGroupDto fulfillmentGroupDto = FulfillmentGroupDto.builder()
                .items(Collections.singletonList(NONEXISTENT_ITEM))
                .build();

        thrown.expect(HttpClientErrorException.class);
        oAuth2RestTemplate.postForLocation(ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL, fulfillmentGroupDto, serverPort, orderId);
    }

    @Test
    public void shouldReturnErrorWhenUpdatingDefaultFulfillmentGroupWithNonExistingItemTest() throws Exception {

        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        long orderId = whenNewOrderCreated();

        URI orderItemHref = whenOrderItemAdded(orderId, SAMPLE_PRODUCT_HREF_1);
        FulfillmentGroupDto fulfillment = whenOrderFulfillmentsRetrieved(orderId).iterator().next();
        final String fulfillmentGroupUrl = fulfillment.getLink("self").getHref();

        final String NON_EXISTING_ITEM = ApiTestUrls.ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + orderId +
                "/items/9999";
        fulfillment.setItems(Collections.singletonList(NON_EXISTING_ITEM));

        thrown.expect(HttpClientErrorException.class);
        oAuth2RestTemplate.put(fulfillmentGroupUrl, fulfillment);
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

    protected URI whenOrderItemAdded(final long orderId, final URI productHref) {
        return whenOrderItemAdded(orderId, productHref, 1);
    }

    protected URI whenOrderItemAdded(final long orderId, final URI productHref, final int quantity) {
        final OrderItemDto dto = OrderItemDto.builder()
                .productHref(productHref.toASCIIString())
                .quantity(quantity)
                .build();
        return oAuth2RestTemplate.postForLocation(ApiTestUrls.ORDERS_BY_ID_ITEMS, dto, serverPort, orderId);
    }

    protected void whenOrderItemDeleted(final URI orderItemHref) {
        oAuth2RestTemplate.delete(orderItemHref);;
    }

    private Collection<FulfillmentGroupDto> whenOrderFulfillmentsRetrieved(final long orderId) {
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

    private FulfillmentGroupDto whenSingleOrderFulfillmentsRetrieved(URI fulfillmentHref) {
        return oAuth2RestTemplate.getForObject(fulfillmentHref, FulfillmentGroupDto.class);
    }

    private ResponseEntity<Resources<FulfillmentGroupDto>> getFulfillmentGroupsForOrder(final RestTemplate clientRestTemplate, final String clientAccessToken, final long orderId) {
        final HttpHeaders clientRequestHeaders = new HttpHeaders();
        clientRequestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        clientRequestHeaders.setContentType(MediaType.APPLICATION_JSON);
        clientRequestHeaders.set("Authorization", "Bearer " + clientAccessToken);

        final List<HttpMessageConverter<?>> currentMessageConverters = clientRestTemplate.getMessageConverters();

        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

        // then: 2.a) there should be two fulfillment groups associated with a created order
        final ResponseEntity<Resources<FulfillmentGroupDto>> receivedOriginalFulfillmentGroupEntity =
                clientRestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<Resources<FulfillmentGroupDto>>() {},
                        serverPort,
                        orderId
                );

        clientRestTemplate.setMessageConverters(currentMessageConverters);

        return receivedOriginalFulfillmentGroupEntity;
    }

    private ResponseEntity<FulfillmentGroupDto> getFulfillmentGroupByUrl(final RestTemplate clientRestTemplate,
                                                                                final String clientAccessToken,
                                                                                final String fulfillmentGroupUrl) {
        final HttpHeaders clientRequestHeaders = new HttpHeaders();
        clientRequestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        clientRequestHeaders.setContentType(MediaType.APPLICATION_JSON);
        clientRequestHeaders.set("Authorization", "Bearer " + clientAccessToken);

        final List<HttpMessageConverter<?>> currentMessageConverters = clientRestTemplate.getMessageConverters();

        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

        final ResponseEntity<FulfillmentGroupDto> receivedNewFulfillmentGroup1Entity =
                clientRestTemplate.exchange(
                        fulfillmentGroupUrl,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<FulfillmentGroupDto>() {}

                );

        clientRestTemplate.setMessageConverters(currentMessageConverters);

        return receivedNewFulfillmentGroup1Entity;
    }

}
