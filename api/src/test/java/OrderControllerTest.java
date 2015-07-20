
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.cart.dto.DiscreteOrderItemDto;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.catalog.dto.CategoryDto;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by mst on 09.07.15.
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = Application.class)
public class OrderControllerTest extends ApiTestBase {


    @Test
    public void Test1() throws URISyntaxException {
        ;

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

    @Test
    public void Test2() throws URISyntaxException {

        // Get anonymous token
        URI orderDtoResponseUri = restTemplate.postForLocation(OAUTH_AUTHORIZATION, null);

        assertNotNull(orderDtoResponseUri);

        String authorizationAnonymousUrl = orderDtoResponseUri.toString().replaceFirst("#", "?");
        List<NameValuePair> authorizationParams = URLEncodedUtils.parse(new URI(authorizationAnonymousUrl), "UTF-8");

        String accessAnonymousToken = authorizationParams.stream()
                .filter(x -> x.getName().equals("access_token")).collect(Collectors.toList()).get(0).getValue();

        System.out.println("Token length: " + accessAnonymousToken.length());
        System.out.println(accessAnonymousToken);

        // Get logged token

        Map<String, String> loginDetails = new HashMap<>();
        loginDetails.put("username", "admin");
        loginDetails.put("password", "admin");

        URI adminUri = restTemplate.postForLocation(LOGIN_URL, loginDetails);


        assertNotNull(adminUri);

        String authorizationLoggedUrl = adminUri.toString().replaceFirst("#", "?");
        List<NameValuePair> authorizationLoggedParams = URLEncodedUtils.parse(new URI(authorizationLoggedUrl), "UTF-8");

        System.out.println(authorizationLoggedParams.size());

        String accessLoggedToken = authorizationLoggedParams.stream()
                //.filter(x -> x.getName().equals("access_token"))
                .collect(Collectors.toList()).get(0).getValue();

        System.out.println(adminUri);


        //System.out.println("Token length: " + accessLoggedToken.length());
        //System.out.println(accessLoggedToken);

        //System.out.println("Are tokens the same?: " + accessAnonymousToken.equals(accessLoggedToken));

    }
}