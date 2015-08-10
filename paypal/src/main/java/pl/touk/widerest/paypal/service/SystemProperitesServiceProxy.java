package pl.touk.widerest.paypal.service;


import org.apache.velocity.exception.ResourceNotFoundException;
import org.broadleafcommerce.common.config.dao.SystemPropertiesDao;
import org.broadleafcommerce.common.config.domain.SystemProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("wdSystemProperties")
public class SystemProperitesServiceProxy {

    @Resource(name = "blSystemPropertiesDao")
    protected SystemPropertiesDao systemPropertiesDao;

    public final static String CLIENT_ID = "paypal_client_id";
    public final static String SECRET = "paypal_secret";

    public SystemProperty getSystemPropertyByName(String name) {
        return systemPropertiesDao.readAllSystemProperties().stream()
                .filter(e -> e.getName().equals(name))
                .findAny()
                .orElse(null);
    }

    public SystemProperty setOrUpdatePropertyByName(String name, String value) {

        if(getSystemPropertyByName(name) != null) {
            systemPropertiesDao.deleteSystemProperty(
                    systemPropertiesDao.readSystemPropertyByName(name)
            );
        }
        SystemProperty prop = systemPropertiesDao.createNewSystemProperty();
        prop.setName(name);
        prop.setValue(value);
        return systemPropertiesDao.saveSystemProperty(prop);
    }
}
