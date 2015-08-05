package pl.touk.widerest.multitenancy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.jwt.crypto.sign.MacSigner;

@Configuration
public class MultiTenancyConfig {

    @Value("${mulititenacy.tokenSecret:secret}")
    private String tenantTokenSecret;

    @Bean
    public MacSigner macSigner() {
        return new MacSigner(tenantTokenSecret);
    }

}
