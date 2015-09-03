package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

@Slf4j
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    public static final String TENANT_ATTRIBUTE = "TENANT";

    @Override
    public String resolveCurrentTenantIdentifier() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(attributes -> attributes.getAttribute(TENANT_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .map(Tenant.class::cast)
                .map(Tenant::getId)
                .orElse("")
        ;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
