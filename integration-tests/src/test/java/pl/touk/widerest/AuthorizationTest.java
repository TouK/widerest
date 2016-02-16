package pl.touk.widerest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.Application;
import pl.touk.widerest.security.oauth2.Scope;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringApplicationConfiguration(classes = Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class AuthorizationTest extends ApiTestBase {

    @Test
    public void shouldRequireScopeForAuthoriztion() throws IOException {
        whenAuthorizationRequestedFor(null);
        thenNotAuthorized();
    }

    @Test
    public void shouldAuthorizeNewCustomer() throws IOException {
        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        thenAuthorized();
    }


    @Test
    public void shouldRequireLoginForRegisteredCustomer() throws IOException {
        whenAuthorizationRequestedFor(Scope.CUSTOMER_REGISTERED);
        thenNotAuthorized();
    }

    @Test
    public void shouldAuthorizeRegisteredCustomer() throws IOException {

        whenAuthorizationRequestedFor(Scope.CUSTOMER);
        thenAuthorized();

        String username = RandomStringUtils.random(32, "haskellCurry");
        String password = "uncurry";
        String email = RandomStringUtils.random(32, "haskellCurry") + "@curry.org";

        whenRegistrationPerformed(username, password, email);
        whenLoggedIn("site", username, password);
        whenAuthorizationRequestedFor(Scope.CUSTOMER_REGISTERED);
        thenAuthorized();
    }

    @Test
    public void shouldAuthorizeMergingCarts() throws IOException {
        // given
        whenAuthorizationRequestedFor(Scope.CUSTOMER);

        final String anonymousUserToken = oAuth2RestTemplate.getOAuth2ClientContext().getAccessToken().getValue();
        createNewOrder(anonymousUserToken);

        cookieStore.clear();

        whenAuthorizationRequestedFor(Scope.CUSTOMER);

        final String username = RandomStringUtils.random(32, "haskellCurry");
        final String password = "uncurry";
        final String email = String.format("%s@curry.org", RandomStringUtils.random(32, "haskellCurry"));

        // when
        whenRegistrationPerformed(username, password, email);
        whenLoggedIn("site", username, password);
        whenAuthorizationRequestedFor(Scope.CUSTOMER_REGISTERED);

        final String userToken = oAuth2RestTemplate.getOAuth2ClientContext().getAccessToken().getValue();
        createNewOrder(userToken);

        oAuth2RestTemplate.postForObject(CUSTOMERS_URL + "/merge", anonymousUserToken, String.class, serverPort);
        final String response =  oAuth2RestTemplate.getForObject(ORDERS_COUNT, String.class, serverPort);

        //then

        assertThat(response).isEqualTo("1");
    }

    @Test
    public void shouldAuthorizeAdmin() throws IOException {
        whenLoggedIn("backoffice", "admin", "admin");
        whenAuthorizationRequestedFor(Scope.STAFF);
        thenAuthorized();
    }

    protected void thenAuthorized(boolean value) {
        Assert.assertEquals(
                value,
                oAuth2RestTemplate.getOAuth2ClientContext().getAccessToken() != null
        );
    }
    protected void thenAuthorized() {
        thenAuthorized(true);
    }

    protected void thenNotAuthorized() {
        thenAuthorized(false);
    }
}
