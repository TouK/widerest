package pl.touk.widerest.paypal.gateway;

import com.paypal.base.rest.APIContext;

public class PayPalSessionImpl implements PayPalSession {

    private APIContext apiContext;

    public PayPalSessionImpl(String token) {
        apiContext = new APIContext(token);
    }

    public APIContext getApiContext() {
        return apiContext;
    }
}
