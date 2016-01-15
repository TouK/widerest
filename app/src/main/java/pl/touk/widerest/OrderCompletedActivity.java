package pl.touk.widerest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.sendwithus.exception.SendWithUsException;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.broadleafcommerce.core.checkout.service.workflow.CheckoutSeed;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.workflow.BaseActivity;
import org.broadleafcommerce.core.workflow.ProcessContext;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import pl.touk.widerest.api.DtoConverters;
import pl.touk.widerest.api.cart.dto.OrderDto;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.api.settings.SettingsService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by mst on 08.10.15.
 */
public class OrderCompletedActivity extends BaseActivity<ProcessContext<CheckoutSeed>> implements SettingsConsumer {

    private static final String ORDER_COMPLETED_HOOK = "orderCompletedHook";

    @Resource
    ObjectMapper objectMapper;

    @Resource
    SendwithusService sendwithusService;

    CamelContext camelContext;

    ProducerTemplate template;

    protected SettingsService settingsService;

    @PostConstruct
    public void init() throws Exception {
        camelContext = new DefaultCamelContext();
        camelContext.start();
        template = new DefaultProducerTemplate(camelContext);
        template.start();
    }

    @PreDestroy
    public void destroy() throws Exception {
        template.stop();
        camelContext.stop();
    }

    @Override
    public ProcessContext<CheckoutSeed> execute(ProcessContext<CheckoutSeed> context) throws Exception {
        final Order order = context.getSeedData().getOrder();

        if(order.getStatus() != OrderStatus.SUBMITTED) {
            return context;
        }

        Optional<String> orderCompletedHookUrl = settingsService.getProperty(ORDER_COMPLETED_HOOK);

        if(orderCompletedHookUrl.isPresent()) {
            sendOrderToHook(orderCompletedHookUrl.get(), order);
        };

        return context;
    }

    private void sendOrderToHook(String hookUrl, Order order) throws JsonProcessingException, SendWithUsException {

        OrderDto dto = DtoConverters.orderEntityToDto.apply(order);

        UriComponents hookUriComponents = UriComponentsBuilder.fromUriString(hookUrl).build();

        if ("sendwithus".equals(hookUriComponents.getScheme())) {

            if (!hookUriComponents.getQueryParams().containsKey("to")) {
                hookUriComponents = UriComponentsBuilder.newInstance().uriComponents(hookUriComponents).queryParam("to", order.getEmailAddress()).build();
            }
            ;
            sendwithusService.sendOrder(hookUriComponents, objectMapper.treeToValue(objectMapper.valueToTree(dto), Map.class));

        } else {
            template.sendBodyAndHeader(hookUrl, objectMapper.writeValueAsString(dto), Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);

        }
    }

    @Override
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public Set<String> getHandledProperties() {
        return Sets.newHashSet(ORDER_COMPLETED_HOOK);
    }

}
