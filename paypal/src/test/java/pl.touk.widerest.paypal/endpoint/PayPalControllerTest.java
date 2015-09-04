package pl.touk.widerest.paypal.endpoint;

import org.broadleafcommerce.common.cache.StatisticsService;
import org.broadleafcommerce.common.cache.StatisticsServiceImpl;
import org.broadleafcommerce.common.config.dao.ModuleConfigurationDao;
import org.broadleafcommerce.common.config.dao.ModuleConfigurationDaoImpl;
import org.broadleafcommerce.common.config.dao.SystemPropertiesDao;
import org.broadleafcommerce.common.config.dao.SystemPropertiesDaoImpl;
import org.broadleafcommerce.common.config.service.ModuleConfigurationService;
import org.broadleafcommerce.common.config.service.ModuleConfigurationServiceImpl;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrencyImpl;
import org.broadleafcommerce.common.i18n.dao.ISODao;
import org.broadleafcommerce.common.i18n.dao.ISODaoImpl;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.broadleafcommerce.common.i18n.service.ISOServiceImpl;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayCheckoutService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProviderImpl;
import org.broadleafcommerce.common.persistence.EntityConfiguration;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.checkout.service.CheckoutService;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.order.domain.*;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.type.FulfillmentType;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.payment.dao.OrderPaymentDao;
import org.broadleafcommerce.core.payment.dao.OrderPaymentDaoImpl;
import org.broadleafcommerce.core.payment.service.*;
import org.broadleafcommerce.profile.core.dao.*;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerImpl;
import org.broadleafcommerce.profile.core.service.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import pl.touk.widerest.paypal.gateway.PayPalGatewayConfigurationService;
import pl.touk.widerest.paypal.service.SystemPropertiesServiceProxy;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PayPalControllerTest.TestConfiguration.class)
public class PayPalControllerTest {

    @Before
    public void setUpUserDetails() {
        userDetails =
                new CustomerUserDetails(1337L, "anonymous", "lolcatz", Collections.EMPTY_LIST);
    }

    // Given user with mocked order
    // When user sends payment request
    // Then user should be redirected
    @Test
    public void shouldUserBeRedirectedToPayPal() throws PaymentException {

        whenInPayPal.userSendsPaymentRequest(userDetails, 1L);

        assert(
            thenInPayPal.userIsRedirectedToPayPal(whenInPayPal.paymentRequestResponse)
        );

    }

    @Test
    public void shouldUserReceiveNothing() throws PaymentException, CheckoutException {

        whenInPayPal.userEntersReturnPageWithNoArgs(userDetails, 1L);

        assert(
            thenInPayPal.userDoesntGetRedirected(whenInPayPal.returnRequestResponse)
        );
    }


    public class PayPalBehaviour {
        public PayPalBehaviour() {
        }

        public void userSendsPaymentRequest(UserDetails userDetails, Long orderId)  throws PaymentException {
            HttpServletRequest httpServletRequest = new MockHttpServletRequest("GET",
                    "/orders/1/paypal/");
            paymentRequestResponse = payPalController.initiate(httpServletRequest, userDetails, orderId);
        }

        public void userEntersReturnPageWithNoArgs(UserDetails userDetails, Long orderId) throws PaymentException, CheckoutException {
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

        public Boolean userDoesntGetRedirected(ResponseEntity response) {
            // Currently, due to Swagger limitations, the redirect url is returned in headers
            // with status code 202
            return !response.getStatusCode().is2xxSuccessful();
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
                    "paypal.secret:EO422dn3gQLgDbuwqTjzrFgFtaRLRR5BdHEESmha49TM",
            })
    @PropertySource("classpath:test.properties")
    @ComponentScan({"pl.touk.widerest.paypal"})
    public static class TestConfiguration {

        @Bean(name = "blOrderService")
        public OrderService orderService() {
            OrderService orderServiceMock = mock(OrderService.class);
            Order preparedOrder = new OrderImpl();
            Customer customer = new CustomerImpl();
            customer.setId(1337L);
            preparedOrder.setCustomer(customer);
            preparedOrder.setId(1L);

            OrderItem orderItem = new OrderItemImpl();
            orderItem.setName("Kanapka");
            orderItem.setPrice(new Money(6.0));
            orderItem.setId(1L);
            orderItem.setQuantity(4);
            orderItem.setOrder(preparedOrder);
            preparedOrder.addOrderItem(orderItem);
            BroadleafCurrencyImpl currency = new BroadleafCurrencyImpl();
            currency.setCurrencyCode("USD");
            preparedOrder.setCurrency(currency);
            //preparedOrder.setTotal(new Money(24));
            preparedOrder.setStatus(OrderStatus.IN_PROCESS);
            preparedOrder.setTotalFulfillmentCharges(new Money(14.99));
            preparedOrder.setSubTotal(new Money(4 * 6.0));
            preparedOrder.setTotalTax(new Money(0));
            preparedOrder.setTotal(new Money(38.99));

            Customer preparedCustomer = new CustomerImpl();
            preparedCustomer.setId(1337L);
            preparedOrder.setCustomer(preparedCustomer);

            when(orderServiceMock.findOrderById(1L)).thenReturn(preparedOrder);
            return orderServiceMock;
        }

