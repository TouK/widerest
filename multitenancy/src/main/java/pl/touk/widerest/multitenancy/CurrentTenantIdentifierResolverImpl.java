package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

@Slf4j
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(attributes -> attributes.getAttribute(MultiTenancyConfig.TENANT_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .map(Tenant.class::cast)
                .map(Tenant::getId)
                .orElse(MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER)
        ;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
