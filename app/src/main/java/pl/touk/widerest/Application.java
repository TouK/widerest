package pl.touk.widerest;

import com.google.common.collect.Sets;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.api.settings.SettingsService;
import pl.touk.widerest.boot.BroadleafApplicationContextInitializer;
import pl.touk.widerest.paypal.gateway.PayPalSession;

import java.util.Set;

@SpringBootApplication
public class Application extends WebMvcConfigurerAdapter {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/oauth/confirm_access").setViewName("authorize");
        registry.addRedirectViewController("/", "/swagger-ui.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/css/");
        registry.addResourceHandler("/cmsstatic/img/**").addResourceLocations("classpath:/cms/static/img/");
    }

    @Bean
    public SettingsConsumer paypalSettingsConsumer() {
        return new SettingsConsumer() {
            @Override
            public void setSettingsService(SettingsService settingsService) {

            }

            @Override
            public Set<String> getHandledProperties() {
                return Sets.newHashSet(PayPalSession.CLIENT_ID, PayPalSession.SECRET);
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
                    "classpath:/applicationContext-sendwithus.xml\n" +
                    "classpath:/applicationContext-paypal.xml\n" +
                    "classpath:/applicationContext-security.xml\n"
            );
        }
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                .initializers(new ContextInitializer())
                .run(args);
    }

}
