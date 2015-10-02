package pl.touk.widerest;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultRequestEnhancer;
import org.springframework.security.oauth2.client.token.OAuth2AccessTokenSupport;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.multitenancy.TenantHeaderRequestFilter;
import pl.touk.widerest.multitenancy.TenantRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringApplicationConfiguration(classes = Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class MultiTenantAuthTest extends ApiTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldNotMixAuthorizationsBetweenTenants() {

        // given two tenants
        String tenantToken1 = restTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build(), String.class, serverPort);
        String tenantToken2 = restTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword("test").adminEmail("test@test.xx").build(), String.class, serverPort);


        // when authorization made for the first tenant
        List<String> scopes = new ArrayList<>();
        scopes.add("site");
        scopes.add("backoffice");

        ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        resourceDetails.setGrantType("password");
        resourceDetails.setAccessTokenUri("http://localhost:" + serverPort + "/oauth/token");
        resourceDetails.setClientId("test");
        resourceDetails.setScope(scopes);

        resourceDetails.setUsername("backoffice/admin");
        resourceDetails.setPassword("test");

        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);

        DefaultRequestEnhancer requestEnhancer = new DefaultRequestEnhancer() {
            @Override
            public void enhance(AccessTokenRequest request, OAuth2ProtectedResourceDetails resource, MultiValueMap<String, String> form, HttpHeaders headers) {
                headers.put(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, Lists.newArrayList(tenantToken1));
                super.enhance(request, resource, form, headers);
            }
        };
        oAuth2RestTemplate.setAccessTokenProvider(
                new AccessTokenProviderChain(
                        Arrays.<AccessTokenProvider>asList(
                                new AuthorizationCodeAccessTokenProvider(), new ImplicitAccessTokenProvider(),
                                new ResourceOwnerPasswordAccessTokenProvider(), new ClientCredentialsAccessTokenProvider()
                        ).stream()
                                .map(OAuth2AccessTokenSupport.class::cast)
                                .peek(i -> i.setTokenRequestEnhancer(requestEnhancer))
                                .map(AccessTokenProvider.class::cast)
                                .collect(Collectors.toList())
                ));


        // then client error thrown
        thrown.expect(AccessTokenRequiredException.class);

        // when the second tenant's resource called
        HttpHeaders headers = new HttpHeaders();
        headers.add(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, tenantToken2);
        ResponseEntity<List> responseEntity = oAuth2RestTemplate.exchange("http://localhost:{serverPort}/orders", HttpMethod.GET, new HttpEntity<>(headers), List.class, serverPort);

    }

}
