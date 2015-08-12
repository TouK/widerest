package pl.touk.widerest.paypal.endpoint;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.*;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.checkout.service.CheckoutService;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.paypal.exception.FulfillmentOptionNotSetException;
import pl.touk.widerest.paypal.gateway.PayPalMessageConstants;
import pl.touk.widerest.paypal.gateway.PayPalPaymentGatewayType;
import pl.touk.widerest.paypal.gateway.PayPalRequestDto;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;

@Controller
@ResponseBody
@RequestMapping("/orders/{id}/paypal")
@Api(value = "paypal", description = "PayPal payment endpoint")
public class PayPalController {

    @Resource(name = "blOrderToPaymentRequestDTOService")
    private OrderToPaymentRequestDTOService orderToPaymentRequestDTOService;

    @Resource(name = "blPaymentGatewayConfigurationServiceProvider")
    private PaymentGatewayConfigurationServiceProvider paymentGatewayConfigurationServiceProvider;

    @Resource(name = "blOrderService")
    private OrderService orderService;

    @Resource(name="blCheckoutService")
    protected CheckoutService checkoutService;

    @Resource(name = "blFulfillmentGroupService")
    private FulfillmentGroupService fulfillmentGroupService;

    @Resource(name = "blPaymentGatewayCheckoutService")
    private PaymentGatewayCheckoutService paymentGatewayCheckoutService;

    private PaymentGatewayConfigurationService configurationService;
    private PaymentGatewayHostedService hostedService;
    private PaymentGatewayWebResponseService webResponseService;
    private PaymentGatewayTransactionConfirmationService transactionConfirmationService;

    @PostConstruct
    public void afterPropertiesSet() {
        configurationService = paymentGatewayConfigurationServiceProvider.getGatewayConfigurationService(PayPalPaymentGatewayType.PAYPAL);
        hostedService = configurationService.getHostedService();
        webResponseService = configurationService.getWebResponseService();
        transactionConfirmationService = configurationService.getTransactionConfirmationService();
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "Initiate order payment execution using PayPal",
            notes = "Initiates for one chosen order",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 303, message = "Redirects to PayPal checkout website"),
            @ApiResponse(code = 403, message = "Access denied to given order")
            // and throws
    })
    public ResponseEntity initiate(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) throws PaymentException {

        if(userDetails instanceof AdminUserDetails) {
            throw new PaymentException("Admins can't make orders");
        }
        CustomerUserDetails customerUserDetails = (CustomerUserDetails)userDetails;

        // find order
        Order order = Optional.ofNullable(orderService.findOrderById(orderId))
                .filter(PayPalController::archivedOrderFilter)
                .orElseThrow(() -> new ResourceNotFoundException(""));

        if(!order.getCustomer().getId().equals(customerUserDetails.getId())) {
            throw new IllegalAccessError("Access Denied");
        }

        if(fulfillmentGroupService.getFirstShippableFulfillmentGroup(order).getFulfillmentOption()
            == null) {
            throw new FulfillmentOptionNotSetException("");
        }

        String SELF_URL = strapRootURL(request.getRequestURL().toString()) + "/orders/"+orderId+"/paypal";

        String returnUrl = SELF_URL+"/return?"
                +PayPalMessageConstants.QUERY_AMOUNT+"="+order.getTotal().toString();
        String cancelUrl = SELF_URL+"/cancel?"+PayPalMessageConstants.QUERY_ORDER_ID+"="+orderId;


        // Assuming the order has items in one currency, just get one and get currency
        if(order.getCurrency() == null) {
            order.setCurrency(
                Optional.ofNullable(
                    Optional.ofNullable(order.getDiscreteOrderItems().get(0))
                            .orElseThrow(() -> new ResourceNotFoundException(""))
                            .getSku().getCurrency()
                ).orElse(order.getLocale().getDefaultCurrency())
            );
        }


        PaymentRequestDTO paymentRequestDTO =
                orderToPaymentRequestDTOService.translateOrder(order)
                        .additionalField(PayPalMessageConstants.RETURN_URL, returnUrl)
                        .additionalField(PayPalMessageConstants.CANCEL_URL, cancelUrl)
                        .additionalField(PayPalRequestDto.SHIPPING_COST, order.getTotalFulfillmentCharges())
                        .orderDescription("TODO");

        paymentRequestDTO = populateLineItemsAndSubscriptions(order, paymentRequestDTO);

        PaymentResponseDTO paymentResponse = hostedService.requestHostedEndpoint(paymentRequestDTO);

        //return redirect URI from the paymentResponse

        String redirectURI = Optional.ofNullable(paymentResponse.getResponseMap().get(PayPalMessageConstants.REDIRECT_URL))
                .orElseThrow(() -> new ResourceNotFoundException(""));

        HttpHeaders responseHeader = new HttpHeaders();


        responseHeader.setLocation(URI.create(redirectURI));
        return new ResponseEntity<>(responseHeader, HttpStatus.SEE_OTHER);
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
            @AuthenticationPrincipal UserDetails userDetails,
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
        PaymentResponseDTO payPalResponse = webResponseService.translateWebResponse(request);

        if(!payPalResponse.isValid()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }


        // create orderpayment
        paymentGatewayCheckoutService.applyPaymentToOrder(payPalResponse, configurationService.getConfiguration());


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
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) {

        // Redirects to main page

        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.setLocation(ServletUriComponentsBuilder.fromHttpUrl(strapRootURL(request.getRequestURL().toString()))
                .build().toUri());

        return new ResponseEntity<>(responseHeader, HttpStatus.SEE_OTHER);
    }


    private PaymentRequestDTO populateLineItemsAndSubscriptions(Order order, PaymentRequestDTO paymentRequest) {
        for (OrderItem item : order.getOrderItems()) {
            String name = null;

            /* (mst) Previously, there was SKU's Description used here to set item's name
                    but because it is not required in our implementation, I chose to use SKU's Name instead */

            if (item instanceof BundleOrderItem) {
                name = ((BundleOrderItem) item).getSku().getName();
            } else if (item instanceof DiscreteOrderItem) {
                name = ((DiscreteOrderItem) item).getSku().getName();
            } else {
                name = item.getName();
            }

            String category = item.getCategory() == null ? null : item.getCategory().getName();
            paymentRequest = paymentRequest
                    .lineItem()
                    .name(name)
                    .amount(String.valueOf(item.getAveragePrice())) // TODO: blad przyblizen przy mnozeniu
                    .category(category)                             // podczas liczenia kwot w paypalu
                    .quantity(String.valueOf(item.getQuantity()))
                    .done();
        }


        return paymentRequest;
    }

    private String strapRootURL(String url) {
        // /\/orders(.*)/g
        return url.replaceFirst("/orders(.*)", "");
    }

    private static boolean archivedOrderFilter(Order order) {
        return !order.getStatus().equals(OrderStatus.SUBMITTED);
    }
    
}