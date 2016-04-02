package pl.touk.widerest.api;


import javaslang.Tuple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpServerErrorException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.security.oauth2.Scope;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CurrencyControllerTest extends AbstractTest {

    @Test
    public void shouldGetDefaultCurrency() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenLoggedInBackoffice(authorizationServerClient, Tuple.of("admin", "admin"));
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.STAFF, oAuth2RestTemplate -> {

                final String currentCurrencyCode = oAuth2RestTemplate.getForObject(ApiTestUrls.DEFAULT_CURRENCY_URL, String.class, serverPort);

                assertThat(currentCurrencyCode, not(isEmptyString()));
            });
        });
    }

    @Test
    public void shouldSetDefaultCurrency() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenLoggedInBackoffice(authorizationServerClient, Tuple.of("admin", "admin"));
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.STAFF, oAuth2RestTemplate -> {

                final String eurCurrencyCode = "eur";

                oAuth2RestTemplate.put(ApiTestUrls.DEFAULT_CURRENCY_URL, eurCurrencyCode, serverPort);

                final String currentCurrencyCode = oAuth2RestTemplate.getForObject(ApiTestUrls.DEFAULT_CURRENCY_URL, String.class, serverPort);

                assertThat(currentCurrencyCode, equalTo(eurCurrencyCode.toUpperCase()));
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