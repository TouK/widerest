package pl.touk.widerest.security.authentication;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import javax.annotation.Resource;

@Component
public class AnonymousUserInterceptor implements WebRequestInterceptor {

    @Resource
    AnonymousUserDetailsService anonymousUserDetailsService;

    @Override
    public void preHandle(WebRequest request) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication instanceof AnonymousAuthenticationToken) {
            UserDetails anonymousUser = anonymousUserDetailsService.createAnonymousUserDetails();
            authentication = new SiteAuthenticationToken(
                    anonymousUser, null, anonymousUser.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    @Override
    public void postHandle(WebRequest request, ModelMap model) throws Exception {

    }

    @Override
    public void afterCompletion(WebRequest request, Exception ex) throws Exception {

    }
}
