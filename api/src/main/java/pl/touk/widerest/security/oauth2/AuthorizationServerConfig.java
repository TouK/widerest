package pl.touk.widerest.security.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import pl.touk.widerest.security.authentication.AnonymousUserInterceptor;
import pl.touk.widerest.security.oauth2.oob.OobAuthorizationServerEndpointsConfiguration;
import pl.touk.widerest.security.oauth2.oob.OobAuthorizationServerSecurityConfiguration;

import javax.annotation.Resource;

@Configuration
@Import({OobAuthorizationServerEndpointsConfiguration.class, OobAuthorizationServerSecurityConfiguration.class})
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

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

    @Bean
    AuthorizationCodeServices authorizationCodeServices() {
        return new InMemoryAuthorizationCodeServices();
    }

    @Value("${widerest.oauth2.token-expiration:#{30 * 60}}")
    int tokenExpirationTime;

    @Bean
    public AuthorizationServerTokenServices tokenServices() {
        DefaultTokenServices tokenServices = new DefaultTokenServices();
        tokenServices.setTokenStore(tokenStore);
        tokenServices.setClientDetailsService(implicitClientDetailsService);
        tokenServices.setTokenEnhancer(tokenEnhancer);
        tokenServices.setAccessTokenValiditySeconds(tokenExpirationTime);
        return tokenServices;
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.addInterceptor(anonymousUserInterceptor)
                .tokenStore(tokenStore)
                .tokenEnhancer(tokenEnhancer)
                .authenticationManager(authenticationManager)
                .requestFactory(new ScopedOAuth2RequestFactory(implicitClientDetailsService))
                .requestValidator(oAuth2RequestValidator)
                .authorizationCodeServices(authorizationCodeServices())
                .tokenServices(tokenServices())
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
