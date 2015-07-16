package pl.touk.widerest.security.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class BackofficeAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public BackofficeAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public BackofficeAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
