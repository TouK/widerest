package pl.touk.widerest.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.CompositeTokenGranter;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import pl.touk.widerest.security.AnonymousTokenGranter;
import pl.touk.widerest.security.AnonymousUserDetailsService;
import pl.touk.widerest.security.UserAuthenticationConverterImpl;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableAuthorizationServer
@EnableGlobalAuthentication
public class BackofficeAuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Resource
    AnonymousUserDetailsService anonymousUserDetailsService;

    @Resource(name = "backofficeAuthenticationManager")
    AuthenticationManager authenticationManager;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        auth.parentAuthenticationManager(authenticationManager);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .pathMapping("/oauth/token", "/admin/oauth/token")
                .pathMapping("/oauth/authorize", "/admin/oauth/authorize")
                .pathMapping("/oauth/confirm_access", "/admin/oauth/confirm_access")
                .tokenStore(tokenStore())
                .tokenEnhancer(jwtTokenEnhancer())
                .authenticationManager(authenticationManager);

        List<TokenGranter> granters = new ArrayList<TokenGranter>(Arrays.asList(endpoints.getTokenGranter()));
        granters.add(new AnonymousTokenGranter(anonymousUserDetailsService, endpoints.getTokenServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));
        endpoints.tokenGranter(new CompositeTokenGranter(granters));
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
                .withClient("backoffice").secret("secret").authorizedGrantTypes("password", "implicit", "authorization_code").scopes("site","backoffice").autoApprove(true);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.allowFormAuthenticationForClients().realm("oauth/widerest");
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(jwtTokenEnhancer());
    }

    @Bean
    public JwtAccessTokenConverter jwtTokenEnhancer() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        ((DefaultAccessTokenConverter)jwtAccessTokenConverter.getAccessTokenConverter()).setUserTokenConverter(new UserAuthenticationConverterImpl());
        return jwtAccessTokenConverter;
    }


}
