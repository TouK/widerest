package pl.touk.widerest.api;

import javaslang.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.security.oauth2.Scope;

import javax.annotation.Resource;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static pl.touk.widerest.base.ApiTestUrls.SETTINGS_BY_NAME_URL;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SettingsControllerTest extends AbstractTest {

    @Resource
    protected Collection<SettingsConsumer> settingsConsumers;

    @Test
    public void  shouldSetAKnownPropertyTest() throws Throwable {

        givenAuthorizationServerClient(authorizationServerClient -> {
            whenLoggedInBackoffice(authorizationServerClient, Tuple.of("admin", "admin"));
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.STAFF, oAuth2RestTemplate -> {
                final String propertyKey = settingsConsumers.iterator().next().getHandledProperties().iterator().next();
                final String propertyValue = "value";


                oAuth2RestTemplate.put(SETTINGS_BY_NAME_URL, propertyValue, serverPort, propertyKey);

                final ResponseEntity<String> receivedSettingEntity = oAuth2RestTemplate.getForEntity(SETTINGS_BY_NAME_URL, String.class, serverPort, propertyKey);

                Assert.assertTrue(receivedSettingEntity.getStatusCode().is2xxSuccessful());
                assertThat(receivedSettingEntity.getBody(), equalTo(propertyValue));
            });
        });

    }

}
