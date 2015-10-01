package pl.touk.widerest.api.settings;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class SettingsConfig {

    @Bean
    public Set<String> availableSystemPropertyNames() {
        return new HashSet<>();
    }
}
