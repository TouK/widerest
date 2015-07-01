package pl.touk.widerest.security;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public class ClientDispatchedAuthenticationProvider extends DaoAuthenticationProvider {

    @Getter
    @Setter
    private String clientId;

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Object details = authentication.getDetails();
        if (details instanceof Map &&
                StringUtils.equals(
                        String.valueOf(((Map) details).get("client_id")),
                        clientId
                )) {
            return super.authenticate(authentication);
        }
        return null;
    }

}
