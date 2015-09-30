package pl.touk.widerest.paypal.gateway;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;

public interface PayPalSession {

    String CLIENT_ID = "paypalClientId";
    String SECRET = "paypalSecret";

    APIContext getApiContext() throws PayPalRESTException;
    void createNewApiContextFromToken(String token);
}
