package pl.touk.widerest.api.settings;

import java.util.Set;

public interface SettingsConsumer {

    void setSettingsService(SettingsService settingsService);

    Set<String> getHandledProperties();
}
