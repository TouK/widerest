package pl.touk.widerest.paypal.endpoint;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.HttpMethod;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;
import org.broadleafcommerce.common.cache.StatisticsService;
import org.broadleafcommerce.common.cache.StatisticsServiceImpl;
import org.broadleafcommerce.common.config.dao.SystemPropertiesDao;
import org.broadleafcommerce.common.config.dao.SystemPropertiesDaoImpl;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrencyImpl;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProviderImpl;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.checkout.service.CheckoutService;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.domain.OrderItemImpl;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.FulfillmentGroupServiceImpl;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOService;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOServiceImpl;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.paypal.gateway.PayPalGatewayConfigurationService;
import pl.touk.widerest.paypal.gateway.PayPalMessageConstants;
import pl.touk.widerest.paypal.gateway.PayPalSession;
import pl.touk.widerest.paypal.gateway.PayPalSessionImpl;
import pl.touk.widerest.paypal.service.SystemProperitesServiceProxy;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PayPalControllerTest.TestConfiguration.class)
public class PayPalControllerTest {

    @Before
    public void setUp() {
        userDetails =
                new CustomerUserDetails(Long.valueOf(1337), "anonymous", "lolcatz", Collections.EMPTY_LIST);
    }

    // Given user with mocked order
    // When user sends payment request
    // Then user should be redirected
    @Test
    public void shouldUserBeRedirectedToPayPal() {

        try {
            whenInPayPal.userSendsPaymentRequest(userDetails, Long.valueOf(1));
        } catch (PaymentException e) {
            // Could not send payment
            assert(false);
        }

        assert(
            thenInPayPal.userIsRedirectedToPayPal(whenInPayPal.paymentRequestResponse)
        );

    }

    @Test
    public void shouldUserReceiveNothing() {
        try {
            whenInPayPal.userEntersReturnPageWithNoArgs(userDetails, Long.valueOf(1));
        } catch (Exception e) {
            // Sth went wrong
            e.printStackTrace();
            assert(false);
        }

        assert(
            thenInPayPal.userDoesntReceiveAnythingSpecial(whenInPayPal.returnRequestResponse)
        );
    }


    public class PayPalBehaviour {
        public PayPalBehaviour() {
        }

        public void userSendsPaymentRequest(UserDetails userDetails, Long orderId)  throws PaymentException {
            HttpServletRequest httpServletRequest = new MockHttpServletRequest("GET",
                    "http://localhost:8080/orders/1/paypal/");
            paymentRequestResponse = payPalController.initiate(httpServletRequest, userDetails, orderId);
        }

        public void userEntersReturnPageWithNoArgs(UserDetails userDetails, Long orderId) throws PaymentException, CheckoutException {
            // Przykladowe: http://localhost:8080/orders/1/paypal/return?paymentId=PAY-1RG403957J192763EKW3DDSY&token=EC-2V96560140856305R&PayerID=FXHKFGTPBJR4J
            // http://localhost:8080/orders/1/paypal/return?paymentId=PAY-7EE74569FE911601VKW3X7QQ&token=EC-2KE661416F427612K&PayerID=FXHKFGTPBJR4J
            HttpServletRequest httpServletRequest = new MockHttpServletRequest("GET",
                    "http://localhost:8080/orders/1/paypal/return");

            returnRequestResponse = payPalController.handleReturn(httpServletRequest, userDetails, orderId);
        }

        public ResponseEntity paymentRequestResponse;
        public ResponseEntity returnRequestResponse;
    }

    public class PayPalExpectations {
        public PayPalExpectations() {
        }

        public Boolean userIsRedirectedToPayPal(ResponseEntity response) {
            return response.getHeaders().getLocation().toString().contains("paypal");
        }

        public Boolean userDoesntReceiveAnythingSpecial(ResponseEntity response) {
            return response.getStatusCode().value() == 404;
        }
    }

    @Resource
    private PayPalController payPalController = new PayPalController();

    private PayPalBehaviour whenInPayPal = new PayPalBehaviour();
    private PayPalExpectations thenInPayPal = new PayPalExpectations();

    private CustomerUserDetails userDetails;

    @Configuration
    @IntegrationTest(
            {
                    "paypal.clientId:EBWKjlELKMYqRNQ6sYvFo64FtaRLRR5BdHEESmha49TM",
                    "paypal.secret:EO422dn3gQLgDbuwqTjzrFgFtaRLRR5BdHEESmha49TM"
            })
    @ComponentScan({"pl.touk.widerest.paypal"})
    public static class TestConfiguration {

//        @Bean
//        public PayPalSession payPalSession() throws PayPalRESTException {
//
//            return new PayPalSessionImpl(clientIdCredential, secretCredential);
//        }

