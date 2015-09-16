package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.multitenancy.dto.AuthUser;

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


    public void addUserTenantToken(String tenantToken, String userId) {

        final ResponseEntity<AuthUser> responseEntity = restTemplate.getForEntity("http://" + appDomain + USER_BY_ID_ENDPOINT,
                AuthUser.class, userId);

        final AuthUser authUser = responseEntity.getBody();

        authUser.getTokens().add(tenantToken);

        final RestTemplate patchRestTemplate = new RestTemplate();
        patchRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        final HttpEntity<AuthUser> requestEntity = new HttpEntity<>(authUser);

        patchRestTemplate.exchange("http://" + appDomain + USER_BY_ID_ENDPOINT, HttpMethod.PATCH, requestEntity, Void.class,  userId);

    }

    public List<String> getUserTenantTokens(String userId) {

        final ResponseEntity<AuthUser> responseEntity = restTemplate.getForEntity("http://" + appDomain + USER_BY_ID_ENDPOINT,
                AuthUser.class, userId);

        return responseEntity.getBody().getTokens();
    }
}
