package pl.touk.widerest.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.util.Collection;
import java.util.function.Supplier;

@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    public static final String API_PATH = "/v1";

    @Autowired
    TokenStore tokenStore;

    @Autowired(required = false)
    Supplier<String> resourceIdSupplier;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources
                .tokenStore(tokenStore)
                .resourceId(null)
                .authenticationManager(new OAuth2AuthenticationManager() {
                    @Override
                    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                        OAuth2Authentication auth = (OAuth2Authentication) super.authenticate(authentication);
                        Collection<String> resourceIds = auth.getOAuth2Request().getResourceIds();
                        if (resourceIds != null && !resourceIds.isEmpty() && (resourceIdSupplier == null || !resourceIds.contains(resourceIdSupplier.get()))) {
                            throw new OAuth2AccessDeniedException("Invalid token does not contain resource id (" + resourceIdSupplier.get() + ")");
                        }
                        return auth;
                    }
                })
        ;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
            .requestMatchers()
                .antMatchers(API_PATH + "/**")
                .and()
            .authorizeRequests()
                .anyRequest().permitAll()
        ;
    }
}