        @Bean(name = "blOrderService")
        public OrderService orderService() {
            OrderService orderServiceMock = Mockito.mock(OrderService.class);
            Order preparedOrder = new OrderImpl();
            Customer customer = new CustomerImpl();
            customer.setId(Long.valueOf(1337));
            preparedOrder.setCustomer(customer);
            preparedOrder.setId(Long.valueOf(1));

            OrderItem orderItem = new OrderItemImpl();
            orderItem.setName("Kanapka");
            orderItem.setPrice(new Money(6.0));
            orderItem.setId(Long.valueOf(1));
            orderItem.setQuantity(4);
            orderItem.setOrder(preparedOrder);
            preparedOrder.addOrderItem(orderItem);
            BroadleafCurrencyImpl currency = new BroadleafCurrencyImpl();
            currency.setCurrencyCode("USD");
            preparedOrder.setCurrency(currency);
            preparedOrder.setTotal(new Money(24));

            when(orderServiceMock.findOrderById(Long.valueOf(1))).thenReturn(preparedOrder);
            return orderServiceMock;
        }

        @Bean(name="blCheckoutService")
        public CheckoutService checkoutService() throws CheckoutException {
            CheckoutService checkoutServiceMock = Mockito.mock(CheckoutService.class);

            when(checkoutServiceMock.performCheckout(anyObject())).thenReturn(null);
            return checkoutServiceMock;
        }


        @Bean
        public OrderToPaymentRequestDTOService blOrderToPaymentRequestDTOService() {
            //return new OrderToPaymentRequestDTOServiceImpl();
            OrderToPaymentRequestDTOService orderToPaymentRequestDTOService =
                    Mockito.mock(OrderToPaymentRequestDTOServiceImpl.class);

            PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO()
                    .orderId("1337")
                    .customer()
                        .customerId("1")
                        .firstName("Boguslaw")
                        .lastName("Miroslawski")
                        .email("bogus@miro.pl")
                        .phone("+48900880700")
                        .done()
                    .shipTo()
                        .addressFirstName("Asdf")
                        .addressLastName("Gsadf")
                        .addressCompanyName("")
                        .addressLine1("al. Bohaterow Wrzesnia 8 3/4")
                        .addressLine2("")
                        .addressCityLocality("Warszawa")
                        .addressStateRegion("")
                        .addressPostalCode("02-110")
                        .addressCountryCode("PL")
                        .addressPhone("+48700100200")
                        .addressEmail("mail@mail.pl")
                    .done()
                    .billTo()
                        .addressFirstName("Asdf")
                        .addressLastName("Gsadf")
                        .addressCompanyName("")
                        .addressLine1("al. Bohaterow Wrzesnia 8 3/4")
                        .addressLine2("")
                        .addressCityLocality("Warszawa")
                        .addressStateRegion("")
                        .addressPostalCode("02-110")
                        .addressCountryCode("PL")
                        .addressPhone("+48700100200")
                        .addressEmail("mail@mail.pl")
                    .done()
                        .transactionTotal("24.00")
                        .shippingTotal("0.0")
                        .taxTotal("0.0")
                        .orderCurrencyCode("USD")
                    ;

            when(orderToPaymentRequestDTOService.translateOrder(anyObject())).thenReturn(paymentRequestDTO);

            return orderToPaymentRequestDTOService;
        }

        @Bean(name = "blFulfillmentGroupService")
        public FulfillmentGroupService fgService() {
            return mock(FulfillmentGroupService.class);
//            return new FulfillmentGroupServiceImpl();
        }

        @Bean(name = "blPaymentGatewayConfigurationServiceProvider")
        public PaymentGatewayConfigurationServiceProvider paymentGatewayConfigurationServiceProvider() {
            return new PaymentGatewayConfigurationServiceProviderImpl();
            //return new Mockito().mock(PaymentGatewayConfigurationServiceProvider.class);
        }

        @Bean(name = "blPaymentGatewayConfigurationServices")
        public PaymentGatewayConfigurationService paymentGatewayConfigurationService() {
            return new PayPalGatewayConfigurationService();
            //return new Mockito().mock(PaymentGatewayConfigurationService.class);
        }

        @Bean(name = "blSystemPropertiesDao")
        public SystemPropertiesDao systemPropertiesDao() {
            return new SystemPropertiesDaoImpl();
        }

        @Bean(name = "wdSystemProperties")
        public SystemProperitesServiceProxy systemProperitesServiceProxy() {
            return new SystemProperitesServiceProxy();
        }

    }
}