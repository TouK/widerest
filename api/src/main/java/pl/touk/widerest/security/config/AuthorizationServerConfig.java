package pl.touk.widerest.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import pl.touk.widerest.security.authentication.AnonymousUserDetailsService;
import pl.touk.widerest.security.authentication.AnonymousUserInterceptor;
import pl.touk.widerest.security.authentication.PrefixBasedAuthenticationManager;

import javax.annotation.Resource;

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

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .addInterceptor(anonymousUserInterceptor)
                .tokenStore(tokenStore)
                .tokenEnhancer(tokenEnhancer)
                .authenticationManager(new PrefixBasedAuthenticationManager(authenticationManager))
        ;
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
                .withClient("test").authorizedGrantTypes("password", "implicit", "authorization_code").scopes("site","backoffice").autoApprove(true);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.allowFormAuthenticationForClients().realm("oauth/widerest");
    }

}
