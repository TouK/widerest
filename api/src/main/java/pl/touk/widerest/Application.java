package pl.touk.widerest;

import org.broadleafcommerce.openadmin.server.security.domain.AdminUser;
import org.broadleafcommerce.openadmin.server.security.domain.AdminUserImpl;
import org.broadleafcommerce.openadmin.server.security.service.AdminSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import pl.touk.widerest.multitenancy.TenantAdminService;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;

@Configuration
@EnableAutoConfiguration(exclude = { HibernateJpaAutoConfiguration.class })
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
    public TenantAdminService tenantAdminService(AdminSecurityService adminSecurityService) {
        return (email, password) -> {
            AdminUser adminUser = new AdminUserImpl();
            adminUser.setLogin("admin");
            adminUser.setName("admin");
            adminUser.setEmail(email);
            adminUser.setPassword(password);
            adminSecurityService.saveAdminUser(adminUser);
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
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                .initializers(new BroadleafApplicationContextInitializer())
                .run(args);
    }

}
