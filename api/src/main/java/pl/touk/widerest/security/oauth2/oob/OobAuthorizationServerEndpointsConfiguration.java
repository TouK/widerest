package pl.touk.widerest.security.oauth2.oob;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.OAuth2RequestValidator;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpointHandlerMapping;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;

@Primary
@Configuration
@Slf4j
public class OobAuthorizationServerEndpointsConfiguration extends AuthorizationServerEndpointsConfiguration {

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Primary
    @Bean
    @Override
    public AuthorizationEndpoint authorizationEndpoint() throws Exception {
        log.error("overriden");

        AuthorizationEndpoint authorizationEndpoint = new OobAuthorizationEndpoint();

        FrameworkEndpointHandlerMapping mapping = getEndpointsConfigurer().getFrameworkEndpointHandlerMapping();
        authorizationEndpoint.setUserApprovalPage(extractPath(mapping, "/oauth/confirm_access"));
        authorizationEndpoint.setProviderExceptionHandler(exceptionTranslator());
        authorizationEndpoint.setErrorPage(extractPath(mapping, "/oauth/error"));
        authorizationEndpoint.setTokenGranter(tokenGranter());
        authorizationEndpoint.setClientDetailsService(clientDetailsService);
        authorizationEndpoint.setAuthorizationCodeServices(authorizationCodeServices());
        authorizationEndpoint.setOAuth2RequestFactory(oauth2RequestFactory());
        authorizationEndpoint.setOAuth2RequestValidator(oauth2RequestValidator());
        authorizationEndpoint.setUserApprovalHandler(userApprovalHandler());
        return authorizationEndpoint;

    }

    private OAuth2RequestFactory oauth2RequestFactory() throws Exception {
        return getEndpointsConfigurer().getOAuth2RequestFactory();
    }

    private UserApprovalHandler userApprovalHandler() throws Exception {
        return getEndpointsConfigurer().getUserApprovalHandler();
    }

    private OAuth2RequestValidator oauth2RequestValidator() throws Exception {
        return getEndpointsConfigurer().getOAuth2RequestValidator();
    }

    private AuthorizationCodeServices authorizationCodeServices() throws Exception {
        return getEndpointsConfigurer().getAuthorizationCodeServices();
    }

    private WebResponseExceptionTranslator exceptionTranslator() {
        return getEndpointsConfigurer().getExceptionTranslator();
    }

    private TokenGranter tokenGranter() throws Exception {
        return getEndpointsConfigurer().getTokenGranter();
    }

    private String extractPath(FrameworkEndpointHandlerMapping mapping, String page) {
        String path = mapping.getPath(page);
        if (path.contains(":")) {
            return path;
        }
        return "forward:" + path;
    }

}
