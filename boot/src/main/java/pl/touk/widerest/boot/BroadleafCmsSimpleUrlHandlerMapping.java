package pl.touk.widerest.boot;

import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import javax.annotation.Resource;
import java.util.Properties;

public class BroadleafCmsSimpleUrlHandlerMapping extends SimpleUrlHandlerMapping {

    @Resource(name="blConfiguration")
    protected org.broadleafcommerce.common.config.RuntimeEnvironmentPropertiesConfigurer configurer;

    @Override
    public void setMappings(Properties mappings) {
        Properties clone = new Properties();
        for (Object propertyName: mappings.keySet()) {
            String newName = configurer.getStringValueResolver().resolveStringValue(propertyName.toString());
            clone.put(newName, mappings.get(propertyName));
        }
        super.setMappings(clone);
    }
}