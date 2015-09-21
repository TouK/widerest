package pl.touk.widerest.auth0;

import com.auth0.spring.security.auth0.Auth0JWTToken;
import com.auth0.spring.security.auth0.Auth0UserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.multitenancy.TenantTokenStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by mst on 16.09.15.
 */
@Slf4j
@Service
public class Auth0TenantTokenStore implements TenantTokenStore {

    @Value("${auth0.domain}")
    private String appDomain;

    private static final String USERS_ENDPOINT = "/api/v2/users";
    private static final String USER_BY_ID_ENDPOINT = USERS_ENDPOINT + "/{id}";

    private final RestTemplate restTemplate = new RestTemplate();


    @Override
    public void addTenantToken(String tenantToken, Authentication authentication) {

        final List<String> currentTenantTokens = getTenantTokens(authentication);

        final List<String> updatedTenantTokens = new ArrayList<>();
        updatedTenantTokens.addAll(currentTenantTokens);

        updatedTenantTokens.add(tenantToken);

        final UserTenantsMetadataDto userTenantsMetadataDto = new UserTenantsMetadataDto();
        userTenantsMetadataDto.setTenants(updatedTenantTokens);

        final Auth0UserDto updatedTenantTokensDto = Auth0UserDto.builder()
                .userTenantsMetadataDto(userTenantsMetadataDto)
                .build();

        final RestTemplate patchRestTemplate = new RestTemplate();
        patchRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + ((Auth0JWTToken) authentication).getJwt());
        requestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        final HttpEntity<Auth0UserDto> requestEntity = new HttpEntity<>(updatedTenantTokensDto, requestHeaders);

        patchRestTemplate.exchange(
                "https://" + appDomain + USER_BY_ID_ENDPOINT,
                HttpMethod.PATCH,
                requestEntity,
                Void.class,
                (String) ((Auth0UserDetails) authentication.getPrincipal()).getAuth0Attribute("sub")
        );

    }

    @Override
    public List<String> getTenantTokens(Authentication authentication) {

        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", "Bearer " + ((Auth0JWTToken)authentication).getJwt());
        requestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        final HttpEntity httpRequestEntity = new HttpEntity(requestHeaders);

        final ResponseEntity<Auth0UserDto> responseEntity = restTemplate.exchange(
                "https://" + appDomain + USER_BY_ID_ENDPOINT,
                HttpMethod.GET,
                httpRequestEntity,
                Auth0UserDto.class,
                (String) ((Auth0UserDetails) authentication.getPrincipal()).getAuth0Attribute("sub")
        );

        final Auth0UserDto auth0UserDto = responseEntity.getBody();

        if(auth0UserDto.getUserTenantsMetadataDto() != null && auth0UserDto.getUserTenantsMetadataDto().getTenants() != null) {
            return auth0UserDto.getUserTenantsMetadataDto().getTenants();
        } else {
            return Collections.emptyList();
        }
    }
}
