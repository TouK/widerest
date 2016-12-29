package pl.touk.widerest.api.orders.payments;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.broadleafcommerce.common.payment.PaymentGatewayType;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.payment.service.PaymentGatewayCustomerService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayHostedService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransparentRedirectService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.common.ResourceNotFoundException;
import pl.touk.widerest.security.oauth2.ResourceServerConfig;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Optional;
import java.util.function.Predicate;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/orders/{orderId}/payment", produces = { MediaTypes.HAL_JSON_VALUE })
@Api(value = "orders", description = "Order payments endpoint", produces = MediaTypes.HAL_JSON_VALUE)
@Slf4j
public class PaymentController {

    @Resource(name = "blOrderService")
    private OrderService orderService;

    @Resource(name = "blOrderToPaymentRequestDTOService")
    private OrderToPaymentRequestDTOService orderToPaymentRequestDTOService;

    @Resource(name = "blPaymentGatewayConfigurationServiceProvider")
    private PaymentGatewayConfigurationServiceProvider paymentGatewayConfigurationServiceProvider;

    private TypeIdResolver paymentTypeIdResolver;

    @Autowired
    public void initPaymentTypeIdResolver(ObjectMapper objectMapper) throws JsonMappingException {
        SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
        JavaType javaType = objectMapper.getTypeFactory().constructType(PaymentDto.class);
        paymentTypeIdResolver = objectMapper.getSerializerFactory().createTypeSerializer(serializationConfig, javaType).getTypeIdResolver();
    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(
            value = "Initiate order payment execution using the given payment provider",
            notes = "Initiates for one chosen order",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Redirects to checkout website"),
            @ApiResponse(code = 403, message = "Access denied to given order")
            // and throws
    })
    @Transactional
    public ResponseEntity initiatePayment(
            @Valid @RequestBody PaymentDto paymentDto,
            @ApiIgnore @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
            @PathVariable(value = "orderId") Long orderId
    ) {

        final Order order = Optional.ofNullable(orderService.findOrderById(orderId))
                .filter(notYetSubmitted)
                .orElseThrow(() -> new ResourceNotFoundException());

        if (!order.getCustomer().getId().equals(customerUserDetails.getId())) {
            throw new AccessDeniedException("The ordere does not belong to the customer");
        }

//        orderValidationService.validateOrderBeforeCheckout(order);

        final PaymentRequestDTO paymentRequestDTO =
                orderToPaymentRequestDTOService.translateOrder(order)
                        .additionalField("PAYMENT_DETAILS", paymentDto);
        populateLineItemsAndSubscriptions(order, paymentRequestDTO);

        PaymentGatewayConfigurationService configurationService = findPaymentGatewayConfigurationService(paymentDto);

        PaymentGatewayHostedService hostedService = configurationService.getHostedService();
        PaymentGatewayTransparentRedirectService transparentRedirectService = configurationService.getTransparentRedirectService();
        PaymentGatewayCustomerService customerService = configurationService.getCustomerService();

        try {
            if (customerService != null) {
                customerService.createGatewayCustomer(paymentRequestDTO);
            }

            if (hostedService != null) {
                return ResponseEntity.ok(hostedService.requestHostedEndpoint(paymentRequestDTO).getResponseMap());
            }
            if (transparentRedirectService != null) {
                return ResponseEntity.ok(transparentRedirectService.createAuthorizeForm(paymentRequestDTO).getResponseMap());
            }
        } catch (PaymentException e) {
            log.error("Error while initiating payment", e);
        }

        return ResponseEntity.unprocessableEntity().build();
    }

    private PaymentRequestDTO populateLineItemsAndSubscriptions(final Order order, final PaymentRequestDTO
            paymentRequest) {
        for (OrderItem item : order.getOrderItems()) {

            /* (mst) Previously, there was SKU's Description used here to set item's name
                    but because it is not required in our implementation, I chose to use SKU's Name instead */

            final String name = Match(item).of(
                    Case(instanceOf(BundleOrderItem.class), it -> it.getSku().getName()),
                    Case(instanceOf(DiscreteOrderItem.class), it -> it.getSku().getName()),
                    Case($(), OrderItem::getName)
            );

            final String category = Optional.ofNullable(item.getCategory())
                    .map(Category::getName)
                    .orElse(null);

            paymentRequest.lineItem()
                    .name(name)
                    .amount(String.valueOf(item.getAveragePrice()))
                    .category(category)
                    .quantity(String.valueOf(item.getQuantity()))
                    .total(order.getTotal().toString())
                    .done();
        }

        return paymentRequest;
    }

    private PaymentGatewayConfigurationService findPaymentGatewayConfigurationService(PaymentDto paymentDto) {

        final String provider = paymentTypeIdResolver.idFromValue(paymentDto);
        final PaymentGatewayType paymentGatewayType = PaymentGatewayType.getInstance(provider);

        return paymentGatewayConfigurationServiceProvider.getGatewayConfigurationService(
                paymentGatewayType
        );
    }


    private static Predicate<Order> notYetSubmitted = order -> !order.getStatus().equals(OrderStatus.SUBMITTED);

}
