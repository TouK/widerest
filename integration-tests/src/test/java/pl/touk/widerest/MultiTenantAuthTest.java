package pl.touk.widerest;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
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
import org.springframework.security.oauth2.common.exceptions.UserDeniedAuthorizationException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;
import pl.touk.multitenancy.TenantHeaderRequestFilter;
import pl.touk.multitenancy.TenantRequest;
import pl.touk.widerest.api.cart.orders.dto.OrderDto;
import pl.touk.widerest.base.ApiTestBase;

import java.util.Arrays;
import java.util.stream.Collectors;

import static pl.touk.widerest.base.ApiTestUrls.ORDERS_URL;

@SpringApplicationConfiguration(classes = Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class MultiTenantAuthTest extends ApiTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldNotMixAuthorizationsBetweenTenants() {

        String tenant1Password = "password1";
        String tenant2Password = "password2";

        // given two tenants
        String tenant1Identifier = restTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword(tenant1Password).adminEmail("admin@tenant1.com").build(), String.class, serverPort);
        String tenant2Identifier = restTemplate.postForObject("http://localhost:{serverPort}/tenant", TenantRequest.builder().adminPassword(tenant2Password).adminEmail("admin@tenant2.com").build(), String.class, serverPort);

        // when authorization made for the first tenant
        ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
        resourceDetails.setGrantType("password");
        resourceDetails.setAccessTokenUri("http://localhost:" + serverPort + "/oauth/token");
        resourceDetails.setClientId(tenant1Identifier);
        resourceDetails.setClientSecret("");
        resourceDetails.setScope(Arrays.asList("staff"));
        resourceDetails.setUsername("backoffice/admin");
        resourceDetails.setPassword(tenant1Password);

        OAuth2RestTemplate tenant1RestTemplate = new OAuth2RestTemplate(resourceDetails);

        DefaultRequestEnhancer requestEnhancer = new DefaultRequestEnhancer() {
            @Override
            public void enhance(AccessTokenRequest request, OAuth2ProtectedResourceDetails resource, MultiValueMap<String, String> form, HttpHeaders headers) {
                headers.put(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, Arrays.asList(tenant1Identifier));
                super.enhance(request, resource, form, headers);
            }
        };
        tenant1RestTemplate.setAccessTokenProvider(
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

        //ResponseEntity<List> responseEntity;
        HttpHeaders headers = new HttpHeaders();

        // when the first tenant's resource called
        headers.set(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, tenant1Identifier);

        final ResponseEntity<Resources<OrderDto>> responseEntity =
                tenant1RestTemplate.exchange(ORDERS_URL, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<Resources<OrderDto>>() {}, serverPort);

//        responseEntity = tenant1RestTemplate.exchange(API_BASE_URL + "/orders", HttpMethod.GET, new HttpEntity<>(headers), List.class, serverPort);

        // then it is ok
        Assert.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());

        // but it will not be ok
        thrown.expect(UserDeniedAuthorizationException.class);

        // when the second tenant's resource called
        headers.set(TenantHeaderRequestFilter.TENANT_TOKEN_HEADER, tenant2Identifier);
//        responseEntity = tenant1RestTemplate.exchange(API_BASE_URL + "/orders", HttpMethod.GET, new HttpEntity<>(headers), List.class, serverPort);

        final ResponseEntity<Resources<OrderDto>> responseEntity2 =
                tenant1RestTemplate.exchange(ORDERS_URL, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<Resources<OrderDto>>() {}, serverPort);

    }

}
