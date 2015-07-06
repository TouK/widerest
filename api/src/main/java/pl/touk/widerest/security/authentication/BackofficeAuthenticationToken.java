package pl.touk.widerest.security.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class BackofficeAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public BackofficeAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

}
