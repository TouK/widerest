package pl.touk.widerest.security.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class SiteAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public SiteAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public SiteAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
