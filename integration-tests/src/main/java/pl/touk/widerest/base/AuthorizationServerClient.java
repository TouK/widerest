package pl.touk.widerest.base;

import com.google.common.collect.Lists;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.client.HttpMessageConverterExtractor;
import pl.touk.widerest.security.oauth2.OutOfBandUriHandler;
import pl.touk.widerest.security.oauth2.Scope;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class AuthorizationServerClient {

    protected final BasicCookieStore cookieStore = new BasicCookieStore();
    protected final CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).disableRedirectHandling().build();

    protected final String serverPort;

    public AuthorizationServerClient(String serverPort) {
        this.serverPort = serverPort;
    }

    public void logIn(final String usertype, final String username, final String password) throws IOException {
        final HttpUriRequest request = RequestBuilder
                .post()
                .setUri("http://localhost:" + serverPort + "/login")
                .addParameter("usertype", usertype)
                .addParameter("username", username)
                .addParameter("password", password)
                .build();
        try (CloseableHttpResponse response = httpClient.execute(request)) {}
    }


    public OAuth2RestTemplate requestAuthorization (final Scope scope) throws IOException {
        final HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        final ClientHttpRequest request;
        request = httpRequestFactory.createRequest(
                URI.create("http://localhost:" + serverPort + "/oauth/authorize?client_id=default&response_type=token&redirect_uri=" + OutOfBandUriHandler.OOB_URI + (scope != null ? "&scope=" + scope : "")),
                HttpMethod.GET
        );

        final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2HalRestTemplate(new BaseOAuth2ProtectedResourceDetails());

        try (ClientHttpResponse response = request.execute()) {
            final HttpMessageConverterExtractor<Map> e = new HttpMessageConverterExtractor(Map.class, Arrays.asList(new MappingJackson2HttpMessageConverter()));
            final Map<String, String> map = e.extractData(response);
            final Optional<String> accessToken = Optional.ofNullable(map.get("access_token"));
            oAuth2RestTemplate.getOAuth2ClientContext().setAccessToken(accessToken.map(DefaultOAuth2AccessToken::new).orElse(null));
            return oAuth2RestTemplate;
        }

    }

    private static class OAuth2HalRestTemplate extends OAuth2RestTemplate {

        public OAuth2HalRestTemplate(OAuth2ProtectedResourceDetails resource) {
            super(resource);
            setMessageConverters(Lists.newArrayList(
                    new AllEncompassingFormHttpMessageConverter(),
                    new MappingHalJackson2HttpMessageConverter(),
                    new MappingJackson2HttpMessageConverter()
            ));
        }
    }

}
