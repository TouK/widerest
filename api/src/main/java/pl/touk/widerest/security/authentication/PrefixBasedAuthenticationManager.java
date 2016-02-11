package pl.touk.widerest.security.authentication;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class PrefixBasedAuthenticationManager implements AuthenticationManager {

    private AuthenticationManager authenticationManager;

    public PrefixBasedAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return authenticationManager.authenticate(
                convertAuthentication(authentication)
        );
    }

    public static Authentication convertAuthentication(Authentication authentication) {
        return Optional.of(authentication.getPrincipal())
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(PrefixBasedAuthenticationManager::getAuthDataFromString)
                .map(authDetails -> {
                    final String usertype = authDetails.getLeft();
                    final String username = authDetails.getRight();
                    final Object password = authentication.getCredentials();

                    switch (usertype) {
                        case "backoffice":
                            return new BackofficeAuthenticationToken(username, password);
                        case "site":
                            return new SiteAuthenticationToken(username, password);
                        default:
                            return authentication;
                    }
                }).orElse(authentication);
    }

    public static Pair<String, String> getAuthDataFromString(String authenticationString) throws AuthenticationException {

        if(authenticationString == null) {
            throw new BadCredentialsException("Credentials not passed");
        }

        final String[] result = StringUtils.split(authenticationString, "/");

        if(result.length == 0 || result.length > 2) {
            throw new BadCredentialsException("Wrong credentials provided");
        } else if(result.length == 1) {
            throw new BadCredentialsException("Missing username or usertype");
        } else {
            /* proper form: usertype/username has been provided */
            return new ImmutablePair<>(result[0], result[1]);
        }
    }

}
