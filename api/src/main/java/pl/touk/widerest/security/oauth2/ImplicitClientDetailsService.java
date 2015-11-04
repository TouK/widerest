package pl.touk.widerest.security.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/* Creates client details for any id issued by the application */
@Service
public class ImplicitClientDetailsService implements ClientDetailsService {

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
        clientDetails.setScope(
                Arrays.asList(Scope.values()).stream().map(Scope::toString).collect(Collectors.toList())
        );
        clientDetails.setAutoApproveScopes(clientDetails.getScope());
        if (resourceIdSupplier != null)
            clientDetails.setResourceIds(Arrays.asList(resourceIdSupplier.get()));
        return clientDetails;

    }
}
