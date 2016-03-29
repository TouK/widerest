package pl.touk.widerest.security.oauth2.oob;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.config.annotation.configuration.ClientDetailsServiceConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerSecurityConfiguration;

@Primary
@Configuration
@Order(-2)
@Import({ ClientDetailsServiceConfiguration.class, OobAuthorizationServerEndpointsConfiguration.class })
public class OobAuthorizationServerSecurityConfiguration extends AuthorizationServerSecurityConfiguration {
}
