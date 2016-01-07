package pl.touk.widerest;

import org.broadleafcommerce.common.config.dao.SystemPropertiesDao;
import org.broadleafcommerce.common.config.domain.SystemProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Component
public class OrderHooksResolver {

    private static final String ORDER_COMPLETED_HOOK = "orderCompletedHook";

    @Resource(name = "blSystemPropertiesDao")
    protected SystemPropertiesDao systemPropertiesDao;

    @Resource
    protected Set<String> availableSystemPropertyNames;

    @PostConstruct
    public void init() {
        Collections.addAll(availableSystemPropertyNames, ORDER_COMPLETED_HOOK);
    }

    public Optional<String> getOrderCompletedHookUrl() {
        return Optional.ofNullable(systemPropertiesDao.readSystemPropertyByName(ORDER_COMPLETED_HOOK))
                .map(SystemProperty::getValue);
    }
}
