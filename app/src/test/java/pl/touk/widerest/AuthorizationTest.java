package pl.touk.widerest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.security.oauth2.Scope;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@SpringApplicationConfiguration(classes = Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class AuthorizationTest extends ApiTestBase {

    OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(new BaseOAuth2ProtectedResourceDetails());

    BasicCookieStore cookieStore = new BasicCookieStore();
    CloseableHttpClient authorizationServerClient = HttpClients.custom().setDefaultCookieStore(cookieStore).disableRedirectHandling().build();

    @Before
    public void clearSession() {
        cookieStore.clear();
    }

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
    public void shouldAuthorizeAdmin() throws IOException {
        whenLoggedIn("backoffice", "admin", "admin");
        whenAuthorizationRequestedFor(Scope.STAFF);
        thenAuthorized();
    }

    protected void whenRegistrationPerformed(String username, String password, String email) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("email", email);
        map.add("username", username);
        map.add("password", password);
        map.add("passwordConfirm", password);
        HttpEntity requestEntity = new HttpEntity(map, new HttpHeaders());
        restTemplate.postForEntity(API_BASE_URL + "/customers/register", requestEntity, HttpHeaders.class, serverPort);
    }

    protected void whenLoggedIn(String usertype, String username, String password) throws IOException {
        HttpUriRequest request = RequestBuilder
                .post()
                .setUri("http://localhost:" + serverPort + "/login")
                .addParameter("usertype", usertype)
                .addParameter("username", username)
                .addParameter("password", password)
                .build();
        try (CloseableHttpResponse response = authorizationServerClient.execute(request)) {
        }
    }

    protected void whenAuthorizationRequestedFor(Scope scope) throws IOException {
        HttpUriRequest request = RequestBuilder.get()
                .setUri("http://localhost:" + serverPort + "/oauth/authorize?client_id=default&response_type=token&redirect_uri=%2F" + (scope != null ? "&scope=" + scope : ""))
                .build();

        try (CloseableHttpResponse response = authorizationServerClient.execute(request)) {
            org.apache.http.HttpEntity entity = response.getEntity();
            Optional<String> accessToken = Optional.ofNullable(response.getFirstHeader("Location"))
                    .flatMap(header -> URLEncodedUtils.parse(URI.create(header.getValue().replace('#', '?')), "UTF-8").stream().filter(param -> param.getName().equals("access_token")).findAny())
                    .map(NameValuePair::getValue);
            restTemplate.getOAuth2ClientContext().setAccessToken(accessToken.map(DefaultOAuth2AccessToken::new).orElse(null));
        }
    }

    protected void thenAuthorized(boolean value) {
        Assert.assertEquals(
                value,
                restTemplate.getOAuth2ClientContext().getAccessToken() != null
        );
    }
    protected void thenAuthorized() {
        thenAuthorized(true);
    }

    protected void thenNotAuthorized() {
        thenAuthorized(false);
    }
}
