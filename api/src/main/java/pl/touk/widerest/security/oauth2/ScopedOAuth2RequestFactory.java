package pl.touk.widerest.security.oauth2;

import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import pl.touk.widerest.security.authentication.PrefixBasedAuthenticationManager;

import java.util.Map;
import java.util.Set;

public class ScopedOAuth2RequestFactory extends DefaultOAuth2RequestFactory {

    public ScopedOAuth2RequestFactory(ClientDetailsService clientDetailsService) {
        super(clientDetailsService);
    }

    @Override
    public AuthorizationRequest createAuthorizationRequest(Map<String, String> authorizationParameters) {
        Set<String> scopes = OAuth2Utils.parseParameterList(authorizationParameters.get(OAuth2Utils.SCOPE));
        if ((scopes == null || scopes.isEmpty())) {
            throw new InvalidScopeException("scope parameter is required");
        }

        return super.createAuthorizationRequest(authorizationParameters);
    }

    @Override
    public TokenRequest createTokenRequest(Map<String, String> requestParameters, ClientDetails authenticatedClient) {
        Set<String> scopes = OAuth2Utils.parseParameterList(requestParameters.get(OAuth2Utils.SCOPE));
        if ((scopes == null || scopes.isEmpty())) {
            String usertype = PrefixBasedAuthenticationManager.getAuthDataFromString(requestParameters.get("username")).getLeft();
            if ("backoffice".equals(usertype)) {
                requestParameters.put(OAuth2Utils.SCOPE, Scope.STAFF.toString());
            } else if ("site".equals(usertype)) {
                requestParameters.put(OAuth2Utils.SCOPE, Scope.CUSTOMER_REGISTERED.toString());
            }
        }
        return super.createTokenRequest(requestParameters, authenticatedClient);
    }
}
