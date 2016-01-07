package pl.touk.widerest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpMessageConverterExtractor;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.security.oauth2.OutOfBandUriHandler;
import pl.touk.widerest.security.oauth2.Scope;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@SpringApplicationConfiguration(classes = Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(authorizationServerClient);
        ClientHttpRequest request = httpRequestFactory.createRequest(
                URI.create("http://localhost:" + serverPort + "/oauth/authorize?client_id=default&response_type=token&redirect_uri=" + OutOfBandUriHandler.OOB_URI + (scope != null ? "&scope=" + scope : "")),
                HttpMethod.GET
        );

        try (ClientHttpResponse response = request.execute()) {
            HttpMessageConverterExtractor<Map> e = new HttpMessageConverterExtractor(Map.class, Arrays.asList(new MappingJackson2HttpMessageConverter()));
            Map<String, String> map = e.extractData(response);
            Optional<String> accessToken = Optional.ofNullable(map.get("access_token"));
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
