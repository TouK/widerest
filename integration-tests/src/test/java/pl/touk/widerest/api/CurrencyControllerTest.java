package pl.touk.widerest.api;


import javaslang.Tuple;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrencyImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpServerErrorException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.security.oauth2.Scope;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CurrencyControllerTest extends AbstractTest {

    @Test
    public void shouldGetDefaultCurrency() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenLoggedInBackoffice(authorizationServerClient, Tuple.of("admin", "admin"));
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.STAFF, oAuth2RestTemplate -> {

                final ResponseEntity<BroadleafCurrencyImpl> result = oAuth2RestTemplate.getForEntity(ApiTestUrls.DEFAULT_CURRENCY_URL, BroadleafCurrencyImpl.class, serverPort);
                final BroadleafCurrency currency = result.getBody();

                assertTrue(result.getStatusCode().is2xxSuccessful());
                assertTrue(currency.getCurrencyCode().length() > 0);
            });
        });
    }

    @Test
    public void shouldSetDefaultCurrency() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenLoggedInBackoffice(authorizationServerClient, Tuple.of("admin", "admin"));
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.STAFF, oAuth2RestTemplate -> {

                final String SETTING_VALUE = "eur";

                oAuth2RestTemplate.put(ApiTestUrls.DEFAULT_CURRENCY_URL, SETTING_VALUE, serverPort);

                final ResponseEntity<BroadleafCurrencyImpl> receivedSettingEntity = oAuth2RestTemplate.getForEntity(ApiTestUrls.DEFAULT_CURRENCY_URL, BroadleafCurrencyImpl.class, serverPort);

                Assert.assertTrue(receivedSettingEntity.getStatusCode().is2xxSuccessful());
                assertThat(receivedSettingEntity.getBody().getCurrencyCode(), equalTo(SETTING_VALUE.toUpperCase()));
            });
        });
    }


    @Test(expected = HttpServerErrorException.class)
    public void shouldThrowExceptionOnEmptyCredentials() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenLoggedInBackoffice(authorizationServerClient, Tuple.of("admin", "admin"));
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.STAFF, oAuth2RestTemplate -> {

                final String SETTING_VALUE = "this_is_definitely_not_a_currency";

                oAuth2RestTemplate.put(ApiTestUrls.DEFAULT_CURRENCY_URL, SETTING_VALUE, serverPort);
            });
        });

    }
}