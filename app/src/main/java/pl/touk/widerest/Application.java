package pl.touk.widerest;

import com.spotify.docker.client.DockerCertificateException;
import org.broadleafcommerce.openadmin.server.security.domain.AdminUser;
import org.broadleafcommerce.openadmin.server.security.service.AdminSecurityService;
import org.hibernate.Session;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import pl.touk.widerest.multitenancy.MultiTenancyConfig;
import pl.touk.widerest.multitenancy.TenantRequest;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Configuration
@EnableAutoConfiguration(exclude = { /*HibernateJpaAutoConfiguration.class,*/ LiquibaseAutoConfiguration.class })
@ComponentScan("pl.touk.widerest")
@EnableTransactionManagement
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

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource", name = "url", havingValue = "false", matchIfMissing = true)
    public DataSource dataSource() throws DockerCertificateException {
        DockerizedDataSource dockerizedDataSource = new DockerizedDataSource();
        return dockerizedDataSource;
    }

    @Bean
    Consumer<String> clientIdValidator(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        return clientId -> {
            if (!currentTenantIdentifierResolver.resolveCurrentTenantIdentifier().equals(clientId))
                throw new NoSuchClientException("No client with requested id: " + clientId);
        };
    }


    @Bean
    public Supplier<String> resourceIdSupplier(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        return currentTenantIdentifierResolver::resolveCurrentTenantIdentifier;
    }

    @Bean
    public Consumer<TenantRequest> setTenantDetails(AdminSecurityService adminSecurityService, CurrentTenantIdentifierResolver tenantIdentifierResolver) {
        return new Consumer<TenantRequest>() {

            @PersistenceContext(unitName = "blPU")
            protected EntityManager em;

            @Override
            public void accept(TenantRequest tenantRequest) {
                Object tenantInRequest = RequestContextHolder.getRequestAttributes().getAttribute(MultiTenancyConfig.TENANT_IDENTIFIER_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                String tenantIdentifier = tenantIdentifierResolver.resolveCurrentTenantIdentifier();
                String tenantIdInEntityManager = em.unwrap(Session.class).getTenantIdentifier();
                AdminUser adminUser = adminSecurityService.readAdminUserByUserName("admin");
                adminUser.setEmail(tenantRequest.getAdminEmail());
                adminUser.setUnencodedPassword(tenantRequest.getAdminPassword());
                adminSecurityService.saveAdminUser(adminUser);
            }
        };
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/oauth/confirm_access").setViewName("authorize");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/css/");
        registry.addResourceHandler("/cmsstatic/img/**").addResourceLocations("classpath:/cms/static/img/");
    }

    @Component
    public static class ReorderedHttpMessageConverters extends HttpMessageConverters {
        @Override
        protected List<HttpMessageConverter<?>> postProcessConverters(List<HttpMessageConverter<?>> converters) {
            for (Iterator<HttpMessageConverter<?>> iterator = converters.iterator(); iterator.hasNext();) {
                HttpMessageConverter<?> converter = iterator.next();
                if (converter instanceof StringHttpMessageConverter) {
                    iterator.remove();
                    converters.add(converter);
                    break;
                }
            }
            return converters;
        }
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                .initializers(new BroadleafApplicationContextInitializer())
                .run(args);
    }

}
