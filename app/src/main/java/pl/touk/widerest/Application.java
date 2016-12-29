package pl.touk.widerest;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.api.settings.SettingsService;
import pl.touk.widerest.boot.BroadleafBeansPostProcessor;
import pl.touk.widerest.boot.ReorderedHttpMessageConverters;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.SecurityConfiguration;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SpringBootApplication
@Import(BraodleafConfiguration.class)
public class Application extends WebMvcConfigurerAdapter implements TransactionManagementConfigurer {

    @Autowired
    PlatformTransactionManager blTransactionManager;

    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return blTransactionManager;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        return propertySourcesPlaceholderConfigurer;
    }

    @Resource(name = "entityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Primary
    @Bean
    public EntityManagerFactory primaryEntityManagerFactory() {
        return entityManagerFactory;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/oauth/confirm_access").setViewName("authorize");
        registry.addRedirectViewController("/", "/swagger-ui.html");
    }

    @Bean
    public SecurityConfiguration security() {
        return new SecurityConfiguration(
                "default",
                "secret",
                "test-app-realm",
                "test-app",
                null,
                ApiKeyVehicle.HEADER,
                "api_key",
                " "
        );
    }

    @Bean
    @Order(-10000)
    static public BroadleafBeansPostProcessor broadleafBeansPostProcessor() {
        return new BroadleafBeansPostProcessor();
    }

    @Autowired(required = false)
    private final List<HttpMessageConverter<?>> converters = Collections.emptyList();

    @Bean
    public ReorderedHttpMessageConverters httpMessageConverters() {
        return new ReorderedHttpMessageConverters(converters);
    }

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

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                .run(args);
    }

}
