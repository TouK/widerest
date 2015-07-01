package pl.touk.widerest.security;

import lombok.Setter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

public class AnonymousTokenGranter extends AbstractTokenGranter {

    private static final String GRANT_TYPE = "anonymous";

    protected AnonymousUserDetailsService anonymousUserDetailsService;

    @Setter
    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    public AnonymousTokenGranter(AnonymousUserDetailsService anonymousUserDetailsService, AuthorizationServerTokenServices tokenServices, ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory) {
        super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
        this.anonymousUserDetailsService = anonymousUserDetailsService;
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        OAuth2Request storedOAuth2Request = getRequestFactory().createOAuth2Request(client, tokenRequest);

        UserDetails anonymousUser = anonymousUserDetailsService.createAnonymousUser();

        Authentication userAuth = new AnonymousAuthenticationToken("anonymous oauth", anonymousUser, authoritiesMapper.mapAuthorities(anonymousUser.getAuthorities()));

        return new OAuth2Authentication(storedOAuth2Request, userAuth);
    }
}
