package pl.touk.widerest.security.authentication;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

public class PrefixBasedAuthenticationProvider implements AuthenticationProvider {


    private Map<String, AuthenticationProvider> authenticationProviders = new HashMap<>();

    public void addProvider(String site, AuthenticationProvider authenticationProvider) {
        authenticationProviders.put(site, authenticationProvider);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication auth = null;
        String[] tab = StringUtils.split(String.valueOf(authentication.getPrincipal()), "/");

        if(tab.length == 0 || tab.length > 2) {
            throw new BadCredentialsException("Wrong credentials provided");
        } else if(tab.length == 1) {
            auth = new UsernamePasswordAuthenticationToken(tab[0], authentication.getCredentials());
            return authenticationProviders.get("site").authenticate(auth);
        } else if(tab[0].equals("site")) {
            auth = new SiteAuthenticationToken(tab[1], authentication.getCredentials());
        } else if (tab[0].equals("backoffice")) {
            auth = new BackofficeAuthenticationToken(tab[1], authentication.getCredentials());
        } else {
            auth = new UsernamePasswordAuthenticationToken(tab[1], authentication.getCredentials());
        }

        return authenticationProviders.get(tab[0]).authenticate(auth);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
