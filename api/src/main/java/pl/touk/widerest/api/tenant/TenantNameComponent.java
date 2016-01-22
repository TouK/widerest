package pl.touk.widerest.api.tenant;

import com.google.common.collect.Sets;
import org.springframework.stereotype.Controller;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.api.settings.SettingsService;

import javax.annotation.Resource;
import java.util.Set;

@Controller
public class TenantNameComponent implements SettingsConsumer {

    public static final String tenantName = "tenantName";

    @Resource
    private SettingsService service;

    @Override
    public void setSettingsService(SettingsService service) {
        this.service = service;
    }

    @Override
    public Set<String> getHandledProperties() {
        return Sets.newHashSet(tenantName);
    }
}
