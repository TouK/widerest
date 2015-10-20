package pl.touk.widerest.security.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

/* Creates client details for any id issued by the application */
public class ValidIdClientDetailsService implements ClientDetailsService {

    @Autowired(required = false)
    Supplier<String> resourceIdSupplier;

    @Autowired(required = false)
    Consumer<String> clientIdValidator;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        if (clientIdValidator != null) {
            clientIdValidator.accept(clientId);
        }
        BaseClientDetails clientDetails = new BaseClientDetails();
        clientDetails.setClientId(clientId);
        clientDetails.setAuthorizedGrantTypes(Arrays.asList("password", "implicit", "authorization_code"));
        clientDetails.setScope(Arrays.asList("site", "backoffice"));
        clientDetails.setAutoApproveScopes(clientDetails.getScope());
        if (resourceIdSupplier != null)
            clientDetails.setResourceIds(Arrays.asList(resourceIdSupplier.get()));
        return clientDetails;

    }
}
