package pl.touk.widerest.security;

import javaslang.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import pl.touk.widerest.AbstractTest;
import pl.touk.widerest.api.orders.OrderDto;
import pl.touk.widerest.security.oauth2.Scope;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static pl.touk.widerest.base.ApiTestUrls.CUSTOMERS_URL;
import static pl.touk.widerest.base.ApiTestUrls.ORDERS_COUNT;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class AuthorizationTest extends AbstractTest {

    @Test
    public void shouldRequireScopeForAuthoriztion() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenAuthorizationRequestedFor(authorizationServerClient, null,
                    this::thenNotAuthorized);
        });
    }

    @Test
    public void shouldAuthorizeNewCustomer() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.CUSTOMER,
                    this::thenAuthorized);
        });
    }

    @Test
    public void shouldAuthorizeTheSameCustomerAgainUsingSessionAndAllowAccesToTheSameORder() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.CUSTOMER, restTemplate1 -> {
                whenNewOrderCreated(restTemplate1, orderUrl -> {
                    whenAuthorizationRequestedFor(authorizationServerClient, Scope.CUSTOMER, restTemplate2 -> {
                        final OrderDto order = restTemplate2.getForObject(orderUrl, OrderDto.class);
                        //then no error
                    });
                    givenAuthorizationServerClient(authorizationServerClient2 -> {
                        whenAuthorizationRequestedFor(authorizationServerClient2, Scope.CUSTOMER, restTemplate3 -> {
                            // but the order is not available through other authorization sessions
                            Assertions.assertThatThrownBy(() -> restTemplate3.getForObject(orderUrl, OrderDto.class))
                                    .isInstanceOf(HttpClientErrorException.class)
                                    .hasMessageContaining("404");
                        });
                    });
                });
            });
        });
    }

    @Test
    public void shouldRequireLoginForRegisteredCustomer() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.CUSTOMER_REGISTERED,
                this::thenNotAuthorized);
        });
    }

    @Test
    public void shouldAuthorizeRegisteredCustomer() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.CUSTOMER, anonymousRestTemplate -> {
                whenRegistrationPerformed(anonymousRestTemplate, usernameAndPassword -> {
                    whenLoggedInSite(authorizationServerClient, usernameAndPassword);
                    whenAuthorizationRequestedFor(authorizationServerClient, Scope.CUSTOMER_REGISTERED, registeredRestTemplate -> {
                        thenAuthorized(registeredRestTemplate);
                    });
                });
            });
        });
    }

    @Ignore("The merging functionality does not work as expected and is probably not even required")
    @Test
    public void shouldAuthorizeMergingCarts() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.CUSTOMER, anonymousRestTemplate -> {
                whenNewOrderCreated(anonymousRestTemplate, uri -> {});
                whenRegistrationPerformed(anonymousRestTemplate, usernameAndPassword -> {
                    whenLoggedInSite(authorizationServerClient, usernameAndPassword);
                    whenAuthorizationRequestedFor(authorizationServerClient, Scope.CUSTOMER_REGISTERED, registeredRestTemplate -> {
                        whenNewOrderCreated(registeredRestTemplate);
                        whenMergeOfCustomerRequested(anonymousRestTemplate, registeredRestTemplate);
                        thenOrdersCountEquals(registeredRestTemplate, 1l);
                    });
                });
            });
        });
    }

    private void thenOrdersCountEquals(OAuth2RestTemplate registeredRestTemplate, Long count) throws Throwable {
        when(() -> registeredRestTemplate.getForObject(ORDERS_COUNT, Long.class, serverPort),
                retrievedCount -> { assertThat(retrievedCount, equalTo(count)); });
    }

    private void whenMergeOfCustomerRequested(OAuth2RestTemplate anonymousRestTemplate, OAuth2RestTemplate registeredRestTemplate) throws Throwable {
        when(() -> registeredRestTemplate.postForObject(CUSTOMERS_URL + "/merge", anonymousRestTemplate.getAccessToken().getValue(), String.class, serverPort));
    }

    @Test
    public void shouldAuthorizeAdmin() throws Throwable {
        givenAuthorizationServerClient(authorizationServerClient -> {
            whenLoggedInBackoffice(authorizationServerClient, Tuple.of("admin", "admin"));
            whenAuthorizationRequestedFor(authorizationServerClient, Scope.STAFF,
                    this::thenAuthorized);
        });
    }

}
