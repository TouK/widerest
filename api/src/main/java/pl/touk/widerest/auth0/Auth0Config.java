package pl.touk.widerest.auth0;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath:/applicationContext-oauth.xml")
public class Auth0Config {
}
