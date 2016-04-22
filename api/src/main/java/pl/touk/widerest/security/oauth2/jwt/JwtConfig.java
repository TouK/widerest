package pl.touk.widerest.security.oauth2.jwt;

import javaslang.control.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.net.URL;
import java.security.KeyPair;
import java.util.Collection;

@Configuration
public class JwtConfig {

    @Value("${widerest.jwt.key-store}")
    private String keyStore;

    @Value("${widerest.jwt.key-store-password}")
    private String keyStorePassword;

    @Value("${widerest.jwt.key-alias}")
    private String keyAlias;

    @Value("${widerest.jwt.key-password}")
    private String keyPassword;

    @Bean
    public JwtTokenStore tokenStore(JwtAccessTokenConverter jwtTokenEnhancer) {
        return new JwtTokenStore(jwtTokenEnhancer);
    }

    @Bean
    public JwtAccessTokenConverter jwtTokenEnhancer(AccessTokenConverter accessTokenConverter, UserAuthenticationConverter userAuthenticationConverter) {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        jwtAccessTokenConverter.setAccessTokenConverter(accessTokenConverter);
        Try.of(this::loadKeyPair).onSuccess(jwtAccessTokenConverter::setKeyPair);
        return jwtAccessTokenConverter;
    }

    @Autowired
    private Collection<PropertiesLoaderSupport> propertiesLoaders;

    protected KeyPair loadKeyPair() throws FileNotFoundException {
        URL keystoreUrl = ResourceUtils.getURL(keyStore);
        return new KeyStoreKeyFactory(new UrlResource(keystoreUrl), keyStorePassword.toCharArray())
                .getKeyPair(keyAlias, keyPassword.toCharArray());
    }





}
