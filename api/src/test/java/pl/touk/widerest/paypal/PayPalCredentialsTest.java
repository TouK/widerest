package pl.touk.widerest.paypal.endpoint;

import javafx.util.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.cart.dto.AddressDto;
import pl.touk.widerest.base.ApiTestBase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class PayPalCredentialsTest extends ApiTestBase {

    @Test
    public void shouldSetCredentialsBeSameWithReturned() throws URISyntaxException {
        // Given admin user
        Pair<OAuth2RestTemplate, String> admin = generateAdminUser();
        OAuth2RestTemplate restTemplate = admin.getKey();
        String accessToken = restTemplate.getAccessToken().getValue();
        HttpEntity entity = getHttpJsonRequestEntity();

        // When getting token id and secret
        // Then status code should be 5xx
        try {
            restTemplate.exchange(PAYPAL_CREDENTIALS_TOKEN_URL, HttpMethod.GET,
                    entity, String.class, serverPort);
            fail("Error code should be returned");
        } catch(HttpStatusCodeException e) {
            assert(e.getStatusCode().is5xxServerError());
        }

        try {
            restTemplate.exchange(PAYPAL_CREDENTIALS_SECRET_URL, HttpMethod.GET,
                    entity, String.class, serverPort);
            fail("Error code should be returned");
        } catch(HttpStatusCodeException e) {
            assert(e.getStatusCode().is5xxServerError());
        }

        // When setting token id and secret
        String tokenId = "sdfsar3456df";
        String secret = "6dgadgnsd8f";

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + accessToken);
        requestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        entity = new HttpEntity(tokenId, requestHeaders);
        restTemplate.exchange(PAYPAL_CREDENTIALS_TOKEN_URL, HttpMethod.POST,
                entity, String.class, serverPort);

        entity = new HttpEntity(secret, requestHeaders);
        restTemplate.exchange(PAYPAL_CREDENTIALS_SECRET_URL, HttpMethod.POST,
                entity, String.class, serverPort);

        // When getting credentials
        // Then the same credentials should be returned
        assert(restTemplate.exchange(PAYPAL_CREDENTIALS_TOKEN_URL, HttpMethod.GET,
                getHttpJsonRequestEntity(), String.class, serverPort).getBody()
                .equals(tokenId));
        assert(restTemplate.exchange(PAYPAL_CREDENTIALS_SECRET_URL, HttpMethod.GET,
                getHttpJsonRequestEntity(), String.class, serverPort).getBody()
                .equals(secret));
    }
}
