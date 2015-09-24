package pl.touk.widerest.security.authentication;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CustomFormLoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public static final String SPRING_SECURITY_FORM_USERTYPE_KEY = "j_usertype";

    private String usertypeParameter = SPRING_SECURITY_FORM_USERTYPE_KEY;

//    public CustomFormLoginAuthenticationFilter() {
//        setAuthenticationDetailsSource(new WebAuthenticationDetailsSource() {
//            @Override
//            public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
//                return super.buildDetails(context);
//            }
//        });
//    }
//
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        String username = obtainUsername(request);
        String password = obtainPassword(request);
        String usertype = request.getParameter(usertypeParameter);

        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = "";
        }

        if (usertype == null) {
            usertype = "";
        }

        username = username.trim();
        usertype = usertype.trim();

        UsernamePasswordAuthenticationToken authRequest;
        switch (usertype) {
            case "backoffice" :
                authRequest = new BackofficeAuthenticationToken(username, password);
                break;
            case "site" :
                authRequest = new SiteAuthenticationToken(username, password);
                break;
            default:
                authRequest = new UsernamePasswordAuthenticationToken(username, password);
        }

        // Allow subclasses to set the "details" property
        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    public void setUsertypeParameter(String usertypeParameter) {
        this.usertypeParameter = usertypeParameter;
    }
}
