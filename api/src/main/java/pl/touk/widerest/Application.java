package pl.touk.widerest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

@Configuration
@EnableAutoConfiguration(exclude = { PropertyPlaceholderAutoConfiguration.class, HibernateJpaAutoConfiguration.class, WebMvcAutoConfiguration.class})
@ComponentScan("pl.touk.widerest")
@EnableTransactionManagement
public class Application implements TransactionManagementConfigurer {

    @Autowired
    PlatformTransactionManager blTransactionManager;

    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return blTransactionManager;
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                .contextClass(BroadleafEmbeddedApplicationContext.class)
                .run(args);
    }

}
