package pl.touk.widerest.base;

import com.google.common.collect.Sets;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.api.settings.SettingsService;
import pl.touk.widerest.boot.BroadleafApplicationContextInitializer;

import java.util.Set;

@SpringBootApplication
@ComponentScan("pl.touk.widerest")
public class Application {

    @Bean
    public SettingsConsumer samplePropertyConsumer() {
        return new SettingsConsumer() {
            @Override
            public void setSettingsService(SettingsService settingsService) {

            }

            @Override
            public Set<String> getHandledProperties() {
                return Sets.newHashSet("test");
            }
        };
    }

    public static class ContextInitializer extends BroadleafApplicationContextInitializer {

        public ContextInitializer() {
            super(
                    "classpath:/bl-open-admin-contentClient-applicationContext.xml\n" +
                            "classpath:/bl-open-admin-contentCreator-applicationContext.xml\n" +
                            "classpath:/bl-cms-contentClient-applicationContext.xml\n" +
                            "classpath:/bl-common-applicationContext.xml\n" +
                            //"classpath*:/blc-config/site/bl-*-applicationContext.xml\n" +
                            "classpath:/applicationContext.xml\n" +
                            "classpath:/applicationContext-security.xml\n"
            );
        }
    }

}
