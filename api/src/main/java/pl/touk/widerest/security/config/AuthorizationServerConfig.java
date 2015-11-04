package pl.touk.widerest.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import pl.touk.widerest.security.authentication.AnonymousUserDetailsService;
import pl.touk.widerest.security.authentication.AnonymousUserInterceptor;
import pl.touk.widerest.security.oauth2.ImplicitClientDetailsService;
import pl.touk.widerest.security.oauth2.PrincipalMatchOAuth2RequestValidator;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Resource
    AnonymousUserDetailsService anonymousUserDetailsService;

    @Resource
    AuthenticationManager authenticationManager;

    @Resource
    AnonymousUserInterceptor anonymousUserInterceptor;

    @Resource
    TokenStore tokenStore;

    @Resource
    TokenEnhancer tokenEnhancer;

    @Resource
    ImplicitClientDetailsService implicitClientDetailsService;

    @Resource
    PrincipalMatchOAuth2RequestValidator oAuth2RequestValidator;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
               // .addInterceptor(anonymousUserInterceptor)
                .tokenStore(tokenStore)
                .tokenEnhancer(tokenEnhancer)
                .authenticationManager(authenticationManager)
                .requestFactory(new DefaultOAuth2RequestFactory(implicitClientDetailsService) {
                    @Override
                    public AuthorizationRequest createAuthorizationRequest(Map<String, String> authorizationParameters) {
                        Set<String> scopes = OAuth2Utils.parseParameterList(authorizationParameters.get(OAuth2Utils.SCOPE));
                        if ((scopes == null || scopes.isEmpty())) {
                            throw new InvalidScopeException("scope parameter is required");
                        }

                        return super.createAuthorizationRequest(authorizationParameters);
                    }
                })
                .requestValidator(oAuth2RequestValidator)
        ;
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(implicitClientDetailsService);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.allowFormAuthenticationForClients().realm("oauth/widerest");
    }

}
