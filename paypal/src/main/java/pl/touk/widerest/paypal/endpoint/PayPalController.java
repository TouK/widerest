package pl.touk.widerest.paypal.endpoint;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.payment.service.PaymentGatewayHostedService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOService;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.paypal.gateway.PayPalMessageConstants;
import pl.touk.widerest.paypal.gateway.PayPalPaymentGatewayType;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.swing.text.html.Option;
import java.util.Optional;

@Controller
@ResponseBody
@RequestMapping("/orders/{id}/paypal")
public class PayPalController {

    @Resource(name = "blOrderToPaymentRequestDTOService")
    private OrderToPaymentRequestDTOService orderToPaymentRequestDTOService;

    @Resource(name = "blPaymentGatewayConfigurationServiceProvider")
    private PaymentGatewayConfigurationServiceProvider paymentGatewayConfigurationServiceProvider;

    @Resource(name = "blOrderService")
    private OrderService orderService;

    private PaymentGatewayConfigurationService configurationService;
    private PaymentGatewayHostedService hostedService;

    @PostConstruct
    public void afterPropertiesSet() {
        configurationService = paymentGatewayConfigurationServiceProvider.getGatewayConfigurationService(PayPalPaymentGatewayType.PAYPAL);
        hostedService = configurationService.getHostedService();
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity initiate(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) throws PaymentException {

        if(userDetails instanceof AdminUserDetails) {
            throw new PaymentException("Admins can't make orders");
        }
        CustomerUserDetails customerUserDetails = (CustomerUserDetails)userDetails;

        // find order
        Order order = Optional.ofNullable(orderService.findOrderById(orderId))
                .orElseThrow(() -> new ResourceNotFoundException(""));

        if(!order.getCustomer().getId().equals(customerUserDetails.getId())) {
            throw new IllegalAccessError("Access Denied");
        }

        String returnUrl = "http://return";
        String cancelUrl = "http://cancel";

        PaymentRequestDTO paymentRequest =
                orderToPaymentRequestDTOService.translateOrder(order)
                        .additionalField(PayPalMessageConstants.RETURN_URL, returnUrl)
                        .additionalField(PayPalMessageConstants.CANCEL_URL, cancelUrl)
                        .orderDescription("TODO");
        populateLineItemsAndSubscriptions(order, paymentRequest);

        PaymentResponseDTO paymentResponse = hostedService.requestHostedEndpoint(paymentRequest);

        //return redirect URI from the paymentResponse

        String redirectURI = Optional.ofNullable(paymentResponse.getResponseMap().get(PayPalMessageConstants.REDIRECT_URL))
                .orElseThrow(() -> new ResourceNotFoundException(""));

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.setLocation(ServletUriComponentsBuilder.fromHttpUrl(redirectURI)
                        .build().toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.I_AM_A_TEAPOT);

        //return ResponseEntity.notFound().build();

    }

    @RequestMapping(value = "/return", method = RequestMethod.GET)
    public ResponseEntity handleReturn(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) {

        // call checkout workflow
        // handle no funds failures

        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/cancel", method = RequestMethod.GET)
    public ResponseEntity handleCancel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) {

        return ResponseEntity.notFound().build();

    }

    private void populateLineItemsAndSubscriptions(Order order, PaymentRequestDTO paymentRequest) {
        for (OrderItem item : order.getOrderItems()) {
            String name;
            if (item instanceof BundleOrderItem) {
                name = ((BundleOrderItem) item).getSku().getDescription();
            } else if (item instanceof DiscreteOrderItem) {
                name = ((DiscreteOrderItem) item).getSku().getDescription();
            } else {
                name = item.getName();
            }
            String category = item.getCategory() == null ? null : StringUtils.substringBefore(item.getCategory().getFulfillmentType().getType(), "_");
            paymentRequest
                    .lineItem()
                    .name(name)
                    .amount(String.valueOf(item.getTotalPrice().getAmount()))
                    .category(category)
                    .done();
        }
    }



}