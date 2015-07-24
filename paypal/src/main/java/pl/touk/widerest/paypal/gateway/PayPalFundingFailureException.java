package pl.touk.widerest.paypal.gateway;

import lombok.Getter;
import lombok.Setter;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;

import java.net.URI;

public class PayPalFundingFailureException extends PaymentException {

    /**
     * https://developer.paypal.com/docs/classic/api/errorcodes/
     */
    public static final String ERROR_CODE_10417 = "10417";
    public static final String ERROR_CODE_10486 = "10486";

    @Getter
    @Setter
    private URI redirectUrl;

    public PayPalFundingFailureException() {}

    public PayPalFundingFailureException(URI redirectUrl) {
        this.redirectUrl = redirectUrl;
    }


}
