package pl.touk.widerest.paypal.gateway;

import com.paypal.api.payments.*;
import com.paypal.base.rest.PayPalRESTException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
import org.broadleafcommerce.common.payment.dto.AddressDTO;
import org.broadleafcommerce.common.payment.dto.LineItemDTO;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayHostedService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayReportingService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayTransactionConfirmationService;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponseService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PayPalGatewayService implements PaymentGatewayHostedService, PaymentGatewayTransactionConfirmationService, PaymentGatewayWebResponseService, PaymentGatewayReportingService
/*extends AbstractExternalPaymentGatewayCall<PayPalRequest, PayPalResponse>*/ {

    @Resource
    private PayPalSession payPalSession;

    @Override
    public PaymentResponseDTO requestHostedEndpoint(PaymentRequestDTO paymentRequest) throws PaymentException {
        return requestPayPalHostedEndpoint(new PayPalRequestDto(paymentRequest)).getWrapped();
    }

    @Override
    public PaymentResponseDTO translateWebResponse(HttpServletRequest request) throws PaymentException {
        return translatePayPalWebResponse(request).getWrapped();
    }

    @Override
    public PaymentResponseDTO findDetailsByTransaction(PaymentRequestDTO paymentRequest) throws PaymentException {
        return findDetailsByPayPalTransaction(new PayPalRequestDto(paymentRequest)).getWrapped();
    }

    @Override
    public PaymentResponseDTO confirmTransaction(PaymentRequestDTO paymentRequest) throws PaymentException {
        return confirmPayPalTransaction(new PayPalRequestDto(paymentRequest)).getWrapped();
    }

    protected PayPalResponseDto requestPayPalHostedEndpoint(PayPalRequestDto payPalRequest) throws PaymentException {
        log.info("Creating PayPal payment for order {} for amount of {}", payPalRequest.getOrderId(), payPalRequest.getTransactionTotal());
        try {
            List<Transaction> transactions = new ArrayList<>();


            Transaction transaction = new Transaction();
            ItemList itemList = new ItemList();
            List<Item> utilListItem = new ArrayList<>();
            Item temp = null;
            for(LineItemDTO item : payPalRequest.getWrapped().getLineItems()) {
                temp = new Item();
                //temp.setName(item.getName());
                temp.setName("tmp!");
                temp.setQuantity(item.getQuantity());
                temp.setPrice(item.getAmount());
                temp.setCurrency(payPalRequest.getOrderCurrencyCode());
                utilListItem.add(temp);
            }


            Details details = new Details();
            details.setShipping(payPalRequest.getShippingTotal());
            details.setSubtotal(payPalRequest.getOrderSubtotal());
            details.setTax(payPalRequest.getTaxTotal());

            Amount amount = new Amount();
            amount.setCurrency(payPalRequest.getOrderCurrencyCode());
            amount.setTotal(payPalRequest.getTransactionTotal());
            amount.setDetails(details);


            itemList.setItems(utilListItem);

            transaction.setDescription("Examplary Transaction Description");
            transaction.setAmount(amount);
            transaction.setItemList(itemList);
            transactions.add(transaction);


            Payer payer = new Payer();
            payer.setPaymentMethod("paypal");

            Payment payment = new Payment();
            payment.setIntent("sale");


            payment.setPayer(payer);
            payment.setTransactions(transactions);


            RedirectUrls redirectUrls = new RedirectUrls();
            redirectUrls.setCancelUrl(payPalRequest.getCancelUri());
            redirectUrls.setReturnUrl(payPalRequest.getReturnUri());

            payment.setRedirectUrls(redirectUrls);

            Payment createdPayment = payment.create(payPalSession.getApiContext());

            String redirect = createdPayment.getLinks().stream()
                    .filter(x -> x.getRel().equalsIgnoreCase("approval_url"))
                    .findAny()
                    .map(Links::getHref)
                    .orElseThrow(() -> new ResourceNotFoundException(""));

            PayPalResponseDto response = new PayPalResponseDto();
            response.setRedirectUri(redirect);
            response.setAmount(new Money(payPalRequest.getTransactionTotal()));
            response.setPaymentTransactionType(PaymentTransactionType.SETTLED);

            return response;

        } catch (Exception e) {
            //if (e instanceof PaymentException) throw e;
            throw new PaymentException(e);
        }
    }

    protected PayPalResponseDto translatePayPalWebResponse(HttpServletRequest request) throws PaymentException {
        String token = request.getParameter(PayPalMessageConstants.QUERY_TOKEN);
        String payerId = request.getParameter(PayPalMessageConstants.QUERY_PAYER_ID);
        String paymentId = request.getParameter(PayPalMessageConstants.QUERY_PAYMENT_ID);
//        String amount = request.getParameter(PayPalMessageConstants.QUERY_AMOUNT);
        String orderId = request.getAttribute(PayPalMessageConstants.QUERY_ORDER_ID).toString();

        PayPalRequestDto payPalRequest = new PayPalRequestDto(token);

        PayPalResponseDto payPalResponse = findDetailsByPayPalTransaction(payPalRequest);
        payPalResponse.setOrderId(orderId);
        //payPalResponse.setAmount(new Money(amount));
        payPalResponse.setPaymentTransactionType(PaymentTransactionType.UNCONFIRMED);
        payPalResponse.setPayerId(payerId);
        payPalResponse.setPaymentId(paymentId);

        return payPalResponse;
    }

    protected PayPalResponseDto findDetailsByPayPalTransaction(PayPalRequestDto paymentRequest) throws PaymentException {
        PayPalResponseDto response = null;
        try {
            //throw new NotImplementedException("Should call Payment.get()");
            //TODO:
            response = new PayPalResponseDto();
            response.setAccessToken(paymentRequest.getAccessToken());

        } catch (Exception e) {
            throw new PaymentException(e);
        }
        return response;
    }

    protected PayPalResponseDto confirmPayPalTransaction(PayPalRequestDto payPalRequest) throws PaymentException {
        Money transactionAmount = new Money(payPalRequest.getTransactionTotal(), payPalRequest.getOrderCurrencyCode());
        PayPalResponseDto responseDto = new PayPalResponseDto();

        try {
            //throw new NotImplementedException("Should call execute on the payment");
           // Generate new with the same credentials or leave commented?
           //payPalSession.createNewApiContextFromToken(payPalRequest.getAccessToken());
           Payment payment = new Payment();
           payment.setId(payPalRequest.getPaymentId());
           PaymentExecution paymentExecute = new PaymentExecution();
           paymentExecute.setPayerId(payPalRequest.getPayerId());
           payment.execute(payPalSession.getApiContext(), paymentExecute);
            responseDto.setAmount(new Money(payPalRequest.getTransactionTotal()));
            responseDto.setPaymentTransactionType(PaymentTransactionType.AUTHORIZE_AND_CAPTURE);
            return responseDto;

        } catch (Exception e) {
            if(e instanceof PayPalRESTException) {
                throw new PaymentException(((PayPalRESTException) e).getDetails().getMessage(), e);
            }
            throw new PaymentException(e);
        }
    }


}
