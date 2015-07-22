package pl.touk.widerest.security.authentication;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

public class PrefixBasedAuthenticationProvider implements AuthenticationProvider {


    private Map<String, AuthenticationProvider> authenticationProviders = new HashMap<>();

    public void addProvider(String site, AuthenticationProvider authenticationProvider) {
        authenticationProviders.put(site, authenticationProvider);
    }

    public void setProviders(Map<String, AuthenticationProvider> providers) {
        this.authenticationProviders = authenticationProviders;
    }

    public static Pair<String, String> getAuthDataFromString(String authenticationString) throws AuthenticationException {

        if(authenticationString == null) {
            throw new BadCredentialsException("Credentials not passed");
        }

        String[] result = StringUtils.split(authenticationString, "/");

        if(result.length == 0 || result.length > 2) {
            throw new BadCredentialsException("Wrong credentials provided");
        } else if(result.length == 1) {
            throw new BadCredentialsException("Missing username or usertype");
        } else {
            /* proper form: usertype/username has been provided */
            Pair<String, String> resultPair = new ImmutablePair<>(result[0], result[1]);
            return resultPair;
        }
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if(authentication == null || authentication.getPrincipal() == null) {
            throw new AuthenticationServiceException("");
        }

        /* left - usertype, right - username */
        Pair<String, String> authDetails = getAuthDataFromString(String.valueOf(authentication.getPrincipal()));

        String username = authDetails.getRight();
        Object credentials = authentication.getCredentials();
        Authentication auth = null;

        switch(authDetails.getLeft()) {
            case "site":
                auth = new SiteAuthenticationToken(username, credentials);
                break;
            case "backoffice":
                auth = new BackofficeAuthenticationToken(username, credentials);
                break;
            default:
                auth = new UsernamePasswordAuthenticationToken(username, credentials);
        }

        return authenticationProviders.get(authDetails.getLeft()).authenticate(auth);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
