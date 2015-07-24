package pl.touk.widerest.paypal.gateway;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
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
            throw new NotImplementedException("Should create com.paypal.api.payments.Payment");

        } catch (Exception e) {
            if (e instanceof PaymentException) throw e;
            throw new PaymentException(e);
        }
    }

    protected PayPalResponseDto translatePayPalWebResponse(HttpServletRequest request) throws PaymentException {
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(request.getRequestURI()).build().getQueryParams();
        String token = queryParams.getFirst(PayPalMessageConstants.HTTP_TOKEN);
        String orderId = queryParams.getFirst(PayPalMessageConstants.QUERY_ORDER_ID);

        PayPalRequestDto payPalRequest = new PayPalRequestDto(token);

        PayPalResponseDto payPalResponse = findDetailsByPayPalTransaction(payPalRequest);
        payPalResponse.setOrderId(orderId);
        payPalResponse.setPaymentTransactionType(PaymentTransactionType.UNCONFIRMED);

        return payPalResponse;
    }

    protected PayPalResponseDto findDetailsByPayPalTransaction(PayPalRequestDto paymentRequest) throws PaymentException {
        try {
            throw new NotImplementedException("Should call Payment.get()");

        } catch (Exception e) {
            if (e instanceof PaymentException) throw e;
            throw new PaymentException(e);
        }
    }

    protected PayPalResponseDto confirmPayPalTransaction(PayPalRequestDto payPalRequest) throws PaymentException {
        Money transactionAmount = new Money(payPalRequest.getTransactionTotal(), payPalRequest.getOrderCurrencyCode());
        try {
            throw new NotImplementedException("Should call execute on the payment");

        } catch (Exception e) {
            if (e instanceof PaymentException) throw e;
            throw new PaymentException(e);
        }
    }


}
