package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by mst on 16.09.15.
 */
@Slf4j
@Service
public class AuthService {

    @Value("${auth0.domain}")
    private String appDomain;

    private static final String USERS_ENDPOINT = "/api/v2/users";
    private static final String USER_BY_ID_ENDPOINT = USERS_ENDPOINT + "/{id}";

    private final RestTemplate restTemplate = new RestTemplate();


    public void addUserTenantToken(String tenantToken, String userId, String token) {

        final List<String> currentTenantTokens = getUserTenantTokens(userId, token);

        final List<String> updatedTenantTokens = new ArrayList<>();
        updatedTenantTokens.addAll(currentTenantTokens);

        updatedTenantTokens.add(tenantToken);

        final UserTenantsMetadataDto userTenantsMetadataDto = new UserTenantsMetadataDto();
        userTenantsMetadataDto.setTenants(updatedTenantTokens);

        final AuthUserDto updatedTenantTokensDto = AuthUserDto.builder()
                .userTenantsMetadataDto(userTenantsMetadataDto)
                .build();

        final RestTemplate patchRestTemplate = new RestTemplate();
        patchRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", token);
        requestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        final HttpEntity<AuthUserDto> requestEntity = new HttpEntity<>(updatedTenantTokensDto, requestHeaders);

        patchRestTemplate.exchange(
                "https://" + appDomain + USER_BY_ID_ENDPOINT,
                HttpMethod.PATCH,
                requestEntity,
                Void.class,
                userId);

    }

    public List<String> getUserTenantTokens(String userId, String token) {

        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.set("Authorization", token);
        requestHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        final HttpEntity httpRequestEntity = new HttpEntity(requestHeaders);

        final ResponseEntity<AuthUserDto> responseEntity = restTemplate.exchange(
                "https://" + appDomain + USER_BY_ID_ENDPOINT,
                HttpMethod.GET,
                httpRequestEntity,
                AuthUserDto.class, userId);

        final AuthUserDto authUserDto = responseEntity.getBody();

        if(authUserDto.getUserTenantsMetadataDto() != null && authUserDto.getUserTenantsMetadataDto().getTenants() != null) {
            return authUserDto.getUserTenantsMetadataDto().getTenants();
        } else {
            return Collections.emptyList();
        }
    }
}
