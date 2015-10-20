package pl.touk.widerest.security.jwt;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class WiderestAccessTokenConverter extends DefaultAccessTokenConverter {

    public static final String ISS = "iss";
    public static final String WIDEREST_ISS = "widerest";
    public static final String DELIMITER = "/";


    @Resource
    private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

    @Autowired
    @Override
    public void setUserTokenConverter(UserAuthenticationConverter userTokenConverter) {
        super.setUserTokenConverter(userTokenConverter);
    }

    @Override
    public Map<String, ?> convertAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        Map response = super.convertAccessToken(token, authentication);
        response.put(ISS, WIDEREST_ISS + DELIMITER + currentTenantIdentifierResolver.resolveCurrentTenantIdentifier());
        return response;
    }

    @Override
    public OAuth2AccessToken extractAccessToken(String value, Map<String, ?> map) {
        return super.extractAccessToken(value, map);
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
        return super.extractAuthentication(map);
    }
}
