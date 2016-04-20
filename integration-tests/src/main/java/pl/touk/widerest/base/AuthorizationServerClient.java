package pl.touk.widerest.base;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpMessageConverterExtractor;
import pl.touk.widerest.security.oauth2.Scope;
import pl.touk.widerest.security.oauth2.oob.OobAuthorizationEndpoint;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Component
@org.springframework.context.annotation.Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AuthorizationServerClient {

    protected final BasicCookieStore cookieStore = new BasicCookieStore();
    protected final CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).disableRedirectHandling().build();

    @Value("${local.server.port}")
    protected String serverPort;

    @Resource
    protected OAuth2RestTemplate restTemplate;

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

    @Resource
    protected HttpMessageConverters httpMessageConverters;

    public OAuth2RestTemplate requestAuthorization (final Scope scope) throws IOException {
        final HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        final ClientHttpRequest request;
        request = httpRequestFactory.createRequest(
                URI.create("http://localhost:" + serverPort + "/oauth/authorize?client_id=default&response_type=token&redirect_uri=" + OobAuthorizationEndpoint.OOB_URI + (scope != null ? "&scope=" + scope : "")),
                HttpMethod.GET
        );

        try (ClientHttpResponse response = request.execute()) {

            restTemplate.getOAuth2ClientContext().setAccessToken(null);

            final HttpMessageConverterExtractor<Map> e = new HttpMessageConverterExtractor(Map.class, httpMessageConverters.getConverters());
            Optional.ofNullable((Map<String, String>)e.extractData(response))
                    .map(m -> m.get("access_token"))
                    .map(DefaultOAuth2AccessToken::new)
                    .ifPresent(accessToken -> restTemplate.getOAuth2ClientContext().setAccessToken(accessToken));

            return restTemplate;
        }

    }


    @org.springframework.context.annotation.Configuration
    public static class Configuration {

        @Bean
        @org.springframework.context.annotation.Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public OAuth2RestTemplate oAuth2RestTemplate(HttpMessageConverters httpMessageConverters) {
            OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(new BaseOAuth2ProtectedResourceDetails());
            oAuth2RestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            oAuth2RestTemplate.setMessageConverters(httpMessageConverters.getConverters());
            return oAuth2RestTemplate;
        }


    }

}
