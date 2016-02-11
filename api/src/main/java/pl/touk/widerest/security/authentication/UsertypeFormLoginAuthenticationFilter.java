package pl.touk.widerest.security.authentication;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class UsertypeFormLoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public static final String SPRING_SECURITY_FORM_USERTYPE_KEY = "j_usertype";

    private String usertypeParameter = SPRING_SECURITY_FORM_USERTYPE_KEY;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        final String username = Optional.ofNullable(obtainUsername(request))
                .map(String::trim)
                .orElse("");

        final String usertype = Optional.ofNullable(request.getParameter(usertypeParameter))
                .map(String::trim)
                .orElse("");

        final String password = Optional.ofNullable(obtainPassword(request)).orElse("");

        final UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                String.format("%s/%s", usertype, username), password);

        // Allow subclasses to set the "details" property
        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    public void setUsertypeParameter(String usertypeParameter) {
        this.usertypeParameter = usertypeParameter;
    }
}
