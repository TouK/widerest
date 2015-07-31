package pl.touk.widerest.paypal.endpoint;

import com.paypal.api.payments.PaymentExecution;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrency;
import org.broadleafcommerce.common.currency.domain.BroadleafCurrencyImpl;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.*;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.checkout.service.CheckoutService;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.checkout.service.workflow.CheckoutResponse;
import org.broadleafcommerce.core.order.domain.BundleOrderItem;
import org.broadleafcommerce.core.order.domain.DiscreteOrderItem;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOService;
import org.broadleafcommerce.core.web.api.BroadleafWebServicesException;
import org.broadleafcommerce.core.web.api.wrapper.OrderWrapper;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.touk.widerest.paypal.gateway.PayPalMessageConstants;
import pl.touk.widerest.paypal.gateway.PayPalPaymentGatewayType;
import pl.touk.widerest.paypal.gateway.PayPalRequestDto;
import pl.touk.widerest.paypal.gateway.PayPalResponseDto;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
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

    @Resource(name="blCheckoutService")
    protected CheckoutService checkoutService;

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


    @RequestMapping(method = RequestMethod.GET)
    @Transactional
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
                .orElseThrow(() -> new ResourceNotFoundException(""));

        if(!order.getCustomer().getId().equals(customerUserDetails.getId())) {
            throw new IllegalAccessError("Access Denied");
        }

        String SELF_URL = strapRootURL(request.getRequestURL().toString()) + "/orders/"+orderId+"/paypal";

        String returnUrl = SELF_URL+"/return";
        String cancelUrl = SELF_URL+"/cancel";

        BroadleafCurrency bCurrency = new BroadleafCurrencyImpl();
        bCurrency.setCurrencyCode("USD");

        order.setCurrency(bCurrency);

        //PaymentRequestDTO paymentRequestDTO = orderToPaymentRequestDTOService.translateOrder(order);


        PaymentRequestDTO paymentRequestDTO =
                orderToPaymentRequestDTOService.translateOrder(order)
                        .additionalField(PayPalMessageConstants.RETURN_URL, returnUrl)
                        .additionalField(PayPalMessageConstants.CANCEL_URL, cancelUrl)
                        .orderDescription("TODO");

        paymentRequestDTO = populateLineItemsAndSubscriptions(order, paymentRequestDTO);

        PaymentResponseDTO paymentResponse = hostedService.requestHostedEndpoint(paymentRequestDTO);

        //return redirect URI from the paymentResponse

        String redirectURI = Optional.ofNullable(paymentResponse.getResponseMap().get(PayPalMessageConstants.REDIRECT_URL))
                .orElseThrow(() -> new ResourceNotFoundException(""));

        HttpHeaders responseHeader = new HttpHeaders();

        //responseHeader.setLocation(ServletUriComponentsBuilder.fromHttpUrl(redirectURI)
        //                .build().toUri());

        responseHeader.setLocation(URI.create(redirectURI));
        //return new ResponseEntity<>(responseHeader, HttpStatus.MULTIPLE_CHOICES);

        return new ResponseEntity<>(responseHeader, HttpStatus.OK);
    }

    @RequestMapping(value = "/return", method = RequestMethod.GET)
    public ResponseEntity handleReturn(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) throws PaymentException {


        // call checkout workflow
        // handle no funds failures

        if(userDetails instanceof AdminUserDetails) {
            throw new PaymentException("Admins can't make orders");
        }
        CustomerUserDetails customerUserDetails = (CustomerUserDetails)userDetails;

        Order order = Optional.ofNullable(orderService.findOrderById(orderId))
                .orElseThrow(() -> new ResourceNotFoundException(""));
        if(!order.getCustomer().getId().equals(customerUserDetails.getId())) {
            throw new IllegalAccessError("Access Denied");
        }

        // get data from link
        PaymentResponseDTO payPalResponse = webResponseService.translateWebResponse(request);

        // create request

        // strange if it ever happens
        if(payPalResponse.getOrderId() != null && Long.valueOf(payPalResponse.getOrderId()) != order.getId()) {
            throw new IllegalAccessError("Wrong request");
        }

        PayPalRequestDto requestDTO = new PayPalRequestDto();
        requestDTO.setOrderId(orderId.toString());
        requestDTO.setPayerId(payPalResponse.getResponseMap().get(PayPalMessageConstants.PAYER_ID));
        requestDTO.setPaymentId(payPalResponse.getResponseMap().get(PayPalMessageConstants.PAYMENT_ID));
        requestDTO.setAccessToken(payPalResponse.getResponseMap().get(PayPalMessageConstants.ACCESS_TOKEN));
        requestDTO.getWrapped().transactionTotal(order.getTotal().toString());
        requestDTO.getWrapped().orderCurrencyCode(order.getCurrency().getCurrencyCode());
        //TODO: czy to jest potrzebne by pamietac? (PaymentTransactionType)
        //requestDTO.setPaymentTransactionType(payPalResponse.getPaymentTransactionType());

//        // execute payment
//        payPalResponse = transactionConfirmationService.confirmTransaction(requestDTO.getWrapped());
//
//        HttpHeaders responseHeader = new HttpHeaders();
//
//
//        // if there was a problem with execution
//        String url = payPalResponse.getResponseMap().get(PayPalMessageConstants.REDIRECT_URL);
//        if(url != null) {
//            responseHeader.setLocation(ServletUriComponentsBuilder.fromHttpUrl(url)
//                    .build().toUri());
//            return new ResponseEntity<>(null, responseHeader, HttpStatus.MULTIPLE_CHOICES);
//        }

        // Checkout/execute order in broadleaf

        try {
            //CheckoutResponse checkoutResponse =
            checkoutService.performCheckout(order);
        } catch (CheckoutException e) {
            throw BroadleafWebServicesException.build(HttpStatus.NOT_FOUND.value())
                    .addMessage(BroadleafWebServicesException.CART_NOT_FOUND);
        }


        // After success - redirect to main page
        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.setLocation(ServletUriComponentsBuilder.fromHttpUrl(strapRootURL(request.getRequestURL().toString()))
                .build().toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.MULTIPLE_CHOICES);
    }

    @RequestMapping(value = "/cancel", method = RequestMethod.GET)
    public ResponseEntity handleCancel(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "id") Long orderId) {

        // Redirects to main page

        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.setLocation(ServletUriComponentsBuilder.fromHttpUrl(strapRootURL(request.getRequestURL().toString()))
                .build().toUri());

        return new ResponseEntity<>(null, responseHeader, HttpStatus.MULTIPLE_CHOICES);

    }


    private PaymentRequestDTO populateLineItemsAndSubscriptions(Order order, PaymentRequestDTO paymentRequest) {
        for (OrderItem item : order.getOrderItems()) {
            String name = item.getName();
            /*
            if (item instanceof BundleOrderItem) {
                name = ((BundleOrderItem) item).getSku().getDescription();
            } else if (item instanceof DiscreteOrderItem) {
                name = ((DiscreteOrderItem) item).getSku().getDescription();
            } else {
                name = item.getName();
            }*/
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
    
}