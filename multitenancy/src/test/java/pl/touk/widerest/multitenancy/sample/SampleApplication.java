package pl.touk.widerest.multitenancy.sample;

import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import pl.touk.widerest.multitenancy.MultiTenancyService;
import pl.touk.widerest.multitenancy.TenantRequest;

import java.util.function.Consumer;

@SpringBootApplication(exclude = { LiquibaseAutoConfiguration.class })
@ComponentScan(basePackageClasses = MultiTenancyService.class)
@AutoConfigureBefore({ HibernateJpaAutoConfiguration.class })
public class SampleApplication {

    @Bean
    public Consumer<TenantRequest> setTenantDetails() {
        return Mockito.mock(Consumer.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

}
