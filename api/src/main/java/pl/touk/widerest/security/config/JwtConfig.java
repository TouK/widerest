package pl.touk.widerest.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
public class JwtConfig {

    @Bean
    public JwtTokenStore tokenStore(JwtAccessTokenConverter jwtTokenEnhancer) {
        return new JwtTokenStore(jwtTokenEnhancer);
    }

    @Bean
    public JwtAccessTokenConverter jwtTokenEnhancer(AccessTokenConverter accessTokenConverter, UserAuthenticationConverter userAuthenticationConverter) {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        jwtAccessTokenConverter.setAccessTokenConverter(accessTokenConverter);
        return jwtAccessTokenConverter;
    }





}
