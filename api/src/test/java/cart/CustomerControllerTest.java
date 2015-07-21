package cart;

import base.ApiTestBase;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by mst on 17.07.15.
 */
public class CustomerControllerTest extends ApiTestBase {

    @Test
    public void Test2() throws URISyntaxException {

        // Get anonymous token
        URI orderDtoResponseUri = restTemplate.postForLocation(ApiTestBase.OAUTH_AUTHORIZATION, null);

        assertNotNull(orderDtoResponseUri);

        String authorizationAnonymousUrl = orderDtoResponseUri.toString().replaceFirst("#", "?");
        List<NameValuePair> authorizationParams = URLEncodedUtils.parse(new URI(authorizationAnonymousUrl), "UTF-8");

        String accessAnonymousToken = authorizationParams.stream()
                .filter(x -> x.getName().equals("access_token")).collect(Collectors.toList()).get(0).getValue();

        System.out.println("Token length: " + accessAnonymousToken.length());
        System.out.println(accessAnonymousToken);

        // Get admin

        OAuth2RestTemplate adminRestTemplate = oAuth2AdminRestTemplate();

        if(adminRestTemplate == null) {
            System.out.println("Admin template is null: ");
        } else {
            System.out.println("Admin template is NOT NUl;l: " + adminRestTemplate.getAccessToken().getValue());

        }
        /*

        MultiValueMap<String, String> loginDetails = new LinkedMultiValueMap<>();
        loginDetails.add("usertype", "backoffice");
        loginDetails.add("username", "admin");
        loginDetails.add("password", "admin");


        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Content-Type", "application/x-www-form-urlencoded");

        //HttpEntity<String> requestHeadersEntity = new HttpEntity<>(requestHeaders);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(loginDetails, requestHeaders);

        URI adminUri = restTemplate.postForLocation(LOGIN_URL, request);

        assertNotNull(adminUri);

        System.out.println(adminUri);
        String authorizationLoggedUrl = adminUri.toString().replaceFirst("#", "?");
        List<NameValuePair> authorizationLoggedParams = URLEncodedUtils.parse(new URI(authorizationLoggedUrl), "UTF-8");

        System.out.println(authorizationLoggedParams.size());

        String accessLoggedToken = authorizationLoggedParams.stream()
                //.filter(x -> x.getName().equals("access_token"))
                .collect(Collectors.toList()).get(0).getValue();

        System.out.println(adminUri);

    */



    }
}
