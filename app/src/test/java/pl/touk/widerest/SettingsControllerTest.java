package pl.touk.widerest;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.base.ApiTestBase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by mst on 08.10.15.
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringApplicationConfiguration(classes = Application.class)
public class SettingsControllerTest extends ApiTestBase {

    @Test
    public void shouldSetAKnownPropertyTest() {
        final String SETTING_NAME = "paypalClientId";
        final String SETTING_VALUE = "value";


        restTemplate.put(SETTINGS_BY_NAME_URL, SETTING_VALUE, serverPort, SETTING_NAME);

        final ResponseEntity<String> receivedSettingEntity = restTemplate.getForEntity(SETTINGS_BY_NAME_URL, String.class, serverPort, SETTING_NAME);

        Assert.assertTrue(receivedSettingEntity.getStatusCode().is2xxSuccessful());
        assertThat(receivedSettingEntity.getBody(), equalTo(SETTING_VALUE));
    }


}
