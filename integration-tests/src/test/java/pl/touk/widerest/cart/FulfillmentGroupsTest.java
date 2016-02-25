package pl.touk.widerest.cart;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.api.cart.customers.dto.AddressDto;
import pl.touk.widerest.api.cart.orders.dto.FulfillmentGroupDto;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.base.MappingHalJackson2HttpMessageConverter;

import java.net.URISyntaxException;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class FulfillmentGroupsTest extends ApiTestBase {


    @Before
    public void init() {

    }

    @Test
    public void newlyCreatedOrderShouldNotHaveAnyFulfillmentGroupsTest() throws URISyntaxException {
        // when: creating a new order
        final Pair<RestTemplate, String> user = generateAnonymousUser();
        final RestTemplate clientRestTemplate = user.getKey();
        final String clientAccessToken = user.getValue();
        final int clientOrderId = createNewOrder(clientAccessToken);

        // then: order should not have any fulfillment group yet
        final HttpHeaders clientRequestHeaders = new HttpHeaders();
        clientRequestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        clientRequestHeaders.set("Authorization", "Bearer " + clientAccessToken);
        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

        final ResponseEntity<Resources<FulfillmentGroupDto>> receivedFulfillmentGroupEntity =
                clientRestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<Resources<FulfillmentGroupDto>>() {},
                        serverPort,
                        clientOrderId
                );

        assertThat(receivedFulfillmentGroupEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertTrue(receivedFulfillmentGroupEntity.getBody().getContent().isEmpty());
    }

    @Test
    public void shouldCreateAProperFulfillmentGroupForFirstItemInOrderTest() throws URISyntaxException {
        // when: creating a new order and inserting a new item to it
        final Pair<RestTemplate, String> user = generateAnonymousUser();
        final RestTemplate clientRestTemplate = user.getKey();
        final String clientAccessToken = user.getValue();
        final int clientOrderId = createNewOrder(clientAccessToken);

        final String clientOrderUrl  = ApiTestUrls.ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + clientOrderId;
        final ResponseEntity<?> responseAddItemEntity = addItemToOrder(1, 1, clientOrderUrl + "/items", clientAccessToken, restTemplate);
        assertThat(responseAddItemEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        em.clear();

        // then: "a default" fulfillment group with that item should be created
        final HttpHeaders clientRequestHeaders = new HttpHeaders();
        clientRequestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        clientRequestHeaders.set("Authorization", "Bearer " + clientAccessToken);
        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

        final ResponseEntity<Resources<FulfillmentGroupDto>> receivedFulfillmentGroupEntity =
                clientRestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<Resources<FulfillmentGroupDto>>() {},
                        serverPort,
                        clientOrderId
                );

        assertThat(receivedFulfillmentGroupEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedFulfillmentGroupEntity.getBody().getContent().size(), equalTo(1));

        final FulfillmentGroupDto receivedFulfillmentGroupDto = receivedFulfillmentGroupEntity.getBody().getContent().iterator().next();

        assertThat(receivedFulfillmentGroupDto.getItems().size(), equalTo(1));
        assertThat(receivedFulfillmentGroupDto.getItems().get(0).trim(), equalTo(responseAddItemEntity.getHeaders().getLocation().toASCIIString()));
    }


    @Test
    public void shouldAddProperAddressToItemsFulfillmentTest() throws URISyntaxException {
         // when: creating a new order, inserting an item to it and setting an address for its fulfillment group
        final Pair<RestTemplate, String> user = generateAnonymousUser();
        final RestTemplate clientRestTemplate = user.getKey();
        final String clientAccessToken = user.getValue();
        final int clientOrderId = createNewOrder(clientAccessToken);

        final String clientOrderUrl  = ApiTestUrls.ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + clientOrderId;
        final ResponseEntity<?> responseAddItemEntity = addItemToOrder(1, 1, clientOrderUrl + "/items", clientAccessToken, restTemplate);
        assertThat(responseAddItemEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final String addedItemUrl = responseAddItemEntity.getHeaders().getLocation().toASCIIString();

        em.clear();

        final AddressDto addressDto = AddressDto.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .city("Wroclaw")
                .postalCode("02-945")
                .addressLine1("Zakopanska 40")
                .countryCode("PL")
                .build();

        final FulfillmentGroupDto fulfillmentGroupDto = FulfillmentGroupDto.builder()
                .items(Collections.singletonList(addedItemUrl))
                .address(addressDto)
                .build();

        final HttpHeaders clientRequestHeaders = new HttpHeaders();
        clientRequestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        clientRequestHeaders.setContentType(MediaType.APPLICATION_JSON);
        clientRequestHeaders.set("Authorization", "Bearer " + clientAccessToken);

        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

        final ResponseEntity<Resources<FulfillmentGroupDto>> receivedFulfillmentGroupEntity =
                clientRestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<Resources<FulfillmentGroupDto>>() {},
                        serverPort,
                        clientOrderId
                );

        assertThat(receivedFulfillmentGroupEntity.getStatusCode(), equalTo(HttpStatus.OK));

        final String fulfillmentGroupUrl = receivedFulfillmentGroupEntity.getBody().getContent().iterator().next().getLink("self").getHref();

        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingJackson2HttpMessageConverter()));

        final ResponseEntity<Void> responseUpdateFulfillmentGroupEntity =
                clientRestTemplate.exchange(
                        fulfillmentGroupUrl,
                        HttpMethod.PUT,
                        new HttpEntity<>(fulfillmentGroupDto, clientRequestHeaders),
                        Void.class
                );

        assertThat(responseUpdateFulfillmentGroupEntity.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));

        // then: address should get changed and its new values should be properly set
        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

        final ResponseEntity<Resources<FulfillmentGroupDto>> receivedFulfillmentGroupAfterUpdateEntity =
                clientRestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<Resources<FulfillmentGroupDto>>() {},
                        serverPort,
                        clientOrderId
                );


        assertThat(receivedFulfillmentGroupAfterUpdateEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedFulfillmentGroupAfterUpdateEntity.getBody().getContent().size(), equalTo(1));

        final FulfillmentGroupDto updatedFulfillmentGroupDto = receivedFulfillmentGroupAfterUpdateEntity.getBody().getContent().iterator().next();

        assertThat(updatedFulfillmentGroupDto, equalTo(fulfillmentGroupDto));
    }

    @Test
    public void shouldPlaceAllOrderItemsInADefaultFulfillmentGroupTest() throws URISyntaxException {
        // when: creating a new order and adding 2 different items to it
        final Pair<RestTemplate, String> user = generateAnonymousUser();
        final RestTemplate clientRestTemplate = user.getKey();
        final String clientAccessToken = user.getValue();
        final int clientOrderId = createNewOrder(clientAccessToken);

        final String clientOrderUrl  = ApiTestUrls.ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + clientOrderId;
        final ResponseEntity<?> responseAddItem1Entity = addItemToOrder(1, 1, clientOrderUrl + "/items", clientAccessToken, restTemplate);
        assertThat(responseAddItem1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final String addedOrderItem1Url = responseAddItem1Entity.getHeaders().getLocation().toASCIIString();

        final ResponseEntity<?> responseAddItem2Entity = addItemToOrder(2, 2, clientOrderUrl + "/items", clientAccessToken, restTemplate);
        assertThat(responseAddItem2Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final String addedOrderItem2Url = responseAddItem2Entity.getHeaders().getLocation().toASCIIString();

        // then: both items should belong to the same fulfillment group
        final HttpHeaders clientRequestHeaders = new HttpHeaders();
        clientRequestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        clientRequestHeaders.setContentType(MediaType.APPLICATION_JSON);
        clientRequestHeaders.set("Authorization", "Bearer " + clientAccessToken);

        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

        final ResponseEntity<Resources<FulfillmentGroupDto>> receivedFulfillmentGroupEntity =
                clientRestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<Resources<FulfillmentGroupDto>>() {},
                        serverPort,
                        clientOrderId
                );

        assertThat(receivedFulfillmentGroupEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedFulfillmentGroupEntity.getBody().getContent().size(), equalTo(1));

        final FulfillmentGroupDto receivedFulfillmentGroupDto = receivedFulfillmentGroupEntity.getBody().getContent().iterator().next();

        assertThat(receivedFulfillmentGroupDto.getItems().size(), equalTo(2));
        assertTrue(receivedFulfillmentGroupDto.getItems().contains(addedOrderItem1Url));
        assertTrue(receivedFulfillmentGroupDto.getItems().contains(addedOrderItem2Url));

    }


    @Test
    public void shouldHaveTwoDifferentFulfillmentGroupsForTwoDifferentOrderItemsTest() throws URISyntaxException {
        // when: 1) creating a new order and adding 2 different items to it
        final Pair<RestTemplate, String> user = generateAnonymousUser();
        final RestTemplate clientRestTemplate = user.getKey();
        final String clientAccessToken = user.getValue();

        final HttpHeaders clientRequestHeaders = new HttpHeaders();
        clientRequestHeaders.set("Accept", MediaTypes.HAL_JSON_VALUE);
        clientRequestHeaders.setContentType(MediaType.APPLICATION_JSON);
        clientRequestHeaders.set("Authorization", "Bearer " + clientAccessToken);

        final int clientOrderId = createNewOrder(clientAccessToken);

        final String clientOrderUrl  = ApiTestUrls.ORDERS_URL.replaceFirst("\\{port\\}", serverPort) + "/" + clientOrderId;
        final ResponseEntity<?> responseAddItem1Entity = addItemToOrder(1, 1, clientOrderUrl + "/items", clientAccessToken, restTemplate);
        assertThat(responseAddItem1Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final String addedOrderItem1Url = responseAddItem1Entity.getHeaders().getLocation().toASCIIString();

        final ResponseEntity<?> responseAddItem2Entity = addItemToOrder(2, 2, clientOrderUrl + "/items", clientAccessToken, restTemplate);
        assertThat(responseAddItem2Entity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final String addedOrderItem2Url = responseAddItem2Entity.getHeaders().getLocation().toASCIIString();

        // then: 1) there should only exist ONE fulfillment group with both items added to it
        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

        final ResponseEntity<Resources<FulfillmentGroupDto>> receivedOriginalFulfillmentGroupsEntity =
                clientRestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<Resources<FulfillmentGroupDto>>() {},
                        serverPort,
                        clientOrderId
                );

        assertThat(receivedOriginalFulfillmentGroupsEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedOriginalFulfillmentGroupsEntity.getBody().getContent().size(), equalTo(1));

        final FulfillmentGroupDto receivedOriginalFulfillmentGroupDto = receivedOriginalFulfillmentGroupsEntity.getBody().getContent().iterator().next();

        final String firstFulfillmentGroupUrl = receivedOriginalFulfillmentGroupDto.getLink("self").getHref();

        assertThat(receivedOriginalFulfillmentGroupDto.getItems().size(), equalTo(2));
        assertTrue(receivedOriginalFulfillmentGroupDto.getItems().contains(addedOrderItem1Url));
        assertTrue(receivedOriginalFulfillmentGroupDto.getItems().contains(addedOrderItem2Url));

        // when: 2) creating 2nd fulfillment group with 1st item in it
        final AddressDto addressDto = AddressDto.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .city("Wroclaw")
                .postalCode("02-945")
                .addressLine1("Zakopanska 40")
                .countryCode("PL")
                .build();

        final FulfillmentGroupDto fulfillmentGroupDto = FulfillmentGroupDto.builder()
                .items(Collections.singletonList(addedOrderItem1Url))
                .address(addressDto)
                .build();

        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingJackson2HttpMessageConverter()));

        final ResponseEntity<Void> responseNewFulfillmentGroupEntity =
                clientRestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.POST,
                        new HttpEntity<>(fulfillmentGroupDto, clientRequestHeaders),
                        Void.class,
                        serverPort,
                        clientOrderId
                );

        assertThat(responseNewFulfillmentGroupEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        final String createdFulfillmentGroupUrl = responseNewFulfillmentGroupEntity.getHeaders().getLocation().toASCIIString();


        // then: 2) both items should be placed in two different fulfillment groups
        clientRestTemplate.setMessageConverters(Lists.newArrayList(new MappingHalJackson2HttpMessageConverter()));

        // then: 2.a) there should be two fulfillment groups associated with a created order
        final ResponseEntity<Resources<FulfillmentGroupDto>> receivedOriginalFulfillmentGroupEntity =
                clientRestTemplate.exchange(
                        ApiTestUrls.ORDER_BY_ID_FULFILLMENTS_URL,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<Resources<FulfillmentGroupDto>>() {},
                        serverPort,
                        clientOrderId
                );

        assertThat(receivedOriginalFulfillmentGroupEntity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedOriginalFulfillmentGroupEntity.getBody().getContent().size(), equalTo(2));

        final Iterator<FulfillmentGroupDto> it = receivedOriginalFulfillmentGroupEntity.getBody().getContent().iterator();

        final FulfillmentGroupDto fulfillmentGroup1Dto = it.next();
        final FulfillmentGroupDto fulfillmentGroup2Dto = it.next();

        // both fulfillment groups hold only 1 order item reference
        assertThat(fulfillmentGroup1Dto.getItems().size(), equalTo(1));
        assertThat(fulfillmentGroup2Dto.getItems().size(), equalTo(1));

        if(fulfillmentGroup1Dto.getAddress() != null && fulfillmentGroup1Dto.getAddress().equals(addressDto)) {
            // we found the 2nd fulfillment group & it should hold a reference to 1st order item
            assertTrue(fulfillmentGroup1Dto.getItems().get(0).equals(addedOrderItem1Url));
            assertTrue(fulfillmentGroup2Dto.getItems().get(0).equals(addedOrderItem2Url));
        } else {
            assertTrue(fulfillmentGroup2Dto.getItems().get(0).equals(addedOrderItem1Url));
            assertTrue(fulfillmentGroup1Dto.getItems().get(0).equals(addedOrderItem2Url));
        }

        // then: 2.b) refering to both fulfillment groups individually, returns "the same" results

        final ResponseEntity<FulfillmentGroupDto> receivedNewFulfillmentGroup1Entity =
                clientRestTemplate.exchange(
                        createdFulfillmentGroupUrl,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<FulfillmentGroupDto>() {}
                );

        assertThat(receivedNewFulfillmentGroup1Entity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedNewFulfillmentGroup1Entity.getBody().getAddress(), equalTo(addressDto));
        assertThat(receivedNewFulfillmentGroup1Entity.getBody().getItems().size(), equalTo(1));
        assertThat(receivedNewFulfillmentGroup1Entity.getBody().getItems().get(0), equalTo(addedOrderItem1Url));


        final ResponseEntity<FulfillmentGroupDto> receivedNewFulfillmentGroup2Entity =
                clientRestTemplate.exchange(
                        firstFulfillmentGroupUrl,
                        HttpMethod.GET,
                        new HttpEntity<>(clientRequestHeaders),
                        new ParameterizedTypeReference<FulfillmentGroupDto>() {}
                );

        assertThat(receivedNewFulfillmentGroup2Entity.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(receivedNewFulfillmentGroup2Entity.getBody().getItems().size(), equalTo(1));
        assertThat(receivedNewFulfillmentGroup2Entity.getBody().getItems().get(0), equalTo(addedOrderItem2Url));
    }
}
