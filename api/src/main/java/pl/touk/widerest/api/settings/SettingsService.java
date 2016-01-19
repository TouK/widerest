package pl.touk.widerest.api.settings;

import lombok.Getter;
import org.broadleafcommerce.common.config.dao.SystemPropertiesDao;
import org.broadleafcommerce.common.config.domain.SystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class SettingsService {

    @Autowired
    Collection<SettingsConsumer> handlers;

    @Getter
    protected Set<String> availableSystemPropertyNames = new HashSet<>();

    @Resource(name = "blSystemPropertiesDao")
    protected SystemPropertiesDao systemPropertiesDao;

    @PostConstruct
    public void init() {
        handlers.stream().forEach(h -> {
            availableSystemPropertyNames.addAll(h.getHandledProperties());
            h.setSettingsService(this);
        });
    }

    public Optional<String> getProperty(String name) {
        return Optional.ofNullable(systemPropertiesDao.readSystemPropertyByName(name))
                .map(SystemProperty::getValue);

    }
}
