package pl.touk.widerest.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import pl.touk.widerest.security.authentication.CustomAccessTokenConverter;

@Configuration
public class JwtConfig {

    @Bean
    public JwtTokenStore tokenStore(JwtAccessTokenConverter jwtTokenEnhancer) {
        return new JwtTokenStore(jwtTokenEnhancer);
    }


    @Bean
    public JwtAccessTokenConverter jwtTokenEnhancer(UserAuthenticationConverter userAuthenticationConverter) {
        JwtAccessTokenConverter jwtAccessTokenConverter = new CustomAccessTokenConverter();
        ((DefaultAccessTokenConverter)jwtAccessTokenConverter.getAccessTokenConverter()).setUserTokenConverter(userAuthenticationConverter);
        return jwtAccessTokenConverter;
    }





}