        @Bean(name="blCheckoutService")
        public CheckoutService checkoutService() throws CheckoutException {
            CheckoutService checkoutServiceMock = mock(CheckoutService.class);

            when(checkoutServiceMock.performCheckout(anyObject())).thenReturn(null);
            return checkoutServiceMock;
        }


        @Bean
        public OrderToPaymentRequestDTOService blOrderToPaymentRequestDTOService() {
            //return new OrderToPaymentRequestDTOServiceImpl();
            OrderToPaymentRequestDTOService orderToPaymentRequestDTOService =
                    mock(OrderToPaymentRequestDTOServiceImpl.class);

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
            //fulfillmentGroupService.getFirstShippableFulfillmentGroup(order).getFulfillmentOption() != null
            FulfillmentGroupService fulfillmentGroupService =
                    mock(FulfillmentGroupService.class);
            FulfillmentOption fulfillmentOption = new FulfillmentOptionImpl();
            fulfillmentOption.setFulfillmentType(FulfillmentType.PHYSICAL_SHIP);

            FulfillmentGroup fulfillmentGroup = new FulfillmentGroupImpl();
            fulfillmentGroup.setFulfillmentOption(fulfillmentOption);
            fulfillmentGroup.setTotal(new Money(14.99));

            when(fulfillmentGroupService.getFirstShippableFulfillmentGroup(anyObject()))
                    .thenReturn(fulfillmentGroup);
            return fulfillmentGroupService;
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

        @Bean(name = "wdSystemProperties")
        public SystemPropertiesServiceProxy systemProperitesServiceProxy() {
            return mock(SystemPropertiesServiceProxy.class);
        }

        @Bean(name = "blStatisticsService")
        public StatisticsService statisticsService() {
            return Mockito.mock(StatisticsServiceImpl.class);
        }
//
        @Bean(name = "blSystemPropertiesDao")
        public SystemPropertiesDao systemPropertiesDao() {
            return Mockito.mock(SystemPropertiesDaoImpl.class);
        }

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
            PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer =
                    new PropertySourcesPlaceholderConfigurer();
            propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
            return propertySourcesPlaceholderConfigurer;
        }

        @Bean(name = "blEntityConfiguration")
        public EntityConfiguration entityConfiguration() {
            return mock(EntityConfiguration.class);
        }

        @Bean(name = "blMergedEntityContexts")
        public ListFactoryBean listFactoryBean() {
            return mock(ListFactoryBean.class);
        }

        @Bean(name="blPaymentGatewayCheckoutService")
        public PaymentGatewayCheckoutService paymentGatewayCheckoutService() {
            return new DefaultPaymentGatewayCheckoutService();
        }

        @Bean(name="blOrderPaymentService")
        public OrderPaymentService orderPaymentService() {
            return new OrderPaymentServiceImpl();
        }
        @Bean(name="blOrderPaymentDao")
        public OrderPaymentDao orderPaymentDao() {
            return new OrderPaymentDaoImpl();
        }
        @Bean(name="blAddressService")
        public AddressService addressService() {
            return new AddressServiceImpl();
        }
        @Bean(name="blAddressDao")
        public AddressDao addressDao() {
            return new AddressDaoImpl();
        }
        @Bean(name="blModuleConfigurationService")
        public ModuleConfigurationService moduleConfigurationService() {
            return new ModuleConfigurationServiceImpl();
        }
        @Bean(name="blModuleConfigurationDao")
        public ModuleConfigurationDao moduleConfigurationDao() {
            return new ModuleConfigurationDaoImpl();
        }
        @Bean(name="blAddressVerificationProviders")
        public AddressVerificationProvider addressVerificationProvider() {
            return mock(AddressVerificationProvider.class);
        }
        @Bean(name="blStateService")
        public StateService stateService() {
            return new StateServiceImpl();
        }
        @Bean(name="blStateDao")
        public StateDao stateDao() {
            return new StateDaoImpl();
        }
        @Bean(name="blCountryService")
        public CountryService countryService() {
            return new CountryServiceImpl();
        }
        @Bean(name="blCountryDao")
        public CountryDao countryDao() {
            return new CountryDaoImpl();
        }
        @Bean(name="blISOService")
        public ISOService isoService() {
            return new ISOServiceImpl();
        }
        @Bean(name="blISODao")
        public ISODao isoDao() {
            return new ISODaoImpl();
        }
        @Bean(name="blPhoneService")
        public PhoneService phoneService() {
            return new PhoneServiceImpl();
        }
        @Bean(name="blPhoneDao")
        public PhoneDao phoneDao() {
            return new PhoneDaoImpl();
        }

//        @Bean(name = "wdPayPalSession")
//        public PayPalSession payPalSession() {
//            return mock(PayPalSessionImpl.class);
//        }

    }
}