package pl.touk.widerest.paypal.endpoint;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayCheckoutService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfigurationServiceProvider;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.checkout.service.CheckoutService;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.paypal.gateway.PayPalMessageConstants;
import pl.touk.widerest.paypal.gateway.PayPalPaymentGatewayType;
import pl.touk.widerest.paypal.gateway.PayPalRequestDto;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Controller
@ResponseBody
@RequestMapping(value = "/v1" + "/orders/{id}/paypal")
@Api(value = "paypal", description = "PayPal payment endpoint")
public class PayPalController {

    @Resource(name = "blPaymentGatewayConfigurationServiceProvider")
    private PaymentGatewayConfigurationServiceProvider paymentGatewayConfigurationServiceProvider;

    @Resource(name = "blOrderService")
    private OrderService orderService;

    @Resource(name = "blCheckoutService")
    protected CheckoutService checkoutService;

    @Resource(name = "blFulfillmentGroupService")
    private FulfillmentGroupService fulfillmentGroupService;

    @Resource(name = "blPaymentGatewayCheckoutService")
    private PaymentGatewayCheckoutService paymentGatewayCheckoutService;

    private PaymentGatewayConfigurationService configurationService;

    @PostConstruct
    public void afterPropertiesSet() {
        configurationService = paymentGatewayConfigurationServiceProvider.getGatewayConfigurationService(PayPalPaymentGatewayType.PAYPAL);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "/return", method = RequestMethod.GET)
    @ApiOperation(
            value = "Return endpoint for PayPal",
            notes = "User gets redirected here after successful logging and payment",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 303, message = "Redirects to homepage after payment execution")
    })
    public ResponseEntity handleReturn(
            HttpServletRequest request,
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) throws PaymentException, CheckoutException {


        // call checkout workflow
        // handle no funds failures

        if(userDetails instanceof AdminUserDetails) {
            throw new PaymentException("Admins can't make orders");
        }

        CustomerUserDetails customerUserDetails = (CustomerUserDetails)userDetails;

        Order order = Optional.ofNullable(orderService.findOrderById(orderId))
                .filter(PayPalController::archivedOrderFilter)
                .orElseThrow(() -> new ResourceNotFoundException(""));
        if(!order.getCustomer().getId().equals(customerUserDetails.getId())) {
            throw new IllegalAccessError("Access Denied");
        }

        // get data from link
        request.setAttribute(PayPalMessageConstants.QUERY_ORDER_ID, orderId);
        PaymentResponseDTO payPalResponse = configurationService.getWebResponseService().translateWebResponse(request);


        // create orderpayment
        try {
            paymentGatewayCheckoutService.applyPaymentToOrder(payPalResponse, configurationService.getConfiguration());
        } catch (Exception e){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }


        // strange if it ever happens
        if(payPalResponse.getOrderId() != null && Long.valueOf(payPalResponse.getOrderId()) != order.getId()) {
            throw new IllegalAccessError("Wrong request");
        }

        // create request
        PayPalRequestDto requestDTO = new PayPalRequestDto();
        requestDTO.setOrderId(orderId.toString());
        requestDTO.setPayerId(payPalResponse.getResponseMap().get(PayPalMessageConstants.PAYER_ID));
        requestDTO.setPaymentId(payPalResponse.getResponseMap().get(PayPalMessageConstants.PAYMENT_ID));
        requestDTO.setAccessToken(payPalResponse.getResponseMap().get(PayPalMessageConstants.ACCESS_TOKEN));
        requestDTO.getWrapped().transactionTotal(order.getTotal().toString());
        requestDTO.getWrapped().shippingTotal(order.getTotalShipping().toString());
        requestDTO.getWrapped().orderSubtotal(order.getSubTotal().toString());
        requestDTO.getWrapped().taxTotal(order.getTotalTax().toString());
        requestDTO.getWrapped().orderCurrencyCode(order.getCurrency().getCurrencyCode());


        // Checkout/execute order in broadleaf

        try {
            //CheckoutResponse checkoutResponse =
            checkoutService.performCheckout(order);
        } catch (CheckoutException e) {
            //e.printStackTrace();
            //throw e;
            return new ResponseEntity<>(e.getMessage(), HttpStatus.METHOD_NOT_ALLOWED);
        }


        // After success - redirect to main page
        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.setLocation(ServletUriComponentsBuilder.fromHttpUrl(strapRootURL(request.getRequestURL().toString()))
                .build().toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.SEE_OTHER);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "/cancel", method = RequestMethod.GET)
    @ApiOperation(
            value = "Cancel endpoint for PayPal",
            notes = "User gets redirected here after payment failure or denial",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 303, message = "Redirects to homepage")
    })
    public ResponseEntity handleCancel(
            HttpServletRequest request,
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) {

        // Redirects to main page

        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.setLocation(ServletUriComponentsBuilder.fromHttpUrl(strapRootURL(request.getRequestURL().toString()))
                .build().toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.SEE_OTHER);
    }


    private String strapRootURL(String url) {
        // /\/orders(.*)/g
        return url.replaceFirst("/orders(.*)", "");
    }

    private static boolean archivedOrderFilter(Order order) {
        return !order.getStatus().equals(OrderStatus.SUBMITTED);
    }

}