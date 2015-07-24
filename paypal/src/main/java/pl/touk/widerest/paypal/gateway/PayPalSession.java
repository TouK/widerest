package pl.touk.widerest.paypal.gateway;

import com.paypal.base.rest.APIContext;

public interface PayPalSession {
    APIContext getApiContext();
}
