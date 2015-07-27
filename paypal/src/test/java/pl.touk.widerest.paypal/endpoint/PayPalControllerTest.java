package pl.touk.widerest.paypal.endpoint;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProviderImpl;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import pl.touk.widerest.paypal.gateway.PayPalGatewayConfigurationService;
import pl.touk.widerest.paypal.gateway.PayPalMessageConstants;
import pl.touk.widerest.paypal.gateway.PayPalSession;
import pl.touk.widerest.paypal.gateway.PayPalSessionImpl;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PayPalControllerTest.TestConfiguration.class)
public class PayPalControllerTest {

    // Given user with mocked order
    // When user sends payment request
    // Then user should be redirected
    @Test
    public void shouldUserBeRedirectedToPayPal() {
        CustomerUserDetails userDetails =
                new CustomerUserDetails(Long.valueOf(1337), "anonymous", "lolcatz", Collections.EMPTY_LIST);

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


    public class PayPalBehaviour {
        public PayPalBehaviour() {
        }
        public void userSendsPaymentRequest(UserDetails userDetails, Long orderId)  throws PaymentException {
            paymentRequestResponse = payPalController.initiate(userDetails, orderId);
        }

        public ResponseEntity paymentRequestResponse;
    }

    public class PayPalExpectations {
        public PayPalExpectations() {
        }
        public Boolean userIsRedirectedToPayPal(ResponseEntity response) {
            return response.getHeaders().getLocation().toString().contains("paypal");
        }
    }

    @Resource
    private PayPalController payPalController = new PayPalController();

    private PayPalBehaviour whenInPayPal = new PayPalBehaviour();
    private PayPalExpectations thenInPayPal = new PayPalExpectations();

    @Configuration
    @ComponentScan({"pl.touk.widerest.paypal"})
    public static class TestConfiguration {

        @Bean
        public PayPalSession payPalSession() throws PayPalRESTException {
            Map<String, String> sdkConfig = new HashMap<String, String>();
            sdkConfig.put("mode", "sandbox");
            //TODO: unhardcode credentials
            String clientIdCredential = "AQkquBDf1zctJOWGKWUEtKXm6qVhueUEMvXO_-MCI4DQQ4-LWvkDLIN2fGsd",
                    secretCredential = "EL1tVxAjhT7cJimnz5-Nsx9k2reTKSVfErNQF-CmrwJgxRtylkGTKlU4RvrX";

            String accessToken = new OAuthTokenCredential(clientIdCredential, secretCredential, sdkConfig).getAccessToken();
            PayPalSession payPalSession = new PayPalSessionImpl(accessToken);
            payPalSession.getApiContext().setConfigurationMap(sdkConfig);

            return payPalSession;
        }

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
            orderItem.setPrice(new Money(5.99));
            orderItem.setId(Long.valueOf(1));
            orderItem.setQuantity(4);
            orderItem.setOrder(preparedOrder);
            preparedOrder.addOrderItem(orderItem);

            when(orderServiceMock.findOrderById(Long.valueOf(1))).thenReturn(preparedOrder);
            return orderServiceMock;
        }


        @Bean
        public OrderToPaymentRequestDTOService blOrderToPaymentRequestDTOService() {
            //return new OrderToPaymentRequestDTOServiceImpl();
            OrderToPaymentRequestDTOService orderToPaymentRequestDTOService =
                    new Mockito().mock(OrderToPaymentRequestDTOServiceImpl.class);

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
                        .transactionTotal("25.23")
                        .shippingTotal("52.12")
                        .taxTotal("")
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

    }
}