package pl.touk.widerest.paypal.gateway;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;
import org.broadleafcommerce.common.config.dao.SystemPropertiesDao;
import org.broadleafcommerce.common.config.domain.SystemProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class PayPalSessionImpl implements PayPalSession {

    private APIContext apiContext;
    private OAuthTokenCredential oAuthTokenCredential;

    private Map<String, String> sdkConfig;

    @Resource(name = "blSystemPropertiesDao")
    protected SystemPropertiesDao systemPropertiesDao;

    @Resource
    protected Set<String> availableSystemPropertyNames;

    @PostConstruct
    public void init() {
        Collections.addAll(availableSystemPropertyNames, CLIENT_ID, SECRET);
    }

    // Should be replaced so that it uses refresh token instead

    //@Value("${paypal.clientId}")
    private String clientId;// = "AQkquBDf1zctJOWGKWUEtKXm6qVhueUEMvXO_-MCI4DQQ4-LWvkDLIN2fGsd";

    //@Value("${paypal.secret}")
    private String secret;// = "EL1tVxAjhT7cJimnz5-Nsx9k2reTKSVfErNQF-CmrwJgxRtylkGTKlU4RvrX";

    private void setCredentialsFromSysPropertiesOrSetSandbox() {
        clientId = Optional.ofNullable(systemPropertiesDao.readSystemPropertyByName(CLIENT_ID))
                .map(SystemProperty::getValue)
                .orElse("EBWKjlELKMYqRNQ6sYvFo64FtaRLRR5BdHEESmha49TM");

        secret = Optional.ofNullable(systemPropertiesDao.readSystemPropertyByName(SECRET))
                .map(SystemProperty::getValue)
                .orElse("EO422dn3gQLgDbuwqTjzrFgFtaRLRR5BdHEESmha49TM");

    }

    public void initConnection() throws PayPalRESTException {
        // If exception is thrown then the app shouldnt start

        sdkConfig = new HashMap<>();
        sdkConfig.put("mode", "sandbox");

        setCredentialsFromSysPropertiesOrSetSandbox();
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
        if(apiContext == null) {
            initConnection();
        } else {
            refreshWithTheSameToken();
        }

        return apiContext;
    }

    public void refreshWithTheSameToken() throws PayPalRESTException {
        apiContext = new APIContext(oAuthTokenCredential.getAccessToken());
        apiContext.setConfigurationMap(sdkConfig);
    }

    public void createNewApiContextFromToken(String token) {
        apiContext = new APIContext(token);
        apiContext.setConfigurationMap(sdkConfig);
    }
}