package pl.touk.widerest.paypal.gateway;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import lombok.Getter;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
import org.broadleafcommerce.common.payment.dto.LineItemDTO;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;

public class PayPalRequestDto {

    @Getter
    private PaymentRequestDTO wrapped;

    public final static String SHIPPING_COST = "SHIPCOST";

    PayPalRequestDto(PaymentRequestDTO wrapped) {
        this.wrapped = wrapped;
    }

    public PayPalRequestDto() {
        this.wrapped = new PaymentRequestDTO();
    }

    public PayPalRequestDto(String token) {
        this.wrapped = new PaymentRequestDTO();
        this.setAccessToken(token);
    }

    public void setAccessToken(String accessToken) {
        wrapped = wrapped.additionalField(PayPalMessageConstants.ACCESS_TOKEN, accessToken);
    }

    public String getAccessToken() {
        return String.valueOf(wrapped.getAdditionalFields().get(PayPalMessageConstants.ACCESS_TOKEN));
    }

    public String getPayerId() {
        return String.valueOf(wrapped.getAdditionalFields().get(PayPalMessageConstants.PAYER_ID));
    }

    public void setOrderId(String id) {
        wrapped.orderId(id);
    }

    public void setPayerId(String id) {
        wrapped = wrapped.additionalField(PayPalMessageConstants.PAYER_ID, id);
    }

    public String getOrderCurrencyCode() {
        return wrapped.getOrderCurrencyCode();
    }

    public String getTransactionTotal() {
        return wrapped.getTransactionTotal();
    }

    public String getOrderSubtotal() {
        return wrapped.getOrderSubtotal();
    }

    public String getTaxTotal() {
        return wrapped.getTaxTotal();
    }

    public void setTaxTotal(String taxTotal) {
        wrapped.taxTotal(taxTotal);
    }

    public String getReturnUri() {
        return wrapped.getAdditionalFields().get(PayPalMessageConstants.RETURN_URL).toString();
    }

    public String getCancelUri() {
        return wrapped.getAdditionalFields().get(PayPalMessageConstants.CANCEL_URL).toString();
    }

    public void setPaymentId(String id) {
        wrapped = wrapped.additionalField(PayPalMessageConstants.PAYMENT_ID, id);
    }

    public String getPaymentId() {
        return wrapped.getAdditionalFields().get(PayPalMessageConstants.PAYMENT_ID).toString();
    }

    public void setShippingTotal(String cost) {
        wrapped = wrapped.additionalField(SHIPPING_COST, cost);
    }

    public String getShippingTotal() {
        return wrapped.getAdditionalFields().get(SHIPPING_COST).toString();
    }

    public Object getOrderId() {
        return wrapped.getOrderId();
    }

    public List<LineItemDTO> getLineItems() {
        return wrapped.getLineItems();
    }

    public Iterable<LineItemDTO> getSubscribtions() {
        return Iterables.filter(wrapped.getLineItems(), new Predicate<LineItemDTO>() {
            @Override
            public boolean apply(@Nullable LineItemDTO input) {
                return input.getAdditionalFields().get(PayPalMessageConstants.SUBSCRIPTION) != null;
            }
        });
    }

    public String getOrderDescription() {
        return wrapped.getOrderDescription();
    }

    public String getBillingAgreementId() {
        return String.valueOf(wrapped.getAdditionalFields().get(PayPalMessageConstants.BILLING_AGREEMENT_ID));
    }
}
