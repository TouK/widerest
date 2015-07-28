import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;


/**
 * Created by mst on 24.07.15.
 */
@Configuration
@ComponentScan("pl.touk.widerest")
@EnableAutoConfiguration(exclude = { HibernateJpaAutoConfiguration.class })
//@SpringBootApplication
public class PaypalApplication extends WebMvcConfigurerAdapter {



    public static void main(String[] args) {
        new SpringApplicationBuilder(PaypalApplication.class)
                .initializers(new BroadleafApplicationContextInitializer())
                .run(args);
    }


}
