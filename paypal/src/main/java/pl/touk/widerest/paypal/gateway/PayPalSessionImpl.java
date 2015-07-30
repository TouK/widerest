package pl.touk.widerest.paypal.gateway;

import com.paypal.api.openidconnect.CreateFromAuthorizationCodeParameters;
import com.paypal.api.openidconnect.CreateFromRefreshTokenParameters;
import com.paypal.api.openidconnect.Tokeninfo;
import com.paypal.base.exception.OAuthException;
import com.paypal.base.exception.PayPalException;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PayPalSessionImpl implements PayPalSession {

    private APIContext apiContext;
    private OAuthTokenCredential oAuthTokenCredential;

    private Map<String, String> sdkConfig;

    // Should be replaced so that it uses refresh token instead

    //@Value("${paypal.clientId:AQkquBDf1zctJ}")
    private String clientId = "AQkquBDf1zctJOWGKWUEtKXm6qVhueUEMvXO_-MCI4DQQ4-LWvkDLIN2fGsd";

    //@Value("${paypal.secret:EL1tVxAjhT7cJi}")
    private String secret = "EL1tVxAjhT7cJimnz5-Nsx9k2reTKSVfErNQF-CmrwJgxRtylkGTKlU4RvrX";

    public PayPalSessionImpl() throws PayPalRESTException {
        // If exception is thrown then the app shouldnt start

        sdkConfig = new HashMap<String, String>();
        sdkConfig.put("mode", "sandbox");

        oAuthTokenCredential = new OAuthTokenCredential(clientId, secret, sdkConfig);
        apiContext = new APIContext(oAuthTokenCredential.getAccessToken());
        apiContext.setConfigurationMap(sdkConfig);

    }

    public APIContext getApiContext() throws PayPalRESTException {

//      expiresIn() returns seconds
        /*if(oAuthTokenCredential.expiresIn() < 60) {
            oAuthTokenCredential = new OAuthTokenCredential(clientId, secret, sdkConfig);
            apiContext = new APIContext(oAuthTokenCredential.getAccessToken());
            apiContext.setConfigurationMap(sdkConfig);
        }*/

        return apiContext;
    }

    public void createNewApiContextFromToken(String token) {
        apiContext = new APIContext(token);
        apiContext.setConfigurationMap(sdkConfig);
    }
}