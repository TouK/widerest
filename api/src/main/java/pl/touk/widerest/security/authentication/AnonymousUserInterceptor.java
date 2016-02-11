package pl.touk.widerest.security.authentication;

import java.util.Optional;

import javax.annotation.Resource;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

@Component
public class AnonymousUserInterceptor implements WebRequestInterceptor {

    @Resource
    AnonymousUserDetailsService anonymousUserDetailsService;

    @Override
    public void preHandle(WebRequest request) throws Exception {
        Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(auth -> auth instanceof AnonymousAuthenticationToken)
                .ifPresent(auth -> {
                    final UserDetails anonymousUser = anonymousUserDetailsService.createAnonymousUserDetails();
                    SecurityContextHolder.getContext().setAuthentication(new SiteAuthenticationToken(anonymousUser,
                            null, anonymousUser.getAuthorities()));
                });
    }

    @Override
    public void postHandle(WebRequest request, ModelMap model) throws Exception {

    }

    @Override
    public void afterCompletion(WebRequest request, Exception ex) throws Exception {

    }
}
