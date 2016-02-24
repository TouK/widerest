package pl.touk.widerest.cart;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
}
