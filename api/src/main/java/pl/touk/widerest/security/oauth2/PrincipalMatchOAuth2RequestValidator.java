package pl.touk.widerest.security.oauth2;

import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestValidator;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Set;

@Service
public class PrincipalMatchOAuth2RequestValidator extends DefaultOAuth2RequestValidator {

    @Resource
    private CustomerService customerService;

    @Override
    public void validateScope(AuthorizationRequest authorizationRequest, ClientDetails client) throws InvalidScopeException {
        super.validateScope(authorizationRequest, client);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        forceNewAuthenticationIfPrincipalIsNotValidForScope(principal, authorizationRequest.getScope());
    }

    protected void forceNewAuthenticationIfPrincipalIsNotValidForScope(User principal, Set<String> scopeSet) {
        for(String scope : scopeSet) {
            if (Scope.CUSTOMER.matches(scope)) {
                if ( !(principal instanceof CustomerUserDetails) ) {
                    throw new InsufficientAuthenticationException("Not logged in as a customer");
                }
                Customer customer = customerService.readCustomerById(((CustomerUserDetails) principal).getId());
                if (Scope.CUSTOMER_REGISTERED.matches(scope)) {
                    if (!customer.isRegistered()) {
                        throw new InsufficientAuthenticationException("Not logged in as a registered customer");
                    }
                }

            } else if (Scope.STAFF.matches(scope) && ( !(principal instanceof AdminUserDetails) )) {
                throw new InsufficientAuthenticationException("Not logged in as an admin user");
            }
        }
    }

    @Override
    public void validateScope(TokenRequest tokenRequest, ClientDetails client) throws InvalidScopeException {
        super.validateScope(tokenRequest, client);
    }
}
