package pl.touk.widerest.security.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;

public class TokenTypeSelectedAuthenticationProvider<T extends UsernamePasswordAuthenticationToken> extends DaoAuthenticationProvider {

    private Class<T> supportedTokenType;

    public TokenTypeSelectedAuthenticationProvider(Class<T> type) {
        this.supportedTokenType = type;
    }

    @Transactional
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return super.authenticate(authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (supportedTokenType.isAssignableFrom(authentication));
    }
}
