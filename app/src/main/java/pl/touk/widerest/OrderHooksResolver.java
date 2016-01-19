package pl.touk.widerest;

import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.api.settings.SettingsService;

import java.util.Optional;
import java.util.Set;

@Component
public class OrderHooksResolver implements SettingsConsumer {

    private static final String ORDER_COMPLETED_HOOK = "orderCompletedHook";

    protected SettingsService settingsService;

    public Optional<String> getOrderCompletedHookUrl() {
        return settingsService.getProperty(ORDER_COMPLETED_HOOK);
    }

    @Override
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public Set<String> getHandledProperties() {
        return Sets.newHashSet(ORDER_COMPLETED_HOOK);
    }
}
