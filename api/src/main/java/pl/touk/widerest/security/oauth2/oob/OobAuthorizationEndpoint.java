package pl.touk.widerest.security.oauth2.oob;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Map;

public class OobAuthorizationEndpoint extends AuthorizationEndpoint {

    public static final String OOB_URI = "urn:ietf:wg:oauth:2.0:oob";

    @Override
    public ModelAndView handleOAuth2Exception(OAuth2Exception e, ServletWebRequest webRequest) throws Exception {
        return interceptOutOfBandRedirect(super.handleOAuth2Exception(e, webRequest));
    }

    @Override
    public ModelAndView authorize(Map<String, Object> model, @RequestParam Map<String, String> parameters, SessionStatus sessionStatus, Principal principal) {
        return interceptOutOfBandRedirect(super.authorize(model, parameters, sessionStatus, principal));
    }

    protected ModelAndView interceptOutOfBandRedirect(ModelAndView mav) {
        View view = mav.getView();
        if (view instanceof RedirectView) {
            String location = ((RedirectView) view).getUrl();
            if (location.startsWith(OOB_URI)) {
                UriComponents uriComponents = UriComponentsBuilder.newInstance().query(location.substring(OOB_URI.length() + 1)).build();
                return new ModelAndView(new MappingJackson2JsonView(), uriComponents.getQueryParams().toSingleValueMap());
            }
        }
        return mav;
    }
}
