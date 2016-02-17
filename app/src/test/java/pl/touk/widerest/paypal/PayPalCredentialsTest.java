package pl.touk.widerest.paypal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.Application;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.paypal.gateway.PayPalSession;

import java.net.URISyntaxException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class, initializers = Application.ContextInitializer.class, inheritInitializers = false)
public class PayPalCredentialsTest extends ApiTestBase {

    public static final String PAYPAL_CREDENTIALS_ID_URL = SYSTEM_PROPERTIES_URL + "/" + PayPalSession.CLIENT_ID;
    public static final String PAYPAL_CREDENTIALS_SECRET_URL = SYSTEM_PROPERTIES_URL + "/" + PayPalSession.SECRET;

    @Test
    public void shouldSetCredentialsBeSameWithReturned() throws URISyntaxException {

        RestTemplate restTemplate = oAuth2AdminRestTemplate();

        // Given not yet set PayPal credentials
        // When trying to get them
        // Then status code should be 204 NO CONTENT
        ResponseEntity<String> response;

        response = restTemplate.getForEntity(PAYPAL_CREDENTIALS_ID_URL, String.class, serverPort);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));

        response = restTemplate.getForEntity(PAYPAL_CREDENTIALS_SECRET_URL, String.class, serverPort);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));

        // When setting token id and secret
        String paypalClientId = "sdfsar3456df";
        String paypalSecret = "6dgadgnsd8f";

        response = restTemplate.exchange(PAYPAL_CREDENTIALS_ID_URL, HttpMethod.PUT,
                new HttpEntity(paypalClientId), String.class, serverPort);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        response = restTemplate.exchange(PAYPAL_CREDENTIALS_SECRET_URL, HttpMethod.PUT,
                new HttpEntity(paypalSecret), String.class, serverPort);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        // When getting credentials
        String storedPaypalClientId = restTemplate.getForObject(PAYPAL_CREDENTIALS_ID_URL, String.class, serverPort);
        String storedPaypalSecret = restTemplate.getForObject(PAYPAL_CREDENTIALS_SECRET_URL, String.class, serverPort);

        // Then the same credentials should be returned
        assertThat(storedPaypalClientId, equalTo(paypalClientId));
        assertThat(storedPaypalSecret, equalTo(paypalSecret));
    }
}
