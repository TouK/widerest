package pl.touk.widerest.cart;

import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.Application;
import pl.touk.widerest.base.ApiTestBase;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by mst on 17.07.15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class CustomerControllerTest extends ApiTestBase {

    @Test
    public void shouldReturnTwoDifferentTokensForAnonUserAndAdminTest() throws URISyntaxException {

        // Get anonymous token
        URI orderDtoResponseUri = restTemplate.postForLocation(ApiTestBase.OAUTH_AUTHORIZATION, null, serverPort);
        String accessAnonymousToken = getAccessTokenFromLocationUrl(orderDtoResponseUri.toString());

        // Get admin token
        String accessAdminToken = oAuth2AdminRestTemplate().getAccessToken().getValue();
        //then
        assertThat(accessAdminToken, not(equalTo(accessAnonymousToken)));
    }
}

