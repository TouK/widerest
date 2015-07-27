package pl.touk.widerest.paypal.gateway;

import com.paypal.api.payments.*;
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
            //TODO: unhardcode credentials
            String clientIdCredential = "AQkquBDf1zctJOWGKWUEtKXm6qVhueUEMvXO_-MCI4DQQ4-LWvkDLIN2fGsd",
                    secretCredential = "EL1tVxAjhT7cJimnz5-Nsx9k2reTKSVfErNQF-CmrwJgxRtylkGTKlU4RvrX";
            payPalSession =
                    new PayPalSessionImpl(clientIdCredential, secretCredential);


            List<Transaction> transactions = new ArrayList<Transaction>();
            Transaction transaction = null;
            Amount amount = null;

            for( LineItemDTO item : payPalRequest.getWrapped().getLineItems()) {
                transaction = new Transaction();
                amount = new Amount();

                amount.setCurrency(payPalRequest.getOrderCurrencyCode());
                amount.setTotal("14.90");

                transaction.setDescription(item.getName());
                transaction.setAmount(amount);
                transactions.add(transaction);
            }

            AddressDTO<PaymentRequestDTO> billingAddressDTO = payPalRequest.getWrapped().getBillTo();
            AddressDTO<PaymentRequestDTO> shippingAddressDTO = payPalRequest.getWrapped().getShipTo();
            Address billingAddress = new Address();
            ShippingAddress shippingAddress = new ShippingAddress();
            PayerInfo payerInfo = new PayerInfo();

            billingAddress = billingAddress.setCity(billingAddressDTO.getAddressCityLocality());
            billingAddress = billingAddress.setCountryCode(billingAddressDTO.getAddressCountryCode());
            billingAddress = billingAddress.setLine1(billingAddressDTO.getAddressLine1());
            billingAddress = billingAddress.setLine2(billingAddressDTO.getAddressLine2());
            billingAddress = billingAddress.setPostalCode(billingAddressDTO.getAddressPostalCode());
            billingAddress = billingAddress.setPhone(billingAddressDTO.getAddressPhone());
            billingAddress = billingAddress.setState(billingAddressDTO.getAddressStateRegion());

            shippingAddress.setCity(shippingAddressDTO.getAddressCityLocality());
            shippingAddress.setCountryCode(shippingAddressDTO.getAddressCountryCode());
            shippingAddress.setLine1(shippingAddressDTO.getAddressLine1());
            shippingAddress.setLine2(shippingAddressDTO.getAddressLine2());
            shippingAddress.setPostalCode(shippingAddressDTO.getAddressPostalCode());
            shippingAddress.setPhone(shippingAddressDTO.getAddressPhone());
            shippingAddress.setState(shippingAddressDTO.getAddressStateRegion());

            payerInfo.setBillingAddress(billingAddress);
            payerInfo.setShippingAddress(shippingAddress);
            payerInfo.setFirstName(shippingAddressDTO.getAddressFirstName());
            payerInfo.setLastName(shippingAddressDTO.getAddressLastName());

            Payer payer = new Payer();
            payer.setPaymentMethod("paypal");
            payer.setPayerInfo(payerInfo);

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

            return response;

        } catch (Exception e) {
            //if (e instanceof PaymentException) throw e;
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
