package pl.touk.widerest;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pl.touk.widerest.boot.BroadleafBeanDefinitionRegistrar;

@Configuration
@Import({
        BraodleafConfiguration.Registrar.class
})
public class BraodleafConfiguration {

    static public class Registrar extends BroadleafBeanDefinitionRegistrar {
        @Override
        public String getPatchLocation() {
            return "classpath:/bl-open-admin-contentClient-applicationContext.xml\n" +
                    "classpath:/bl-open-admin-contentCreator-applicationContext.xml\n" +
                    "classpath:/bl-cms-contentClient-applicationContext.xml\n" +
                    "classpath:/bl-menu-applicationContext.xml\n" +
                    "classpath*:/blc-config/site/bl-*-applicationContext.xml\n" +
                    "classpath:/applicationContext.xml\n" +
                    "classpath:/applicationContext-security.xml\n" +
                    "classpath:/applicationContext-servlet-cms-contentClient.xml\n"
                    ;
        }
    }

}
