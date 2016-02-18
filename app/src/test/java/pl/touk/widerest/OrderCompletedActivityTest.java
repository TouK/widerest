package pl.touk.widerest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.broadleafcommerce.core.checkout.service.workflow.CheckoutSeed;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.workflow.ProcessContext;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import pl.touk.widerest.api.cart.orders.converters.OrderConverter;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.Optional;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class OrderCompletedActivityTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Mock
    private OrderHooksResolver hooksResolver;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private SendwithusService sendwithusService = new SendwithusService();

    @InjectMocks
    private OrderCompletedActivity orderCompletedActivity = new OrderCompletedActivity();

    private OrderConverter orderConverter;

    @Before
    public void before() throws Exception {
        orderCompletedActivity.init();

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        orderConverter = new OrderConverter();
    }

    @After
    public void after() throws Exception {
        orderCompletedActivity.destroy();
    }

    @Test
    public void shouldHandleSmtpHook() throws Exception {

        Customer customer = mock(Customer.class);
        Order order = mock(Order.class);
        CheckoutSeed seedData = mock(CheckoutSeed.class);
        ProcessContext<CheckoutSeed> context = mock(ProcessContext.class);

        when(context.getSeedData()).thenReturn(seedData);
        when(seedData.getOrder()).thenReturn(order);
        when(order.getStatus()).thenReturn(OrderStatus.SUBMITTED);
        when(order.getCustomer()).thenReturn(customer);
        when(order.getEmailAddress()).thenReturn("tw.test@touk.pl");

        ServerSetup serverSetup = greenMail.getSmtp().getServerSetup();

        String hookUrl = UriComponentsBuilder.newInstance()
                .port(serverSetup.getPort())
                .host(serverSetup.getBindAddress())
                .scheme(serverSetup.getProtocol())
                .queryParam("to", "test@test.pl")
                .toUriString();

//        String hookUrl = "sendwithus://test_c92a0fa5bee7da24847f78d8b4e3963451d00eb5@?templateId=tem_NrXPntKhwnFjDCCbL2E8Rc";

        when(hooksResolver.getOrderCompletedHookUrl()).thenReturn(Optional.of(hookUrl));

        orderCompletedActivity.execute(context);

        assertThat(greenMail.getReceivedMessages(), arrayWithSize(1));

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        String messageBody = String.valueOf(receivedMessage.getContent());

        assertThat(
                StringUtils.chomp(messageBody),
                Matchers.equalTo(objectMapper.writeValueAsString(orderConverter.createDto(order, false)))
        );

    }

}