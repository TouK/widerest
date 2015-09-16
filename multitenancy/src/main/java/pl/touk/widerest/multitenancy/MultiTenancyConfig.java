package pl.touk.widerest.multitenancy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.jwt.crypto.sign.MacSigner;

@Configuration
@ImportResource("classpath:/applicationContext-oauth.xml")
public class MultiTenancyConfig {

    public static final String TENANT_REQUEST_ATTRIBUTE = MultiTenancyConfig.class.getPackage().getName() + ".Tenant";
    public static final String DEFAULT_TENANT_IDENTIFIER ="default";

    @Value("${mulititenacy.tokenSecret:secret}")
    private String tenantTokenSecret;

    @Bean
    public MacSigner macSigner() {
        return new MacSigner(tenantTokenSecret);
    }


}
