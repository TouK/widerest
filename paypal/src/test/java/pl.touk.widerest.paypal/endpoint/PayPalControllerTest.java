package pl.touk.widerest.paypal.endpoint;

import com.paypal.base.rest.APIContext;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProviderImpl;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.domain.OrderItemImpl;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
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
import pl.touk.widerest.paypal.gateway.PayPalSession;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;

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
            assert(true == false);
        }

        thenInPayPal.userIsRedirectedToPayPal(whenInPayPal.paymentRequestResponse);

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

    private PayPalController payPalController;
    private PayPalBehaviour whenInPayPal;
    private PayPalExpectations thenInPayPal;

    @Configuration
    @ComponentScan({"pl.touk.widerest.paypal"})
    public static class TestConfiguration {

        @Bean
        public PayPalSession payPalSession() {
            return new PayPalSession() {
                @Override
                public APIContext getApiContext() {
                    return null;
                }
            };
        }

        @Bean(name = "blOrderService")
        public OrderService orderService() {
            OrderService orderServiceMock = Mockito.mock(OrderService.class);
            Order preparedOrder = new OrderImpl();
            Customer customer = new CustomerImpl();
            customer.setId(Long.valueOf(1337));
            preparedOrder.setCustomer(customer);

            OrderItem orderItem = new OrderItemImpl();
            orderItem.setName("Kanapka");
            orderItem.setPrice(new Money(5.99));
            orderItem.setId(Long.valueOf(1));
            orderItem.setQuantity(4);
            preparedOrder.addOrderItem(orderItem);

            when(orderServiceMock.findOrderById(Long.valueOf(1))).thenReturn(preparedOrder);
            return orderServiceMock;
        }


        @Bean
        public OrderToPaymentRequestDTOService blOrderToPaymentRequestDTOService() {
            return new OrderToPaymentRequestDTOServiceImpl();
        }

        @Bean(name = "blFulfillmentGroupService")
        public FulfillmentGroupService fgService() {
            return mock(FulfillmentGroupService.class);
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