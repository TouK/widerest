package pl.touk.widerest.auth0;

import com.auth0.spring.security.auth0.Auth0AuthenticationProvider;
import com.auth0.spring.security.auth0.Auth0JWTToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;

@Configuration
@ComponentScan("com.auth0")
public class Auth0Config extends ResourceServerConfigurerAdapter {

    @Value("${auth0.clientSecret}")
    public String clientSecret;

    @Value("${auth0.clientId}")
    public String clientId;

    @Value("${auth0.securedRoute:/tenant/**}")
    public String securedRoot;

    @Bean
    public Auth0AuthenticationProvider auth0AuthenticationProvider() {
        Auth0AuthenticationProvider provider = new Auth0AuthenticationProvider();
        provider.setClientSecret(clientSecret);
        provider.setClientId(clientId);
        provider.setSecuredRoute(securedRoot);
        return provider;
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources
                .authenticationManager(new OAuth2AuthenticationManager() {
                    @Override
                    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                        try {
                            return auth0AuthenticationProvider().authenticate(
                                    new Auth0JWTToken((String) authentication.getPrincipal())
                            );
                        } catch (AuthenticationException ex) {
                            return super.authenticate(authentication);
                        }
                    }
                })
        ;
    }


}
