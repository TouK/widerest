package pl.touk.widerest;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import lombok.extern.slf4j.Slf4j;
import pl.touk.widerest.base.ApiTestBase;
import static pl.touk.widerest.base.ApiTestUrls.ORDERS_COUNT;
import static pl.touk.widerest.base.ApiTestUrls.CUSTOMERS_URL;
import pl.touk.widerest.security.oauth2.Scope;

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
        final Long responseCount =  oAuth2RestTemplate.getForObject(ORDERS_COUNT, Long.class, serverPort);

        //then
        assertThat(responseCount, equalTo(1L));
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
