package pl.touk.widerest.paypal.gateway;

import lombok.extern.slf4j.Slf4j;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;

@Slf4j
public class PayPalResponseDto {

    private PaymentResponseDTO wrapped = new PaymentResponseDTO(PaymentType.THIRD_PARTY_ACCOUNT, PayPalPaymentGatewayType.PAYPAL);
    private String paymentId;

    public PayPalResponseDto() {}

    public PaymentResponseDTO getWrapped() {
        return wrapped;
    }

    public void setPaymentId(String id) {
        paymentId = id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setAmount(Money money) {
        wrapped.amount(money);
    }

    public void setOrderId(String orderId) {
        wrapped.orderId(orderId);
    }

    public void setPayerId(String payerId) {
        wrapped.responseMap(PayPalMessageConstants.PAYER_ID, payerId);
    }

    public void setPaymentTransactionType(PaymentTransactionType paymentTransactionType) {
        wrapped.paymentTransactionType(paymentTransactionType);
    }

    public String getTransactionId() {
        return wrapped.getResponseMap().get(PayPalMessageConstants.TRANSACTION_ID);
    }

    public void setSuccessful(boolean b) {
        wrapped.successful(b);
    }

    public void setRedirectUri(String href) {
        wrapped.responseMap(PayPalMessageConstants.REDIRECT_URL, href);
    }


    public void setTransactionId(String paymentId) {
        wrapped.responseMap(PayPalMessageConstants.TRANSACTION_ID, paymentId);
    }

    public void setAccessToken(String token) {
        wrapped.responseMap(PayPalMessageConstants.ACCESS_TOKEN, token);
    }

    public String getAccessToken() {
        return wrapped.getResponseMap().get(PayPalMessageConstants.ACCESS_TOKEN);
    }

    public void setBillingAgreementId(String billingAgreementId) {
        wrapped.responseMap(PayPalMessageConstants.BILLING_AGREEMENT_ID, billingAgreementId);
    }
}
