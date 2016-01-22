package pl.touk.widerest.api;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import java.util.Optional;

class ChannelInterceptor implements WebRequestInterceptor {


    @Override
    public void preHandle(WebRequest request) throws Exception {
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(OAuth2Authentication.class::isInstance)
                .map(OAuth2Authentication.class::cast)
                .map(OAuth2Authentication::getOAuth2Request)
                .map(oAuth2Request -> oAuth2Request.getClientId())
                .map(clientId -> StringUtils.substringAfter(clientId, "."))
                .ifPresent(RequestUtils::setRequestChannel);

    }

    @Override
    public void postHandle(WebRequest request, ModelMap model) throws Exception {
    }

    @Override
    public void afterCompletion(WebRequest request, Exception ex) throws Exception {
    }
}
