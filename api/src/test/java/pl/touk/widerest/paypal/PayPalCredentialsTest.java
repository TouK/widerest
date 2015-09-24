package pl.touk.widerest.paypal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.Application;
import pl.touk.widerest.base.ApiTestBase;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class PayPalCredentialsTest extends ApiTestBase {

    @Test
    public void shouldSetCredentialsBeSameWithReturned() throws URISyntaxException {

        RestTemplate restTemplate = oAuth2AdminRestTemplate();

        // Given not yet set PayPal credentials
        // When trying to get them
        // Then status code should be 404
        try {
            restTemplate.getForObject(PAYPAL_CREDENTIALS_ID_URL, String.class, serverPort);
            fail("Error code should be returned");
        } catch(HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

        try {
            restTemplate.getForObject(PAYPAL_CREDENTIALS_SECRET_URL, String.class, serverPort);
            fail("Error code should be returned");
        } catch(HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

        // When setting token id and secret
        String paypalClientId = "sdfsar3456df";
        String paypalSecret = "6dgadgnsd8f";

        restTemplate.postForEntity(PAYPAL_CREDENTIALS_ID_URL,
                paypalClientId, String.class, serverPort);

        restTemplate.postForEntity(PAYPAL_CREDENTIALS_SECRET_URL,
                paypalSecret, String.class, serverPort);


        // When getting credentials
        String storedPaypalClientId = restTemplate.getForObject(PAYPAL_CREDENTIALS_ID_URL, String.class, serverPort);
        String storedPaypalSecret = restTemplate.getForObject(PAYPAL_CREDENTIALS_SECRET_URL, String.class, serverPort);

        // Then the same credentials should be returned
        assertEquals(paypalClientId, storedPaypalClientId);
        assertEquals(paypalSecret, storedPaypalSecret);
    }
}
