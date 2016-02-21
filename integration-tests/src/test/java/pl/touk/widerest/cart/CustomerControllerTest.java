package pl.touk.widerest.cart;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.Application;
import pl.touk.widerest.api.cart.service.CustomerServiceProxy;
import pl.touk.widerest.base.ApiTestBase;
import pl.touk.widerest.base.ApiTestUrls;
import pl.touk.widerest.base.ApiTestUtils;

import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class CustomerControllerTest extends ApiTestBase {

    @Resource(name="blCustomerService")
    private CustomerService customerService;

    @Resource(name = "wdCustomerService")
    private CustomerServiceProxy customerServiceProxy;


    @Test
    public void userShouldBeAbleToRegister() throws URISyntaxException {
        givenAnonymousUser();

        // Given unique username, password and unique email
        String username = RandomStringUtils.random(32, "haskellCurry");
        String password = "uncurry";
        String email = RandomStringUtils.random(32, "haskellCurry") + "@curry.org";

        customerBehaviour.whenUserTriesToRegister(username, password, email);

        customerExpectations.userShouldBeRegistered(username, password, email);

    }


    private Pair<RestTemplate, String> userCredentials;
    private HttpHeaders httpRequestHeader = new HttpHeaders();
    private ResponseEntity<HttpHeaders> response = null;

    private CustomerBehaviour customerBehaviour = new CustomerBehaviour();
    private CustomerExpectations customerExpectations = new CustomerExpectations();

    private class CustomerBehaviour {
        public CustomerBehaviour() {
        }

        public void whenUserTriesToRegister(final String name, final String password, final String email) {
            RestTemplate restTemplate = userCredentials.getKey();
            String token = userCredentials.getValue();
            httpRequestHeader.set("Accept", MediaTypes.HAL_JSON_VALUE);
            httpRequestHeader.set("Authorization", "Bearer " + token);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
            map.add("email", email);
            map.add("username", name);
            map.add("password", password);
            map.add("passwordConfirm", password);

            HttpEntity httpRequestEntity = new HttpEntity(map, httpRequestHeader);

            response =
                    restTemplate.exchange(ApiTestUrls.CUSTOMERS_URL+"/register",
                            HttpMethod.POST, httpRequestEntity, HttpHeaders.class, serverPort);
        }

    }

    private class CustomerExpectations {
        public CustomerExpectations() {
        }

        public void userShouldBeRegistered(final String username, final String password, final String email) {
            // Read user details server-side
            final Customer remoteCustomer =
                customerService.readCustomerByUsername(username);

            assertTrue(remoteCustomer.isRegistered());
            assertThat(remoteCustomer.getEmailAddress(), is(email));

            PasswordEncoder encoder = new BCryptPasswordEncoder();
            assertTrue(encoder.matches(password, remoteCustomer.getPassword()));

        }
    }

    private void givenAnonymousUser() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        URI FirstResponseUri = restTemplate.postForLocation(ApiTestUrls.OAUTH_AUTHORIZATION, null, serverPort);
        userCredentials = Pair.of(restTemplate, ApiTestUtils.strapTokenFromURI(FirstResponseUri));
    }


}

